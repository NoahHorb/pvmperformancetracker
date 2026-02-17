package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.pvmperformancetracker.models.NpcCombatStats;
import net.runelite.client.game.ItemStats;

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

        // Other multipliers (salve amulet, slayer helm, etc.)
        double otherMultiplier = 1.0;

        double effectiveStrength = Math.floor(strengthLevel * prayerMultiplier * otherMultiplier) + 8 + 3;
        double baseDamage = Math.floor(effectiveStrength * (strengthBonus + 64) / 640.0);
        int maxHit = (int) Math.floor(baseDamage * voidMultiplier);

        // DEBUG LOGGING
        log.debug("=== Max Hit Calculation ===");
        log.debug("Strength level: {}", strengthLevel);
        log.debug("Strength bonus: {}", strengthBonus);
        log.debug("Prayer multiplier: {}", String.format("%.2f", prayerMultiplier));
        log.debug("Void multiplier: {}", String.format("%.2f", voidMultiplier));
        log.debug("Effective strength: {}", String.format("%.2f", effectiveStrength));
        log.debug("Base damage: {}", String.format("%.2f", baseDamage));
        log.debug("Final max hit: {}", maxHit);
        log.debug("===========================");

        return maxHit;
    }

    /**
     * Calculate ranged max hit
     */
    private int calculateRangedMaxHit()
    {
        int rangedLevel = client.getBoostedSkillLevel(Skill.RANGED);
        int rangedStrength = getPlayerRangedStrength();

        double prayerMultiplier = getRangedStrengthPrayerMultiplier();
        double voidMultiplier = hasVoidRanged() ? 1.10 : 1.0;

        double effectiveRanged = Math.floor(rangedLevel * prayerMultiplier) + 8 + 3;
        double baseDamage = Math.floor(effectiveRanged * (rangedStrength + 64) / 640.0);

        return (int) Math.floor(baseDamage * voidMultiplier);
    }

    /**
     * Calculate magic max hit
     */
    private int calculateMagicMaxHit()
    {
        int magicLevel = client.getBoostedSkillLevel(Skill.MAGIC);
        int magicDamage = getPlayerMagicDamage();

        double prayerMultiplier = getMagicDamagePrayerMultiplier();

        // Base spell damage varies by spell - this is simplified
        int spellBaseDamage = 20; // Placeholder

        double maxHit = spellBaseDamage * (1 + magicDamage / 100.0) * prayerMultiplier;

        return (int) Math.floor(maxHit);
    }

    /**
     * Calculate player's hit accuracy against NPC
     */
    public double calculateAccuracy(NpcCombatStats npcStats, String attackStyle)
    {
        if (npcStats == null)
        {
            return 0.5;
        }

        int attackRoll = calculatePlayerAttackRoll(attackStyle);
        int defenceRoll = calculateNpcDefenceRoll(npcStats, attackStyle);

        // Standard accuracy formula
        double accuracy;
        if (attackRoll > defenceRoll)
        {
            accuracy = 1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0));
        }
        else
        {
            accuracy = attackRoll / (2.0 * (defenceRoll + 1.0));
        }

        // DEBUG LOGGING
        log.debug("=== Accuracy Calculation ===");
        log.debug("Attack style: {}", attackStyle);
        log.debug("Player attack roll: {}", attackRoll);
        log.debug("NPC defence roll: {}", defenceRoll);
        log.debug("Accuracy: {} ({}%)",
                String.format("%.4f", accuracy),
                String.format("%.2f", accuracy * 100));
        log.debug("============================");

        return accuracy;
    }


    /**
     * Calculate probability of death from NPC attack
     *
     * @param currentHp Player's current HP
     * @param npcStats NPC combat stats
     * @param attackStyle The attack style being used by the NPC
     * @param hasCorrectPrayer Whether player has the correct protection prayer active
     * @return Probability of death (0.0 to 1.0)
     */
    public double calculateDeathProbability(int currentHp, NpcCombatStats npcStats,
                                            String attackStyle, boolean hasCorrectPrayer)
    {
        if (currentHp <= 0 || npcStats == null)
        {
            return 0.0;
        }

        // Use primary attack style if none specified
        if (attackStyle == null || attackStyle.isEmpty())
        {
            attackStyle = npcStats.getPrimaryAttackStyle();
        }

        // Get max and min hit for this specific attack style
        int maxHit = npcStats.getMaxHitForStyle(attackStyle);
        int minHit = npcStats.getMinHitForStyle(attackStyle);

        if (maxHit == 0)
        {
            return 0.0;
        }

        // Get prayer reduction multiplier for this NPC and attack style
        double prayerReduction = getPrayerReductionMultiplier(npcStats, attackStyle, hasCorrectPrayer);

        // Apply prayer reduction
        int effectiveMaxHit = (int) Math.floor(maxHit * prayerReduction);
        int effectiveMinHit = (int) Math.floor(minHit * prayerReduction);

        // If max hit can't kill player, no death risk
        if (effectiveMaxHit < currentHp)
        {
            return 0.0;
        }

        // Calculate NPC's hit chance against player
        double hitChance = calculateNpcAccuracyAgainstPlayer(npcStats, attackStyle);

        // If minimum hit is already lethal, 100% chance of death when hit lands
        if (effectiveMinHit >= currentHp)
        {
            return hitChance;
        }

        // Calculate probability of lethal damage in the range [minHit, maxHit]
        int possibleHits = effectiveMaxHit - effectiveMinHit + 1;
        int lethalHits = effectiveMaxHit - currentHp + 1;

        double lethalDamageChance = (double) lethalHits / possibleHits;

        return hitChance * lethalDamageChance;
    }

    /**
     * Get prayer reduction multiplier for a specific NPC and attack style
     * Different NPCs have different prayer effectiveness
     *
     * @param npcStats NPC combat stats
     * @param attackStyle Attack style being used
     * @param hasCorrectPrayer Whether player has correct prayer active
     * @return Multiplier (1.0 = no reduction, 0.0 = full block, 0.6 = 40% reduction)
     */
    private double getPrayerReductionMultiplier(NpcCombatStats npcStats, String attackStyle, boolean hasCorrectPrayer)
    {
        if (!hasCorrectPrayer)
        {
            return 1.0; // No prayer = full damage
        }

        String npcName = npcStats.getBaseName();
        if (npcName == null)
        {
            npcName = npcStats.getName();
        }

        // Normalize attack style
        String normalizedStyle = attackStyle != null ? attackStyle.toLowerCase() : "";

        // Special cases where prayer doesn't work or has reduced effectiveness

        // Dragonfire attacks - prayer doesn't reduce dragonfire
        if (normalizedStyle.contains("dragonfire") || normalizedStyle.contains("fire"))
        {
            return 1.0; // Prayer doesn't affect dragonfire
        }

        // Typeless attacks - prayer doesn't work
        if (normalizedStyle.equals("typeless"))
        {
            return 1.0;
        }

        // NPC-specific prayer effectiveness
        if (npcName != null)
        {
            String lowerName = npcName.toLowerCase();

            // Vardorvis - Prayer only reduces by ~25% instead of 40%
            if (lowerName.contains("vardorvis"))
            {
                return 0.75; // 25% reduction instead of 40%
            }

            // Dagannoth Rex/Prime/Supreme - Prayer completely blocks their primary style
            if (lowerName.contains("dagannoth") &&
                    (lowerName.contains("rex") || lowerName.contains("prime") || lowerName.contains("supreme")))
            {
                // Check if using correct prayer against their primary style
                if ((lowerName.contains("rex") && normalizedStyle.contains("melee")) ||
                        (lowerName.contains("prime") && normalizedStyle.contains("magic")) ||
                        (lowerName.contains("supreme") && normalizedStyle.contains("ranged")))
                {
                    return 0.0; // Complete block
                }
            }

            // Corporeal Beast - Prayer reduces by 50%
            if (lowerName.contains("corporeal"))
            {
                return 0.5;
            }

            // Add more special cases here as needed
        }

        // Standard protection prayers - 40% reduction (multiply by 0.6)
        return 0.6;
    }

    /**
     * Calculate NPC's accuracy against player
     */
    private double calculateNpcAccuracyAgainstPlayer(NpcCombatStats npcStats, String attackStyle)
    {
        if (npcStats == null)
        {
            return 0.5;
        }

        // Get NPC's attack roll for this style
        int npcAttackRoll = calculateNpcAttackRoll(npcStats, attackStyle);

        // Get player's defence roll against this style
        int playerDefenceRoll = calculatePlayerDefenceRoll(attackStyle);

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
     * Calculate NPC's attack roll for a specific attack style
     */
    private int calculateNpcAttackRoll(NpcCombatStats npcStats, String attackStyle)
    {
        // Get NPC attack level for this style
        int attackLevel = npcStats.getAttackLevelForStyle(attackStyle);

        // Get NPC attack bonus for this style
        int attackBonus = npcStats.getAttackBonus(attackStyle);

        int effectiveAttackLevel = attackLevel + 9;
        return effectiveAttackLevel * (attackBonus + 64);
    }

    /**
     * Calculate player's defence roll against a specific attack style
     */
    private int calculatePlayerDefenceRoll(String attackStyle)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return 0;
        }

        int defenceLevel = client.getBoostedSkillLevel(Skill.DEFENCE);
        int defenceBonus = getPlayerDefenceBonus(attackStyle);

        // Prayer multipliers
        double prayerMultiplier = getDefencePrayerMultiplier();

        double effectiveDefence = Math.floor(defenceLevel * prayerMultiplier) + 9;
        return (int) (effectiveDefence * (defenceBonus + 64));
    }

    /**
     * Calculate player's attack roll
     */
    private int calculatePlayerAttackRoll(String attackStyle)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return 0;
        }

        int attackLevel;
        int attackBonus;
        double prayerMultiplier;
        double voidMultiplier;

        if (attackStyle == null)
        {
            attackStyle = "melee";
        }

        switch (attackStyle.toLowerCase())
        {
            case "magic":
                attackLevel = client.getBoostedSkillLevel(Skill.MAGIC);
                attackBonus = getPlayerMagicAttack();
                prayerMultiplier = getAttackPrayerMultiplier();
                voidMultiplier = hasVoidMagic() ? 1.45 : 1.0;
                break;
            case "ranged":
                attackLevel = client.getBoostedSkillLevel(Skill.RANGED);
                attackBonus = getPlayerRangedAttack();
                prayerMultiplier = getAttackPrayerMultiplier();
                voidMultiplier = hasVoidRanged() ? 1.10 : 1.0;
                break;
            default: // melee (slash/stab/crush)
                attackLevel = client.getBoostedSkillLevel(Skill.ATTACK);
                attackBonus = getPlayerMeleeAttack(attackStyle);
                prayerMultiplier = getAttackPrayerMultiplier();
                voidMultiplier = hasVoidMelee() ? 1.10 : 1.0;
                break;
        }

        double effectiveLevel = Math.floor(attackLevel * prayerMultiplier * voidMultiplier) + 8 + 3;
        int attackRoll = (int) (effectiveLevel * (attackBonus + 64));

        // DEBUG LOGGING
        log.debug("=== Attack Roll Calculation ===");
        log.debug("Attack style: {}", attackStyle);
        log.debug("Attack level: {}", attackLevel);
        log.debug("Attack bonus: {}", attackBonus);
        log.debug("Prayer multiplier: {}", String.format("%.2f", prayerMultiplier));
        log.debug("Void multiplier: {}", String.format("%.2f", voidMultiplier));
        log.debug("Effective level: {}", String.format("%.2f", effectiveLevel));
        log.debug("Attack roll: {}", attackRoll);
        log.debug("================================");

        return attackRoll;
    }

    /**
     * Calculate NPC's defence roll
     */
    private int calculateNpcDefenceRoll(NpcCombatStats npcStats, String attackStyle)
    {
        int defenceLevel = npcStats.getDefenceLevelOrDefault();
        int defenceBonus = npcStats.getDefenceBonus(attackStyle);

        int effectiveDefence = defenceLevel + 9;
        int defenceRoll = effectiveDefence * (defenceBonus + 64);

        // DEBUG LOGGING
        log.debug("=== NPC Defence Roll ===");
        log.debug("NPC: {}", npcStats.getName());
        log.debug("Defence level: {}", defenceLevel);
        log.debug("Defence bonus ({}): {}", attackStyle, defenceBonus);
        log.debug("Effective defence: {}", effectiveDefence);
        log.debug("Defence roll: {}", defenceRoll);
        log.debug("========================");

        return defenceRoll;
    }

    // ==================== Player Equipment Bonus Helpers ====================

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
        log.debug("Player strength bonus: {}", totalBonus);
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
        log.debug("Player range str bonus: {}", totalBonus);
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
        log.debug("Player magic dmg bonus: {}", totalBonus);
        return totalBonus;
    }

    /**
     * Get player's melee attack bonus
     */
    private int getPlayerMeleeAttack(String attackStyle)
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
                    // Use appropriate melee bonus based on style
                    if (attackStyle != null)
                    {
                        switch (attackStyle.toLowerCase())
                        {
                            case "stab":
                                totalBonus += itemStats.getEquipment().getAstab();
                                break;
                            case "slash":
                                totalBonus += itemStats.getEquipment().getAslash();
                                break;
                            case "crush":
                            default:
                                totalBonus += itemStats.getEquipment().getAcrush();
                                break;
                        }
                    }
                    else
                    {
                        totalBonus += itemStats.getEquipment().getAslash(); // Default
                    }
                }
            }
        }
        log.debug("Player {} attack bonus: {}", attackStyle, totalBonus);
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
        log.debug("Player ranged attack bonus: {}", totalBonus);
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
        log.debug("Player magic attack bonus: {}", totalBonus);
        return totalBonus;
    }

    /**
     * Get player's defence bonus for a specific attack style
     */
    private int getPlayerDefenceBonus(String attackStyle)
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
                    if (attackStyle != null)
                    {
                        switch (attackStyle.toLowerCase())
                        {
                            case "stab":
                                totalBonus += itemStats.getEquipment().getDstab();
                                break;
                            case "slash":
                                totalBonus += itemStats.getEquipment().getDslash();
                                break;
                            case "crush":
                            case "melee":
                                totalBonus += itemStats.getEquipment().getDcrush();
                                break;
                            case "magic":
                                totalBonus += itemStats.getEquipment().getDmagic();
                                break;
                            case "ranged":
                                totalBonus += itemStats.getEquipment().getDrange();
                                break;
                            default:
                                totalBonus += itemStats.getEquipment().getDcrush();
                                break;
                        }
                    }
                }
            }
        }
        log.debug("Player {} defence bonus: {}", attackStyle, totalBonus);

        return totalBonus;
    }

    // ==================== Prayer Multiplier Helpers ====================

    private double getStrengthPrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.BURST_OF_STRENGTH))
            return 1.05;
        if (client.isPrayerActive(Prayer.SUPERHUMAN_STRENGTH))
            return 1.10;
        if (client.isPrayerActive(Prayer.ULTIMATE_STRENGTH))
            return 1.15;
        if (client.isPrayerActive(Prayer.CHIVALRY))
            return 1.18;
        if (client.isPrayerActive(Prayer.PIETY))
            return 1.23;
        return 1.0;
    }

    private double getRangedStrengthPrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.SHARP_EYE))
            return 1.05;
        if (client.isPrayerActive(Prayer.HAWK_EYE))
            return 1.10;
        if (client.isPrayerActive(Prayer.EAGLE_EYE))
            return 1.15;
        if (client.isPrayerActive(Prayer.RIGOUR))
            return 1.23;
        return 1.0;
    }

    private double getMagicDamagePrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.MYSTIC_WILL))
            return 1.05;
        if (client.isPrayerActive(Prayer.MYSTIC_LORE))
            return 1.10;
        if (client.isPrayerActive(Prayer.MYSTIC_MIGHT))
            return 1.15;
        if (client.isPrayerActive(Prayer.AUGURY))
            return 1.25;
        return 1.0;
    }

    private double getAttackPrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.CLARITY_OF_THOUGHT))
            return 1.05;
        if (client.isPrayerActive(Prayer.IMPROVED_REFLEXES))
            return 1.10;
        if (client.isPrayerActive(Prayer.INCREDIBLE_REFLEXES))
            return 1.15;
        if (client.isPrayerActive(Prayer.CHIVALRY))
            return 1.15;
        if (client.isPrayerActive(Prayer.PIETY))
            return 1.20;
        if (client.isPrayerActive(Prayer.RIGOUR))
            return 1.20;
        if (client.isPrayerActive(Prayer.AUGURY))
            return 1.25;
        return 1.0;
    }

    private double getDefencePrayerMultiplier()
    {
        if (client.isPrayerActive(Prayer.THICK_SKIN))
            return 1.05;
        if (client.isPrayerActive(Prayer.ROCK_SKIN))
            return 1.10;
        if (client.isPrayerActive(Prayer.STEEL_SKIN))
            return 1.15;
        if (client.isPrayerActive(Prayer.CHIVALRY))
            return 1.20;
        if (client.isPrayerActive(Prayer.PIETY))
            return 1.25;
        if (client.isPrayerActive(Prayer.RIGOUR))
            return 1.25;
        if (client.isPrayerActive(Prayer.AUGURY))
            return 1.25;
        return 1.0;
    }

    // ==================== Void Equipment Helpers ====================

    private boolean hasVoidMelee()
    {
        // Simplified - check for void equipment
        // In reality, you'd need to check for full set
        return false;
    }

    private boolean hasVoidRanged()
    {
        return false;
    }

    private boolean hasVoidMagic()
    {
        return false;
    }
}