package net.runelite.client.plugins.pvmperformancetracker.party;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;

import java.util.*;

/**
 * Manages party member tracking and stats synchronization
 * Integrates with RuneLite's Party plugin
 */
@Slf4j
public class PartyStatsManager
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;
    private final PartyService partyService;

    // Map of party member ID -> player name
    private final Map<Long, String> partyMemberNames = new HashMap<>();

    // Track which players are in our party
    private final Set<String> partyPlayers = new HashSet<>();

    public PartyStatsManager(PvMPerformanceTrackerPlugin plugin, Client client, PartyService partyService)
    {
        this.plugin = plugin;
        this.client = client;
        this.partyService = partyService;
    }

    /**
     * Update party member list
     */
    public void updatePartyMembers()
    {
        partyPlayers.clear();
        partyMemberNames.clear();

        if (partyService == null || partyService.getMembers().isEmpty())
        {
            log.debug("No party service or no party members");
            return;
        }

        // Add local player
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null && localPlayer.getName() != null)
        {
            partyPlayers.add(localPlayer.getName());
        }

        // Add party members
        for (PartyMember member : partyService.getMembers())
        {
            String memberName = member.getDisplayName();
            if (memberName != null && !memberName.isEmpty())
            {
                partyPlayers.add(memberName);
                partyMemberNames.put(member.getMemberId(), memberName);
                log.debug("Added party member: {}", memberName);
            }
        }

        log.debug("Updated party members: {} players", partyPlayers.size());
    }

    /**
     * Check if a player is in the party
     */
    public boolean isInParty(String playerName)
    {
        return partyPlayers.contains(playerName);
    }

    /**
     * Get all party member names
     */
    public Set<String> getPartyMemberNames()
    {
        return new HashSet<>(partyPlayers);
    }

    /**
     * Get player name by party member ID
     */
    public String getPlayerNameByMemberId(long memberId)
    {
        return partyMemberNames.get(memberId);
    }

    /**
     * Check if party tracking is enabled
     */
    public boolean isPartyTrackingEnabled()
    {
        return plugin.getConfig().enablePartyTracking() && !partyPlayers.isEmpty();
    }

    /**
     * Attempt to find a party member by their in-game player object
     */
    public String findPartyMemberName(Player player)
    {
        if (player == null || player.getName() == null)
        {
            return null;
        }

        String playerName = player.getName();

        if (partyPlayers.contains(playerName))
        {
            return playerName;
        }

        return null;
    }

    /**
     * Get nearby party members (for damage tracking)
     */
    public List<Player> getNearbyPartyMembers()
    {
        List<Player> nearbyParty = new ArrayList<>();

        if (client.getPlayers() == null)
        {
            return nearbyParty;
        }

        for (Player player : client.getPlayers())
        {
            if (player != null && player.getName() != null)
            {
                if (partyPlayers.contains(player.getName()))
                {
                    nearbyParty.add(player);
                }
            }
        }

        return nearbyParty;
    }

    /**
     * Track damage dealt by a party member (estimated from hitsplats)
     */
    public void trackPartyMemberDamage(String playerName, int damage, String target)
    {
        if (!isInParty(playerName))
        {
            return;
        }

        // Add to fight tracker
        plugin.getFightTracker().addDamageDealt(playerName, damage, target);

        log.debug("Tracked party member damage: {} dealt {} to {}", playerName, damage, target);
    }

    /**
     * Estimate party member attack (rough approximation)
     */
    public void estimatePartyMemberAttack(String playerName, int weaponSpeed)
    {
        if (!isInParty(playerName))
        {
            return;
        }

        // Record the attack for tick loss calculation
        plugin.getFightTracker().recordAttack(playerName, weaponSpeed);
    }
}