package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

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
        // Daggers - 4 ticks
        if (weaponName.contains("dagger") || weaponName.contains("knife"))
        {
            return 4;
        }

        // Scimitars, whips - 4 ticks
        if (weaponName.contains("scimitar") || weaponName.contains("whip"))
        {
            return 4;
        }

        // Rapiers, swords - 4 ticks
        if (weaponName.contains("rapier") || weaponName.contains("sword"))
        {
            return 4;
        }

        // Maces, hammers - 5 ticks
        if (weaponName.contains("mace") || weaponName.contains("hammer"))
        {
            return 5;
        }

        // Godswords, 2h - 6 ticks
        if (weaponName.contains("godsword") || weaponName.contains("2h"))
        {
            return 6;
        }

        // Scythe - 5 ticks
        if (weaponName.contains("scythe"))
        {
            return 5;
        }

        // Bows - 5 ticks (shortbow 4, longbow 6)
        if (weaponName.contains("shortbow"))
        {
            return 4;
        }
        if (weaponName.contains("longbow"))
        {
            return 6;
        }
        if (weaponName.contains("bow"))
        {
            return 5;
        }

        // Crossbows - 6 ticks (rapid 5)
        if (weaponName.contains("crossbow"))
        {
            return 6; // Assume accurate stance
        }

        // Blowpipe - 2 ticks
        if (weaponName.contains("blowpipe"))
        {
            return 2;
        }

        // Ballista - 6 ticks
        if (weaponName.contains("ballista"))
        {
            return 6;
        }

        // Darts, knives - 3 ticks
        if (weaponName.contains("dart") || weaponName.contains("throwing"))
        {
            return 3;
        }

        // Chinchompas - 4 ticks
        if (weaponName.contains("chinchompa"))
        {
            return 4;
        }

        // Magic - most spells are 5 ticks
        if (weaponName.contains("staff") || weaponName.contains("wand"))
        {
            return 5;
        }

        // Trident - 4 ticks
        if (weaponName.contains("trident") || weaponName.contains("sanguinesti"))
        {
            return 4;
        }

        // Harmonised nightmare staff - 4 ticks
        if (weaponName.contains("harmonised"))
        {
            return 4;
        }

        // Default
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
     * Get attack speed adjusted for player's attack style
     * Some stances modify attack speed (rapid vs accurate for ranged)
     */
    public int getAdjustedWeaponSpeed()
    {
        int baseSpeed = getCurrentWeaponSpeed();

        // Check attack style varbit
        int attackStyleVarbit = client.getVarpValue(VarPlayer.ATTACK_STYLE);

        // For ranged weapons, rapid stance reduces speed by 1
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment != null)
        {
            Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
            if (weapon != null)
            {
                ItemComposition itemComp = client.getItemDefinition(weapon.getId());
                String weaponName = itemComp.getName().toLowerCase();

                // Check if it's a ranged weapon and rapid stance is active
                if (isRangedWeapon(weaponName) && attackStyleVarbit == 1)
                {
                    return Math.max(2, baseSpeed - 1); // Rapid stance
                }
            }
        }

        return baseSpeed;
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