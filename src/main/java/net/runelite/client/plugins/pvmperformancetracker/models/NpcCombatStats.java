package net.runelite.client.plugins.pvmperformancetracker.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * NPC combat stats from custom NPC database
 * Supports NPCs with multiple attack styles and varying max/min hits
 */
@Data
public class NpcCombatStats
{
    private String name;

    @SerializedName("baseName")
    private String baseName;

    private String phase;
    private String version;
    private String id; // Can be comma-separated list like "8059,8061"

    @SerializedName("combatLevel")
    private Integer combatLevel;

    @SerializedName("hitpoints")
    private Integer hitpoints;

    private Integer size;

    // Max and min hits per attack style
    @SerializedName("maxHit")
    private Map<String, Integer> maxHit;

    @SerializedName("minHit")
    private Map<String, Integer> minHit;

    @SerializedName("attackSpeed")
    private Integer attackSpeed;

    @SerializedName("attackStyle")
    private String attackStyle; // e.g., "[[Slash]], [[Magic]], [[Ranged]]"

    private Boolean aggressive;
    private Boolean poisonous;
    private String[] attributes;

    private Map<String, Boolean> immunities;

    // Combat levels
    @SerializedName("attackLevel")
    private Integer attackLevel;

    @SerializedName("strengthLevel")
    private Integer strengthLevel;

    @SerializedName("defenceLevel")
    private Integer defenceLevel;

    @SerializedName("magicLevel")
    private Integer magicLevel;

    @SerializedName("rangedLevel")
    private Integer rangedLevel;

    // Attack bonuses
    @SerializedName("attackBonus")
    private Integer attackBonus; // Generic melee attack bonus

    @SerializedName("strengthBonus")
    private Integer strengthBonus;

    @SerializedName("rangedAttackBonus")
    private Integer rangedAttackBonus;

    @SerializedName("rangedStrengthBonus")
    private Integer rangedStrengthBonus;

    @SerializedName("magicAttackBonus")
    private Integer magicAttackBonus;

    @SerializedName("magicStrengthBonus")
    private Integer magicStrengthBonus;

    // Defence bonuses
    @SerializedName("stabDefence")
    private Integer stabDefence;

    @SerializedName("slashDefence")
    private Integer slashDefence;

    @SerializedName("crushDefence")
    private Integer crushDefence;

    @SerializedName("magicDefence")
    private Integer magicDefence;

    @SerializedName("rangedDefence")
    private Integer rangedDefence;

    // Special ammo defences (optional)
    @SerializedName("lightAmmoDefence")
    private Integer lightAmmoDefence;

    @SerializedName("standardAmmoDefence")
    private Integer standardAmmoDefence;

    @SerializedName("heavyAmmoDefence")
    private Integer heavyAmmoDefence;

    // Elemental weakness (optional)
    @SerializedName("elementalWeakness")
    private ElementalWeakness elementalWeakness;

    @SerializedName("slayerLevel")
    private Integer slayerLevel;

    @SerializedName("slayerXp")
    private Integer slayerXp;

    @SerializedName("wikiPage")
    private String wikiPage;

    private String examine;

    /**
     * Get max hit for a specific attack style
     * Returns 0 if not found
     */
    public int getMaxHitForStyle(String style)
    {
        if (maxHit == null || style == null)
        {
            return 0;
        }

        String normalizedStyle = normalizeAttackStyle(style);
        return maxHit.getOrDefault(normalizedStyle, 0);
    }

    /**
     * Get min hit for a specific attack style
     * Returns 0 if not found
     */
    public int getMinHitForStyle(String style)
    {
        if (minHit == null || style == null)
        {
            return 0;
        }

        String normalizedStyle = normalizeAttackStyle(style);
        return minHit.getOrDefault(normalizedStyle, 0);
    }

    /**
     * Get the highest max hit across all attack styles
     */
    public int getHighestMaxHit()
    {
        if (maxHit == null || maxHit.isEmpty())
        {
            return 0;
        }

        return maxHit.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    /**
     * Get all available attack styles from maxHit map
     */
    public String[] getAvailableAttackStyles()
    {
        if (maxHit == null || maxHit.isEmpty())
        {
            return new String[0];
        }

        return maxHit.keySet().toArray(new String[0]);
    }

    /**
     * Check if NPC has multiple attack styles
     */
    public boolean hasMultipleAttackStyles()
    {
        return maxHit != null && maxHit.size() > 1;
    }

    /**
     * Get primary attack style (first one, or fallback priority)
     * Priority: melee > magic > ranged > other
     */
    public String getPrimaryAttackStyle()
    {
        if (maxHit == null || maxHit.isEmpty())
        {
            return "melee";
        }

        // Check for standard combat styles in priority order
        if (maxHit.containsKey("melee") || maxHit.containsKey("slash") ||
                maxHit.containsKey("stab") || maxHit.containsKey("crush"))
        {
            // Return the first melee variant found
            if (maxHit.containsKey("slash")) return "slash";
            if (maxHit.containsKey("stab")) return "stab";
            if (maxHit.containsKey("crush")) return "crush";
            if (maxHit.containsKey("melee")) return "melee";
        }

        if (maxHit.containsKey("magic")) return "magic";
        if (maxHit.containsKey("ranged")) return "ranged";

        // Return first available style as fallback
        return maxHit.keySet().iterator().next();
    }

    /**
     * Normalize attack style string for consistent lookups
     */
    private String normalizeAttackStyle(String style)
    {
        if (style == null)
        {
            return "melee";
        }

        return style.toLowerCase().trim();
    }

    /**
     * Get defensive bonus for given attack style
     */
    public int getDefenceBonus(String style)
    {
        if (style == null)
        {
            return 0;
        }

        String normalized = normalizeAttackStyle(style);

        switch (normalized)
        {
            case "stab":
                return stabDefence != null ? stabDefence : 0;
            case "slash":
                return slashDefence != null ? slashDefence : 0;
            case "crush":
            case "melee":
                return crushDefence != null ? crushDefence : 0;
            case "magic":
                return magicDefence != null ? magicDefence : 0;
            case "ranged":
                return rangedDefence != null ? rangedDefence : 0;
            default:
                return 0;
        }
    }

    /**
     * Get attack bonus for given attack style
     */
    public int getAttackBonus(String style)
    {
        if (style == null)
        {
            return 0;
        }

        String normalized = normalizeAttackStyle(style);

        switch (normalized)
        {
            case "stab":
            case "slash":
            case "crush":
            case "melee":
                return attackBonus != null ? attackBonus : 0;
            case "magic":
                return magicAttackBonus != null ? magicAttackBonus : 0;
            case "ranged":
                return rangedAttackBonus != null ? rangedAttackBonus : 0;
            default:
                return 0;
        }
    }

    /**
     * Get strength bonus for given attack style
     */
    public int getStrengthBonus(String style)
    {
        if (style == null)
        {
            return 0;
        }

        String normalized = normalizeAttackStyle(style);

        switch (normalized)
        {
            case "stab":
            case "slash":
            case "crush":
            case "melee":
                return strengthBonus != null ? strengthBonus : 0;
            case "magic":
                return magicStrengthBonus != null ? magicStrengthBonus : 0;
            case "ranged":
                return rangedStrengthBonus != null ? rangedStrengthBonus : 0;
            default:
                return 0;
        }
    }

    /**
     * Get attack level for given attack style
     */
    public int getAttackLevelForStyle(String style)
    {
        if (style == null)
        {
            return attackLevel != null ? attackLevel : 1;
        }

        String normalized = normalizeAttackStyle(style);

        switch (normalized)
        {
            case "stab":
            case "slash":
            case "crush":
            case "melee":
                return attackLevel != null ? attackLevel : 1;
            case "magic":
                return magicLevel != null ? magicLevel : 1;
            case "ranged":
                return rangedLevel != null ? rangedLevel : 1;
            default:
                return attackLevel != null ? attackLevel : 1;
        }
    }

    /**
     * Get defence level with default
     */
    public int getDefenceLevelOrDefault()
    {
        return defenceLevel != null ? defenceLevel : 1;
    }

    /**
     * Get attack level with default
     */
    public int getAttackLevelOrDefault()
    {
        return attackLevel != null ? attackLevel : 1;
    }

    /**
     * Get strength level with default
     */
    public int getStrengthLevelOrDefault()
    {
        return strengthLevel != null ? strengthLevel : 1;
    }

    /**
     * Inner class for elemental weakness
     */
    @Data
    public static class ElementalWeakness
    {
        private String type;
        private Integer percent;
    }
}