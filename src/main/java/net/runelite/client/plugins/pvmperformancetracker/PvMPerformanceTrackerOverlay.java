package net.runelite.client.plugins.pvmperformancetracker;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.plugins.pvmperformancetracker.model.CombatSession;
import net.runelite.client.plugins.pvmperformancetracker.helpers.CombatSessionManager;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;

public class PvMPerformanceTrackerOverlay extends OverlayPanel
{
    private static final DecimalFormat DF = new DecimalFormat("#,###");
    private static final DecimalFormat DF_DECIMAL = new DecimalFormat("#,##0.00");

    private final Client client;
    private final PvMPerformanceTrackerPlugin plugin;
    private final PvMPerformanceTrackerConfig config;

    @Inject
    private PvMPerformanceTrackerOverlay(Client client, PvMPerformanceTrackerPlugin plugin, PvMPerformanceTrackerConfig config)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_RIGHT);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay() || !config.enableTracking())
        {
            return null;
        }

        CombatSessionManager sessionManager = plugin.getSessionManager();
        if (sessionManager == null)
        {
            return null;
        }

        CombatSession currentSession = sessionManager.getCurrentSession();
        if (currentSession == null || !currentSession.isActive())
        {
            return null;
        }

        // Set background color
        panelComponent.setBackgroundColor(config.overlayBackgroundColor());

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("PvM Performance")
                .color(config.overlayColor())
                .build());

        // Combat target info
        if (currentSession.getPrimaryTarget() != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Target:")
                    .right(currentSession.getPrimaryTarget())
                    .leftColor(config.overlayColor())
                    .rightColor(Color.YELLOW)
                    .build());
        }

        // Combat time
        if (config.showCombatTime())
        {
            long duration = currentSession.getDurationSeconds();
            String timeStr = formatDuration(duration);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Time:")
                    .right(timeStr)
                    .leftColor(config.overlayColor())
                    .rightColor(Color.WHITE)
                    .build());
        }

        // Total Damage
        if (config.showTotalDamage())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Damage:")
                    .right(DF.format(currentSession.getTotalDamage()))
                    .leftColor(config.overlayColor())
                    .rightColor(Color.GREEN)
                    .build());
        }

        // DPS
        if (config.showDPS())
        {
            double dps = currentSession.getDPS();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("DPS:")
                    .right(DF_DECIMAL.format(dps))
                    .leftColor(config.overlayColor())
                    .rightColor(Color.CYAN)
                    .build());
        }

        // Accuracy
        if (config.showAccuracy())
        {
            double accuracy = currentSession.getAccuracyPercentage();
            Color accuracyColor = getAccuracyColor(accuracy);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Accuracy:")
                    .right(DF_DECIMAL.format(accuracy) + "%")
                    .leftColor(config.overlayColor())
                    .rightColor(accuracyColor)
                    .build());
        }

        // Max Hit
        if (config.showMaxHit())
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Max Hit:")
                    .right(DF.format(currentSession.getMaxHit()))
                    .leftColor(config.overlayColor())
                    .rightColor(Color.ORANGE)
                    .build());
        }

        // Hit count
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Hits:")
                .right(currentSession.getSuccessfulHits() + "/" + currentSession.getTotalAttacks())
                .leftColor(config.overlayColor())
                .rightColor(Color.LIGHT_GRAY)
                .build());

        return super.render(graphics);
    }

    private String formatDuration(long seconds)
    {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0)
        {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        }
        else
        {
            return String.format("%d:%02d", minutes, secs);
        }
    }

    private Color getAccuracyColor(double accuracy)
    {
        if (accuracy >= 90)
        {
            return Color.GREEN;
        }
        else if (accuracy >= 75)
        {
            return Color.YELLOW;
        }
        else if (accuracy >= 50)
        {
            return Color.ORANGE;
        }
        else
        {
            return Color.RED;
        }
    }
}