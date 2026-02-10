package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.plugins.pvmperformancetracker.enums.AttackStyle;

@Slf4j
public class DamageCalculator
{
    private final Client client;

    public DamageCalculator(Client client)
    {
        this.client = client;
    }

    /**
     * Determines the attack style based on the player's current weapon and combat style
     */
    public AttackStyle getCurrentAttackStyle()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return AttackStyle.UNKNOWN;
        }

        // Check for active spell (magic)
        if (client.getVarbitValue(Varbits.DEFENSIVE_CASTING_MODE) == 1)
        {
            return AttackStyle.MAGIC;
        }

        // Get weapon
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment != null)
        {
            Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());

            if (weapon != null)
            {
                int weaponId = weapon.getId();

                // Check if it's a ranged weapon
                if (isRangedWeapon(weaponId))
                {
                    return AttackStyle.RANGED;
                }

                // Check if it's a magic weapon
                if (isMagicWeapon(weaponId))
                {
                    return AttackStyle.MAGIC;
                }
            }
        }

        // Get attack style varbit
        int attackStyleVarbit = client.getVarpValue(VarPlayer.ATTACK_STYLE);

        // Determine style based on attack style setting
        // This is a simplified check - could be expanded based on weapon type
        return AttackStyle.MELEE;
    }

    /**
     * Gets the name of the currently equipped weapon
     */
    public String getCurrentWeapon()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return "Unarmed";
        }

        Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        if (weapon == null)
        {
            return "Unarmed";
        }

        ItemComposition itemComp = client.getItemDefinition(weapon.getId());
        return itemComp.getName();
    }

    /**
     * Check if a weapon ID corresponds to a ranged weapon
     */
    private boolean isRangedWeapon(int weaponId)
    {
        ItemComposition itemComp = client.getItemDefinition(weaponId);
        if (itemComp == null)
        {
            return false;
        }

        String name = itemComp.getName().toLowerCase();

        return name.contains("bow") ||
                name.contains("crossbow") ||
                name.contains("dart") ||
                name.contains("knife") ||
                name.contains("javelin") ||
                name.contains("thrownaxe") ||
                name.contains("chinchompa") ||
                name.contains("ballista") ||
                name.contains("blowpipe") ||
                name.contains("crystal bow") ||
                name.contains("zaryte");
    }

    /**
     * Check if a weapon ID corresponds to a magic weapon
     */
    private boolean isMagicWeapon(int weaponId)
    {
        ItemComposition itemComp = client.getItemDefinition(weaponId);
        if (itemComp == null)
        {
            return false;
        }

        String name = itemComp.getName().toLowerCase();

        return name.contains("staff") ||
                name.contains("wand") ||
                name.contains("trident") ||
                name.contains("sanguinesti") ||
                name.contains("harmonised") ||
                name.contains("kodai") ||
                name.contains("nightmare staff") ||
                name.contains("tome");
    }

    /**
     * Gets attack style from animation ID
     * This is used as a fallback when other methods fail
     */
    public AttackStyle getAttackStyleFromAnimation(int animationId)
    {
        // Common melee animations
        if (animationId == 422 || animationId == 423 || animationId == 401 ||
                animationId == 428 || animationId == 440 || animationId == 1378)
        {
            return AttackStyle.MELEE;
        }

        // Common ranged animations
        if (animationId == 426 || animationId == 5061 || animationId == 4230 ||
                animationId == 7552 || animationId == 7617)
        {
            return AttackStyle.RANGED;
        }

        // Common magic animations
        if (animationId == 1162 || animationId == 1167 || animationId == 8532 ||
                animationId == 7855 || animationId == 1978)
        {
            return AttackStyle.MAGIC;
        }

        return AttackStyle.UNKNOWN;
    }

    /**
     * Checks if the player is currently in combat
     */
    public boolean isInCombat()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return false;
        }

        Actor interacting = localPlayer.getInteracting();
        return interacting instanceof NPC;
    }

    /**
     * Gets the current combat target
     */
    public NPC getCurrentTarget()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return null;
        }

        Actor interacting = localPlayer.getInteracting();
        if (interacting instanceof NPC)
        {
            return (NPC) interacting;
        }

        return null;
    }
}