package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.helpers.CombatSessionManager;
import net.runelite.client.plugins.pvmperformancetracker.model.CombatSession;

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

    public void onActorDeath(ActorDeath event)
    {
        if (!plugin.getConfig().enableTracking())
        {
            return;
        }

        Actor actor = event.getActor();

        // Check if player died
        if (actor.equals(client.getLocalPlayer()))
        {
            handlePlayerDeath();
            return;
        }

        // Check if an NPC died
        if (actor instanceof NPC)
        {
            handleNPCDeath((NPC) actor);
        }
    }

    public void onInteractingChanged(InteractingChanged event)
    {
        if (!plugin.getConfig().enableTracking())
        {
            return;
        }

        Actor source = event.getSource();
        Actor target = event.getTarget();

        // Only track when local player starts/stops interacting
        if (!source.equals(client.getLocalPlayer()))
        {
            return;
        }

        // Player started attacking an NPC
        if (target instanceof NPC)
        {
            handleCombatStart((NPC) target);
        }
        // Player stopped attacking (target is null)
        else if (target == null)
        {
            handleCombatStop();
        }
    }

    private void handlePlayerDeath()
    {
        log.debug("Player died");

        CombatSessionManager sessionManager = plugin.getSessionManager();

        if (plugin.getConfig().endOnDeath() && sessionManager.hasActiveSession())
        {
            log.debug("Ending session due to player death");
            sessionManager.endCurrentSession();
        }
    }

    private void handleNPCDeath(NPC npc)
    {
        CombatSessionManager sessionManager = plugin.getSessionManager();
        CombatSession session = sessionManager.getCurrentSession();

        if (session == null || !session.isActive())
        {
            return;
        }

        String npcName = npc.getName();

        // Check if this NPC was the primary target
        if (npcName != null && npcName.equals(session.getPrimaryTarget()))
        {
            log.debug("Primary target {} died", npcName);

            // End session if configured to end on boss death
            if (plugin.getConfig().endOnBossDeath() &&
                    plugin.getBossDetectionHelper().isBoss(npc))
            {
                log.debug("Ending session due to boss death");
                sessionManager.endCurrentSession();
            }
        }
    }

    private void handleCombatStart(NPC npc)
    {
        CombatSessionManager sessionManager = plugin.getSessionManager();

        // Don't start a new session if one is already active
        if (sessionManager.hasActiveSession())
        {
            return;
        }

        // Check if we should track this NPC
        if (!shouldStartSessionForNPC(npc))
        {
            return;
        }

        if (plugin.getConfig().autoStartCombat())
        {
            log.debug("Auto-starting combat session for {}", npc.getName());
            sessionManager.startNewSession();

            // Set the primary target
            CombatSession session = sessionManager.getCurrentSession();
            if (session != null)
            {
                session.setPrimaryTarget(npc.getName());
            }
        }
    }

    private void handleCombatStop()
    {
        CombatSessionManager sessionManager = plugin.getSessionManager();

        // Just update activity time, let the timeout handle ending the session
        if (sessionManager.hasActiveSession())
        {
            log.debug("Player stopped attacking, session will timeout if no further activity");
        }
    }

    private boolean shouldStartSessionForNPC(NPC npc)
    {
        if (npc == null || npc.getName() == null)
        {
            return false;
        }

        // If tracking bosses only, check if this is a boss
        if (plugin.getConfig().trackBossesOnly())
        {
            return plugin.getBossDetectionHelper().isBoss(npc);
        }

        // If tracking all combat, return true
        if (plugin.getConfig().trackAllCombat())
        {
            return true;
        }

        return false;
    }
}