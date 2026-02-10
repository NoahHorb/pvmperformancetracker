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

    // Overall fight - calculated from fight history
    private Fight overallFight;

    @Getter
    private final List<Fight> fightHistory = new ArrayList<>();

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

        // Overall is now calculated from fight history, no need to initialize

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

    updatePanel();
}

/**
 * Reset overall tracking by clearing fight history
 */
public void resetOverallTracking()
{
    clearHistory();
    log.debug("Reset overall tracking (cleared fight history)");
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
    }

    // Overall is calculated from fight history, no need to track ticks
    // No timeout checking - fights end on NPC death or manual end
}

/**
 * Get current tick
 */
public int getCurrentTick()
{
    return currentTick;
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

    // Overall is now calculated from fight history, not tracked separately
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

    // Overall is now calculated from fight history, not tracked separately
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
 * Get Overall fight - aggregates all fights from history
 * This ensures Overall only includes active combat (no downtime between fights)
 */
public Fight getOverallFight()
{
    // Create a synthetic Overall fight from history
    Fight aggregated = new Fight(0);
    aggregated.setBossName("Overall");

    String localPlayerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "Unknown";
    aggregated.setLocalPlayerName(localPlayerName);

    // Keep Overall "active" so it shows in the UI
    // It will just show 0s until there's fight history

    if (fightHistory.isEmpty())
    {
        // No history yet, return empty but active fight
        return aggregated;
    }

    // Aggregate all completed fights
    int totalActiveTicks = 0;

    for (Fight fight : fightHistory)
    {
        totalActiveTicks += fight.getDurationTicks();

        // Merge each player's stats from this fight
        for (String playerName : fight.getPlayerStats().keySet())
        {
            PlayerStats fightStats = fight.getPlayerStats().get(playerName);
            PlayerStats aggregatedStats = aggregated.getOrCreatePlayerStats(playerName);

            // Merge the stats
            aggregatedStats.setDamageDealt(aggregatedStats.getDamageDealt() + fightStats.getDamageDealt());
            aggregatedStats.setTotalAttacks(aggregatedStats.getTotalAttacks() + fightStats.getTotalAttacks());
            aggregatedStats.setSuccessfulHits(aggregatedStats.getSuccessfulHits() + fightStats.getSuccessfulHits());
            aggregatedStats.setAttackingTicksLost(aggregatedStats.getAttackingTicksLost() + fightStats.getAttackingTicksLost());

            // Defensive stats
            aggregatedStats.setAvoidableDamageTaken(aggregatedStats.getAvoidableDamageTaken() + fightStats.getAvoidableDamageTaken());
            aggregatedStats.setPrayableDamageTaken(aggregatedStats.getPrayableDamageTaken() + fightStats.getPrayableDamageTaken());
            aggregatedStats.setUnavoidableDamageTaken(aggregatedStats.getUnavoidableDamageTaken() + fightStats.getUnavoidableDamageTaken());
        }
    }

    // Set the aggregated active combat ticks
    aggregated.setActiveCombatTicks(totalActiveTicks);

    return aggregated;
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