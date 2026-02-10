package net.runelite.client.plugins.pvmperformancetracker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.plugins.pvmperformancetracker.model.CombatSession;
import net.runelite.client.plugins.pvmperformancetracker.model.DamageEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class PvMPerformanceTrackerPanel extends PluginPanel
{
    private static final DecimalFormat DF = new DecimalFormat("#,###");
    private static final DecimalFormat DF_DECIMAL = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PvMPerformanceTrackerPlugin plugin;

    private final JPanel currentSessionPanel = new JPanel();
    private final JPanel sessionHistoryPanel = new JPanel();
    private final JPanel detailsPanel = new JPanel();

    private final JButton clearHistoryButton = new JButton("Clear History");
    private final JButton endSessionButton = new JButton("End Current Session");

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
        titlePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("PvM Performance Tracker");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(title, BorderLayout.WEST);

        add(titlePanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Current Session Section
        JPanel currentSectionWrapper = createSectionWrapper("Current Session");
        currentSessionPanel.setLayout(new BoxLayout(currentSessionPanel, BoxLayout.Y_AXIS));
        currentSessionPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        currentSectionWrapper.add(currentSessionPanel);
        mainPanel.add(currentSectionWrapper);

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        buttonPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        endSessionButton.addActionListener(e -> endCurrentSession());
        clearHistoryButton.addActionListener(e -> clearHistory());

        buttonPanel.add(endSessionButton);
        buttonPanel.add(clearHistoryButton);
        mainPanel.add(buttonPanel);

        // Session History Section
        JPanel historySectionWrapper = createSectionWrapper("Session History");
        sessionHistoryPanel.setLayout(new BoxLayout(sessionHistoryPanel, BoxLayout.Y_AXIS));
        sessionHistoryPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JScrollPane historyScrollPane = new JScrollPane(sessionHistoryPanel);
        historyScrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        historyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        historyScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        historySectionWrapper.add(historyScrollPane);
        mainPanel.add(historySectionWrapper);

        // Details Panel
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        detailsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        add(scrollPane, BorderLayout.CENTER);

        // No data panel
        noDataPanel.setContent("No Combat Data", "Start fighting to begin tracking!");
        noDataPanel.setVisible(true);

        updatePanel();
    }

    private JPanel createSectionWrapper(String title)
    {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        wrapper.setBorder(new EmptyBorder(10, 5, 10, 5));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setBorder(new EmptyBorder(5, 10, 5, 10));

        wrapper.add(titleLabel, BorderLayout.NORTH);

        return wrapper;
    }

    public void updatePanel()
    {
        SwingUtilities.invokeLater(() -> {
            updateCurrentSession();
            updateSessionHistory();
            revalidate();
            repaint();
        });
    }

    private void updateCurrentSession()
    {
        currentSessionPanel.removeAll();

        CombatSession current = plugin.getSessionManager().getCurrentSession();

        if (current == null || !current.isActive())
        {
            JLabel noSession = new JLabel("No active session");
            noSession.setForeground(Color.GRAY);
            noSession.setBorder(new EmptyBorder(10, 10, 10, 10));
            currentSessionPanel.add(noSession);
            return;
        }

        currentSessionPanel.add(createSessionSummaryPanel(current, true));
    }

    private void updateSessionHistory()
    {
        sessionHistoryPanel.removeAll();

        List<CombatSession> history = plugin.getSessionManager().getSessionHistory();

        if (history.isEmpty())
        {
            JLabel noHistory = new JLabel("No session history");
            noHistory.setForeground(Color.GRAY);
            noHistory.setBorder(new EmptyBorder(10, 10, 10, 10));
            sessionHistoryPanel.add(noHistory);
            return;
        }

        for (CombatSession session : history)
        {
            sessionHistoryPanel.add(createSessionSummaryPanel(session, false));
        }
    }

    private JPanel createSessionSummaryPanel(CombatSession session, boolean isCurrent)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(isCurrent ? new Color(60, 60, 70) : ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Target and time
        JLabel targetLabel = new JLabel(session.getPrimaryTarget() != null ? session.getPrimaryTarget() : "Unknown");
        targetLabel.setForeground(Color.WHITE);
        targetLabel.setFont(new Font("Arial", Font.BOLD, 13));
        panel.add(targetLabel);

        String timeStr = session.getStartTime().format(TIME_FORMATTER);
        JLabel timeLabel = new JLabel("Started: " + timeStr);
        timeLabel.setForeground(Color.LIGHT_GRAY);
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        panel.add(timeLabel);

        // Stats grid
        panel.add(Box.createVerticalStrut(5));
        panel.add(createStatRow("Damage:", DF.format(session.getTotalDamage()), Color.GREEN));
        panel.add(createStatRow("DPS:", DF_DECIMAL.format(session.getDPS()), Color.CYAN));
        panel.add(createStatRow("Accuracy:", DF_DECIMAL.format(session.getAccuracyPercentage()) + "%", Color.YELLOW));
        panel.add(createStatRow("Duration:", formatDuration(session.getDurationSeconds()), Color.WHITE));

        // Click handler for details
        panel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                showSessionDetails(session);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                panel.setBackground(new Color(70, 70, 80));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                panel.setBackground(isCurrent ? new Color(60, 60, 70) : ColorScheme.DARKER_GRAY_COLOR);
            }
        });

        return panel;
    }

    private JPanel createStatRow(String label, String value, Color valueColor)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel labelComp = new JLabel(label);
        labelComp.setForeground(Color.LIGHT_GRAY);
        labelComp.setFont(new Font("Arial", Font.PLAIN, 12));

        JLabel valueComp = new JLabel(value);
        valueComp.setForeground(valueColor);
        valueComp.setFont(new Font("Arial", Font.BOLD, 12));

        row.add(labelComp, BorderLayout.WEST);
        row.add(valueComp, BorderLayout.EAST);

        return row;
    }

    private void showSessionDetails(CombatSession session)
    {
        // Create a detailed view dialog
        JDialog dialog = new JDialog();
        dialog.setTitle("Session Details - " + (session.getPrimaryTarget() != null ? session.getPrimaryTarget() : "Unknown"));
        dialog.setModal(true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Overall stats
        content.add(createDetailSection("Overall Statistics",
                new String[][]{
                        {"Total Damage:", DF.format(session.getTotalDamage())},
                        {"DPS:", DF_DECIMAL.format(session.getDPS())},
                        {"Accuracy:", DF_DECIMAL.format(session.getAccuracyPercentage()) + "%"},
                        {"Max Hit:", DF.format(session.getMaxHit())},
                        {"Total Attacks:", String.valueOf(session.getTotalAttacks())},
                        {"Successful Hits:", String.valueOf(session.getSuccessfulHits())},
                        {"Duration:", formatDuration(session.getDurationSeconds())}
                }
        ));

        // Damage breakdown by attack style
        if (plugin.getConfig().trackByAttackStyle())
        {
            content.add(createDamageBreakdownSection(session));
        }

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        dialog.add(scrollPane);
        dialog.setVisible(true);
    }

    private JPanel createDetailSection(String title, String[][] stats)
    {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(10));

        for (String[] stat : stats)
        {
            section.add(createStatRow(stat[0], stat[1], Color.WHITE));
        }

        return section;
    }

    private JPanel createDamageBreakdownSection(CombatSession session)
    {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel("Damage Breakdown");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(10));

        Map<String, Integer> breakdown = session.getDamageByAttackStyle();
        for (Map.Entry<String, Integer> entry : breakdown.entrySet())
        {
            double percentage = (entry.getValue() / (double) session.getTotalDamage()) * 100;
            String value = DF.format(entry.getValue()) + " (" + DF_DECIMAL.format(percentage) + "%)";
            section.add(createStatRow(entry.getKey() + ":", value, Color.CYAN));
        }

        return section;
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

    private void endCurrentSession()
    {
        plugin.getSessionManager().endCurrentSession();
        updatePanel();
    }

    private void clearHistory()
    {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to clear all session history?",
                "Clear History",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION)
        {
            plugin.getSessionManager().clearHistory();
            updatePanel();
        }
    }
}