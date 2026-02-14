package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.pvmperformancetracker.models.NpcCombatStats;

/**
 * OSRS combat formula calculations
 * Based on OSRS Wiki formulas
 */
@Slf4j
public class CombatFormulas
{
    private final Client client;
    private final ItemManager itemManager;

    public CombatFormulas(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
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
        int strengthBonus = getPlayerStrengthBonus();

        // Prayer multipliers
        double prayerMultiplier = getStrengthPrayerMultiplier();

        // Void multiplier
        double voidMultiplier = hasVoidMelee() ? 1.10 : 1.0;

        double effectiveStrength = Math.floor(strengthLevel * prayerMultiplier * voidMultiplier) + 8 + 3; // +3 for aggressive stance

        return (int) Math.floor(0.5 + effectiveStrength * (strengthBonus + 64) / 640.0);
    }

    /**
     * Calculate ranged max hit
     */
    private int calculateRangedMaxHit()
    {
        int rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
        int rangedStrength = getPlayerRangedStrength();

        // Prayer multipliers
        double prayerMultiplier = getRangedPrayerMultiplier();

        // Void multiplier
        double voidMultiplier = hasVoidRanged() ? 1.10 : 1.0;

        double effectiveRanged = Math.floor(rangedLevel * prayerMultiplier * voidMultiplier) + 8 + 3;

        return (int) Math.floor(0.5 + effectiveRanged * (rangedStrength + 64) / 640.0);
    }

    /**
     * Calculate magic max hit
     */
    private int calculateMagicMaxHit()
    {
        int magicLevel = client.getBoostedSkillLevel(Skill.MAGIC);
        int magicDamageBonus = getPlayerMagicDamage();

        // Prayer multipliers
        double prayerMultiplier = getMagicPrayerMultiplier();

        // Base magic damage (depends on spell - using average)
        int baseSpellDamage = 20; // Placeholder - could detect actual spell

        double effectiveMagic = Math.floor(magicLevel * prayerMultiplier);

        // Magic damage formula: Base Spell Damage * (1 + Magic Damage Bonus / 100)
        return (int) (baseSpellDamage * (1.0 + magicDamageBonus / 100.0));
    }

    /**
     * Calculate player accuracy against NPC
     */
    public double calculateAccuracy(NpcCombatStats npcStats, String attackStyle)
    {
        if (npcStats == null)
        {
            return 0.5;
        }

        int playerAttackRoll = calculatePlayerAttackRoll(attackStyle);
        int npcDefenceRoll = calculateNpcDefenceRoll(npcStats, attackStyle);

        // Standard accuracy formula
        if (playerAttackRoll > npcDefenceRoll)
        {
            return 1.0 - (npcDefenceRoll + 2.0) / (2.0 * (playerAttackRoll + 1.0));
        }
        else
        {
            return playerAttackRoll / (2.0 * (npcDefenceRoll + 1.0));
        }
    }

    /**
     * Calculate player's attack roll
     */
    private int calculatePlayerAttackRoll(String attackStyle)
    {
        int attackBonus = 0;
        int attackLevel = 0;
        double prayerMultiplier = 1.0;
        double voidMultiplier = 1.0;

        if (attackStyle == null)
        {
            attackStyle = "melee";
        }

        switch (attackStyle.toLowerCase())
        {
            case "magic":
                attackLevel = client.getBoostedSkillLevel(Skill.MAGIC);
                attackBonus = getPlayerMagicAttack();
                prayerMultiplier = getMagicPrayerMultiplier();
                break;
            case "ranged":
                attackLevel = client.getBoostedSkillLevel(Skill.RANGED);
                attackBonus = getPlayerRangedAttack();
                prayerMultiplier = getRangedPrayerMultiplier();
                voidMultiplier = hasVoidRanged() ? 1.10 : 1.0;
                break;
            default: // melee
                attackLevel = client.getBoostedSkillLevel(Skill.ATTACK);
                attackBonus = getPlayerMeleeAttack(attackStyle);
                prayerMultiplier = getAttackPrayerMultiplier();
                voidMultiplier = hasVoidMelee() ? 1.10 : 1.0;
                break;
        }

        double effectiveLevel = Math.floor(attackLevel * prayerMultiplier * voidMultiplier) + 8 + 3;
        return (int) (effectiveLevel * (attackBonus + 64));
    }

    /**
     * Calculate NPC's defence roll
     */
    private int calculateNpcDefenceRoll(NpcCombatStats npcStats, String attackStyle)
    {
        int defenceLevel = npcStats.getDefenceLevelOrDefault();
        int defenceBonus = npcStats.getDefenceBonus(attackStyle);

        int effectiveDefence = defenceLevel + 9;
        return effectiveDefence * (defenceBonus + 64);
    }

    /**
     * Get player's strength bonus from equipment
     */
    private int getPlayerStrengthBonus()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return 0;
        }

        int totalBonus = 0;
        for (Item item : equipment.getItems())
        {
            if (item.getId() > 0)
            {
                ItemStats itemStats = itemManager.getItemStats(item.getId());
                if (itemStats != null && itemStats.getEquipment() != null)
                {
                    totalBonus += itemStats.getEquipment().getStr();
                }
            }
        }
        return totalBonus;
    }

    /**
     * Get player's ranged strength from equipment
     */
    private int getPlayerRangedStrength()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return 0;
        }

        int totalBonus = 0;
        for (Item item : equipment.getItems())
        {
            if (item.getId() > 0)
            {
                ItemStats itemStats = itemManager.getItemStats(item.getId());
                if (itemStats != null && itemStats.getEquipment() != null)
                {
                    totalBonus += itemStats.getEquipment().getRstr();
                }
            }
        }
        return totalBonus;
    }

    /**
     * Get player's magic damage bonus from equipment
     */
    private int getPlayerMagicDamage()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return 0;
        }

        int totalBonus = 0;
        for (Item item : equipment.getItems())
        {
            if (item.getId() > 0)
            {
                ItemStats itemStats = itemManager.getItemStats(item.getId());
                if (itemStats != null && itemStats.getEquipment() != null)
                {
                    totalBonus += itemStats.getEquipment().getMdmg();
                }
            }
        }
        return totalBonus;
    }

    /**
     * Get player's melee attack bonus
     */
    private int getPlayerMeleeAttack(String style)
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return 0;
        }

        int totalBonus = 0;
        for (Item item : equipment.getItems())
        {
            if (item.getId() > 0)
            {
                ItemStats itemStats = itemManager.getItemStats(item.getId());
                if (itemStats != null && itemStats.getEquipment() != null)
                {
                    // Get appropriate attack bonus based on style
                    switch (style.toLowerCase())
                    {
                        case "stab":
                            totalBonus += itemStats.getEquipment().getAstab();
                            break;
                        case "slash":
                            totalBonus += itemStats.getEquipment().getAslash();
                            break;
                        case "crush":
                            totalBonus += itemStats.getEquipment().getAcrush();
                            break;
                        default:
                            totalBonus += itemStats.getEquipment().getAslash(); // Default slash
                            break;
                    }
                }
            }
        }
        return totalBonus;
    }

    /**
     * Get player's ranged attack bonus
     */
    private int getPlayerRangedAttack()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return 0;
        }

        int totalBonus = 0;
        for (Item item : equipment.getItems())
        {
            if (item.getId() > 0)
            {
                ItemStats itemStats = itemManager.getItemStats(item.getId());
                if (itemStats != null && itemStats.getEquipment() != null)
                {
                    totalBonus += itemStats.getEquipment().getArange();
                }
            }
        }
        return totalBonus;
    }

    /**
     * Get player's magic attack bonus
     */
    private int getPlayerMagicAttack()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return 0;
        }

        int totalBonus = 0;
        for (Item item : equipment.getItems())
        {
            if (item.getId() > 0)
            {
                ItemStats itemStats = itemManager.getItemStats(item.getId());
                if (itemStats != null && itemStats.getEquipment() != null)
                {
                    totalBonus += itemStats.getEquipment().getAmagic();
                }
            }
        }
        return totalBonus;
    }

    /**
     * Get player's defence bonus against attack type
     */
    private int getPlayerDefenceBonus(String attackType)
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return 0;
        }

        int totalBonus = 0;
        for (Item item : equipment.getItems())
        {
            if (item.getId() > 0)
            {
                ItemStats itemStats = itemManager.getItemStats(item.getId());
                if (itemStats != null && itemStats.getEquipment() != null)
                {
                    switch (attackType.toLowerCase())
                    {
                        case "stab":
                            totalBonus += itemStats.getEquipment().getDstab();
                            break;
                        case "slash":
                            totalBonus += itemStats.getEquipment().getDslash();
                            break;
                        case "crush":
                            totalBonus += itemStats.getEquipment().getDcrush();
                            break;
                        case "magic":
                            totalBonus += itemStats.getEquipment().getDmagic();
                            break;
                        case "ranged":
                            totalBonus += itemStats.getEquipment().getDrange();
                            break;
                        default:
                            totalBonus += itemStats.getEquipment().getDslash();
                            break;
                    }
                }
            }
        }
        return totalBonus;
    }

    /**
     * Check if player has void melee set
     */
    private boolean hasVoidMelee()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return false;
        }

        boolean hasHelm = false;
        boolean hasTop = false;
        boolean hasBottom = false;
        boolean hasGloves = false;

        for (Item item : equipment.getItems())
        {
            int id = item.getId();
            // Void melee helm
            if (id == 11665)
            {
                hasHelm = true;
            }
            // Void knight top
            if (id == 8839 || id == 10611 || id == 13072)
            {
                hasTop = true;
            }
            // Void knight robe
            if (id == 8840 || id == 10612 || id == 13073)
            {
                hasBottom = true;
            }
            // Void knight gloves
            if (id == 8842)
            {
                hasGloves = true;
            }
        }

        return hasHelm && hasTop && hasBottom && hasGloves;
    }

    /**
     * Check if player has void ranged set
     */
    private boolean hasVoidRanged()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return false;
        }

        boolean hasHelm = false;
        boolean hasTop = false;
        boolean hasBottom = false;
        boolean hasGloves = false;

        for (Item item : equipment.getItems())
        {
            int id = item.getId();
            // Void ranger helm
            if (id == 11664)
            {
                hasHelm = true;
            }
            // Void knight top
            if (id == 8839 || id == 10611 || id == 13072)
            {
                hasTop = true;
            }
            // Void knight robe
            if (id == 8840 || id == 10612 || id == 13073)
            {
                hasBottom = true;
            }
            // Void knight gloves
            if (id == 8842)
            {
                hasGloves = true;
            }
        }

        return hasHelm && hasTop && hasBottom && hasGloves;
    }

    /**
     * Get strength prayer multiplier
     */
    private double getStrengthPrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.BURST_OF_STRENGTH))
        {
            return 1.05;
        }
        if (client.isPrayerActive(Prayer.SUPERHUMAN_STRENGTH))
        {
            return 1.10;
        }
        if (client.isPrayerActive(Prayer.ULTIMATE_STRENGTH))
        {
            return 1.15;
        }
        if (client.isPrayerActive(Prayer.CHIVALRY))
        {
            return 1.18;
        }
        if (client.isPrayerActive(Prayer.PIETY))
        {
            return 1.23;
        }
        return 1.0;
    }

    /**
     * Get attack prayer multiplier
     */
    private double getAttackPrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.CLARITY_OF_THOUGHT))
        {
            return 1.05;
        }
        if (client.isPrayerActive(Prayer.IMPROVED_REFLEXES))
        {
            return 1.10;
        }
        if (client.isPrayerActive(Prayer.INCREDIBLE_REFLEXES))
        {
            return 1.15;
        }
        if (client.isPrayerActive(Prayer.CHIVALRY))
        {
            return 1.15;
        }
        if (client.isPrayerActive(Prayer.PIETY))
        {
            return 1.20;
        }
        return 1.0;
    }

    /**
     * Get ranged prayer multiplier (attack and strength)
     */
    private double getRangedPrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.SHARP_EYE))
        {
            return 1.05;
        }
        if (client.isPrayerActive(Prayer.HAWK_EYE))
        {
            return 1.10;
        }
        if (client.isPrayerActive(Prayer.EAGLE_EYE))
        {
            return 1.15;
        }
        if (client.isPrayerActive(Prayer.RIGOUR))
        {
            return 1.23;
        }
        return 1.0;
    }

    /**
     * Get magic prayer multiplier
     */
    private double getMagicPrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.MYSTIC_WILL))
        {
            return 1.05;
        }
        if (client.isPrayerActive(Prayer.MYSTIC_LORE))
        {
            return 1.10;
        }
        if (client.isPrayerActive(Prayer.MYSTIC_MIGHT))
        {
            return 1.15;
        }
        if (client.isPrayerActive(Prayer.AUGURY))
        {
            return 1.25;
        }
        return 1.0;
    }

    /**
     * Calculate probability of death from a hit
     * Given current HP and NPC attack (with min/max hit range)
     */
    public double calculateDeathProbability(int currentHp, NpcCombatStats npcStats, boolean isPrayerActive)
    {
        return calculateDeathProbability(currentHp, npcStats, isPrayerActive, null, 0);
    }

    /**
     * Calculate probability of death from a hit
     * Overload that accepts minimum hit for more accurate calculations
     */
    public double calculateDeathProbability(int currentHp, NpcCombatStats npcStats, boolean isPrayerActive, Integer npcMaxHit, int npcMinHit)
    {
        if (currentHp <= 0 || npcStats == null)
        {
            return 0.0;
        }

        // Get NPC's max hit (use provided or from stats)
        int maxHit = npcMaxHit != null ? npcMaxHit : (npcStats.getMaxHit() != null ? npcStats.getMaxHit() : 0);

        if (maxHit == 0)
        {
            return 0.0;
        }

        // Adjust for prayer
        int effectiveMaxHit = maxHit;
        int effectiveMinHit = npcMinHit;

        if (isPrayerActive)
        {
            // Protection prayers reduce damage by 40%
            effectiveMaxHit = (int) (maxHit * 0.6);
            effectiveMinHit = (int) (npcMinHit * 0.6);
        }

        // If max hit can't kill player, no death risk
        if (effectiveMaxHit < currentHp)
        {
            return 0.0;
        }

        // Calculate NPC's hit chance against player
        double hitChance = calculateNpcAccuracyAgainstPlayer(npcStats);

        // Probability of hitting lethal damage
        // OLD (Wrong): P(lethal) = (maxHit - currentHp + 1) / (maxHit + 1)
        // NEW (Correct): P(lethal) = (maxHit - currentHp + 1) / (maxHit - minHit + 1)

        // If minimum hit is already lethal, 100% chance of death when hit lands
        if (effectiveMinHit >= currentHp)
        {
            return hitChance; // Guaranteed death if hit lands
        }

        // Calculate probability of lethal damage in the range [minHit, maxHit]
        int possibleHits = effectiveMaxHit - effectiveMinHit + 1;
        int lethalHits = effectiveMaxHit - currentHp + 1;

        double lethalDamageChance = (double) lethalHits / possibleHits;

        return hitChance * lethalDamageChance;
    }

    /**
     * Calculate NPC's accuracy against player
     */
    private double calculateNpcAccuracyAgainstPlayer(NpcCombatStats npcStats)
    {
        if (npcStats == null)
        {
            return 0.5;
        }

        // Get NPC's attack roll
        int npcAttackRoll = calculateNpcAttackRoll(npcStats);

        // Get player's defence roll
        int playerDefenceRoll = calculatePlayerDefenceRoll(npcStats);

        // Standard accuracy formula
        if (npcAttackRoll > playerDefenceRoll)
        {
            return 1.0 - (playerDefenceRoll + 2.0) / (2.0 * (npcAttackRoll + 1.0));
        }
        else
        {
            return npcAttackRoll / (2.0 * (playerDefenceRoll + 1.0));
        }
    }

    /**
     * Calculate NPC's attack roll
     */
    private int calculateNpcAttackRoll(NpcCombatStats npcStats)
    {
        // Get NPC attack level and bonus based on their attack type
        String attackType = npcStats.getPrimaryAttackType();
        if (attackType == null)
        {
            attackType = "melee";
        }

        int attackLevel;
        int attackBonus;

        switch (attackType.toLowerCase())
        {
            case "magic":
                attackLevel = npcStats.getMagicLevel() != null ? npcStats.getMagicLevel() : 1;
                attackBonus = npcStats.getAttackBonus("magic");
                break;
            case "ranged":
                attackLevel = npcStats.getRangedLevel() != null ? npcStats.getRangedLevel() : 1;
                attackBonus = npcStats.getAttackBonus("ranged");
                break;
            default: // melee
                attackLevel = npcStats.getAttackLevelOrDefault();
                attackBonus = npcStats.getAttackBonus(attackType);
                break;
        }

        int effectiveLevel = attackLevel + 9;
        return effectiveLevel * (attackBonus + 64);
    }

    /**
     * Calculate player's defence roll against NPC
     */
    private int calculatePlayerDefenceRoll(NpcCombatStats npcStats)
    {
        String attackType = npcStats.getPrimaryAttackType();
        if (attackType == null)
        {
            attackType = "melee";
        }

        // Get player defence level
        int defenceLevel = client.getBoostedSkillLevel(Skill.DEFENCE);

        // Get player's defensive bonus against this attack type
        int defenceBonus = getPlayerDefenceBonus(attackType);

        // Prayer multipliers for defence
        double prayerMultiplier = getDefencePrayerMultiplier();

        double effectiveDefence = Math.floor((defenceLevel * prayerMultiplier) + 8 + 1); // +1 for defensive style

        return (int) (effectiveDefence * (defenceBonus + 64));
    }

    /**
     * Get prayer defence multiplier
     */
    private double getDefencePrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.THICK_SKIN))
        {
            return 1.05;
        }
        if (client.isPrayerActive(Prayer.ROCK_SKIN))
        {
            return 1.10;
        }
        if (client.isPrayerActive(Prayer.STEEL_SKIN))
        {
            return 1.15;
        }
        if (client.isPrayerActive(Prayer.CHIVALRY))
        {
            return 1.20;
        }
        if (client.isPrayerActive(Prayer.PIETY))
        {
            return 1.25;
        }
        if (client.isPrayerActive(Prayer.RIGOUR))
        {
            return 1.25;
        }
        if (client.isPrayerActive(Prayer.AUGURY))
        {
            return 1.25;
        }
        return 1.0;
    }

    /**
     * Calculate cumulative death probability
     * P(death over multiple hits) = 1 - Product(1 - p_i)
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