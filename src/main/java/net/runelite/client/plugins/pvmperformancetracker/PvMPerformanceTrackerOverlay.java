package net.runelite.client.plugins.pvmperformancetracker;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;
import net.runelite.client.plugins.pvmperformancetracker.models.PlayerStats;
import net.runelite.client.plugins.pvmperformancetracker.enums.TrackingMode;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class PvMPerformanceTrackerOverlay extends Overlay
{
    private static final DecimalFormat DF = new DecimalFormat("#,###");
    private static final DecimalFormat DF_DECIMAL = new DecimalFormat("#,##0.0");
    private static final DecimalFormat DF_COMPACT = new DecimalFormat("#");

    // Sizing constants
    private static final int MIN_WIDTH = 200;
    private static final int MAX_WIDTH = 500;
    private static final int TITLE_HEIGHT = 20;
    private static final int BAR_HEIGHT = 24;
    private static final int BAR_PADDING = 2;
    private static final int SIDE_PADDING = 8;
    private static final int TOP_PADDING = 6;

    // Colors
    private static final Color BACKGROUND_COLOR = new Color(30, 30, 30, 220);
    private static final Color TITLE_BACKGROUND = new Color(50, 50, 50, 220);
    private static final Color BAR_BACKGROUND = new Color(40, 40, 40, 180);
    private static final Color BAR_BORDER = new Color(60, 60, 60);

    // Player colors - different shade for each player
    private static final Color[] PLAYER_COLORS = {
            new Color(100, 150, 100, 200),  // Green
            new Color(100, 130, 170, 200),  // Blue
            new Color(170, 120, 100, 200),  // Orange
            new Color(150, 100, 150, 200),  // Purple
            new Color(100, 150, 150, 200),  // Teal
            new Color(170, 150, 100, 200),  // Gold
            new Color(150, 100, 100, 200),  // Red
            new Color(120, 140, 100, 200),  // Olive
    };

    private final Client client;
    private final PvMPerformanceTrackerPlugin plugin;
    private final PvMPerformanceTrackerConfig config;

    // Dynamic sizing based on overlay bounds
    private int currentWidth = 350;

    @Inject
    private PvMPerformanceTrackerOverlay(Client client, PvMPerformanceTrackerPlugin plugin, PvMPerformanceTrackerConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.MED);
        setResizable(true);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay() || !config.enableTracking())
        {
            return null;
        }

        Fight activeFight = getActiveFight();
        if (activeFight == null)
        {
            return null;
        }

        // For Overall mode, show even if no activity yet
        // For Current Fight mode, show if fight exists (even if ended)
        boolean isOverall = activeFight.getBossName() != null && activeFight.getBossName().equals("Overall");
        if (!isOverall && !activeFight.hasActivity())
        {
            return null;
        }

        // Get preferred bounds if set by user resizing
        Rectangle bounds = getBounds();
        if (bounds != null && bounds.width > 0)
        {
            currentWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, bounds.width));
        }

        // Setup rendering
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Get sorted players
        List<PlayerEntry> players = getSortedPlayers(activeFight);

        // Calculate total height
        int totalHeight = TITLE_HEIGHT + TOP_PADDING + (players.size() * (BAR_HEIGHT + BAR_PADDING)) + TOP_PADDING;

        // Draw background
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, currentWidth, totalHeight);

        // Draw title
        drawTitle(graphics, activeFight);

        // Draw player bars
        int yOffset = TITLE_HEIGHT + TOP_PADDING;
        int maxDamage = players.isEmpty() ? 1 : players.get(0).stats.getDamageDealt();

        for (int i = 0; i < players.size(); i++)
        {
            drawPlayerBar(graphics, players.get(i), yOffset, maxDamage, activeFight, i);
            yOffset += BAR_HEIGHT + BAR_PADDING;
        }

        return new Dimension(currentWidth, totalHeight);
    }

    private void drawTitle(Graphics2D graphics, Fight fight)
    {
        graphics.setColor(TITLE_BACKGROUND);
        graphics.fillRect(0, 0, currentWidth, TITLE_HEIGHT);

        String title = buildTitle(fight);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.BOLD, 12));

        // Center title
        FontMetrics fm = graphics.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int titleX = (currentWidth - titleWidth) / 2;

        graphics.drawString(title, titleX, TITLE_HEIGHT - 6);
    }

    private String buildTitle(Fight fight)
    {
        StringBuilder title = new StringBuilder();

        if (fight.getBossName() != null && !fight.getBossName().isEmpty())
        {
            // Simple format: "Baba [00:29:60]"
            title.append(fight.getBossName());
            String timer = formatTimer(fight.getDurationTicks());
            title.append(" [").append(timer).append("]");
        }
        else
        {
            title.append("PvM Performance");
        }

        return title.toString();
    }

    private void drawPlayerBar(Graphics2D graphics, PlayerEntry entry, int y, int maxDamage, Fight fight, int playerIndex)
    {
        String localPlayerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "";
        boolean isLocalPlayer = entry.playerName.equals(localPlayerName);

        // Full bar width (extends entire overlay)
        int fullBarWidth = currentWidth - (SIDE_PADDING * 2);

        // Calculate filled portion based on damage percentage
        double damagePercent = maxDamage > 0 ? (double) entry.stats.getDamageDealt() / maxDamage : 0;
        int filledWidth = (int) (fullBarWidth * damagePercent);

        // Draw background bar (unfilled portion)
        graphics.setColor(BAR_BACKGROUND);
        graphics.fillRect(SIDE_PADDING, y, fullBarWidth, BAR_HEIGHT);

        // Draw filled portion
        Color barColor = isLocalPlayer ? PLAYER_COLORS[0] : PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
        graphics.setColor(barColor);
        graphics.fillRect(SIDE_PADDING, y, filledWidth, BAR_HEIGHT);

        // Draw bar border
        graphics.setColor(BAR_BORDER);
        graphics.drawRect(SIDE_PADDING, y, fullBarWidth, BAR_HEIGHT);

        // Build text content
        String statsText = buildStatsText(entry, fight);

        // Draw text inside bar
        graphics.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = graphics.getFontMetrics();

        // Check if text fits
        int textWidth = fm.stringWidth(entry.playerName + " " + statsText);
        String displayText;

        if (textWidth > fullBarWidth - 12)
        {
            // Need to truncate or compact
            String compactStats = buildCompactStatsText(entry, fight);
            String truncatedName = entry.playerName;

            // Try truncating name first
            while (fm.stringWidth(truncatedName + "... " + compactStats) > fullBarWidth - 12 && truncatedName.length() > 3)
            {
                truncatedName = truncatedName.substring(0, truncatedName.length() - 1);
            }

            if (truncatedName.length() < entry.playerName.length())
            {
                displayText = truncatedName + "... " + compactStats;
            }
            else
            {
                displayText = entry.playerName + " " + compactStats;
            }
        }
        else
        {
            displayText = entry.playerName + " " + statsText;
        }

        Color textColor = isLocalPlayer ? Color.YELLOW : Color.WHITE;
        graphics.setColor(textColor);

        int textX = SIDE_PADDING + 6;
        int textY = y + (BAR_HEIGHT / 2) + (fm.getAscent() / 2) - 1;

        graphics.drawString(displayText, textX, textY);
    }

    private String buildStatsText(PlayerEntry entry, Fight fight)
    {
        StringBuilder text = new StringBuilder();

        text.append(DF.format(entry.stats.getDamageDealt()));
        text.append(" (");
        text.append(formatMetric(config.overlayMetric1(), entry.stats, fight));
        text.append(", ");
        text.append(formatMetric(config.overlayMetric2(), entry.stats, fight));
        text.append(")");

        return text.toString();
    }

    private String buildCompactStatsText(PlayerEntry entry, Fight fight)
    {
        // Ultra compact: "100(10, -12)"
        StringBuilder text = new StringBuilder();

        text.append(DF_COMPACT.format(entry.stats.getDamageDealt()));
        text.append("(");
        text.append(formatMetricCompact(config.overlayMetric1(), entry.stats, fight));
        text.append(", ");
        text.append(formatMetricCompact(config.overlayMetric2(), entry.stats, fight));
        text.append(")");

        return text.toString();
    }

    private String formatMetric(PvMPerformanceTrackerConfig.OverlayMetric metric, PlayerStats stats, Fight fight)
    {
        switch (metric)
        {
            case DPS:
                double dps = stats.calculateDPS(fight.getDurationTicks());
                return DF_DECIMAL.format(dps) + " DPS";

            case DAMAGE:
                return DF.format(stats.getDamageDealt()) + " DMG";

            case TICKS_LOST:
                int ticksLost = calculateTicksLost(stats, fight);
                return "-" + ticksLost + "T";

            case EXPECTED_DPS:
                double expDps = stats.getExpectedDps(fight.getDurationTicks());
                return DF_DECIMAL.format(expDps) + " EDPS";

            case EXPECTED_DAMAGE:
                return DF.format((int)stats.getExpectedDamageDealt()) + " ED";

            case ACCURACY:
                int totalAtks = stats.getTotalAttacks();
                int hits = stats.getSuccessfulHits();
                double accuracy = totalAtks > 0 ? (hits * 100.0 / totalAtks) : 0.0;
                return DF_DECIMAL.format(accuracy) + "%";

            case DAMAGE_TAKEN:
                return DF.format(stats.getDamageTaken()) + " DT";

            case AVOIDABLE_DAMAGE:
                return DF.format(stats.getAvoidableDamageTaken()) + " AD";

            case PRAYABLE_DAMAGE:
                return DF.format(stats.getPrayableDamageTaken()) + " PD";

            case UNAVOIDABLE_DAMAGE:
                return DF.format(stats.getUnavoidableDamageTaken()) + " UD";

            case CHANCES_OF_DEATH:
                return stats.getChancesOfDeath() + " DC";

            case DEATH_CHANCE_PERCENT:
                return DF_DECIMAL.format(stats.getDeathChancePercentage()) + "%";

            default:
                return "0";
        }
    }

    private String formatMetricCompact(PvMPerformanceTrackerConfig.OverlayMetric metric, PlayerStats stats, Fight fight)
    {
        switch (metric)
        {
            case DPS:
                double dps = stats.calculateDPS(fight.getDurationTicks());
                return DF_COMPACT.format(dps);

            case DAMAGE:
                return DF_COMPACT.format(stats.getDamageDealt());

            case TICKS_LOST:
                int ticksLost = calculateTicksLost(stats, fight);
                return "-" + ticksLost;

            case EXPECTED_DPS:
                double expDps = stats.getExpectedDps(fight.getDurationTicks());
                return DF_COMPACT.format(expDps);

            case EXPECTED_DAMAGE:
                return DF_COMPACT.format((int)stats.getExpectedDamageDealt());

            case ACCURACY:
                int totalAtks = stats.getTotalAttacks();
                int hits = stats.getSuccessfulHits();
                double accuracy = totalAtks > 0 ? (hits * 100.0 / totalAtks) : 0.0;
                return DF_COMPACT.format(accuracy);

            case DAMAGE_TAKEN:
                return DF_COMPACT.format(stats.getDamageTaken());

            case AVOIDABLE_DAMAGE:
                return DF_COMPACT.format(stats.getAvoidableDamageTaken());

            case PRAYABLE_DAMAGE:
                return DF_COMPACT.format(stats.getPrayableDamageTaken());

            case UNAVOIDABLE_DAMAGE:
                return DF_COMPACT.format(stats.getUnavoidableDamageTaken());

            case CHANCES_OF_DEATH:
                return String.valueOf(stats.getChancesOfDeath());

            case DEATH_CHANCE_PERCENT:
                return DF_COMPACT.format(stats.getDeathChancePercentage());

            default:
                return "0";
        }
    }

    private int calculateTicksLost(PlayerStats stats, Fight fight)
    {
        // Get current tick from fight tracker
        int currentTick = plugin.getFightTracker().getCurrentTick();

        // Check if this is Overall mode
        boolean isOverall = fight.getBossName() != null && fight.getBossName().equals("Overall");

        if (isOverall)
        {
            // Overall is already synced with base + current, just return the value
            return Math.max(0, stats.getAttackingTicksLost());
        }
        else
        {
            // Current Fight - calculate real-time
            int ticksLost = stats.calculateTicksLost(currentTick, fight.isActive());
            return Math.max(0, ticksLost);
        }
    }

    private int getMetricValue(PvMPerformanceTrackerConfig.OverlayMetric metric, PlayerStats stats)
    {
        switch (metric)
        {
            case DAMAGE:
                return stats.getDamageDealt();
            case EXPECTED_DAMAGE:
                return (int)stats.getExpectedDamageDealt();
            case DAMAGE_TAKEN:
                return stats.getDamageTaken();
            case AVOIDABLE_DAMAGE:
                return stats.getAvoidableDamageTaken();
            case PRAYABLE_DAMAGE:
                return stats.getPrayableDamageTaken();
            case UNAVOIDABLE_DAMAGE:
                return stats.getUnavoidableDamageTaken();
            case CHANCES_OF_DEATH:
                return stats.getChancesOfDeath();
            default:
                return 0;
        }
    }

    private String formatTimer(int ticks)
    {
        int totalMs = ticks * 600;
        int minutes = totalMs / 60000;
        int seconds = (totalMs % 60000) / 1000;
        int centiseconds = (totalMs % 1000) / 10;

        return String.format("%02d:%02d:%02d", minutes, seconds, centiseconds);
    }

    private Fight getActiveFight()
    {
        if (plugin.getFightTracker() == null)
        {
            return null;
        }

        TrackingMode mode = config.trackingMode();

        if (mode == TrackingMode.CURRENT_FIGHT)
        {
            return plugin.getFightTracker().getCurrentFight();
        }
        else
        {
            return plugin.getFightTracker().getOverallFight();
        }
    }

    private List<PlayerEntry> getSortedPlayers(Fight fight)
    {
        List<PlayerEntry> players = new ArrayList<>();

        Map<String, PlayerStats> allStats = fight.getPlayerStats();
        for (Map.Entry<String, PlayerStats> entry : allStats.entrySet())
        {
            players.add(new PlayerEntry(entry.getKey(), entry.getValue()));
        }

        // Sort by damage descending
        players.sort((a, b) -> Integer.compare(b.stats.getDamageDealt(), a.stats.getDamageDealt()));

        return players;
    }

    private static class PlayerEntry
    {
        final String playerName;
        final PlayerStats stats;

        PlayerEntry(String playerName, PlayerStats stats)
        {
            this.playerName = playerName;
            this.stats = stats;
        }
    }
}