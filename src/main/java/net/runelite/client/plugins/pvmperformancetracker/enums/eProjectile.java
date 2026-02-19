package net.runelite.client.plugins.pvmperformancetracker.enums;

/**
 * Projectile IDs for all bosses with more than one attack style.
 *
 * Used in AttackStyleMapping.initializeCommonBossMappings() as the second
 * argument to registerProjectile().
 *
 * Naming convention: BOSSNAME_DESCRIPTION
 * e.g. VORKATH_RANGED, VORKATH_DRAGONFIRE
 *
 * Values are confirmed from in-game observation and the OSRS Wiki.
 * Mark unconfirmed values with a TODO comment so they can be verified.
 *
 * Using an interface (not enum) so constants resolve directly to int without .id
 */
public interface eProjectile
{
    // -----------------------------------------------------------------------
    // Vorkath
    // -----------------------------------------------------------------------
    int VORKATH_RANGED              = 1477;   // Standard ranged attack (blue bolt)
    int VORKATH_MAGIC               = 1479;   // Magic attack (green bolt)
    int VORKATH_DRAGONFIRE          = 1481;   // Dragonfire (orange fireball)
    int VORKATH_DRAGONFIRE_RAPID    = 1482;   // Rapid fire dragonfire (zombie spawn phase)
    int VORKATH_ACID                = 1483;   // Acid pool spawn
    int VORKATH_POISON_BLOB         = 1484;   // Poison blob (zombie spawn)

    // -----------------------------------------------------------------------
    // Zulrah
    // -----------------------------------------------------------------------
    int ZULRAH_RANGED               = 1044;   // Green/tanzanite ranged attack
    int ZULRAH_MAGIC                = 1046;   // Tanzanite magic attack (toxic cloud)
    int ZULRAH_SNAKELINGS           = 1045;   // TODO: confirm — snakeling spawn projectile

    // -----------------------------------------------------------------------
    // TzTok-Jad
    // -----------------------------------------------------------------------
    int JAD_MAGIC                   = 448;    // Magic attack (fireball)
    int JAD_RANGED                  = 451;    // Ranged attack (boulder)

    // -----------------------------------------------------------------------
    // The Nightmare / Phosani's Nightmare
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int NIGHTMARE_RANGED            = 1767;   // TODO: confirm
    int NIGHTMARE_MAGIC             = 1768;   // TODO: confirm

    // -----------------------------------------------------------------------
    // The Leviathan
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int LEVIATHAN_RANGED            = 2447;   // TODO: confirm
    int LEVIATHAN_MAGIC             = 2448;   // TODO: confirm

    // -----------------------------------------------------------------------
    // The Whisperer
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int WHISPERER_MAGIC             = 2490;   // TODO: confirm
    int WHISPERER_RANGED            = 2491;   // TODO: confirm

    // -----------------------------------------------------------------------
    // Cerberus
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int CERBERUS_GHOST_SKULL        = 1242;   // TODO: confirm — ranged/typeless ghost attack

    // -----------------------------------------------------------------------
    // General Graardor
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int GRAARDOR_RANGED             = 1199;   // TODO: confirm

    // -----------------------------------------------------------------------
    // Commander Zilyana
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int ZILYANA_MAGIC               = 1207;   // TODO: confirm

    // -----------------------------------------------------------------------
    // K'ril Tsutsaroth
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int KRIL_MAGIC                  = 1203;   // TODO: confirm

    // -----------------------------------------------------------------------
    // Kree'arra
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int KREEARRA_RANGED             = 1183;   // TODO: confirm — wind attack
    int KREEARRA_MAGIC              = 1184;   // TODO: confirm — lightning
    int KREEARRA_MELEE_PROJ         = 1185;   // TODO: confirm — close range

    // -----------------------------------------------------------------------
    // Nex
    // TODO: confirm projectile IDs from logs
    // -----------------------------------------------------------------------
    int NEX_RANGED                  = 1242;   // TODO: confirm
    int NEX_MAGIC                   = 1243;   // TODO: confirm
}