package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.plugins.pvmperformancetracker.models.NpcCombatStats;

/**
 * OSRS combat formula calculations
 * Based on OSRS Wiki formulas
 */
@Slf4j
public class CombatFormulas
{
    private final Client client;

    public CombatFormulas(Client client)
    {
        this.client = client;
    }

    /**
     * Calculate expected damage dealt by player to NPC
     * Expected Damage = (Max Hit / 2) * Accuracy
     */
    public double calculateExpectedDamage(NpcCombatStats npcStats, String attackStyle)
    {
        if (npcStats == null)
        {
            return 0.0;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return 0.0;
        }

        int maxHit = calculateMaxHit(attackStyle);
        double accuracy = calculateAccuracy(npcStats, attackStyle);

        return (maxHit / 2.0) * accuracy;
    }

    /**
     * Calculate player's max hit
     */
    public int calculateMaxHit(String attackStyle)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return 0;
        }

        // Determine combat style
        if (attackStyle == null)
        {
            attackStyle = "melee"; // Default
        }

        switch (attackStyle.toLowerCase())
        {
            case "magic":
                return calculateMagicMaxHit();
            case "ranged":
                return calculateRangedMaxHit();
            default: // melee (stab/slash/crush)
                return calculateMeleeMaxHit();
        }
    }

    /**
     * Calculate melee max hit
     * Formula: floor(0.5 + Strength Level * (Strength Bonus + 64) / 640)
     */
    private int calculateMeleeMaxHit()
    {
        int strengthLevel = client.getBoostedSkillLevel(Skill.STRENGTH);
        int strengthBonus = getEquipmentBonus(EquipmentInventorySlot.WEAPON, "melee_strength");

        // Prayer multipliers
        double prayerMultiplier = getPrayerStrengthMultiplier();

        // Void knight multiplier (simplified - would need to check actual equipment)
        double voidMultiplier = 1.0;

        double effectiveStrength = Math.floor((strengthLevel * prayerMultiplier * voidMultiplier) + 8 + 3); // +3 for aggressive
        double baseDamage = (effectiveStrength * (strengthBonus + 64)) / 640.0;

        return (int) Math.floor(0.5 + baseDamage);
    }

    /**
     * Calculate ranged max hit
     */
    private int calculateRangedMaxHit()
    {
        int rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
        int rangedStrength = getEquipmentBonus(EquipmentInventorySlot.WEAPON, "ranged_strength");

        double prayerMultiplier = getPrayerRangedStrengthMultiplier();
        double voidMultiplier = 1.0;

        double effectiveRanged = Math.floor((rangedLevel * prayerMultiplier * voidMultiplier) + 8 + 3);
        double baseDamage = (effectiveRanged * (rangedStrength + 64)) / 640.0;

        return (int) Math.floor(0.5 + baseDamage);
    }

    /**
     * Calculate magic max hit
     */
    private int calculateMagicMaxHit()
    {
        // Magic max hit depends on spell being used
        // For powered staves, use equipment bonus
        // For spells, use spell base damage + magic damage bonus

        int magicDamageBonus = getEquipmentBonus(EquipmentInventorySlot.WEAPON, "magic_damage");

        // This is simplified - would need spell detection
        // Assume powered staff for now
        int baseMax = 28; // Example: Trident base max

        return (int) (baseMax * (1 + magicDamageBonus / 100.0));
    }

    /**
     * Calculate hit chance (accuracy)
     */
    public double calculateAccuracy(NpcCombatStats npcStats, String attackStyle)
    {
        if (npcStats == null)
        {
            return 0.5; // Default 50%
        }

        int attackRoll = calculatePlayerAttackRoll(attackStyle);
        int defenceRoll = calculateNpcDefenceRoll(npcStats, attackStyle);

        // Standard accuracy formula
        if (attackRoll > defenceRoll)
        {
            return 1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0));
        }
        else
        {
            return attackRoll / (2.0 * (defenceRoll + 1.0));
        }
    }

    /**
     * Calculate player's attack roll
     */
    private int calculatePlayerAttackRoll(String attackStyle)
    {
        int attackLevel;
        int attackBonus;

        switch (attackStyle.toLowerCase())
        {
            case "magic":
                attackLevel = client.getBoostedSkillLevel(Skill.MAGIC);
                attackBonus = getEquipmentBonus(EquipmentInventorySlot.WEAPON, "magic_attack");
                break;
            case "ranged":
                attackLevel = client.getBoostedSkillLevel(Skill.RANGED);
                attackBonus = getEquipmentBonus(EquipmentInventorySlot.WEAPON, "ranged_attack");
                break;
            default: // melee
                attackLevel = client.getBoostedSkillLevel(Skill.ATTACK);
                attackBonus = getEquipmentBonus(EquipmentInventorySlot.WEAPON, "attack_" + attackStyle);
                break;
        }

        double prayerMultiplier = getPrayerAttackMultiplier(attackStyle);
        double effectiveLevel = Math.floor((attackLevel * prayerMultiplier) + 8 + 3); // +3 for accurate

        return (int) (effectiveLevel * (attackBonus + 64));
    }

    /**
     * Calculate NPC's defence roll
     */
    private int calculateNpcDefenceRoll(NpcCombatStats npcStats, String attackStyle)
    {
        int defenceLevel = npcStats.getDefenceLevel() != null ? npcStats.getDefenceLevel() : 1;
        int defenceBonus = npcStats.getDefenceBonus(attackStyle);

        int effectiveDefence = defenceLevel + 9;

        return effectiveDefence * (defenceBonus + 64);
    }

    /**
     * Get equipment bonus (simplified - would need actual equipment inspection)
     */
    private int getEquipmentBonus(EquipmentInventorySlot slot, String bonusType)
    {
        // This is a placeholder - would need to inspect actual equipment
        // For now, return estimated values based on common gear
        return 80; // Example value
    }

    /**
     * Get prayer strength multiplier
     */
    private double getPrayerStrengthMultiplier()
    {
        // Check active prayers
        // Burst of Strength: 1.05
        // Superhuman Strength: 1.10
        // Ultimate Strength: 1.15
        // Chivalry: 1.18
        // Piety: 1.23
        return 1.0; // Default: no prayer
    }

    /**
     * Get prayer ranged strength multiplier
     */
    private double getPrayerRangedStrengthMultiplier()
    {
        // Sharp Eye / Hawk Eye: 1.05 / 1.10
        // Eagle Eye: 1.15
        // Rigour: 1.23
        return 1.0; // Default: no prayer
    }

    /**
     * Get prayer attack multiplier
     */
    private double getPrayerAttackMultiplier(String attackStyle)
    {
        // Similar to strength multipliers
        return 1.0; // Default: no prayer
    }

    /**
     * Calculate probability of death from a hit
     * Given current HP and NPC attack
     */
    public double calculateDeathProbability(int currentHp, NpcCombatStats npcStats, boolean isPrayerActive)
    {
        if (currentHp <= 0 || npcStats == null)
        {
            return 0.0;
        }

        // Get NPC's max hit
        Integer npcMaxHit = npcStats.getMaxHit();
        if (npcMaxHit == null || npcMaxHit == 0)
        {
            return 0.0;
        }

        // Adjust for prayer
        int effectiveMaxHit = npcMaxHit;
        if (isPrayerActive)
        {
            // Protection prayers reduce damage
            String attackType = npcStats.getPrimaryAttackType();
            if (attackType != null)
            {
                effectiveMaxHit = (int) (npcMaxHit * 0.6); // 40% reduction for protection prayers
            }
        }

        // If max hit can't kill player, no death risk
        if (effectiveMaxHit < currentHp)
        {
            return 0.0;
        }

        // Calculate NPC's hit chance against player
        double hitChance = calculateNpcAccuracyAgainstPlayer(npcStats);

        // Probability of hitting lethal damage (currentHp or more)
        // Assuming uniform distribution from 0 to max hit
        double lethalDamageChance = (effectiveMaxHit - currentHp + 1.0) / (effectiveMaxHit + 1.0);

        return hitChance * lethalDamageChance;
    }

    /**
     * Calculate NPC's accuracy against player
     */
    private double calculateNpcAccuracyAgainstPlayer(NpcCombatStats npcStats)
    {
        // Simplified - would need player's defensive stats
        return 0.5; // Default 50% hit chance
    }

    /**
     * Calculate cumulative death probability
     * P(death in n tries) = 1 - (1 - p)^n
     * But for sequential with different probabilities: 1 - Product(1 - p_i)
     */
    public double calculateCumulativeDeathProbability(double currentCumulative, double newHitProbability)
    {
        // Convert cumulative probability back to survival probability
        double survivalProbability = 1.0 - currentCumulative;

        // Add new hit's survival probability
        double newSurvivalProbability = survivalProbability * (1.0 - newHitProbability);

        // Return new cumulative death probability
        return 1.0 - newSurvivalProbability;
    }
}