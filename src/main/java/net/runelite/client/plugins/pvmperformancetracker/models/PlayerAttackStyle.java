package net.runelite.client.plugins.pvmperformancetracker.models;

import lombok.Value;

/**
 * Represents a player's attack style and weapon information
 */
@Value
public class PlayerAttackStyle
{
    /**
     * Attack style (slash, stab, crush, ranged, magic)
     */
    String style;

    /**
     * Weapon attack speed in game ticks
     */
    int weaponSpeedTicks;

    /**
     * Animation ID used
     */
    int animationId;

    /**
     * Weapon ID (if applicable)
     */
    Integer weaponId;

    /**
     * Whether this is a special attack
     */
    boolean isSpecialAttack;

    /**
     * Get normalized melee style (slash/stab/crush) or return original style
     */
    public String getNormalizedMeleeStyle()
    {
        if (style == null)
        {
            return "melee";
        }

        String lower = style.toLowerCase();
        if (lower.equals("slash") || lower.equals("stab") || lower.equals("crush"))
        {
            return lower;
        }

        // Non-melee styles return as-is
        return lower;
    }

    /**
     * Check if this is a melee attack
     */
    public boolean isMelee()
    {
        if (style == null)
        {
            return false;
        }

        String lower = style.toLowerCase();
        return lower.equals("slash") || lower.equals("stab") ||
                lower.equals("crush") || lower.equals("melee");
    }

    /**
     * Check if this is a ranged attack
     */
    public boolean isRanged()
    {
        return style != null && style.equalsIgnoreCase("ranged");
    }

    /**
     * Check if this is a magic attack
     */
    public boolean isMagic()
    {
        return style != null && style.equalsIgnoreCase("magic");
    }

    /**
     * Create a PlayerAttackStyle from animation and weapon
     */
    public static PlayerAttackStyle fromAnimation(int animationId, Integer weaponId, String style, int weaponSpeed)
    {
        return new PlayerAttackStyle(style, weaponSpeed, animationId, weaponId, false);
    }

    /**
     * Create a special attack variant
     */
    public PlayerAttackStyle asSpecialAttack()
    {
        return new PlayerAttackStyle(style, weaponSpeedTicks, animationId, weaponId, true);
    }
}