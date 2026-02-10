package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.model.CombatSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class CombatSessionManager
{
    private final PvMPerformanceTrackerPlugin plugin;

    @Getter
    private CombatSession currentSession;

    @Getter
    private final List<CombatSession> sessionHistory = new ArrayList<>();

    private int ticksSinceLastCheck = 0;
    private static final int CHECK_INTERVAL_TICKS = 10; // Check every 10 ticks (6 seconds)

    public CombatSessionManager(PvMPerformanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
    }

    public void startNewSession()
    {
        if (currentSession != null && currentSession.isActive())
        {
            endCurrentSession();
        }

        currentSession = new CombatSession();
        log.debug("Started new combat session: {}", currentSession.getSessionId());

        updatePanel();
    }

    public void endCurrentSession()
    {
        if (currentSession == null || !currentSession.isActive())
        {
            return;
        }

        currentSession.endSession();

        // Only save sessions that meet minimum duration requirement
        int minDuration = plugin.getConfig().minimumCombatTime();
        if (currentSession.getDurationSeconds() >= minDuration)
        {
            addToHistory(currentSession);
            log.debug("Ended combat session: {} - Duration: {}s, Damage: {}",
                    currentSession.getSessionId(),
                    currentSession.getDurationSeconds(),
                    currentSession.getTotalDamage());
        }
        else
        {
            log.debug("Session too short, not saving: {}s < {}s",
                    currentSession.getDurationSeconds(),
                    minDuration);
        }

        currentSession = null;
        updatePanel();
    }

    private void addToHistory(CombatSession session)
    {
        sessionHistory.add(0, session); // Add to beginning of list

        // Trim history if it exceeds max size
        int maxHistory = plugin.getConfig().maxSessionHistory();
        while (sessionHistory.size() > maxHistory)
        {
            sessionHistory.remove(sessionHistory.size() - 1);
        }
    }

    public void onGameTick()
    {
        ticksSinceLastCheck++;

        if (ticksSinceLastCheck >= CHECK_INTERVAL_TICKS)
        {
            ticksSinceLastCheck = 0;
            checkSessionTimeout();
        }
    }

    private void checkSessionTimeout()
    {
        if (currentSession == null || !currentSession.isActive())
        {
            return;
        }

        int timeoutSeconds = plugin.getConfig().combatTimeout();

        if (currentSession.isTimedOut(timeoutSeconds))
        {
            log.debug("Combat session timed out after {}s of inactivity", timeoutSeconds);
            endCurrentSession();
        }
    }

    public boolean hasActiveSession()
    {
        return currentSession != null && currentSession.isActive();
    }

    public void updateSessionActivity()
    {
        if (currentSession != null && currentSession.isActive())
        {
            currentSession.updateLastActivity();
        }
    }

    public void clearHistory()
    {
        sessionHistory.clear();
        log.debug("Cleared session history");
        updatePanel();
    }

    public List<CombatSession> getSessionHistory()
    {
        return Collections.unmodifiableList(sessionHistory);
    }

    public CombatSession getSessionById(String sessionId)
    {
        for (CombatSession session : sessionHistory)
        {
            if (session.getSessionId().equals(sessionId))
            {
                return session;
            }
        }
        return null;
    }

    private void updatePanel()
    {
        if (plugin.getPanel() != null)
        {
            plugin.getPanel().updatePanel();
        }
    }
}