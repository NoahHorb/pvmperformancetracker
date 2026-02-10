package net.runelite.client.plugins.pvmperformancetracker.models;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single fight or overall tracking session
 * Replaces the old CombatSession model
 */
@Data
public class Fight
{
    private final String fightId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    private String bossName;
    private int bossNpcId;

    private int startTick;
    private int endTick;
    private boolean active;

    // For Overall mode - track active combat ticks only (not downtime between fights)
    private int activeCombatTicks;
    private boolean currentlyInCombat;

    // Map of player name -> their stats
    private final Map<String, PlayerStats> playerStats = new ConcurrentHashMap<>();

    // Track the local player's name
    private String localPlayerName;

    public Fight()
    {
        this.fightId = java.util.UUID.randomUUID().toString();
        this.startTime = LocalDateTime.now();
        this.active = true;
        this.startTick = 0;
        this.endTick = 0;
        this.activeCombatTicks = 0;
        this.currentlyInCombat = false;
    }

    public Fight(int currentTick)
    {
        this();
        this.startTick = currentTick;
    }

    /**
     * Get or create player stats for a given player
     */
    public PlayerStats getOrCreatePlayerStats(String playerName)
    {
        return playerStats.computeIfAbsent(playerName, k -> new PlayerStats(playerName));
    }

    /**
     * End the fight
     */
    public void endFight(int currentTick)
    {
        this.active = false;
        this.endTick = currentTick;
        this.endTime = LocalDateTime.now();

        // Finalize all player stats
        for (PlayerStats stats : playerStats.values())
        {
            stats.finalizeFight(getDurationTicks());
        }
    }

    /**
     * Get duration in ticks
     * For Overall mode: returns only active combat ticks (excludes downtime)
     * For Current Fight: returns full fight duration
     */
    public int getDurationTicks()
    {
        // For Overall mode, use active combat ticks only
        if (bossName != null && bossName.equals("Overall") && activeCombatTicks > 0)
        {
            return activeCombatTicks;
        }

        // For Current Fight mode or if no active tracking yet
        if (active)
        {
            return endTick - startTick;
        }
        return endTick - startTick;
    }

    /**
     * Update the current tick (for active fights)
     */
    public void updateCurrentTick(int currentTick)
    {
        if (active)
        {
            this.endTick = currentTick;

            // For Overall mode: only count ticks when actively in combat
            if (currentlyInCombat)
            {
                activeCombatTicks++;
            }
        }
    }

    /**
     * Mark that combat is currently happening (Overall mode tracking)
     */
    public void setInCombat(boolean inCombat)
    {
        this.currentlyInCombat = inCombat;
    }

    /**
     * Get total damage dealt by all players
     */
    public int getTotalDamage()
    {
        return playerStats.values().stream()
                .mapToInt(PlayerStats::getDamageDealt)
                .sum();
    }

    /**
     * Get local player's stats
     */
    public PlayerStats getLocalPlayerStats()
    {
        if (localPlayerName == null)
        {
            return null;
        }
        return playerStats.get(localPlayerName);
    }

    /**
     * Check if this fight has any activity
     * Changed to allow 0 damage (splashing) - just needs player stats
     */
    public boolean hasActivity()
    {
        return !playerStats.isEmpty();
    }

    /**
     * Get a copy of all player stats (for thread safety)
     */
    public Map<String, PlayerStats> getPlayerStats()
    {
        return new HashMap<>(playerStats);
    }
}