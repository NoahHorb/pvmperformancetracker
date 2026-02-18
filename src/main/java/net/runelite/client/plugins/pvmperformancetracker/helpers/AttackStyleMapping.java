package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.Data;

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
 *    after the animation fires (all melee). These are stored in scheduledDirectHitDelay.
 *    Projectile-based attacks (ranged/magic) are tracked separately via projectile events
 *    because their travel time depends on distance.
 *
 * 2. MECHANICS — animations that spawn delayed hazards (e.g. Vardorvis axes) where
 *    the damage can land anywhere from 1–N ticks later. These are NOT stored as
 *    pending direct attacks. Instead, a "mechanic active" flag is set so that any
 *    hitsplat that does NOT land on the NPC's expected direct-hit tick can be
 *    attributed to the mechanic.
 */
@Data
public class AttackStyleMapping
{
    // NPC ID -> (Animation ID -> Attack Style)  [direct attacks only]
    private static final Map<Integer, Map<Integer, String>> animationMappings = new HashMap<>();

    // NPC ID -> (Animation ID -> hitsplat delay in ticks from animation)
    // For melee this is always 1. Stored separately so NpcAttackTracker can
    // schedule the exact expected landing tick without hardcoding.
    private static final Map<Integer, Map<Integer, Integer>> animationDelays = new HashMap<>();

    // NPC ID -> (Projectile ID -> Attack Style)
    private static final Map<Integer, Map<Integer, String>> projectileMappings = new HashMap<>();

    // NPC ID -> Set of animation IDs that are MECHANICS, not direct attacks.
    // Mechanic damage can land on any tick after the animation, so we cannot
    // schedule a landing tick. Instead NpcAttackTracker sets a mechanicActive flag.
    private static final Map<Integer, Set<Integer>> mechanicAnimations = new HashMap<>();

    // NPC ID -> style string for mechanic animations (what to call the hit when it lands)
    private static final Map<Integer, Map<Integer, String>> mechanicStyles = new HashMap<>();

    // -----------------------------------------------------------------------
    // Registration API
    // -----------------------------------------------------------------------

    /**
     * Register a direct-attack animation.
     * @param npcId      NPC id
     * @param animId     Animation id
     * @param style      Attack style string (e.g. "slash", "magic")
     * @param delayTicks Ticks from animation to hitsplat (1 for melee)
     */
    public static void registerDirectAttack(int npcId, int animId, String style, int delayTicks)
    {
        animationMappings.computeIfAbsent(npcId, k -> new HashMap<>()).put(animId, style);
        animationDelays.computeIfAbsent(npcId, k -> new HashMap<>()).put(animId, delayTicks);
    }

    /**
     * Register a mechanic animation.
     * Mechanic damage can land on any tick, so it is NOT scheduled as a direct hit.
     * @param npcId      NPC id
     * @param animId     Animation id of the mechanic spawn
     * @param style      Style string used when classifying mechanic damage (e.g. "axes")
     */
    public static void registerMechanic(int npcId, int animId, String style)
    {
        mechanicAnimations.computeIfAbsent(npcId, k -> new HashSet<>()).add(animId);
        mechanicStyles.computeIfAbsent(npcId, k -> new HashMap<>()).put(animId, style);
    }

    /**
     * Register a projectile mapping for a specific NPC.
     */
    public static void registerProjectile(int npcId, int projectileId, String attackStyle)
    {
        projectileMappings.computeIfAbsent(npcId, k -> new HashMap<>())
                .put(projectileId, attackStyle);
    }

    // -----------------------------------------------------------------------
    // Query API
    // -----------------------------------------------------------------------

    /** Returns true if this animation is a registered mechanic (not a direct attack). */
    public static boolean isMechanicAnimation(int npcId, int animId)
    {
        Set<Integer> set = mechanicAnimations.get(npcId);
        return set != null && set.contains(animId);
    }

    /** Returns true if this animation is a registered direct attack. */
    public static boolean isDirectAttackAnimation(int npcId, int animId)
    {
        Map<Integer, String> map = animationMappings.get(npcId);
        return map != null && map.containsKey(animId);
    }

    /** Returns the attack style for a direct-attack animation, or null. */
    public static String getStyleFromAnimation(int npcId, int animId)
    {
        Map<Integer, String> map = animationMappings.get(npcId);
        return map != null ? map.get(animId) : null;
    }

    /**
     * Returns the hitsplat delay (in ticks from animation) for a direct attack, or -1 if unknown.
     * For melee this is 1. This value is used by NpcAttackTracker to compute the exact
     * tick on which the hitsplat should appear.
     */
    public static int getDirectAttackDelay(int npcId, int animId)
    {
        Map<Integer, Integer> map = animationDelays.get(npcId);
        if (map == null)
        {
            return -1;
        }
        Integer delay = map.get(animId);
        return delay != null ? delay : -1;
    }

    /** Returns the style string associated with a mechanic animation, or null. */
    public static String getMechanicStyle(int npcId, int animId)
    {
        Map<Integer, String> map = mechanicStyles.get(npcId);
        return map != null ? map.get(animId) : null;
    }

    /** Returns the attack style from a projectile ID, or null. */
    public static String getStyleFromProjectile(int npcId, int projectileId)
    {
        Map<Integer, String> map = projectileMappings.get(npcId);
        return map != null ? map.get(projectileId) : null;
    }

    /** Returns true if there are any mappings (direct or projectile) for this NPC. */
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
     * Initialise mappings for all supported bosses.
     * Call once at plugin startup.
     * TODO - add all NPCs with  > 1 different attack styles and/or mechanics.
     */
    public static void initializeCommonBossMappings()
    {
        // -------------------------------------------------------------------
        // Vardorvis (12223, 12224)
        // 6-tick attack cycle. Melee hitsplat lands exactly 1 tick after anim.
        // -------------------------------------------------------------------
        for (int id : new int[]{12223, 12224})
        {
            // Direct slash attacks — hitsplat 1 tick after animation
            registerDirectAttack(id, 10340, "slash", 1);
            registerDirectAttack(id, 10342, "slash", 1);

            // Axes mechanic — 10341 spawns axes that travel the room.
            // Damage can land on any tick 1–9 after spawn; not schedulable.
            registerMechanic(id, 10341, "axes");
        }

        // -------------------------------------------------------------------
        // Vorkath (8059, 8061)
        // Melee = 1-tick delay; projectile attacks tracked via projectile events.
        // -------------------------------------------------------------------
        for (int id : new int[]{8059, 8061})
        {
            registerDirectAttack(id, 7948, "slash", 1);  // Melee swipe

            // All ranged/magic/dragonfire attacks are projectile-based;
            // their landing tick depends on player distance so we use projectile tracking.
            registerProjectile(id, 1477, "ranged");
            registerProjectile(id, 1479, "magic");
            registerProjectile(id, 1481, "dragonfire");
        }

        // -------------------------------------------------------------------
        // Zulrah (2042 Serpentine, 2043 Magma, 2044 Tanzanite)
        // All attacks are projectile-based.
        // -------------------------------------------------------------------
        registerProjectile(2042, 1044, "ranged");

        registerProjectile(2043, 1046, "typeless");

        registerProjectile(2044, 1044, "ranged");
        registerProjectile(2044, 1046, "magic");

        // -------------------------------------------------------------------
        // TzTok-Jad (2745)
        // -------------------------------------------------------------------
        registerProjectile(2745, 448, "magic");
        registerProjectile(2745, 451, "ranged");
        // Melee swipe — close range, 1-tick delay
        registerDirectAttack(2745, 2656, "melee", 1);
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