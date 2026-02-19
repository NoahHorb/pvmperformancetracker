package net.runelite.client.plugins.pvmperformancetracker.helpers;

import net.runelite.client.plugins.pvmperformancetracker.enums.eAnimation;
import net.runelite.client.plugins.pvmperformancetracker.enums.eNPC;
import net.runelite.client.plugins.pvmperformancetracker.enums.eProjectile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps NPC animation and projectile IDs to attack styles.
 *
 * Two categories of animations are tracked:
 *
 * 1. DIRECT ATTACKS — animations where the NPC's hitsplat lands exactly 1 tick
 *    after the animation fires (all melee). Stored in animationDelays.
 *    Projectile-based attacks use registerProjectile() instead because their
 *    travel time depends on distance at spawn time.
 *
 * 2. MECHANICS — animations that spawn delayed hazards (e.g. Vardorvis axes)
 *    where damage can land on any tick after the animation. These are NOT
 *    scheduled as direct hits. Instead, a "mechanic active" flag is set in
 *    NpcAttackTracker so any hitsplat that doesn't match a scheduled direct-hit
 *    tick can be attributed to the mechanic.
 *
 * To add a new boss:
 *   1. Add its NPC ID(s) to eNPC
 *   2. Add its animation ID(s) to eAnimation (confirm from logs first)
 *   3. Add its projectile ID(s) to eProjectile (confirm from logs first)
 *   4. Add a section in initializeCommonBossMappings() below
 */
public class AttackStyleMapping
{
    // NPC ID -> (Animation ID -> Attack Style)  [direct attacks only]
    private static final Map<Integer, Map<Integer, String>> animationMappings = new HashMap<>();

    // NPC ID -> (Animation ID -> hitsplat delay in ticks)
    private static final Map<Integer, Map<Integer, Integer>> animationDelays = new HashMap<>();

    // NPC ID -> (Projectile ID -> Attack Style)
    private static final Map<Integer, Map<Integer, String>> projectileMappings = new HashMap<>();

    // NPC ID -> Set of animation IDs that are MECHANICS (not direct attacks)
    private static final Map<Integer, Set<Integer>> mechanicAnimations = new HashMap<>();

    // NPC ID -> (Animation ID -> style string for mechanic hits)
    private static final Map<Integer, Map<Integer, String>> mechanicStyles = new HashMap<>();

    // -----------------------------------------------------------------------
    // Registration API
    // -----------------------------------------------------------------------

    /**
     * Register a direct-attack animation.
     * The hitsplat lands exactly {@code delayTicks} after the animation fires.
     * For all melee attacks this is 1.
     */
    public static void registerDirectAttack(int npcId, int animId, String style, int delayTicks)
    {
        animationMappings.computeIfAbsent(npcId, k -> new HashMap<>()).put(animId, style);
        animationDelays.computeIfAbsent(npcId, k -> new HashMap<>()).put(animId, delayTicks);
    }

    /**
     * Register a mechanic animation.
     * Mechanic damage can land on any tick, so it is NOT scheduled as a direct hit.
     * NpcAttackTracker will set an "active mechanic" flag instead.
     */
    public static void registerMechanic(int npcId, int animId, String style)
    {
        mechanicAnimations.computeIfAbsent(npcId, k -> new HashSet<>()).add(animId);
        mechanicStyles.computeIfAbsent(npcId, k -> new HashMap<>()).put(animId, style);
    }

    /**
     * Register a projectile mapping.
     * Landing tick is computed at spawn time from flight distance — see NpcAttackTracker.
     */
    public static void registerProjectile(int npcId, int projectileId, String attackStyle)
    {
        projectileMappings.computeIfAbsent(npcId, k -> new HashMap<>())
                .put(projectileId, attackStyle);
    }

    // -----------------------------------------------------------------------
    // Query API
    // -----------------------------------------------------------------------

    public static boolean isMechanicAnimation(int npcId, int animId)
    {
        Set<Integer> set = mechanicAnimations.get(npcId);
        return set != null && set.contains(animId);
    }

    public static boolean isDirectAttackAnimation(int npcId, int animId)
    {
        Map<Integer, String> map = animationMappings.get(npcId);
        return map != null && map.containsKey(animId);
    }

    public static String getStyleFromAnimation(int npcId, int animId)
    {
        Map<Integer, String> map = animationMappings.get(npcId);
        return map != null ? map.get(animId) : null;
    }

    public static int getDirectAttackDelay(int npcId, int animId)
    {
        Map<Integer, Integer> map = animationDelays.get(npcId);
        if (map == null) return -1;
        Integer delay = map.get(animId);
        return delay != null ? delay : -1;
    }

    public static String getMechanicStyle(int npcId, int animId)
    {
        Map<Integer, String> map = mechanicStyles.get(npcId);
        return map != null ? map.get(animId) : null;
    }

    public static String getStyleFromProjectile(int npcId, int projectileId)
    {
        Map<Integer, String> map = projectileMappings.get(npcId);
        return map != null ? map.get(projectileId) : null;
    }

    public static boolean hasMappings(int npcId)
    {
        return animationMappings.containsKey(npcId)
                || projectileMappings.containsKey(npcId)
                || mechanicAnimations.containsKey(npcId);
    }

    // -----------------------------------------------------------------------
    // Boss mappings
    // -----------------------------------------------------------------------

    /**
     * Register all boss attack mappings.
     * Call once at plugin startup from PvMPerformanceTrackerPlugin.startUp().
     *
     * Bosses with unconfirmed IDs are left commented out until verified from logs.
     * To enable a boss: confirm its IDs, remove the comment block, done.
     */
    public static void initializeCommonBossMappings()
    {
        // -------------------------------------------------------------------
        // Vardorvis
        // 6-tick attack cycle. Direct melee hitsplat lands 1 tick after anim.
        // Axes mechanic spawns travelling axes — damage non-schedulable (1-9t).
        // -------------------------------------------------------------------
        for (int id : eNPC.VARDORVIS)
        {
            registerDirectAttack(id, eAnimation.VARDORVIS_SLASH,     "slash", 1);
            registerDirectAttack(id, eAnimation.VARDORVIS_DASH,      "slash", 1);
            registerMechanic    (id, eAnimation.VARDORVIS_AXE_SPAWN, "axes");
        }

        // -------------------------------------------------------------------
        // Vorkath
        // Melee is animation-based (1-tick delay).
        // All other attacks are projectile-based — landing tick = flight time.
        // -------------------------------------------------------------------
        for (int id : eNPC.VORKATH_AWAKENED)
        {
            registerDirectAttack(id, eAnimation.VORKATH_MELEE,       "slash",      1);

            registerProjectile  (id, eProjectile.VORKATH_RANGED,     "ranged");
            registerProjectile  (id, eProjectile.VORKATH_MAGIC,      "magic");
            registerProjectile  (id, eProjectile.VORKATH_DRAGONFIRE, "dragonfire");
        }

        // -------------------------------------------------------------------
        // Zulrah
        // All attacks are projectile-based.
        // Each phase ID has distinct attack styles, handled per-ID below.
        //   2042 Serpentine — ranged only
        //   2043 Magma      — typeless (melee range, uses magic projectile ID)
        //   2044 Tanzanite  — magic + ranged
        // -------------------------------------------------------------------
        for (int id : eNPC.ZULRAH) {
            //registerDirectAttack();

            //registerMechanic();

            registerProjectile(id, eProjectile.ZULRAH_RANGED, "ranged");
            registerProjectile(id, eProjectile.ZULRAH_MAGIC, "typeless");
        }
        // -------------------------------------------------------------------
        // TzTok-Jad
        // Melee is animation-based. Ranged and magic are projectile-based.
        // -------------------------------------------------------------------
        for (int id : eNPC.TZTOK_JAD)
        {
            registerDirectAttack(id, eAnimation.JAD_MELEE,   "melee",  1);
            registerProjectile  (id, eProjectile.JAD_MAGIC,  "magic");
            registerProjectile  (id, eProjectile.JAD_RANGED, "ranged");
        }

        // -------------------------------------------------------------------
        // The Nightmare / Phosani's Nightmare
        // TODO: confirm all IDs from logs before enabling
        // -------------------------------------------------------------------
        // for (int id : eNPC.THE_NIGHTMARE)
        // {
        //     registerDirectAttack(id, eAnimation.NIGHTMARE_MELEE,   "melee",  1);
        //     registerProjectile  (id, eProjectile.NIGHTMARE_RANGED, "ranged");
        //     registerProjectile  (id, eProjectile.NIGHTMARE_MAGIC,  "magic");
        // }
        // for (int id : eNPC.PHOSANI_S_NIGHTMARE)
        // {
        //     registerDirectAttack(id, eAnimation.NIGHTMARE_MELEE,   "melee",  1);
        //     registerProjectile  (id, eProjectile.NIGHTMARE_RANGED, "ranged");
        //     registerProjectile  (id, eProjectile.NIGHTMARE_MAGIC,  "magic");
        // }

        // -------------------------------------------------------------------
        // Duke Sucellus
        // TODO: confirm IDs from logs before enabling
        // -------------------------------------------------------------------
        // for (int id : eNPC.DUKE_SUCELLUS)
        // {
        //     registerDirectAttack(id, eAnimation.DUKE_MELEE,        "melee",  1);
        //     registerMechanic    (id, eAnimation.DUKE_POISON_SPAWN,  "poison");
        // }

        // -------------------------------------------------------------------
        // The Leviathan
        // TODO: confirm IDs from logs before enabling
        // -------------------------------------------------------------------
        // for (int id : eNPC.THE_LEVIATHAN)
        // {
        //     registerProjectile(id, eProjectile.LEVIATHAN_RANGED,    "ranged");
        //     registerProjectile(id, eProjectile.LEVIATHAN_MAGIC,     "magic");
        //     registerMechanic  (id, eAnimation.LEVIATHAN_ROCK_SPAWN, "rocks");
        // }
    }

    /** Clear all mappings (for testing). */
    public static void clearAllMappings()
    {
        animationMappings.clear();
        animationDelays.clear();
        projectileMappings.clear();
        mechanicAnimations.clear();
        mechanicStyles.clear();
    }
}