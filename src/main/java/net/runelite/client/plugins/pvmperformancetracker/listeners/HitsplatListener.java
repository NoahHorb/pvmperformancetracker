package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.enums.DamageType;
import net.runelite.client.plugins.pvmperformancetracker.helpers.CombatFormulas;
import net.runelite.client.plugins.pvmperformancetracker.helpers.FightTracker;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;
import net.runelite.client.plugins.pvmperformancetracker.models.NpcCombatStats;
import net.runelite.client.plugins.pvmperformancetracker.models.PlayerStats;
import net.runelite.client.plugins.pvmperformancetracker.party.PartyStatsManager;

@Slf4j
public class HitsplatListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    // Track the last NPC the local player attacked (for hitsplat attribution)
    private NPC lastAttackedNPC;

    public HitsplatListener(PvMPerformanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }

    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (!plugin.getConfig().enableTracking())
        {
            return;
        }

        Actor target = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        // Handle damage TO NPCs (offensive tracking)
        if (target instanceof NPC)
        {
            handleDamageToNPC((NPC) target, hitsplat);
        }

        // Handle damage TO players (defensive tracking - local player only)
        if (target instanceof Player && target.equals(client.getLocalPlayer()))
        {
            handleDamageToPlayer(hitsplat);
        }
    }

    /**
     * Handle damage dealt to NPCs.
     * Fight start is now handled by AnimationListener so expected damage is captured from attack #1.
     * HitsplatListener still handles: fight switching on target change, recording actual damage.
     */
    private void handleDamageToNPC(NPC npc, Hitsplat hitsplat)
    {
        // Check if we should track this NPC
        if (!shouldTrackNPC(npc))
        {
            log.debug("Skipping NPC: not tracked");
            return;
        }

        // Only track player damage hitsplats
        if (!isPlayerDamageHitsplat(hitsplat))
        {
            log.debug("Skipping hitsplat: not player damage type");
            return;
        }

        String npcName = npc.getName();
        int npcId = npc.getId();
        int damage = hitsplat.getAmount();

        String playerName = determineHitsplatSource(npc);
        if (playerName == null)
        {
            log.debug("Skipping hitsplat to {}: could not determine source (not party member or local player)", npcName);
            return;
        }
        log.debug("Processing hitsplat: {} damage to {} from {}", damage, npcName, playerName);

        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null)
        {
            return;
        }

        Fight currentFight = fightTracker.getCurrentFight();

        log.debug("Hitsplat check: currentFight={}, active={}, targetId={}, currentId={}",
                currentFight != null ? currentFight.getBossName() : "null",
                currentFight != null ? currentFight.isActive() : false,
                npcId,
                currentFight != null ? currentFight.getBossNpcId() : -1);

        if (currentFight == null || !currentFight.isActive())
        {
            // AnimationListener should have started the fight already.
            // If we still don't have one (e.g. animation wasn't detected), start it now as a fallback.
            log.debug("No active fight at hitsplat time — starting fight as fallback for {} (damage: {})", npcName, damage);
            fightTracker.startNewFight(npcName, npcId);
            currentFight = fightTracker.getCurrentFight();
        }
        else if (currentFight.getBossNpcId() != npcId)
        {
            // Player switched targets — end current fight and start a new one
            log.debug("Target changed — ending fight vs {} and starting new fight vs {}", currentFight.getBossName(), npcName);
            fightTracker.endCurrentFight();
            fightTracker.startNewFight(npcName, npcId);
            currentFight = fightTracker.getCurrentFight();
        }

        // Record the damage
        if (currentFight != null && currentFight.isActive())
        {
            fightTracker.addDamageDealt(playerName, damage, npcName);

            // Track this NPC for death detection
            if (plugin.getCombatEventListener() != null)
            {
                plugin.getCombatEventListener().recordDamageToNPC(npc);
            }

            log.debug("{} dealt {} damage to {} (fight: {})",
                    playerName, damage, npcName, currentFight.getBossName());
        }
        else
        {
            log.debug("NOT recording damage — fight is null or inactive after start attempt");
        }
    }
    /**
     * Handle damage taken by the local player.
     *
     * First filters to only tracked hitsplat types (DAMAGE_ME, BLOCK_ME, etc.),
     * then routes:
     *   - Known DoT types → recorded as UNAVOIDABLE, tracker skipped
     *   - Everything else → direct NPC attack, resolved through NpcAttackTracker
     */
    private void handleDamageToPlayer(Hitsplat hitsplat)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return;
        }

        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null || !fightTracker.hasActiveFight())
        {
            return;
        }

        Fight currentFight = fightTracker.getCurrentFight();
        if (currentFight == null)
        {
            return;
        }

        String playerName = localPlayer.getName();
        if (playerName == null)
        {
            return;
        }

        PlayerStats stats = currentFight.getPlayerStats().get(playerName);
        if (stats == null)
        {
            return;
        }

        // Only process hitsplat types we track (DAMAGE_ME, BLOCK_ME, coloured variants)
        // This is the same filter used in handleDamageToNPC — it covers both
        // the player's outgoing hits AND incoming hits from NPCs.
        if (!isPlayerDamageHitsplat(hitsplat))
        {
            log.debug("[HitsplatListener] Skipping untracked incoming hitsplat type={}",
                    hitsplat.getHitsplatType());
            return;
        }

        int hitsplatType = hitsplat.getHitsplatType();
        int damage       = hitsplat.getAmount();
        int currentTick  = client.getTickCount();

        log.debug("[HitsplatListener] Incoming player hitsplat: type={} damage={} tick={}",
                hitsplatType, damage, currentTick);

        // ---- DoT / environmental damage — bypass tracker ----
        // Poison, venom, burn, bleed, disease are not tied to NPC attack animations.
        // Sending them to NpcAttackTracker would incorrectly consume a scheduled
        // direct-hit slot or be misattributed to an active mechanic.
        if (isEnvironmentalOrDoTHitsplat(hitsplatType))
        {
            log.debug("[HitsplatListener] DoT/environmental hit (type={} damage={}) — UNAVOIDABLE, skipping tracker",
                    hitsplatType, damage);

            stats.addDamageTaken(damage, currentTick, DamageType.UNAVOIDABLE);

            // If this tick's damage meets or exceeds current HP it was a guaranteed kill
            int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
            if (damage >= currentHp)
            {
                stats.addDeathChance(1.0);
                log.debug("[HitsplatListener] DoT hit is lethal ({} dmg >= {} HP) — 100% death chance recorded",
                        damage, currentHp);
            }
            return;
        }

        // ---- Direct NPC attack — resolve style through tracker ----
        log.debug("[HitsplatListener] Direct NPC attack: type={} damage={} tick={}",
                hitsplatType, damage, currentTick);

        NPC attackingNpc = findAttackingNpc(currentFight.getBossNpcId());
        if (attackingNpc == null)
        {
            log.debug("[HitsplatListener] Could not find attacking NPC for bossId={}",
                    currentFight.getBossNpcId());
            return;
        }

        // Variant-aware stat lookup
        NpcCombatStats npcStats = null;
        if (plugin.getNpcStatsProvider() != null && plugin.getNpcStatsProvider().isLoaded())
        {
            npcStats = plugin.getNpcStatsProvider().getNpcStats(attackingNpc);
        }

        // Determine attack style
        String attackStyleUsed = null;
        if (npcStats != null)
        {
            if (npcStats.hasMultipleAttackStyles() && plugin.getNpcAttackTracker() != null)
            {
                int npcIndex = attackingNpc.getIndex();
                plugin.getNpcAttackTracker().logCurrentState(npcIndex);
                attackStyleUsed = plugin.getNpcAttackTracker().resolveIncomingHit(npcIndex);

                if (attackStyleUsed == null)
                {
                    attackStyleUsed = npcStats.getPrimaryAttackStyle();
                    log.debug("[HitsplatListener] Tracker returned null — falling back to primary: {}",
                            attackStyleUsed);
                }
            }
            else
            {
                attackStyleUsed = npcStats.getPrimaryAttackStyle();
            }
        }

        log.debug("[HitsplatListener] Resolved: damage={} style={} tick={} npc={}",
                damage, attackStyleUsed, currentTick, attackingNpc.getName());

        // Classify, record, and calculate death probability
        DamageType damageType = classifyDamage(attackingNpc, damage, attackStyleUsed, npcStats);
        stats.addDamageTaken(damage, currentTick, damageType);

        log.debug("[HitsplatListener] Player took {} damage (type={} style={} tick={})",
                damage, damageType, attackStyleUsed != null ? attackStyleUsed : "unknown", currentTick);

        if (npcStats != null && attackStyleUsed != null)
        {
            double deathProbability = calculateDeathProbability(attackingNpc, attackStyleUsed, npcStats);
            if (deathProbability > 0.0)
            {
                stats.addDeathChance(deathProbability);
                log.debug("[HitsplatListener] Death probability: {}% (rolled {})",
                        String.format("%.1f", deathProbability * 100), damage);
            }
        }
    }

    /**
     * Returns true for hitsplat types that are known environmental/DoT sources.

     * We also include the RuneLite named constants for poison/venom/disease/bleed
     * since those are stable and well-known. If a constant resolves to the same
     * integer as a named constant, Java de-duplicates them automatically.
     *
     * If you encounter a new DoT type showing as "Treating as direct NPC attack"
     * in the logs, add its type integer here.
     */
    private boolean isEnvironmentalOrDoTHitsplat(int type)
    {
        return type == HitsplatID.POISON
                || type == HitsplatID.VENOM
                || type == HitsplatID.DISEASE
                || type == HitsplatID.BURN
                || type == HitsplatID.BLEED;
    }

    /**
     * Find the NPC that is attacking the player
     */
    private NPC findAttackingNpc(int bossNpcId)
    {
        // Find the boss NPC in the world
        for (NPC npc : client.getNpcs())
        {
            if (npc != null && npc.getId() == bossNpcId)
            {
                return npc;
            }
        }
        return null;
    }

    /**
     * Classify damage type
     */
    private DamageType classifyDamage(NPC attackingNpc, int damage, String attackStyleUsed, NpcCombatStats npcStats)
    {
        if (attackingNpc == null || npcStats == null)
        {
            return DamageType.UNKNOWN;
        }

        // If attack style is unknown, try to get primary style as fallback
        if (attackStyleUsed == null || attackStyleUsed.isEmpty())
        {
            attackStyleUsed = npcStats.getPrimaryAttackStyle();
            log.debug("Using fallback attack style: {}", attackStyleUsed);
        }

        // Check if player had the CORRECT prayer active for this specific attack style
        boolean hadCorrectPrayer = isPrayerActiveForStyle(attackStyleUsed);

        // If prayer was active and still took damage, it's UNAVOIDABLE
        // (either typeless damage, or damage that goes through prayer)
        if (hadCorrectPrayer && damage > 0)
        {
            return DamageType.UNAVOIDABLE;
        }

        // If prayer wasn't active but should have been, it's PRAYABLE
        if (!hadCorrectPrayer && attackStyleUsed != null)
        {
            // Check if this attack style is prayable (melee/ranged/magic)
            String normalized = attackStyleUsed.toLowerCase();
            if (normalized.equals("melee") || normalized.equals("slash") ||
                    normalized.equals("stab") || normalized.equals("crush") ||
                    normalized.equals("ranged") || normalized.equals("magic"))
            {
                return DamageType.PRAYABLE;
            }
        }

        // Otherwise it's AVOIDABLE (could have been dodged by positioning/movement)
        return damage == 0 ? DamageType.AVOIDABLE : DamageType.UNAVOIDABLE;
    }


    /**
     * Check if player has correct prayer for the attack style
     */
    private boolean isPrayerActiveForStyle(String attackStyle)
    {
        if (attackStyle == null)
        {
            return false;
        }

        String normalized = attackStyle.toLowerCase();

        // Check melee prayers
        if (normalized.equals("melee") || normalized.equals("slash") ||
                normalized.equals("stab") || normalized.equals("crush"))
        {
            return client.isPrayerActive(Prayer.PROTECT_FROM_MELEE);
        }

        // Check ranged prayer
        if (normalized.equals("ranged"))
        {
            return client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES);
        }

        // Check magic prayer
        if (normalized.equals("magic"))
        {
            return client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
        }

        return false;
    }

    /**
     * Calculate death probability
     */
    private double calculateDeathProbability(NPC attackingNpc, String attackStyleUsed, NpcCombatStats npcStats)
    {

        double deathProb = 0.0;
        if (attackingNpc == null || npcStats == null)
        {
            return deathProb;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return deathProb;
        }

        // Get current HP
        int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);

        // Check if player has the CORRECT prayer active for this attack style
        boolean hasCorrectPrayer = isPrayerActiveForStyle(attackStyleUsed);

        // Calculate death probability using combat formulas
        CombatFormulas combatFormulas = plugin.getCombatFormulas();
        if (combatFormulas != null)
        {
            deathProb = combatFormulas.calculateDeathProbability(
                    currentHp, npcStats, attackStyleUsed, hasCorrectPrayer);
        }
        return deathProb;
    }

    /**
     * Check if we should track this NPC
     */
    private boolean shouldTrackNPC(NPC npc)
    {
        if (npc == null || npc.getName() == null)
        {
            return false;
        }

        if (plugin.getConfig().trackBossesOnly())
        {
            return plugin.getBossDetectionHelper() != null &&
                    plugin.getBossDetectionHelper().isBoss(npc);
        }

        return true;
    }

    /**
     * Check if hitsplat is player damage -> these are incoming or outgoing
     */
    private boolean isPlayerDamageHitsplat(Hitsplat hitsplat)
    {
        int type = hitsplat.getHitsplatType();

        return type == HitsplatID.DAMAGE_ME ||
                type == HitsplatID.DAMAGE_MAX_ME ||  // Max hit (bright red)
                type == HitsplatID.BLOCK_ME ||
                type == HitsplatID.DAMAGE_ME_CYAN ||
                type == HitsplatID.DAMAGE_ME_ORANGE ||
                type == HitsplatID.DAMAGE_ME_YELLOW ||
                type == HitsplatID.DAMAGE_ME_WHITE;
    }

    /**
     * Determine which player caused the hitsplat
     * ONLY RETURNS: local player OR party members (NOT random nearby players)
     */
    private String determineHitsplatSource(NPC target)
    {
        Player localPlayer = client.getLocalPlayer();

        // Check if local player is attacking this NPC
        if (localPlayer != null && localPlayer.getInteracting() == target)
        {
            lastAttackedNPC = target; // Track for fallback
            return localPlayer.getName();
        }

        // Fallback: if local player recently attacked this NPC (getInteracting can be null between attacks)
        if (localPlayer != null && target == lastAttackedNPC)
        {
            return localPlayer.getName();
        }

        // Additional fallback: check if local player is in combat stance and this is the nearest hostile NPC
        if (localPlayer != null && localPlayer.getAnimation() != -1)
        {
            Actor localTarget = localPlayer.getInteracting();
            // If player has no current target but is animating, this might be the target
            if (localTarget == null || localTarget == target)
            {
                lastAttackedNPC = target;
                return localPlayer.getName();
            }
        }

        // Check party members ONLY (if party tracking is enabled)
        PartyStatsManager partyManager = plugin.getPartyStatsManager();
        if (partyManager != null && partyManager.isPartyTrackingEnabled())
        {
            // Get party members who are nearby
            for (Player player : partyManager.getNearbyPartyMembers())
            {
                if (player.getInteracting() == target)
                {
                    return player.getName();
                }
            }
        }

        // NO FALLBACK to random players - if we can't confirm it's local player or party member, return null
        return null;
    }
}