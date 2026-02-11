package net.runelite.client.plugins.pvmperformancetracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;
import net.runelite.client.plugins.pvmperformancetracker.models.PlayerStats;
import net.runelite.client.plugins.pvmperformancetracker.enums.TrackingMode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public class PvMPerformanceTrackerPanel extends PluginPanel
{
    private static final DecimalFormat DF = new DecimalFormat("#,###");
    private static final DecimalFormat DF_DECIMAL = new DecimalFormat("#,##0.0");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int PANEL_WIDTH = 225; // Standard RuneLite sidebar width

    private final PvMPerformanceTrackerPlugin plugin;

    private final JPanel currentFightPanel = new JPanel();
    private final JPanel overallPanel = new JPanel();
    private final JPanel fightHistoryPanel = new JPanel();

    private final JButton endCurrentFightButton = new JButton("End Fight");
    private final JButton resetOverallButton = new JButton("Reset Overall");
    private final JButton clearHistoryButton = new JButton("Clear History");

    private final PluginErrorPanel noDataPanel = new PluginErrorPanel();

    public PvMPerformanceTrackerPanel(PvMPerformanceTrackerPlugin plugin)
    {
        super(false);
        this.plugin = plugin;

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());

        // Title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        titlePanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("PvM Tracker");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 14));
        titlePanel.add(title, BorderLayout.CENTER);

        add(titlePanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Current Fight Section
        JPanel currentFightWrapper = createSectionWrapper("Current Fight");
        currentFightPanel.setLayout(new BoxLayout(currentFightPanel, BoxLayout.Y_AXIS));
        currentFightPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        currentFightWrapper.add(currentFightPanel);
        mainPanel.add(currentFightWrapper);

        // Overall Section
        JPanel overallWrapper = createSectionWrapper("Overall");
        overallPanel.setLayout(new BoxLayout(overallPanel, BoxLayout.Y_AXIS));
        overallPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        overallWrapper.add(overallPanel);
        mainPanel.add(overallWrapper);

        // Buttons - compact layout with MUCH smaller buttons (ALL ON ONE ROW)
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 2, 2));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(3, 8, 3, 8));

        // Prevent button panel from expanding
        buttonPanel.setMaximumSize(new Dimension(PANEL_WIDTH, 22));

        endCurrentFightButton.addActionListener(e -> endCurrentFight());
        resetOverallButton.addActionListener(e -> resetOverall());
        clearHistoryButton.addActionListener(e -> clearHistory());

        // Make buttons MUCH smaller
        Font buttonFont = new Font("Arial", Font.PLAIN, 8);
        Dimension buttonSize = new Dimension((PANEL_WIDTH - 24) / 3, 18);

        endCurrentFightButton.setFont(buttonFont);
        endCurrentFightButton.setPreferredSize(buttonSize);
        endCurrentFightButton.setMaximumSize(buttonSize);
        endCurrentFightButton.setText("End Fight");

        resetOverallButton.setFont(buttonFont);
        resetOverallButton.setPreferredSize(buttonSize);
        resetOverallButton.setMaximumSize(buttonSize);
        resetOverallButton.setText("Reset");

        clearHistoryButton.setFont(buttonFont);
        clearHistoryButton.setPreferredSize(buttonSize);
        clearHistoryButton.setMaximumSize(buttonSize);
        clearHistoryButton.setText("Clear");

        buttonPanel.add(endCurrentFightButton);
        buttonPanel.add(resetOverallButton);
        buttonPanel.add(clearHistoryButton);
        mainPanel.add(buttonPanel);

        // Fight History Section - FIXED SIZE (larger than other sections)
        JPanel historyWrapper = new JPanel(new BorderLayout());
        historyWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        historyWrapper.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel historyTitle = new JLabel("Fight History");
        historyTitle.setForeground(Color.WHITE);
        historyTitle.setFont(new Font("Arial", Font.BOLD, 12));
        historyTitle.setBorder(new EmptyBorder(3, 5, 3, 5));
        historyWrapper.add(historyTitle, BorderLayout.NORTH);

        fightHistoryPanel.setLayout(new BoxLayout(fightHistoryPanel, BoxLayout.Y_AXIS));
        fightHistoryPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane historyScrollPane = new JScrollPane(fightHistoryPanel);
        historyScrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        historyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        historyScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        historyScrollPane.setBorder(null);

        // Set FIXED preferred size for the scroll pane to prevent buttons from expanding
        historyScrollPane.setPreferredSize(new Dimension(PANEL_WIDTH, 400));
        historyScrollPane.setMinimumSize(new Dimension(PANEL_WIDTH, 300));

        historyWrapper.add(historyScrollPane);
        mainPanel.add(historyWrapper);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        // No data panel
        noDataPanel.setContent("No Combat Data", "Start fighting!");
        noDataPanel.setVisible(false);

        updatePanel();
    }

    private JPanel createSectionWrapper(String title)
    {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        wrapper.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Set max height to prevent excessive expansion
        wrapper.setMaximumSize(new Dimension(PANEL_WIDTH, 150));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setBorder(new EmptyBorder(3, 5, 3, 5));

        wrapper.add(titleLabel, BorderLayout.NORTH);

        return wrapper;
    }

    public void updatePanel()
    {
        SwingUtilities.invokeLater(() -> {
            updateCurrentFight();
            updateOverall();
            updateFightHistory();
            revalidate();
            repaint();
        });
    }

    private void updateCurrentFight()
    {
        currentFightPanel.removeAll();

        if (plugin.getFightTracker() == null)
        {
            addNoDataLabel(currentFightPanel);
            return;
        }

        Fight currentFight = plugin.getFightTracker().getCurrentFight();

        // Show current fight if it exists (even if inactive/ended)
        if (currentFight == null)
        {
            addNoDataLabel(currentFightPanel);
            return;
        }

        currentFightPanel.add(createCompactFightPanel(currentFight, true));
    }

    private void updateOverall()
    {
        overallPanel.removeAll();

        if (plugin.getFightTracker() == null)
        {
            addNoDataLabel(overallPanel);
            return;
        }

        Fight overallFight = plugin.getFightTracker().getOverallFight();

        // Always show Overall (will show 0s if empty)
        if (overallFight != null)
        {
            overallPanel.add(createCompactFightPanel(overallFight, false));
        }
        else
        {
            addNoDataLabel(overallPanel);
        }
    }

    private void updateFightHistory()
    {
        fightHistoryPanel.removeAll();

        if (plugin.getFightTracker() == null)
        {
            addNoDataLabel(fightHistoryPanel);
            return;
        }

        java.util.List<Fight> history = plugin.getFightTracker().getFightHistory();

        if (history.isEmpty())
        {
            addNoDataLabel(fightHistoryPanel);
            return;
        }

        // Show only last 10 fights to save space
        int count = 0;
        for (Fight fight : history)
        {
            if (count >= 10) break;
            fightHistoryPanel.add(createCompactFightPanel(fight, false));
            count++;
        }
    }

    private void addNoDataLabel(JPanel panel)
    {
        JLabel noData = new JLabel("No data");
        noData.setForeground(Color.GRAY);
        noData.setFont(new Font("Arial", Font.PLAIN, 10));
        noData.setBorder(new EmptyBorder(5, 8, 5, 8));
        panel.add(noData);
    }

    private JPanel createCompactFightPanel(Fight fight, boolean isCurrent)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(isCurrent ? new Color(50, 60, 50) : ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                new EmptyBorder(6, 8, 6, 8)
        ));
        panel.setMaximumSize(new Dimension(PANEL_WIDTH, 120));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Boss name - truncate if too long
        String bossName = fight.getBossName() != null ? fight.getBossName() : "Unknown";
        if (bossName.length() > 20)
        {
            bossName = bossName.substring(0, 17) + "...";
        }

        JLabel bossLabel = new JLabel(bossName);
        bossLabel.setForeground(Color.WHITE);
        bossLabel.setFont(new Font("Arial", Font.BOLD, 11));
        panel.add(bossLabel);

        // Add timestamp if not current fight
        if (!isCurrent && fight.getStartTime() != null)
        {
            String timeStr = fight.getStartTime().format(TIME_FORMATTER);
            JLabel timeLabel = new JLabel(timeStr);
            timeLabel.setForeground(new Color(150, 150, 150));
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 9));
            panel.add(timeLabel);
        }

        // Compact stats
        PlayerStats localStats = fight.getLocalPlayerStats();
        if (localStats != null)
        {
            boolean isOverall = fight.getBossName() != null && fight.getBossName().equals("Overall");
            int ticksLost;

            if (isOverall)
            {
                // Overall mode: tick loss is already aggregated
                ticksLost = localStats.getAttackingTicksLost();
            }
            else
            {
                // Current Fight: calculate real-time
                int currentTick = plugin.getFightTracker() != null ? plugin.getFightTracker().getCurrentTick() : 0;
                ticksLost = localStats.calculateTicksLost(currentTick, fight.isActive());
            }

            panel.add(createCompactStatRow("DMG:", DF.format(localStats.getDamageDealt())));
            panel.add(createCompactStatRow("DPS:", DF_DECIMAL.format(localStats.calculateDPS(fight.getDurationTicks()))));
            panel.add(createCompactStatRow("TL:", String.valueOf(ticksLost)));
        }
        else
        {
            panel.add(createCompactStatRow("DMG:", DF.format(fight.getTotalDamage())));
        }

        panel.add(createCompactStatRow("Time:", formatDurationTicks(fight.getDurationTicks())));

        // Click handler
        panel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                showFightDetails(fight);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                panel.setBackground(new Color(60, 70, 60));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                panel.setBackground(isCurrent ? new Color(50, 60, 50) : ColorScheme.DARKER_GRAY_COLOR);
            }
        });

        return panel;
    }

    private JPanel createCompactStatRow(String label, String value)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        JLabel labelComp = new JLabel(label);
        labelComp.setForeground(Color.LIGHT_GRAY);
        labelComp.setFont(new Font("Arial", Font.PLAIN, 10));

        JLabel valueComp = new JLabel(value);
        valueComp.setForeground(Color.WHITE);
        valueComp.setFont(new Font("Arial", Font.BOLD, 10));

        row.add(labelComp, BorderLayout.WEST);
        row.add(valueComp, BorderLayout.EAST);

        return row;
    }

    private void showFightDetails(Fight fight)
    {
        // Create a detailed view dialog - compact version
        JDialog dialog = new JDialog();
        dialog.setTitle("Fight Details");
        dialog.setModal(true);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Boss info
        content.add(createDetailSection("Fight Info", new String[][]{
                {"Boss:", fight.getBossName() != null ? fight.getBossName() : "Unknown"},
                {"Total DMG:", DF.format(fight.getTotalDamage())},
                {"Duration:", formatDurationTicks(fight.getDurationTicks())},
                {"Players:", String.valueOf(fight.getPlayerStats().size())}
        }));

        content.add(Box.createVerticalStrut(10));

        // Player breakdown
        JPanel playersSection = new JPanel();
        playersSection.setLayout(new BoxLayout(playersSection, BoxLayout.Y_AXIS));
        playersSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        playersSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(8, 8, 8, 8)
        ));

        JLabel playersTitle = new JLabel("Players");
        playersTitle.setForeground(Color.WHITE);
        playersTitle.setFont(new Font("Arial", Font.BOLD, 12));
        playersSection.add(playersTitle);
        playersSection.add(Box.createVerticalStrut(8));

        for (Map.Entry<String, PlayerStats> entry : fight.getPlayerStats().entrySet())
        {
            String playerName = entry.getKey();
            PlayerStats stats = entry.getValue();

            playersSection.add(createPlayerDetailPanel(playerName, stats, fight));
            playersSection.add(Box.createVerticalStrut(5));
        }

        content.add(playersSection);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        scrollPane.setBorder(null);

        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private JPanel createPlayerDetailPanel(String playerName, PlayerStats stats, Fight fight)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 40, 45));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(5, 5, 5, 5)
        ));

        // Truncate name if needed
        String displayName = playerName;
        if (displayName.length() > 20)
        {
            displayName = displayName.substring(0, 17) + "...";
        }

        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setForeground(Color.YELLOW);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 11));
        panel.add(nameLabel);

        boolean isOverall = fight.getBossName() != null && fight.getBossName().equals("Overall");
        int ticksLost;

        if (isOverall)
        {
            // Overall mode: tick loss already aggregated
            ticksLost = stats.getAttackingTicksLost();
        }
        else
        {
            // Current Fight: calculate real-time
            int currentTick = plugin.getFightTracker() != null ? plugin.getFightTracker().getCurrentTick() : 0;
            ticksLost = stats.calculateTicksLost(currentTick, fight.isActive());
        }

        panel.add(createCompactStatRow("DMG:", DF.format(stats.getDamageDealt())));
        panel.add(createCompactStatRow("DPS:", DF_DECIMAL.format(stats.calculateDPS(fight.getDurationTicks()))));
        panel.add(createCompactStatRow("Ticks Lost:", String.valueOf(ticksLost)));
        panel.add(createCompactStatRow("Accuracy:", DF_DECIMAL.format(stats.getAccuracyPercentage()) + "%"));

        return panel;
    }

    private JPanel createDetailSection(String title, String[][] stats)
    {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(8, 8, 8, 8)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(6));

        for (String[] stat : stats)
        {
            section.add(createCompactStatRow(stat[0], stat[1]));
        }

        return section;
    }

    private String formatDurationTicks(int ticks)
    {
        int totalSeconds = (ticks * 600) / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    private void endCurrentFight()
    {
        if (plugin.getFightTracker() != null)
        {
            plugin.getFightTracker().endCurrentFight();
        }
        updatePanel();
    }

    private void resetOverall()
    {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Reset Overall stats?",
                "Reset Overall",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION && plugin.getFightTracker() != null)
        {
            plugin.getFightTracker().resetOverallTracking();
            updatePanel();
        }
    }

    private void clearHistory()
    {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Clear all fight history?",
                "Clear History",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION && plugin.getFightTracker() != null)
        {
            plugin.getFightTracker().clearHistory();
            updatePanel();
        }
    }
}