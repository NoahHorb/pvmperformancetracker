package net.runelite.client.plugins.pvmperformancetracker.enums;

/**
 * NPC IDs for all bosses with more than one attack style.
 *
 * Used in AttackStyleMapping.initializeCommonBossMappings() to register
 * animations and projectiles against the correct NPC IDs.
 *
 * Values come from the OSRS Wiki / confirmed via in-game NPC id logging.
 * Only bosses with MULTIPLE attack styles belong here — single-style bosses
 * are handled automatically via the database without needing explicit registration.
 *
 * Using an interface (not enum) so constants resolve directly to int without .id
 * e.g. eNPC.VARDORVIS instead of eNPC.VARDORVIS.id
 */
public interface eNPC
{
    // -----------------------------------------------------------------------
    // Vardorvis — 6-tick melee cycle with axe mechanic
    // Post-quest awakened variant has two separate NPC IDs
    // -----------------------------------------------------------------------
    int VARDORVIS                   = 12223;
    int VARDORVIS_2                 = 12224;

    // -----------------------------------------------------------------------
    // Vorkath — rotates between ranged, magic, dragonfire, and melee
    // -----------------------------------------------------------------------
    int VORKATH                     = 8059;
    int VORKATH_DEAD                = 8061;

    // -----------------------------------------------------------------------
    // Zulrah — three rotation phases, each a separate NPC ID
    // -----------------------------------------------------------------------
    int ZULRAH_SERPENTINE           = 2042;  // Green — ranged
    int ZULRAH_MAGMA                = 2043;  // Red — typeless (melee range)
    int ZULRAH_TANZANITE            = 2044;  // Blue — magic + ranged

    // -----------------------------------------------------------------------
    // TzTok-Jad — magic, ranged, melee
    // -----------------------------------------------------------------------
    int TZTOK_JAD                   = 2745;

    // -----------------------------------------------------------------------
    // The Nightmare / Phosani's Nightmare
    // -----------------------------------------------------------------------
    int NIGHTMARE                   = 9425;
    int NIGHTMARE_2                 = 9426;
    int NIGHTMARE_3                 = 9427;
    int PHOSANI_NIGHTMARE           = 9416;
    int PHOSANI_NIGHTMARE_2         = 9417;
    int PHOSANI_NIGHTMARE_3         = 9418;

    // -----------------------------------------------------------------------
    // Duke Sucellus
    // -----------------------------------------------------------------------
    int DUKE_SUCELLUS               = 12191;
    int DUKE_SUCELLUS_AWAKENED      = 12192;

    // -----------------------------------------------------------------------
    // The Leviathan
    // -----------------------------------------------------------------------
    int LEVIATHAN                   = 12215;
    int LEVIATHAN_AWAKENED          = 12216;

    // -----------------------------------------------------------------------
    // The Whisperer
    // -----------------------------------------------------------------------
    int WHISPERER                   = 12218;
    int WHISPERER_AWAKENED          = 12219;

    // -----------------------------------------------------------------------
    // Cerberus
    // -----------------------------------------------------------------------
    int CERBERUS                    = 5862;

    // -----------------------------------------------------------------------
    // Corporeal Beast
    // -----------------------------------------------------------------------
    int CORPOREAL_BEAST             = 319;

    // -----------------------------------------------------------------------
    // General Graardor (Bandos GWD)
    // -----------------------------------------------------------------------
    int GENERAL_GRAARDOR            = 6260;

    // -----------------------------------------------------------------------
    // Commander Zilyana (Saradomin GWD)
    // -----------------------------------------------------------------------
    int COMMANDER_ZILYANA           = 596;

    // -----------------------------------------------------------------------
    // K'ril Tsutsaroth (Zamorak GWD)
    // -----------------------------------------------------------------------
    int KRIL_TSUTSAROTH             = 3129;

    // -----------------------------------------------------------------------
    // Kree'arra (Armadyl GWD)
    // -----------------------------------------------------------------------
    int KREEARRA                    = 3162;

    // -----------------------------------------------------------------------
    // Nex
    // -----------------------------------------------------------------------
    int NEX                         = 11278;
}