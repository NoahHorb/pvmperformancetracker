package net.runelite.client.plugins.pvmperformancetracker;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.RuneLite;
import net.runelite.client.plugins.pvmperformancetracker.helpers.*;
import net.runelite.client.plugins.pvmperformancetracker.listeners.*;
import net.runelite.client.plugins.pvmperformancetracker.party.PartyStatsManager;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
		name = "PvM Performance Tracker",
		description = "Track your combat performance and DPS in PvM encounters with party-wide analytics",
		tags = {"combat", "pvm", "dps", "damage", "performance", "tracker", "party", "raid"}
)
public class PvMPerformanceTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PvMPerformanceTrackerConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PvMPerformanceTrackerOverlay overlay;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private PartyService partyService;

	@Getter
	private PvMPerformanceTrackerPanel panel;

	private NavigationButton navigationButton;

	// Core Managers and Helpers
	@Getter
	private FightTracker fightTracker;

	@Getter
	private DamageCalculator damageCalculator;

	@Getter
	private DamageClassifier damageClassifier;

	@Getter
	private WeaponSpeedHelper weaponSpeedHelper;

	@Getter
	private BossDetectionHelper bossDetectionHelper;

	@Getter
	private PartyStatsManager partyStatsManager;

	@Getter
	private NpcStatsProvider npcStatsProvider;

	@Getter
	private CombatFormulas combatFormulas;

	// Listeners
	private HitsplatListener hitsplatListener;
	private AnimationListener animationListener;

	@Getter
	private CombatEventListener combatEventListener;

	@Override
	protected void startUp() throws Exception
	{
		log.info("PvM Performance Tracker started!");

		// Initialize managers and helpers
		fightTracker = new FightTracker(this, client);
		damageCalculator = new DamageCalculator(client);
		damageClassifier = new DamageClassifier(client);
		weaponSpeedHelper = new WeaponSpeedHelper(client);
		bossDetectionHelper = new BossDetectionHelper();
		partyStatsManager = new PartyStatsManager(this, client, partyService);

		// Initialize NPC stats provider (async to avoid blocking startup)
		npcStatsProvider = new NpcStatsProvider(RuneLite.RUNELITE_DIR);
		combatFormulas = new CombatFormulas(client);

		// Load NPC database in background
		new Thread(() -> {
			try {
				npcStatsProvider.initialize();
			} catch (Exception e) {
				log.error("Failed to initialize NPC stats provider", e);
			}
		}, "NPC-Stats-Loader").start();

		// Initialize listeners
		hitsplatListener = new HitsplatListener(this);
		animationListener = new AnimationListener(this);
		combatEventListener = new CombatEventListener(this);

		// Add overlay
		overlayManager.add(overlay);

		// Setup panel
		panel = new PvMPerformanceTrackerPanel(this);

		// Create panel icon (placeholder - replace with actual icon)
		final BufferedImage icon = createPlaceholderIcon();

		navigationButton = NavigationButton.builder()
				.tooltip("PvM Performance Tracker")
				.icon(icon)
				.priority(7)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navigationButton);

		// Update party members
		partyStatsManager.updatePartyMembers();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("PvM Performance Tracker stopped!");

		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navigationButton);

		// End active fight
		if (fightTracker != null && fightTracker.hasActiveFight())
		{
			fightTracker.endCurrentFight();
		}

		// Reset overall if configured
		if (config.autoResetOverallOnLogout() && fightTracker != null)
		{
			fightTracker.resetOverallTracking();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (fightTracker != null)
		{
			fightTracker.onGameTick();
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (hitsplatListener != null)
		{
			hitsplatListener.onHitsplatApplied(event);
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (animationListener != null)
		{
			animationListener.onAnimationChanged(event);
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (combatEventListener != null)
		{
			combatEventListener.onActorDeath(event);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// Player logged in - update party members
			if (partyStatsManager != null)
			{
				partyStatsManager.updatePartyMembers();
			}
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// End fight on logout
			if (fightTracker != null && fightTracker.hasActiveFight())
			{
				fightTracker.endCurrentFight();
			}

			// Reset overall if configured
			if (config.autoResetOverallOnLogout() && fightTracker != null)
			{
				fightTracker.resetOverallTracking();
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("pvmperformancetracker"))
		{
			return;
		}

		// Update party members when party tracking is toggled
		if (event.getKey().equals("enablePartyTracking"))
		{
			if (partyStatsManager != null)
			{
				partyStatsManager.updatePartyMembers();
			}
		}

		// Update panel
		if (panel != null)
		{
			panel.updatePanel();
		}
	}

	@Provides
	PvMPerformanceTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvMPerformanceTrackerConfig.class);
	}

	public Client getClient()
	{
		return client;
	}

	public PvMPerformanceTrackerConfig getConfig()
	{
		return config;
	}

	/**
	 * Create a placeholder icon (16x16)
	 * Replace with actual icon file later
	 */
	private BufferedImage createPlaceholderIcon()
	{
		try
		{
			// Try to load actual icon
			return ImageUtil.loadImageResource(getClass(), "/panel_icon.png");
		}
		catch (Exception e)
		{
			// Create placeholder
			BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = icon.createGraphics();
			g.setColor(java.awt.Color.GREEN);
			g.fillRect(0, 0, 16, 16);
			g.setColor(java.awt.Color.WHITE);
			g.drawString("P", 4, 12);
			g.dispose();
			return icon;
		}
	}
}