package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.helpers.FightTracker;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;

@Slf4j
public class CombatEventListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

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

        // Check if this NPC is our current fight target
        // IMPORTANT: Only end fight if this is the EXACT NPC we're fighting
        boolean isCurrentTarget = (npcId == currentFight.getBossNpcId() &&
                npcName != null &&
                npcName.equals(currentFight.getBossName()));

        // Additional check: did we actually deal damage to this NPC?
        // This prevents other players' kills from ending our fight
        boolean weAttackedThisNPC = currentFight.getTotalDamage() > 0;

        if (isCurrentTarget && weAttackedThisNPC)
        {
            log.debug("Fight target {} died, ending fight", npcName);

            // End the fight on killing blow
            if (plugin.getConfig().endOnBossDeath())
            {
                fightTracker.endCurrentFight();
            }
        }
        else if (npcId == currentFight.getBossNpcId())
        {
            // Same NPC type died but we didn't attack it (someone else killed it)
            log.debug("NPC {} died nearby but not our target (no damage dealt)", npcName);
        }
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

        // End fight if player dies (configurable)
        if (fightTracker.hasActiveFight())
        {
            log.debug("Ending fight due to player death");
            fightTracker.endCurrentFight();
        }
    }
}