package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;
import net.runelite.client.plugins.pvmperformancetracker.models.PlayerStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages fight tracking for both Current Fight and Overall modes
 */
@Slf4j
public class FightTracker
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    @Getter
    private Fight currentFight;

    // Overall fight - persistent object updated every tick
    @Getter
    private Fight overallFight;

    //@Getter
    private final List<Fight> fightHistory = new ArrayList<>();

    @Getter
    private int currentTick;

    public FightTracker(PvMPerformanceTrackerPlugin plugin, Client client)
    {
        this.plugin = plugin;
        this.client = client;
        this.currentTick = 0;
    }

    /**
     * Start a new current fight
     */
    public void startNewFight(String bossName, int bossNpcId)
    {
        // End current fight if one exists
        if (currentFight != null && currentFight.isActive())
        {
            endCurrentFight();
        }

        currentFight = new Fight(currentTick);
        currentFight.setBossName(bossName);
        currentFight.setBossNpcId(bossNpcId);

        String localPlayerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "Unknown";
        currentFight.setLocalPlayerName(localPlayerName);

        log.debug("Started new fight: {} ({})", bossName, bossNpcId);

        // Initialize Overall if it doesn't exist
        if (overallFight == null || !overallFight.isActive())
        {
            overallFight = new Fight(currentTick);
            overallFight.setBossName("Overall");
            overallFight.setLocalPlayerName(localPlayerName);
            log.debug("Initialized Overall tracking");
        }

        // Clear damaged NPC tracking for new fight
        if (plugin.getCombatEventListener() != null)
        {
            plugin.getCombatEventListener().onFightStart();
        }

        updatePanel();
    }

    /**
     * End the current fight
     */
    public void endCurrentFight()
    {
        if (currentFight == null || !currentFight.isActive())
        {
            return;
        }

        currentFight.endFight(currentTick);

        // Lock current fight stats into Overall's base (so next fight adds to this)
        if (overallFight != null && overallFight.isActive())
        {
            lockCurrentIntoOverall();
        }

        // Check minimum duration requirement (in ticks)
        int minDurationTicks = plugin.getConfig().minimumFightTime();

        // Add to history if it has activity and meets minimum duration
        if (currentFight.hasActivity() && currentFight.getDurationTicks() >= minDurationTicks)
        {
            addToHistory(currentFight);

            log.debug("Ended fight: {} - Duration: {} ticks, Damage: {}",
                    currentFight.getBossName(),
                    currentFight.getDurationTicks(),
                    currentFight.getTotalDamage());
        }
        else if (!currentFight.hasActivity())
        {
            log.debug("Fight had no activity, not saving");
        }
        else
        {
            log.debug("Fight too short, not saving: {} ticks < {} ticks",
                    currentFight.getDurationTicks(),
                    minDurationTicks);
        }

        // DON'T null out currentFight - keep it displayed until new fight starts
        // currentFight = null; // REMOVED

        updatePanel();
    }

    /**
     * Reset overall tracking
     */
    public void resetOverallTracking()
    {
        // Reset Overall fight
        String localPlayerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "Unknown";
        overallFight = new Fight(currentTick);
        overallFight.setBossName("Overall");
        overallFight.setLocalPlayerName(localPlayerName);

        clearHistory();
        log.debug("Reset overall tracking");
        updatePanel();
    }

    /**
     * Update current tick (called every game tick)
     */
    public void onGameTick()
    {
        currentTick++;

        if (currentFight != null && currentFight.isActive())
        {
            currentFight.updateCurrentTick(currentTick);

            // Mark Overall as in combat and update its ticks
            if (overallFight != null && overallFight.isActive())
            {
                overallFight.setInCombat(true);
                overallFight.updateCurrentTick(currentTick);

                // Update Overall = base Overall + current fight (every tick for real-time)
                syncOverallWithCurrent();
            }
        }
        else
        {
            // No active fight - mark Overall as out of combat
            if (overallFight != null && overallFight.isActive())
            {
                overallFight.setInCombat(false);
                overallFight.updateCurrentTick(currentTick);
            }
        }
    }

    /**
     * Sync Overall stats to show base + current fight in real-time
     * This creates the "Overall = Overall + CurrentFight" effect
     */
    private void syncOverallWithCurrent()
    {
        if (currentFight == null || overallFight == null)
        {
            return;
        }

        // For each player in current fight, update Overall to show cumulative stats
        for (String playerName : currentFight.getPlayerStats().keySet())
        {
            PlayerStats currentStats = currentFight.getPlayerStats().get(playerName);
            PlayerStats overallStats = overallFight.getOrCreatePlayerStats(playerName);

            // Calculate what Overall should show: base + current
            overallStats.syncWithCurrentFight(currentStats, currentTick);
        }
    }

    /**
     * Lock current fight stats into Overall's base values
     * Called when a fight ends
     */
    private void lockCurrentIntoOverall()
    {
        if (currentFight == null || overallFight == null)
        {
            return;
        }

        // Lock current fight stats into Overall's permanent base
        for (String playerName : currentFight.getPlayerStats().keySet())
        {
            PlayerStats currentStats = currentFight.getPlayerStats().get(playerName);
            PlayerStats overallStats = overallFight.getOrCreatePlayerStats(playerName);

            // Permanently add current fight to Overall's base
            overallStats.lockInFightStats(currentStats);
        }

        log.debug("Locked current fight into Overall base");
    }

    /**
     * Add damage dealt for a player
     */
    public void addDamageDealt(String playerName, int damage, String target)
    {
        // Add to current fight only
        if (currentFight != null && currentFight.isActive())
        {
            PlayerStats stats = currentFight.getOrCreatePlayerStats(playerName);
            stats.addDamageDealt(damage, currentTick, target);
        }

        // Overall gets updated via syncOverallWithCurrent() every tick
    }

    /**
     * Record an attack for tick loss calculation
     */
    public void recordAttack(String playerName, int weaponSpeed)
    {
        // Add to current fight only
        if (currentFight != null && currentFight.isActive())
        {
            PlayerStats stats = currentFight.getOrCreatePlayerStats(playerName);
            stats.recordAttack(weaponSpeed, currentTick);
        }

        // Overall gets updated via syncOverallWithCurrent() every tick
    }

    /**
     * Check if there's an active fight
     */
    public boolean hasActiveFight()
    {
        return (currentFight != null && currentFight.isActive());
    }

    /**
     * Add fight to history
     */
    private void addToHistory(Fight fight)
    {
        fightHistory.add(0, fight); // Add to beginning

        // Limit history size
        int maxHistory = plugin.getConfig().maxSessionHistory();
        while (fightHistory.size() > maxHistory)
        {
            fightHistory.remove(fightHistory.size() - 1);
        }

        log.debug("Added fight to history. Total: {}", fightHistory.size());
    }

    /**
     * Clear fight history
     */
    public void clearHistory()
    {
        fightHistory.clear();
        log.debug("Cleared fight history");
        updatePanel();
    }

    /**
     * Get fight history (immutable)
     */
    public List<Fight> getFightHistory()
    {
        return Collections.unmodifiableList(fightHistory);
    }

    /**
     * Update UI panel
     */
    private void updatePanel()
    {
        if (plugin.getPanel() != null)
        {
            plugin.getPanel().updatePanel();
        }
    }
}