package net.runelite.client.plugins.pvmperformancetracker;

import net.runelite.client.config.*;
import net.runelite.client.plugins.pvmperformancetracker.enums.TrackingMode;

import java.awt.Color;
//TODO - add preset overlay options - i.e hcim mode (shows chance of % total chances)
@ConfigGroup("pvmperformancetracker")
public interface PvMPerformanceTrackerConfig extends Config
{
	@ConfigSection(
			name = "General Settings",
			description = "General plugin settings",
			position = 0
	)
	String generalSection = "general";

	@ConfigSection(
			name = "Overlay Settings",
			description = "Overlay display settings",
			position = 1
	)
	String overlaySection = "overlay";

	@ConfigSection(
			name = "Party Tracking",
			description = "Party-wide tracking settings",
			position = 2
	)
	String partySection = "party";

	@ConfigSection(
			name = "Combat Detection",
			description = "Combat session detection settings",
			position = 3
	)
	String combatSection = "combat";

	// General Settings
	@ConfigItem(
			keyName = "enablePlugin",
			name = "Enable Tracking",
			description = "Enable or disable combat tracking",
			position = 0,
			section = generalSection
	)
	default boolean enableTracking()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackingMode",
			name = "Tracking Mode",
			description = "Current Fight (resets per boss) or Overall (manual reset)",
			position = 1,
			section = generalSection
	)
	default TrackingMode trackingMode()
	{
		return TrackingMode.CURRENT_FIGHT;
	}

	@ConfigItem(
			keyName = "trackBossesOnly",
			name = "Track Bosses Only",
			description = "Only track combat against boss monsters",
			position = 2,
			section = generalSection
	)
	default boolean trackBossesOnly()
	{
		return false;
	}

	@ConfigItem(
			keyName = "autoStartCombat",
			name = "Auto Start Sessions",
			description = "Automatically start tracking when entering combat",
			position = 3,
			section = generalSection
	)
	default boolean autoStartCombat()
	{
		return true;
	}

	// Overlay Settings
	@ConfigItem(
			keyName = "showOverlay",
			name = "Show Overlay",
			description = "Display the performance overlay",
			position = 0,
			section = overlaySection
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
			keyName = "overlayMetric1",
			name = "Overlay Metric 1",
			description = "First metric to display in overlay",
			position = 1,
			section = overlaySection
	)
	default OverlayMetric overlayMetric1()
	{
		return OverlayMetric.DPS;
	}

	@ConfigItem(
			keyName = "overlayMetric2",
			name = "Overlay Metric 2",
			description = "Second metric to display in overlay",
			position = 2,
			section = overlaySection
	)
	default OverlayMetric overlayMetric2()
	{
		return OverlayMetric.TICKS_LOST;
	}

	@ConfigItem(
			keyName = "highlightAvoidableDamage",
			name = "Highlight Avoidable Damage",
			description = "Highlight players with high avoidable damage taken",
			position = 3,
			section = overlaySection
	)
	default boolean highlightAvoidableDamage()
	{
		return false;
	}

	@ConfigItem(
			keyName = "avoidableDamageThreshold",
			name = "Avoidable Damage Threshold",
			description = "Damage threshold for highlighting (if enabled)",
			position = 4,
			section = overlaySection
	)
	@Range(min = 50, max = 1000)
	default int avoidableDamageThreshold()
	{
		return 200;
	}

	// Party Tracking Settings
	@ConfigItem(
			keyName = "enablePartyTracking",
			name = "Enable Party Tracking",
			description = "Track performance of party members",
			position = 0,
			section = partySection
	)
	default boolean enablePartyTracking()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showEstimatedLabel",
			name = "Show 'Estimated' Label",
			description = "Label party member stats as estimated",
			position = 1,
			section = partySection
	)
	default boolean showEstimatedLabel()
	{
		return true;
	}

	// Combat Detection Settings
	@ConfigItem(
			keyName = "endOnBossDeath",
			name = "End on Boss Death",
			description = "End current fight when boss dies",
			position = 0,
			section = combatSection
	)
	default boolean endOnBossDeath()
	{
		return true;
	}

	@ConfigItem(
			keyName = "minimumFightTime",
			name = "Minimum Fight Time (ticks)",
			description = "Minimum fight duration to save (0 = save all)",
			position = 1,
			section = combatSection
	)
	@Range(min = 0, max = 600)
	default int minimumFightTime()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "maxSessionHistory",
			name = "Max Fight History",
			description = "Maximum number of past fights to keep",
			position = 2,
			section = combatSection
	)
	@Range(min = 10, max = 1000)
	default int maxSessionHistory()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "autoResetOverallOnLogout",
			name = "Auto Reset Overall on Logout",
			description = "Automatically reset Overall mode when logging out",
			position = 3,
			section = combatSection
	)
	default boolean autoResetOverallOnLogout()
	{
		return false;
	}

	/**
	 * Overlay metric options
	 */
	enum OverlayMetric
	{
		DPS("DPS"),
		DAMAGE("Total Damage"),
		TICKS_LOST("Ticks Lost"),
		EXPECTED_DPS("Expected DPS"),
		EXPECTED_DAMAGE("Expected Damage"),
		ACCURACY("Accuracy %"),
		DAMAGE_TAKEN("Damage Taken"),
		AVOIDABLE_DAMAGE("Avoidable Damage"),
		PRAYABLE_DAMAGE("Prayable Damage"),
		UNAVOIDABLE_DAMAGE("Unavoidable Damage"),
		CHANCES_OF_DEATH("Death Chances"),
		DEATH_CHANCE_PERCENT("Death Chance %");

		private final String displayName;

		OverlayMetric(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}
}