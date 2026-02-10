package net.runelite.client.plugins.pvmperformancetracker;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.plugins.pvmperformancetracker.helpers.*;
import net.runelite.client.plugins.pvmperformancetracker.listeners.*;
import net.runelite.client.plugins.pvmperformancetracker.model.*;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
		name = "PvM Performance Tracker",
		description = "Track your combat performance and DPS in PvM encounters",
		tags = {"combat", "pvm", "dps", "damage", "performance", "tracker"}
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

	@Getter
	private PvMPerformanceTrackerPanel panel;

	private NavigationButton navigationButton;

	// Managers and Helpers
	@Getter
	private CombatSessionManager sessionManager;

	@Getter
	private DamageCalculator damageCalculator;

	@Getter
	private CombatStatisticsHelper statisticsHelper;

	@Getter
	private BossDetectionHelper bossDetectionHelper;

	// Listeners
	private CombatEventListener combatEventListener;
	private HitsplatListener hitsplatListener;
	private AnimationListener animationListener;
	private ChatMessageListener chatMessageListener;

	@Override
	protected void startUp() throws Exception
	{
		log.info("PvM Performance Tracker started!");

		// Initialize managers and helpers
		sessionManager = new CombatSessionManager(this);
		damageCalculator = new DamageCalculator(client);
		statisticsHelper = new CombatStatisticsHelper();
		bossDetectionHelper = new BossDetectionHelper();

		// Initialize listeners
		combatEventListener = new CombatEventListener(this);
		hitsplatListener = new HitsplatListener(this);
		animationListener = new AnimationListener(this);
		chatMessageListener = new ChatMessageListener(this);

		// Add overlay
		overlayManager.add(overlay);

		// Setup panel
		panel = new PvMPerformanceTrackerPanel(this);

		//final BufferedImage icon = ImageUtil.loadImageResource(.class, "/util/combat_icon.png");
		final BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setColor(Color.GREEN);
		g.fillRect(0, 0, 16, 16);
		g.dispose();

		navigationButton = NavigationButton.builder()
				.tooltip("PvM Performance Tracker")
				.icon(icon)
				.priority(7)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navigationButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("PvM Performance Tracker stopped!");

		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(navigationButton);

		// Clean up sessions
		if (sessionManager != null)
		{
			sessionManager.endCurrentSession();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (sessionManager != null)
		{
			sessionManager.onGameTick();
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
	public void onInteractingChanged(InteractingChanged event)
	{
		if (combatEventListener != null)
		{
			combatEventListener.onInteractingChanged(event);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// Player logged in
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// End session on logout
			if (sessionManager != null)
			{
				sessionManager.endCurrentSession();
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (chatMessageListener != null)
		{
			chatMessageListener.onChatMessage(event);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("pvmperformancetracker"))
		{
			return;
		}

		// Update overlay or panel based on config changes
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
}