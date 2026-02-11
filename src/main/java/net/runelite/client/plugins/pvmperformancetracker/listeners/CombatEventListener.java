package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.helpers.FightTracker;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class CombatEventListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    // Track NPCs we've damaged in current fight
    private final Set<Integer> damagedNpcIndices = new HashSet<>();

    public CombatEventListener(PvMPerformanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }

    /**
     * Handle actor deaths - this is where fights END
     */
    public void onActorDeath(ActorDeath event)
    {
        if (!plugin.getConfig().enableTracking())
        {
            return;
        }

        Actor actor = event.getActor();

        // Check if an NPC died - END FIGHT on NPC death
        if (actor instanceof NPC)
        {
            handleNPCDeath((NPC) actor);
        }

        // Check if player died (local player)
        if (actor.equals(client.getLocalPlayer()))
        {
            handlePlayerDeath();
        }
    }

    /**
     * NPC died - check if it's our current fight target and end fight
     * Uses NPC index tracking to identify which specific NPC we're fighting
     */
    private void handleNPCDeath(NPC npc)
    {
        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null)
        {
            return;
        }

        Fight currentFight = fightTracker.getCurrentFight();

        if (currentFight == null || !currentFight.isActive())
        {
            return;
        }

        String npcName = npc.getName();
        int npcId = npc.getId();
        int npcIndex = npc.getIndex();

        // Check if this NPC matches our fight parameters (ID + name)
        boolean matchesFightParams = (npcId == currentFight.getBossNpcId() &&
                npcName != null &&
                npcName.equals(currentFight.getBossName()));

        if (!matchesFightParams)
        {
            return; // Different NPC type entirely
        }

        // Check if this is the specific NPC we damaged
        boolean isOurNPC = damagedNpcIndices.contains(npcIndex);

        if (isOurNPC)
        {
            log.debug("Our fight target {} died (index: {}), ending fight", npcName, npcIndex);

            if (plugin.getConfig().endOnBossDeath())
            {
                fightTracker.endCurrentFight();
                damagedNpcIndices.clear(); // Clear for next fight
            }
        }
        else
        {
            // Same NPC type but not one we damaged
            log.debug("NPC {} died nearby (index: {}) but we didn't damage it (damaged indices: {})",
                    npcName, npcIndex, damagedNpcIndices);
        }
    }

    /**
     * Track that we damaged this NPC
     * Called from HitsplatListener when we deal damage
     */
    public void recordDamageToNPC(NPC npc)
    {
        if (npc != null)
        {
            damagedNpcIndices.add(npc.getIndex());
            log.debug("Recorded damage to NPC index: {}", npc.getIndex());
        }
    }

    /**
     * Clear damaged NPC tracking when fight starts
     */
    public void onFightStart()
    {
        damagedNpcIndices.clear();
        log.debug("Cleared damaged NPC indices for new fight");
    }

    /**
     * Player died - optionally end fight
     */
    private void handlePlayerDeath()
    {
        log.debug("Local player died");

        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null)
        {
            return;
        }

        // End fight if player dies
        if (fightTracker.hasActiveFight())
        {
            log.debug("Ending fight due to player death");
            fightTracker.endCurrentFight();
            damagedNpcIndices.clear();
        }
    }
}