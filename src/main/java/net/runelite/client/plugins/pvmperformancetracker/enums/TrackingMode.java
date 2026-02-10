package net.runelite.client.plugins.pvmperformancetracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tracking mode for the plugin
 */
@Getter
@RequiredArgsConstructor
public enum TrackingMode
{
    /**
     * Track a single fight (resets on boss death)
     */
    CURRENT_FIGHT("Current Fight"),

    /**
     * Track overall stats across multiple fights (manual reset only)
     */
    OVERALL("Overall");

    private final String displayName;
}