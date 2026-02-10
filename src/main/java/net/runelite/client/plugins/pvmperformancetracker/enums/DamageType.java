package net.runelite.client.plugins.pvmperformancetracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Classification of damage types for defensive metrics
 */
@Getter
@RequiredArgsConstructor
public enum DamageType
{
    /**
     * Damage that could have been avoided by movement or correct positioning
     * Examples: Standing in fire, failing to dodge mechanics
     */
    AVOIDABLE("Avoidable"),

    /**
     * Damage that could have been negated or reduced by correct protection prayers
     * Examples: Not using correct prayer against typed attacks
     */
    PRAYABLE("Prayable"),

    /**
     * Damage that cannot reasonably be prevented
     * Examples: Guaranteed chip damage, environmental DoTs
     */
    UNAVOIDABLE("Unavoidable"),

    /**
     * Damage type not yet classified
     */
    UNKNOWN("Unknown");

    private final String displayName;
}