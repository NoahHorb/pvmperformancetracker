package net.runelite.client.plugins.pvmperformancetracker.models;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps NPC animation and projectile IDs to attack styles
 * Used to determine which attack style (and thus max hit) an NPC used
 */
@Data
public class AttackStyleMapping
{
    // NPC ID -> (Animation ID -> Attack Style)
    private static final Map<Integer, Map<Integer, String>> animationMappings = new HashMap<>();

    // NPC ID -> (Projectile ID -> Attack Style)
    private static final Map<Integer, Map<Integer, String>> projectileMappings = new HashMap<>();

    /**
     * Register an animation mapping for a specific NPC
     */
    public static void registerAnimation(int npcId, int animationId, String attackStyle)
    {
        animationMappings.computeIfAbsent(npcId, k -> new HashMap<>())
                .put(animationId, attackStyle);
    }

    /**
     * Register a projectile mapping for a specific NPC
     */
    public static void registerProjectile(int npcId, int projectileId, String attackStyle)
    {
        projectileMappings.computeIfAbsent(npcId, k -> new HashMap<>())
                .put(projectileId, attackStyle);
    }

    /**
     * Get attack style from animation ID for a specific NPC
     * Returns null if no mapping found
     */
    public static String getStyleFromAnimation(int npcId, int animationId)
    {
        Map<Integer, String> npcAnimations = animationMappings.get(npcId);
        if (npcAnimations == null)
        {
            return null;
        }

        return npcAnimations.get(animationId);
    }

    /**
     * Get attack style from projectile ID for a specific NPC
     * Returns null if no mapping found
     */
    public static String getStyleFromProjectile(int npcId, int projectileId)
    {
        Map<Integer, String> npcProjectiles = projectileMappings.get(npcId);
        if (npcProjectiles == null)
        {
            return null;
        }

        return npcProjectiles.get(projectileId);
    }

    /**
     * Check if we have any mappings for this NPC
     */
    public static boolean hasMappings(int npcId)
    {
        return animationMappings.containsKey(npcId) || projectileMappings.containsKey(npcId);
    }

    /**
     * Initialize mappings for common bosses
     * This should be called once during plugin startup
     */
    public static void initializeCommonBossMappings()
    {
        // Vardorvis (12223)
        registerAnimation(12223, 10340, "melee"); // Standard melee attack
        registerAnimation(12223, 10341, "melee"); // Axe swing
        registerAnimation(12223, 10342, "melee"); // Another melee variant
        // Note: Vardorvis has special mechanics, add more as discovered

        // Vorkath (8059, 8061)
        registerAnimation(8059, 7948, "slash"); // Melee attack
        registerAnimation(8059, 7952, "ranged"); // Ranged attack
        registerAnimation(8059, 7960, "magic"); // Venom attack
        registerAnimation(8059, 7957, "dragonfire"); // Pink dragonfire
        registerAnimation(8059, 7961, "dragonfire_bomb"); // Spawn dragonfire bomb

        // Same for alternate Vorkath ID
        registerAnimation(8061, 7948, "slash");
        registerAnimation(8061, 7952, "ranged");
        registerAnimation(8061, 7960, "magic");
        registerAnimation(8061, 7957, "dragonfire");
        registerAnimation(8061, 7961, "dragonfire_bomb");

        // Projectile mappings for Vorkath
        registerProjectile(8059, 1477, "ranged"); // Regular ranged
        registerProjectile(8059, 1479, "magic"); // Venom pool
        registerProjectile(8059, 1481, "dragonfire"); // Pink dragonfire

        registerProjectile(8061, 1477, "ranged");
        registerProjectile(8061, 1479, "magic");
        registerProjectile(8061, 1481, "dragonfire");

        // Zulrah phases
        // Serpentine (2042) - Ranged only
        registerAnimation(2042, 5069, "ranged");
        registerProjectile(2042, 1044, "ranged");

        // Magma (2043) - Typeless
        registerAnimation(2043, 5069, "typeless");
        registerProjectile(2043, 1046, "typeless");

        // Tanzanite (2044) - Magic and Ranged
        registerAnimation(2044, 5069, "magic"); // Default to magic
        registerProjectile(2044, 1044, "ranged");
        registerProjectile(2044, 1046, "magic");

        // TzTok-Jad (2745)
        registerAnimation(2745, 2656, "magic"); // Mage attack
        registerAnimation(2745, 2652, "ranged"); // Range attack
        registerProjectile(2745, 448, "magic");
        registerProjectile(2745, 451, "ranged");

        // Add more boss mappings here as needed
    }

    /**
     * Clear all mappings (useful for testing)
     */
    public static void clearAllMappings()
    {
        animationMappings.clear();
        projectileMappings.clear();
    }
}