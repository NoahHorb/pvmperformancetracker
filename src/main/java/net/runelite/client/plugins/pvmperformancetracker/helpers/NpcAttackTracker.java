package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Tracks NPC attacks and determines which attack style caused a hitsplat on the player.
 *
 * Two parallel tracking systems:
 *
 * DIRECT HITS — when a direct-attack animation fires, we compute the exact tick the
 *   hitsplat should land (animationTick + delay, where delay == 1 for all melee).
 *   When handleDamageToPlayer fires, we check if the current tick matches any
 *   scheduled direct hit. If it does, we consume that slot and return its style.
 *   Exact tick match only — no tolerance window.
 *
 * MECHANICS — when a mechanic animation fires (e.g. Vardorvis axes 10341) we set
 *   a flag "mechanic is active" for that NPC. If a hitsplat lands on a tick that
 *   does NOT match any scheduled direct hit, and a mechanic is active, the hit is
 *   attributed to the mechanic.
 *
 * Collision case (both land same tick): OSRS raises one HitsplatApplied per source,
 *   so two events arrive. The first consumes the scheduled direct-hit slot; the
 *   second finds no direct-hit slot and falls through to the mechanic — correct.
 *
 * Projectile-based attacks (ranged/magic) produce a ScheduledProjectileHit instead of
 *   a direct-attack entry, since their landing tick depends on distance. Those are
 *   scheduled when the projectile is first seen and resolved the same way.
 */
@Slf4j
public class NpcAttackTracker
{
    // Scheduled hitsplat from a melee/direct animation: lands on exactTick
    private static class ScheduledDirectHit
    {
        final int npcIndex;
        final String style;
        final int exactTick;  // animTick + delay

        ScheduledDirectHit(int npcIndex, String style, int exactTick)
        {
            this.npcIndex = npcIndex;
            this.style = style;
            this.exactTick = exactTick;
        }
    }

    // Scheduled hitsplat from a projectile: lands on estimatedTick (distance-based)
    private static class ScheduledProjectileHit
    {
        final int npcIndex;
        final String style;
        final int estimatedTick;

        ScheduledProjectileHit(int npcIndex, String style, int estimatedTick)
        {
            this.npcIndex = npcIndex;
            this.style = style;
            this.estimatedTick = estimatedTick;
        }
    }

    // Active mechanic for a given NPC index
    private static class ActiveMechanic
    {
        final int npcIndex;
        final String style;        // e.g. "axes"
        final int spawnTick;       // tick when the mechanic animation fired
        final int expiryTick;      // mechanic hazard disappears after this tick

        ActiveMechanic(int npcIndex, String style, int spawnTick, int expiryTick)
        {
            this.npcIndex = npcIndex;
            this.style = style;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
        }
    }

    // How long (in ticks) a mechanic hazard stays live after it spawns.
    // Vardorvis axes: active for ~9 ticks. Use 10 as a safe ceiling.
    private static final int DEFAULT_MECHANIC_LIFESPAN_TICKS = 10;

    private final List<ScheduledDirectHit> scheduledDirectHits = new ArrayList<>();
    private final List<ScheduledProjectileHit> scheduledProjectileHits = new ArrayList<>();
    private final List<ActiveMechanic> activeMechanics = new ArrayList<>();

    // Last confirmed style per NPC index — used as a last-resort fallback
    @Getter
    private final Map<Integer, String> lastConfirmedAttackStyle = new HashMap<>();

    // Current game tick — updated every tick via onGameTick()
    private int currentTick = 0;

    // -----------------------------------------------------------------------
    // Tick update
    // -----------------------------------------------------------------------

    /**
     * Call every game tick from PvMPerformanceTrackerPlugin.onGameTick().
     * Expires stale entries and advances internal clock.
     */
    public void onGameTick(int tick)
    {
        this.currentTick = tick;
        expireStaleEntries();
    }

    /** Remove entries that can no longer be triggered. */
    private void expireStaleEntries()
    {
        // Direct hits can only land on their exact tick; anything past it is dead.
        scheduledDirectHits.removeIf(h ->
        {
            if (currentTick > h.exactTick)
            {
                log.debug("[NpcAttackTracker] Expired stale direct hit: style={} expectedTick={} currentTick={}",
                        h.style, h.exactTick, currentTick);
                return true;
            }
            return false;
        });

        // Projectile hits: allow 1 tick of margin for server jitter, then expire.
        scheduledProjectileHits.removeIf(h ->
        {
            if (currentTick > h.estimatedTick + 1)
            {
                log.debug("[NpcAttackTracker] Expired stale projectile hit: style={} estimatedTick={} currentTick={}",
                        h.style, h.estimatedTick, currentTick);
                return true;
            }
            return false;
        });

        // Mechanics expire after their lifespan.
        activeMechanics.removeIf(m ->
        {
            if (currentTick > m.expiryTick)
            {
                log.debug("[NpcAttackTracker] Mechanic expired: style={} spawnTick={} expiryTick={}",
                        m.style, m.spawnTick, m.expiryTick);
                return true;
            }
            return false;
        });
    }

    // -----------------------------------------------------------------------
    // Recording animations / projectiles
    // -----------------------------------------------------------------------

    /**
     * Called when an NPC plays an animation.
     * Routes to direct-attack scheduling or mechanic flagging based on registry.
     */
    public void recordNpcAnimation(NPC npc, int animationId)
    {
        if (npc == null)
        {
            return;
        }

        int npcId    = npc.getId();
        int npcIndex = npc.getIndex();

        // ---- Mechanic animation ----
        if (AttackStyleMapping.isMechanicAnimation(npcId, animationId))
        {
            String style = AttackStyleMapping.getMechanicStyle(npcId, animationId);
            int expiryTick = currentTick + DEFAULT_MECHANIC_LIFESPAN_TICKS;

            activeMechanics.add(new ActiveMechanic(npcIndex, style, currentTick, expiryTick));

            log.debug("[NpcAttackTracker] Mechanic spawned: npc={} (index={}) anim={} style={} " +
                            "spawnTick={} expiryTick={}",
                    npc.getName(), npcIndex, animationId, style, currentTick, expiryTick);
            return;
        }

        // ---- Direct-attack animation ----
        if (AttackStyleMapping.isDirectAttackAnimation(npcId, animationId))
        {
            String style     = AttackStyleMapping.getStyleFromAnimation(npcId, animationId);
            int delay        = AttackStyleMapping.getDirectAttackDelay(npcId, animationId);
            int expectedTick = currentTick + delay;

            scheduledDirectHits.add(new ScheduledDirectHit(npcIndex, style, expectedTick));

            log.debug("[NpcAttackTracker] Direct hit scheduled: npc={} (index={}) anim={} style={} " +
                            "animTick={} delay={} expectedLandingTick={}",
                    npc.getName(), npcIndex, animationId, style, currentTick, delay, expectedTick);
            return;
        }

        log.debug("[NpcAttackTracker] Unmapped animation: npc={} (index={}) anim={}",
                npc.getName(), npcIndex, animationId);
    }

    /**
     * Called when a projectile from an NPC is detected.
     * Schedules a projectile hit using the projectile's remaining flight time.
     *
     * @param npc            The NPC that fired the projectile
     * @param projectileId   The projectile ID
     * @param flightTimeTicks Estimated ticks until the projectile lands
     *                        (caller should pass Projectile.getRemainingCycles() / 30 or similar)
     */
    public void recordNpcProjectile(NPC npc, int projectileId, int flightTimeTicks)
    {
        if (npc == null)
        {
            return;
        }

        int npcId    = npc.getId();
        int npcIndex = npc.getIndex();

        String style = AttackStyleMapping.getStyleFromProjectile(npcId, projectileId);
        if (style == null)
        {
            log.debug("[NpcAttackTracker] Unmapped projectile: npc={} (index={}) projectile={}",
                    npc.getName(), npcIndex, projectileId);
            return;
        }

        int estimatedLandingTick = currentTick + flightTimeTicks;
        scheduledProjectileHits.add(new ScheduledProjectileHit(npcIndex, style, estimatedLandingTick));

        log.debug("[NpcAttackTracker] Projectile hit scheduled: npc={} (index={}) projectile={} style={} " +
                        "currentTick={} flightTicks={} estimatedLandingTick={}",
                npc.getName(), npcIndex, projectileId, style, currentTick, flightTimeTicks, estimatedLandingTick);
    }

    // -----------------------------------------------------------------------
    // Resolution — called from HitsplatListener when player takes damage
    // -----------------------------------------------------------------------

    /**
     * Determine what attack style caused a hitsplat on the player at the current tick.
     *
     * Resolution order:
     *   1. Exact-tick match in scheduledDirectHits  → direct melee hit (consumed)
     *   2. Exact-tick match in scheduledProjectileHits (±1 tick tolerance) → projectile hit (consumed)
     *   3. Active mechanic for this NPC              → mechanic hit (mechanic stays active)
     *   4. lastConfirmedAttackStyle fallback
     *
     * @param npcIndex  The NPC's index (NPC.getIndex())
     * @return The determined attack style string, or null if completely unknown
     */
    public String resolveIncomingHit(int npcIndex)
    {
        // 1. Check exact-tick direct hit
        Iterator<ScheduledDirectHit> directIter = scheduledDirectHits.iterator();
        while (directIter.hasNext())
        {
            ScheduledDirectHit hit = directIter.next();
            if (hit.npcIndex == npcIndex && hit.exactTick == currentTick)
            {
                log.debug("[NpcAttackTracker] Resolved as DIRECT HIT: npcIndex={} style={} tick={}",
                        npcIndex, hit.style, currentTick);
                directIter.remove();
                lastConfirmedAttackStyle.put(npcIndex, hit.style);
                return hit.style;
            }
        }

        // 2. Check projectile hit (±1 tick for distance jitter)
        Iterator<ScheduledProjectileHit> projIter = scheduledProjectileHits.iterator();
        while (projIter.hasNext())
        {
            ScheduledProjectileHit hit = projIter.next();
            if (hit.npcIndex == npcIndex && Math.abs(currentTick - hit.estimatedTick) <= 1)
            {
                log.debug("[NpcAttackTracker] Resolved as PROJECTILE HIT: npcIndex={} style={} " +
                                "tick={} estimatedTick={}",
                        npcIndex, hit.style, currentTick, hit.estimatedTick);
                projIter.remove();
                lastConfirmedAttackStyle.put(npcIndex, hit.style);
                return hit.style;
            }
        }

        // 3. Check active mechanic
        for (ActiveMechanic mechanic : activeMechanics)
        {
            if (mechanic.npcIndex == npcIndex)
            {
                log.debug("[NpcAttackTracker] Resolved as MECHANIC HIT: npcIndex={} style={} " +
                                "tick={} mechanicSpawnTick={} expiryTick={}",
                        npcIndex, mechanic.style, currentTick, mechanic.spawnTick, mechanic.expiryTick);
                // Do NOT remove the mechanic — it can hit multiple times
                lastConfirmedAttackStyle.put(npcIndex, mechanic.style);
                return mechanic.style;
            }
        }

        // 4. Fallback
        String fallback = lastConfirmedAttackStyle.get(npcIndex);
        if (fallback != null)
        {
            log.debug("[NpcAttackTracker] Resolved via FALLBACK (lastConfirmed): npcIndex={} style={} tick={}",
                    npcIndex, fallback, currentTick);
        }
        else
        {
            log.debug("[NpcAttackTracker] Could not resolve hit: npcIndex={} tick={} " +
                            "directHits={} projectileHits={} activeMechanics={}",
                    npcIndex, currentTick, scheduledDirectHits.size(),
                    scheduledProjectileHits.size(), activeMechanics.size());
        }
        return fallback;
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /** Clear all tracking data (call on fight start/end). */
    public void clear()
    {
        scheduledDirectHits.clear();
        scheduledProjectileHits.clear();
        activeMechanics.clear();
        lastConfirmedAttackStyle.clear();
        log.debug("[NpcAttackTracker] Cleared all tracking data");
    }

    /** Clear tracking for a specific NPC index. */
    public void clearNpc(int npcIndex)
    {
        scheduledDirectHits.removeIf(h -> h.npcIndex == npcIndex);
        scheduledProjectileHits.removeIf(h -> h.npcIndex == npcIndex);
        activeMechanics.removeIf(m -> m.npcIndex == npcIndex);
        lastConfirmedAttackStyle.remove(npcIndex);
        log.debug("[NpcAttackTracker] Cleared tracking for npcIndex={}", npcIndex);
    }

    /** Diagnostic dump — useful during debugging sessions. */
    public void logCurrentState(int npcIndex)
    {
        long directCount = scheduledDirectHits.stream().filter(h -> h.npcIndex == npcIndex).count();
        long projCount   = scheduledProjectileHits.stream().filter(h -> h.npcIndex == npcIndex).count();
        long mechCount   = activeMechanics.stream().filter(m -> m.npcIndex == npcIndex).count();
        log.debug("[NpcAttackTracker] State for npcIndex={} tick={}: directHits={} projectiles={} mechanics={}",
                npcIndex, currentTick, directCount, projCount, mechCount);
    }
}