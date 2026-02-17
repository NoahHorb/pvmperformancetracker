package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.client.plugins.pvmperformancetracker.models.PlayerAttackStyle;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for determining player attack styles and weapon speeds from animation IDs
 * Consolidates functionality from AnimationIds and WeaponSpeedHelper
 */
@Slf4j
public class PlayerAttackStyleHelper
{
    // Animation ID to attack style mappings
    private static final Map<Integer, String> ANIMATION_STYLES = new HashMap<>();

    // Animation ID to weapon speed mappings (in ticks)
    private static final Map<Integer, Integer> ANIMATION_SPEEDS = new HashMap<>();

    // Special weapon speed overrides
    private static final Map<Integer, Integer> WEAPON_SPEED_OVERRIDES = new HashMap<>();

    static
    {
        initializeAnimationMappings();
        initializeWeaponSpeedOverrides();
    }

    /**
     * Get complete attack information from an animation and weapon
     *
     * @param animationId The player's attack animation ID
     * @param weaponId The equipped weapon ID (null if no weapon)
     * @return PlayerAttackStyle with style and speed information
     */
    public static PlayerAttackStyle getAttackStyle(int animationId, Integer weaponId)
    {
        String style = getAttackStyleFromAnimation(animationId, weaponId);
        int weaponSpeed = getWeaponSpeed(animationId, weaponId);

        return PlayerAttackStyle.fromAnimation(animationId, weaponId, style, weaponSpeed);
    }

    /**
     * Get attack style from animation ID
     */
    private static String getAttackStyleFromAnimation(int animationId, Integer weaponId)
    {
        // Check for direct animation mapping first
        String style = ANIMATION_STYLES.get(animationId);
        if (style != null)
        {
            return style;
        }

        // Determine melee style from animation if not in map
        return determineMeleeStyle(animationId, weaponId);
    }

    /**
     * Get weapon speed in ticks
     */
    private static int getWeaponSpeed(int animationId, Integer weaponId)
    {
        // Check weapon-specific overrides first
        if (weaponId != null && WEAPON_SPEED_OVERRIDES.containsKey(weaponId))
        {
            return WEAPON_SPEED_OVERRIDES.get(weaponId);
        }

        // Check animation-specific speed
        Integer speed = ANIMATION_SPEEDS.get(animationId);
        if (speed != null)
        {
            return speed;
        }

        // Default to 4 ticks for unknown weapons
        return 4;
    }

    /**
     * Determine specific melee attack style (slash/stab/crush) from animation
     */
    private static String determineMeleeStyle(int animationId, Integer weaponId)
    {
        // Weapon-specific melee styles
        if (weaponId != null)
        {
            // Scythe of Vitur - always slash
            if (weaponId == ItemID.SCYTHE_OF_VITUR ||
                    weaponId == ItemID.SCYTHE_OF_VITUR_UNCHARGED ||
                    weaponId == ItemID.HOLY_SCYTHE_OF_VITUR ||
                    weaponId == ItemID.HOLY_SCYTHE_OF_VITUR_UNCHARGED ||
                    weaponId == ItemID.SANGUINE_SCYTHE_OF_VITUR ||
                    weaponId == ItemID.SANGUINE_SCYTHE_OF_VITUR_UNCHARGED)
            {
                return "slash";
            }

            // Inquisitor's mace - always crush
            if (weaponId == ItemID.INQUISITORS_MACE)
            {
                return "crush";
            }

            // Keris variants - always stab
            if (weaponId == ItemID.KERIS ||
                    weaponId == ItemID.KERIS_PARTISAN_OF_BREACHING ||
                    weaponId == ItemID.KERIS_PARTISAN_OF_THE_SUN)
            {
                return "stab";
            }
        }

        // Analyze animation pattern
        switch (animationId)
        {
            // Slash animations
            case 390:  // Dragon scimitar slash
            case 440:  // Godsword slash
            case 386:  // Abyssal whip
            case 1658: // Abyssal tentacle
            case 8056: // Scythe slash
            case 7004: // Fang slash
            case 2078: // Saradomin sword
                return "slash";

            // Stab animations
            case 381:  // Dragon dagger stab
            case 400:  // Dragon longsword stab
            case 8145: // Zamorakian hasta
            case 428:  // Dragon spear
            case 8288: // Dragon hunter lance
                return "stab";

            // Crush animations
            case 401:  // Dragon mace
            case 406:  // Dragon battleaxe
            case 395:  // Dragon 2h
            case 7045: // Inquisitor's mace
            case 4503: // Granite maul
            case 1665: // Godsword smash
                return "crush";

            default:
                // Default to slash for unknown melee animations
                return "slash";
        }
    }

    /**
     * Initialize all animation to style mappings
     */
    private static void initializeAnimationMappings()
    {
        // === MELEE ANIMATIONS ===

        // Slash
        ANIMATION_STYLES.put(390, "slash");   // Dragon scimitar
        ANIMATION_STYLES.put(440, "slash");   // Godsword slash
        ANIMATION_STYLES.put(386, "slash");   // Abyssal whip
        ANIMATION_STYLES.put(1658, "slash");  // Abyssal tentacle
        ANIMATION_STYLES.put(8056, "slash");  // Scythe of vitur
        ANIMATION_STYLES.put(7004, "slash");  // Osmumten's fang slash
        ANIMATION_STYLES.put(2078, "slash");  // Saradomin sword
        ANIMATION_STYLES.put(7514, "slash");  // Soulreaper axe slash
        ANIMATION_STYLES.put(10171, "slash"); // Tonalztics slash

        // Stab
        ANIMATION_STYLES.put(381, "stab");    // Dragon dagger
        ANIMATION_STYLES.put(400, "stab");    // Dragon longsword stab
        ANIMATION_STYLES.put(8145, "stab");   // Zamorakian hasta
        ANIMATION_STYLES.put(428, "stab");    // Dragon spear
        ANIMATION_STYLES.put(8288, "stab");   // Dragon hunter lance
        ANIMATION_STYLES.put(9544, "stab");   // Voidwaker stab
        ANIMATION_STYLES.put(7005, "stab");   // Osmumten's fang stab

        // Crush
        ANIMATION_STYLES.put(401, "crush");   // Dragon mace
        ANIMATION_STYLES.put(406, "crush");   // Dragon battleaxe
        ANIMATION_STYLES.put(395, "crush");   // Dragon 2h
        ANIMATION_STYLES.put(7045, "crush");  // Inquisitor's mace
        ANIMATION_STYLES.put(4503, "crush");  // Granite maul
        ANIMATION_STYLES.put(1665, "crush");  // Godsword smash
        ANIMATION_STYLES.put(7516, "crush");  // Soulreaper axe crush
        ANIMATION_STYLES.put(245, "crush");   // Warhammer

        // === RANGED ANIMATIONS ===
        ANIMATION_STYLES.put(426, "ranged");  // Bow
        ANIMATION_STYLES.put(7552, "ranged"); // Twisted bow
        ANIMATION_STYLES.put(7617, "ranged"); // Bow of faerdhinen
        ANIMATION_STYLES.put(9206, "ranged"); // Zaryte crossbow
        ANIMATION_STYLES.put(4230, "ranged"); // Crossbow
        ANIMATION_STYLES.put(7555, "ranged"); // Blowpipe
        ANIMATION_STYLES.put(10656, "ranged"); // Blazing blowpipe
        ANIMATION_STYLES.put(5061, "ranged"); // Chinchompa
        ANIMATION_STYLES.put(8267, "ranged"); // Ballista

        // === MAGIC ANIMATIONS ===
        ANIMATION_STYLES.put(1162, "magic");  // Standard spellbook cast
        ANIMATION_STYLES.put(1978, "magic");  // Ancient spellbook cast
        ANIMATION_STYLES.put(7855, "magic");  // Sanguinesti staff
        ANIMATION_STYLES.put(9487, "magic");  // Tumeken's shadow
        ANIMATION_STYLES.put(1167, "magic");  // Trident of the seas/swamp
        ANIMATION_STYLES.put(8532, "magic");  // Dawnbringer
        ANIMATION_STYLES.put(7617, "magic");  // Harmonised nightmare staff
    }

    /**
     * Initialize weapon speed mappings
     */
    private static void initializeWeaponSpeedOverrides()
    {
        // === 3-TICK WEAPONS ===
        // None currently mapped

        // === 4-TICK WEAPONS (most common) ===
        WEAPON_SPEED_OVERRIDES.put(ItemID.ABYSSAL_WHIP, 4);
        WEAPON_SPEED_OVERRIDES.put(ItemID.ABYSSAL_TENTACLE, 4);
        WEAPON_SPEED_OVERRIDES.put(ItemID.DRAGON_SCIMITAR, 4);
        WEAPON_SPEED_OVERRIDES.put(ItemID.OSMUMTENS_FANG, 4);

        // === 5-TICK WEAPONS ===
        // Godswords
        WEAPON_SPEED_OVERRIDES.put(ItemID.ARMADYL_GODSWORD, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.BANDOS_GODSWORD, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.SARADOMIN_GODSWORD, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.ZAMORAK_GODSWORD, 5);

        // Halberds
        WEAPON_SPEED_OVERRIDES.put(ItemID.DRAGON_HUNTER_LANCE, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.ZAMORAKIAN_HASTA, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.NOXIOUS_HALBERD, 5);

        // Scythe
        WEAPON_SPEED_OVERRIDES.put(ItemID.SCYTHE_OF_VITUR, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.SCYTHE_OF_VITUR_UNCHARGED, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.HOLY_SCYTHE_OF_VITUR, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.HOLY_SCYTHE_OF_VITUR_UNCHARGED, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.SANGUINE_SCYTHE_OF_VITUR, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.SANGUINE_SCYTHE_OF_VITUR_UNCHARGED, 5);

        // === 6-TICK WEAPONS ===
        WEAPON_SPEED_OVERRIDES.put(ItemID.DRAGON_2H_SWORD, 6);
        WEAPON_SPEED_OVERRIDES.put(ItemID.ELDER_MAUL, 6);
        WEAPON_SPEED_OVERRIDES.put(ItemID.INQUISITORS_MACE, 6);

        // === RANGED SPEEDS ===
        WEAPON_SPEED_OVERRIDES.put(ItemID.TWISTED_BOW, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.BOW_OF_FAERDHINEN, 4);
        WEAPON_SPEED_OVERRIDES.put(ItemID.BOW_OF_FAERDHINEN_C, 4);
        WEAPON_SPEED_OVERRIDES.put(ItemID.TOXIC_BLOWPIPE, 2);
        WEAPON_SPEED_OVERRIDES.put(ItemID.BLAZING_BLOWPIPE, 2);
        WEAPON_SPEED_OVERRIDES.put(ItemID.ZARYTE_CROSSBOW, 5);

        // === MAGIC SPEEDS ===
        WEAPON_SPEED_OVERRIDES.put(ItemID.TUMEKENS_SHADOW, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.TUMEKENS_SHADOW_UNCHARGED, 5);
        WEAPON_SPEED_OVERRIDES.put(ItemID.SANGUINESTI_STAFF, 4);
        WEAPON_SPEED_OVERRIDES.put(ItemID.SANGUINESTI_STAFF_UNCHARGED, 4);
        WEAPON_SPEED_OVERRIDES.put(ItemID.HOLY_SANGUINESTI_STAFF, 4);
        WEAPON_SPEED_OVERRIDES.put(ItemID.HOLY_SANGUINESTI_STAFF_UNCHARGED, 4);
    }

    /**
     * Get weapon speed from animation alone (for backwards compatibility)
     */
    public static int getWeaponSpeedTicks(int animationId)
    {
        Integer speed = ANIMATION_SPEEDS.get(animationId);
        if (speed != null)
        {
            return speed;
        }

        // Default to 4 ticks
        return 4;
    }

    /**
     * Initialize animation-specific speeds
     * These are populated based on common animation patterns
     */
    static
    {
        // 4-tick animations (most common)
        ANIMATION_SPEEDS.put(390, 4);  // Dragon scimitar
        ANIMATION_SPEEDS.put(386, 4);  // Whip
        ANIMATION_SPEEDS.put(1658, 4); // Tentacle
        ANIMATION_SPEEDS.put(7004, 4); // Fang
        ANIMATION_SPEEDS.put(7005, 4); // Fang stab

        // 5-tick animations
        ANIMATION_SPEEDS.put(440, 5);  // Godsword
        ANIMATION_SPEEDS.put(1665, 5); // Godsword smash
        ANIMATION_SPEEDS.put(8056, 5); // Scythe
        ANIMATION_SPEEDS.put(8145, 5); // Hasta
        ANIMATION_SPEEDS.put(8288, 5); // Lance

        // 6-tick animations
        ANIMATION_SPEEDS.put(395, 6);  // 2h sword
        ANIMATION_SPEEDS.put(7045, 6); // Inquisitor's mace
        ANIMATION_SPEEDS.put(406, 6);  // Battleaxe

        // Ranged
        ANIMATION_SPEEDS.put(426, 5);  // Regular bow
        ANIMATION_SPEEDS.put(7552, 5); // Twisted bow
        ANIMATION_SPEEDS.put(7617, 4); // Bow of faerdhinen
        ANIMATION_SPEEDS.put(7555, 2); // Blowpipe
        ANIMATION_SPEEDS.put(10656, 2); // Blazing blowpipe

        // Magic
        ANIMATION_SPEEDS.put(1162, 5); // Standard cast
        ANIMATION_SPEEDS.put(1978, 5); // Ancient cast
        ANIMATION_SPEEDS.put(7855, 4); // Sang staff
        ANIMATION_SPEEDS.put(9487, 5); // Tumeken's shadow
    }
}