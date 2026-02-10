package net.runelite.client.plugins.pvmperformancetracker;

import net.runelite.client.config.*;

import java.awt.Color;

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
			name = "Combat Detection",
			description = "Combat session detection settings",
			position = 2
	)
	String combatSection = "combat";

	@ConfigSection(
			name = "Statistics",
			description = "Statistics tracking settings",
			position = 3
	)
	String statsSection = "statistics";

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
			keyName = "trackAllCombat",
			name = "Track All Combat",
			description = "Track all combat encounters, not just bosses",
			position = 1,
			section = generalSection
	)
	default boolean trackAllCombat()
	{
		return true;
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
			keyName = "overlayPosition",
			name = "Overlay Position",
			description = "Position of the overlay on screen",
			position = 1,
			section = overlaySection
	)
	default OverlayPosition overlayPosition()
	{
		return OverlayPosition.TOP_RIGHT;
	}

	@ConfigItem(
			keyName = "showDPS",
			name = "Show DPS",
			description = "Display current DPS in overlay",
			position = 2,
			section = overlaySection
	)
	default boolean showDPS()
	{
		return true;
	}

	@ConfigItem(
			keyName ="showTotalDamage",
			name = "Show Total Damage",
			description = "Display total damage dealt in overlay",
			position = 3,
			section = overlaySection
	)

	default boolean showTotalDamage()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showAccuracy",
			name = "Show Accuracy",
			description = "Display hit accuracy percentage in overlay",
			position = 4,
			section = overlaySection
	)
	default boolean showAccuracy()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showMaxHit",
			name = "Show Max Hit",
			description = "Display maximum hit in overlay",
			position = 5,
			section = overlaySection
	)
	default boolean showMaxHit()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showCombatTime",
			name = "Show Combat Time",
			description = "Display active combat time in overlay",
			position = 6,
			section = overlaySection
	)
	default boolean showCombatTime()
	{
		return true;
	}

	@ConfigItem(
			keyName = "overlayColor",
			name = "Overlay Color",
			description = "Color of the overlay text",
			position = 7,
			section = overlaySection
	)
	default Color overlayColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
			keyName = "overlayBackgroundColor",
			name = "Background Color",
			description = "Background color of the overlay",
			position = 8,
			section = overlaySection
	)
	default Color overlayBackgroundColor()
	{
		return new Color(0, 0, 0, 128);
	}

	// Combat Detection Settings
	@ConfigItem(
			keyName = "combatTimeout",
			name = "Combat Timeout (seconds)",
			description = "Seconds of inactivity before ending combat session",
			position = 0,
			section = combatSection
	)
	@Range(min = 5, max = 300)
	default int combatTimeout()
	{
		return 30;
	}

	@ConfigItem(
			keyName = "minimumCombatTime",
			name = "Minimum Session Time (seconds)",
			description = "Minimum combat duration to save session",
			position = 1,
			section = combatSection
	)
	@Range(min = 1, max = 60)
	default int minimumCombatTime()
	{
		return 5;
	}

	@ConfigItem(
			keyName = "endOnDeath",
			name = "End on Death",
			description = "End combat session when you die",
			position = 2,
			section = combatSection
	)
	default boolean endOnDeath()
	{
		return true;
	}

	@ConfigItem(
			keyName = "endOnBossDeath",
			name = "End on Boss Death",
			description = "End combat session when boss dies",
			position = 3,
			section = combatSection
	)
	default boolean endOnBossDeath()
	{
		return true;
	}

	// Statistics Settings
	@ConfigItem(
			keyName = "trackByWeapon",
			name = "Track by Weapon",
			description = "Break down statistics by weapon type",
			position = 0,
			section = statsSection
	)
	default boolean trackByWeapon()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackByAttackStyle",
			name = "Track by Attack Style",
			description = "Break down statistics by attack style (melee/range/magic)",
			position = 1,
			section = statsSection
	)
	default boolean trackByAttackStyle()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackByEnemy",
			name = "Track by Enemy",
			description = "Break down statistics by enemy type",
			position = 2,
			section = statsSection
	)
	default boolean trackByEnemy()
	{
		return true;
	}

	@ConfigItem(
			keyName = "maxSessionHistory",
			name = "Max Session History",
			description = "Maximum number of past sessions to keep",
			position = 3,
			section = statsSection
	)
	@Range(min = 10, max = 1000)
	default int maxSessionHistory()
	{
		return 100;
	}

	@ConfigItem(
			keyName = "showDamageSplits",
			name = "Show Damage Splits",
			description = "Show detailed damage breakdowns by ability/spell",
			position = 4,
			section = statsSection
	)
	default boolean showDamageSplits()
	{
		return true;
	}

	enum OverlayPosition
	{
		TOP_LEFT,
		TOP_CENTER,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_CENTER,
		BOTTOM_RIGHT
	}
}