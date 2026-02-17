package net.runelite.client.plugins.pvmperformancetracker.models;

import lombok.Data;

/**
 * Represents a single attack style for an NPC
 * Used to track animation/projectile mappings to specific attack types
 */
@Data
public class NpcAttackStyle
{
    private final String styleName; // e.g., "magic", "ranged", "slash", "dragonfire"
    private final int maxHit;
    private final int minHit;
    private final int attackBonus;
    private final int strengthBonus;
    private final int attackLevel;

    /**
     * Constructor with all parameters
     */
    public NpcAttackStyle(String styleName, int maxHit, int minHit,
                          int attackBonus, int strengthBonus, int attackLevel)
    {
        this.styleName = styleName;
        this.maxHit = maxHit;
        this.minHit = minHit;
        this.attackBonus = attackBonus;
        this.strengthBonus = strengthBonus;
        this.attackLevel = attackLevel;
    }

    /**
     * Simplified constructor for common cases
     */
    public NpcAttackStyle(String styleName, int maxHit, int minHit)
    {
        this(styleName, maxHit, minHit, 0, 0, 1);
    }

    /**
     * Check if this is a melee attack style
     */
    public boolean isMelee()
    {
        String lower = styleName.toLowerCase();
        return lower.equals("melee") || lower.equals("slash") ||
                lower.equals("stab") || lower.equals("crush");
    }

    /**
     * Check if this is a magic attack style
     */
    public boolean isMagic()
    {
        return styleName.toLowerCase().equals("magic");
    }

    /**
     * Check if this is a ranged attack style
     */
    public boolean isRanged()
    {
        return styleName.toLowerCase().equals("ranged");
    }

    /**
     * Get normalized combat style category
     */
    public String getCombatStyleCategory()
    {
        if (isMelee()) return "melee";
        if (isMagic()) return "magic";
        if (isRanged()) return "ranged";
        return "other";
    }
}