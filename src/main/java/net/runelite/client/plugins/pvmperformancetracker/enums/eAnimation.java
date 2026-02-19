package net.runelite.client.plugins.pvmperformancetracker.enums;

/**
 * Animation IDs for all bosses with more than one attack style.
 *
 * Used in AttackStyleMapping.initializeCommonBossMappings() as the second
 * argument to registerDirectAttack() and registerMechanic().
 *
 * Naming convention: BOSSNAME_DESCRIPTION
 * e.g. VARDORVIS_SLASH, VARDORVIS_AXE_SPAWN
 *
 * Values are confirmed from in-game observation and logs.
 * Mark unconfirmed values with a TODO comment so they can be verified.
 *
 * Using an interface (not enum) so constants resolve directly to int without .id
 */
public interface eAnimation
{
    // -----------------------------------------------------------------------
    // Vardorvis
    // Confirmed from logs and wiki
    // -----------------------------------------------------------------------
    int VARDORVIS_SLASH             = 10340;  // Direct melee slash, hitsplat +1 tick
    int VARDORVIS_DASH              = 10342;  // Dashing melee attack, hitsplat +1 tick
    int VARDORVIS_AXE_SPAWN         = 10341;  // Spawns travelling axes (mechanic, non-schedulable)

    // -----------------------------------------------------------------------
    // Vorkath
    // -----------------------------------------------------------------------
    int VORKATH_MELEE               = 7948;   // Melee swipe, hitsplat +1 tick
    // Note: all other Vorkath attacks are projectile-based — see eProjectile

    // -----------------------------------------------------------------------
    // TzTok-Jad
    // -----------------------------------------------------------------------
    int JAD_MELEE                   = 2656;   // Melee stomp, hitsplat +1 tick
    // Note: Jad ranged and magic are projectile-based — see eProjectile

    // -----------------------------------------------------------------------
    // The Nightmare / Phosani's Nightmare
    // TODO: confirm animation IDs from logs
    // -----------------------------------------------------------------------
    int NIGHTMARE_MELEE             = 8609;   // TODO: confirm
    int NIGHTMARE_RANGED            = 8612;   // TODO: confirm
    int NIGHTMARE_MAGIC             = 8611;   // TODO: confirm

    // -----------------------------------------------------------------------
    // Duke Sucellus
    // TODO: confirm animation IDs from logs
    // -----------------------------------------------------------------------
    int DUKE_MELEE                  = 10077;  // TODO: confirm
    int DUKE_MAGIC                  = 10082;  // TODO: confirm
    int DUKE_POISON_SPAWN           = 10083;  // TODO: confirm — mechanic

    // -----------------------------------------------------------------------
    // The Leviathan
    // TODO: confirm animation IDs from logs
    // -----------------------------------------------------------------------
    int LEVIATHAN_RANGED            = 10070;  // TODO: confirm
    int LEVIATHAN_MAGIC             = 10071;  // TODO: confirm
    int LEVIATHAN_ROCK_SPAWN        = 10072;  // TODO: confirm — mechanic

    // -----------------------------------------------------------------------
    // The Whisperer
    // TODO: confirm animation IDs from logs
    // -----------------------------------------------------------------------
    int WHISPERER_MAGIC             = 10091;  // TODO: confirm
    int WHISPERER_RANGED            = 10092;  // TODO: confirm

    // -----------------------------------------------------------------------
    // Cerberus
    // TODO: confirm animation IDs from logs
    // -----------------------------------------------------------------------
    int CERBERUS_MELEE              = 4489;   // TODO: confirm
    int CERBERUS_RANGED             = 4490;   // TODO: confirm — ghost skulls
    int CERBERUS_MAGIC              = 4491;   // TODO: confirm — lava pools

    // -----------------------------------------------------------------------
    // General Graardor
    // -----------------------------------------------------------------------
    int GRAARDOR_MELEE              = 7070;   // TODO: confirm
    int GRAARDOR_RANGED             = 7071;   // TODO: confirm — uses ranged projectile

    // -----------------------------------------------------------------------
    // Commander Zilyana
    // -----------------------------------------------------------------------
    int ZILYANA_MELEE               = 6964;   // TODO: confirm
    int ZILYANA_MAGIC               = 6967;   // TODO: confirm

    // -----------------------------------------------------------------------
    // K'ril Tsutsaroth
    // -----------------------------------------------------------------------
    int KRIL_MELEE                  = 6948;   // TODO: confirm
    int KRIL_MAGIC                  = 6946;   // TODO: confirm

    // -----------------------------------------------------------------------
    // Kree'arra
    // All attacks are projectile-based — animation is the same for all
    // -----------------------------------------------------------------------
    int KREEARRA_ATTACK             = 6979;   // TODO: confirm

    // -----------------------------------------------------------------------
    // Nex
    // TODO: confirm animation IDs from logs
    // -----------------------------------------------------------------------
    int NEX_MELEE                   = 9168;   // TODO: confirm
    int NEX_RANGED                  = 9170;   // TODO: confirm
    int NEX_MAGIC                   = 9169;   // TODO: confirm
    int NEX_BLOOD                   = 9171;   // TODO: confirm — Blood Sacrifice mechanic
}