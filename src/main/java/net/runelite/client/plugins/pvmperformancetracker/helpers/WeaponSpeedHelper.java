package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.plugins.pvmperformancetracker.enums.AnimationIds;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks weapon attack speeds for calculating attacking ticks lost
 */
@Slf4j
public class WeaponSpeedHelper
{
    private final Client client;

    // Cache of weapon ID -> attack speed (in ticks)
    private final Map<Integer, Integer> weaponSpeedCache = new HashMap<>();

    // Default attack speeds by weapon type
    private static final int DEFAULT_MELEE_SPEED = 4;
    private static final int DEFAULT_RANGED_SPEED = 5;
    private static final int DEFAULT_MAGIC_SPEED = 5;
    private static final int UNARMED_SPEED = 4;

    public WeaponSpeedHelper(Client client)
    {
        this.client = client;
        initializeWeaponSpeeds();
    }

    /**
     * Get the attack speed of the currently equipped weapon
     */
    public int getCurrentWeaponSpeed()
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return DEFAULT_MELEE_SPEED;
        }

        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return UNARMED_SPEED;
        }

        Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        if (weapon == null)
        {
            return UNARMED_SPEED;
        }

        return getWeaponSpeed(weapon.getId());
    }

    /**
     * Get weapon speed by item ID
     */
    public int getWeaponSpeed(int weaponId)
    {
        // Check cache first
        if (weaponSpeedCache.containsKey(weaponId))
        {
            return weaponSpeedCache.get(weaponId);
        }

        // Get weapon name to determine speed
        ItemComposition itemComp = client.getItemDefinition(weaponId);
        if (itemComp == null)
        {
            return DEFAULT_MELEE_SPEED;
        }

        String weaponName = itemComp.getName().toLowerCase();
        int speed = determineWeaponSpeed(weaponName);

        // Cache it
        weaponSpeedCache.put(weaponId, speed);

        return speed;
    }

    /**
     * Determine weapon speed based on weapon name
     */
    private int determineWeaponSpeed(String weaponName)
    {
        // 2-tick weapons
        if (weaponName.contains("blowpipe") ||
                weaponName.contains("dart") && !weaponName.contains("magic"))
        {
            return 2;
        }

        // 3-tick weapons
        if (weaponName.contains("shortbow") ||
                weaponName.contains("chinchompa") ||
                (weaponName.contains("knife") && weaponName.contains("throwing")))
        {
            return 3;
        }

        // 4-tick weapons (most common)
        if (weaponName.contains("scimitar") ||
                weaponName.contains("whip") ||
                weaponName.contains("rapier") ||
                weaponName.contains("trident") ||
                weaponName.contains("sanguinesti") ||
                weaponName.contains("dagger"))
        {
            return 4;
        }

        // 5-tick weapons - CRITICAL: Check this BEFORE 4-tick defaults
        if (weaponName.contains("halberd") ||        // ← FIX: Was missing!
                weaponName.contains("scythe") ||
                weaponName.contains("mace") ||
                weaponName.contains("hammer") && !weaponName.contains("granite") ||
                weaponName.contains("spear") ||
                weaponName.contains("hasta") ||
                weaponName.contains("crossbow") && !weaponName.contains("karil"))
        {
            return 5;
        }

        // 6-tick weapons
        if (weaponName.contains("godsword") ||
                weaponName.contains("2h") ||
                weaponName.contains("ballista") ||
                weaponName.contains("elder maul") ||
                weaponName.contains("longbow"))
        {
            return 6;
        }

        // 7-tick weapons
        if (weaponName.contains("granite maul") && !weaponName.contains("ornate"))
        {
            return 7;
        }

        // Magic staves default
        if (weaponName.contains("staff") || weaponName.contains("wand"))
        {
            // Harmonised nightmare staff is 4 ticks
            if (weaponName.contains("harmonised"))
            {
                return 4;
            }
            return 5;
        }

        // Bows default
        if (weaponName.contains("bow"))
        {
            return 5;
        }

        // Swords default
        if (weaponName.contains("sword"))
        {
            return 4;
        }

        // Default: 4 ticks
        return DEFAULT_MELEE_SPEED;
    }

    /**
     * Initialize known weapon speeds
     */
    private void initializeWeaponSpeeds()
    {
        // Specific weapons with known IDs can be added here
        // For now, we rely on name-based detection

        // Example:
        // weaponSpeedCache.put(11785, 2); // Twisted bow
        // weaponSpeedCache.put(12926, 2); // Blowpipe
    }

    /**
     * Get attack speed adjusted for player's attack style and animation
     * This is the MOST ACCURATE method - use this instead of getCurrentWeaponSpeed()
     */
    public int getAdjustedWeaponSpeedFromAnimation(int animationId)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return DEFAULT_MELEE_SPEED;
        }

        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return UNARMED_SPEED;
        }

        Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        int weaponId = (weapon != null) ? weapon.getId() : -1;

        // Use AnimationIds.getTicks() for PERFECT accuracy
        int ticks = AnimationIds.getTicks(animationId, weaponId);

        if (ticks > 0)
        {
            return ticks;
        }

        // Fallback to name-based detection
        return getCurrentWeaponSpeed();
    }


    /**
     * Check if weapon is a ranged weapon
     */
    private boolean isRangedWeapon(String weaponName)
    {
        return weaponName.contains("bow") ||
                weaponName.contains("crossbow") ||
                weaponName.contains("blowpipe") ||
                weaponName.contains("chinchompa") ||
                weaponName.contains("ballista") ||
                weaponName.contains("dart") ||
                weaponName.contains("knife") ||
                weaponName.contains("javelin");
    }
}