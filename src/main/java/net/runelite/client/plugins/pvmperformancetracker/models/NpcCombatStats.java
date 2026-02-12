package net.runelite.client.plugins.pvmperformancetracker.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * NPC combat stats from OSRSBox database
 * Schema: https://www.osrsbox.com/projects/osrsbox-db/
 */
@Data
public class NpcCombatStats
{
    private int id;
    private String name;

    // Combat stats
    @SerializedName("hitpoints")
    private Integer hitpoints;

    @SerializedName("attack_level")
    private Integer attackLevel;

    @SerializedName("strength_level")
    private Integer strengthLevel;

    @SerializedName("defence_level")
    private Integer defenceLevel;

    @SerializedName("magic_level")
    private Integer magicLevel;

    @SerializedName("ranged_level")
    private Integer rangedLevel;

    // Attack bonuses
    @SerializedName("attack_stab")
    private Integer attackStab;

    @SerializedName("attack_slash")
    private Integer attackSlash;

    @SerializedName("attack_crush")
    private Integer attackCrush;

    @SerializedName("attack_magic")
    private Integer attackMagic;

    @SerializedName("attack_ranged")
    private Integer attackRanged;

    // Defence bonuses
    @SerializedName("defence_stab")
    private Integer defenceStab;

    @SerializedName("defence_slash")
    private Integer defenceSlash;

    @SerializedName("defence_crush")
    private Integer defenceCrush;

    @SerializedName("defence_magic")
    private Integer defenceMagic;

    @SerializedName("defence_ranged")
    private Integer defenceRanged;

    // Other bonuses
    @SerializedName("attack_accuracy")
    private Integer attackAccuracy;

    @SerializedName("melee_strength")
    private Integer meleeStrength;

    @SerializedName("ranged_strength")
    private Integer rangedStrength;

    @SerializedName("magic_damage")
    private Integer magicDamage;

    // Max hit
    @SerializedName("max_hit")
    private Integer maxHit;

    // Attack type
    @SerializedName("attack_type")
    private List<String> attackType; // ["melee", "magic", "ranged"]

    // Aggressive status
    @SerializedName("aggressive")
    private Boolean aggressive;

    // Slayer properties
    @SerializedName("slayer_level")
    private Integer slayerLevel;

    @SerializedName("slayer_xp")
    private Integer slayerXp;

    /**
     * Get primary attack type
     */
    public String getPrimaryAttackType()
    {
        if (attackType == null || attackType.isEmpty())
        {
            return "melee"; // Default
        }
        return attackType.get(0);
    }

    /**
     * Check if NPC uses multiple attack styles
     */
    public boolean hasMultipleAttackStyles()
    {
        return attackType != null && attackType.size() > 1;
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

        switch (style.toLowerCase())
        {
            case "stab":
                return defenceStab != null ? defenceStab : 0;
            case "slash":
                return defenceSlash != null ? defenceSlash : 0;
            case "crush":
                return defenceCrush != null ? defenceCrush : 0;
            case "magic":
                return defenceMagic != null ? defenceMagic : 0;
            case "ranged":
                return defenceRanged != null ? defenceRanged : 0;
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

        switch (style.toLowerCase())
        {
            case "stab":
                return attackStab != null ? attackStab : 0;
            case "slash":
                return attackSlash != null ? attackSlash : 0;
            case "crush":
                return attackCrush != null ? attackCrush : 0;
            case "magic":
                return attackMagic != null ? attackMagic : 0;
            case "ranged":
                return attackRanged != null ? attackRanged : 0;
            default:
                return 0;
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
}