package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.helpers.FightTracker;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;
import net.runelite.client.plugins.pvmperformancetracker.party.PartyStatsManager;

@Slf4j
public class HitsplatListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    public HitsplatListener(PvMPerformanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }

    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (!plugin.getConfig().enableTracking())
        {
            return;
        }

        Actor target = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        // Handle damage TO NPCs (offensive tracking)
        if (target instanceof NPC)
        {
            handleDamageToNPC((NPC) target, hitsplat);
        }

        // Handle damage TO players (defensive tracking - local player only)
        if (target instanceof Player && target.equals(client.getLocalPlayer()))
        {
            handleDamageToPlayer(hitsplat);
        }
    }

    /**
     * Handle damage dealt to NPCs - this is where fights start
     */
    private void handleDamageToNPC(NPC npc, Hitsplat hitsplat)
    {
        // Check if we should track this NPC
        if (!shouldTrackNPC(npc))
        {
            return;
        }

        // Only track player damage hitsplats
        if (!isPlayerDamageHitsplat(hitsplat))
        {
            return;
        }

        int damage = hitsplat.getAmount();
        String targetName = npc.getName();
        int targetId = npc.getId();

        // Try to determine which player dealt this damage
        String playerName = determineHitsplatSource(npc);

        if (playerName == null)
        {
            return;
        }

        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null)
        {
            return;
        }

        // START FIGHT ON FIRST HITSPLAT
        // If no active fight, or active fight is against a different NPC, start new fight
        Fight currentFight = fightTracker.getCurrentFight();

        if (currentFight == null || !currentFight.isActive())
        {
            // No active fight - start new one on this first hitsplat
            log.debug("Starting new fight on first hitsplat to {} (damage: {})", targetName, damage);
            fightTracker.startNewFight(targetName, targetId);
            currentFight = fightTracker.getCurrentFight();
        }
        else if (currentFight.getBossNpcId() != targetId)
        {
            // Different target than current fight
            // In multi-combat, we need to decide: continue current fight or start new one?
            // For now: if this is a boss and current isn't, switch to boss
            // Otherwise, continue current fight and just track damage to this NPC too

            boolean currentIsBoss = plugin.getBossDetectionHelper().isBoss(npc);
            boolean newTargetIsBoss = plugin.getBossDetectionHelper().isBoss(npc);

            if (newTargetIsBoss && !currentIsBoss)
            {
                // Switch to boss target
                log.debug("Switching fight from {} to boss {}", currentFight.getBossName(), targetName);
                fightTracker.endCurrentFight();
                fightTracker.startNewFight(targetName, targetId);
                currentFight = fightTracker.getCurrentFight();
            }
            // Otherwise continue current fight, damage will be tracked under current fight
        }

        // Record the damage (even if 0)
        if (currentFight != null && currentFight.isActive())
        {
            fightTracker.addDamageDealt(playerName, damage, targetName);
            log.debug("{} dealt {} damage to {} (fight: {})",
                    playerName, damage, targetName, currentFight.getBossName());
        }
    }

    /**
     * Handle damage taken by the local player
     */
    private void handleDamageToPlayer(Hitsplat hitsplat)
    {
        // TODO: Implement defensive tracking
        // This will require damage classification using DamageClassifier
        // Need to track:
        // - Source NPC
        // - Animation/projectile
        // - Player's active prayer
        // - Classify as Avoidable/Prayable/Unavoidable
    }

    /**
     * Determine which player caused the hitsplat
     * Priority: local player > party members
     */
    private String determineHitsplatSource(NPC target)
    {
        Player localPlayer = client.getLocalPlayer();

        // Check if local player is attacking this NPC
        if (localPlayer != null && localPlayer.getInteracting() == target)
        {
            return localPlayer.getName();
        }

        // Check party members if party tracking is enabled
        PartyStatsManager partyManager = plugin.getPartyStatsManager();
        if (partyManager != null && partyManager.isPartyTrackingEnabled())
        {
            // Check nearby party members
            for (Player player : partyManager.getNearbyPartyMembers())
            {
                if (player.getInteracting() == target)
                {
                    return player.getName();
                }
            }
        }

        // Fallback: if no one is interacting but local player is in combat, assume it's them
        if (localPlayer != null)
        {
            // Check if player recently attacked (within last few ticks)
            Actor playerTarget = localPlayer.getInteracting();
            if (playerTarget == null || playerTarget == target)
            {
                // No current target or same target - likely this player
                return localPlayer.getName();
            }
        }

        return null;
    }

    /**
     * Check if hitsplat is player damage
     */
    private boolean isPlayerDamageHitsplat(Hitsplat hitsplat)
    {
        int type = hitsplat.getHitsplatType();

        return type == HitsplatID.DAMAGE_ME ||
                type == HitsplatID.DAMAGE_MAX_ME ||  // Max hit (bright red)
                type == HitsplatID.BLOCK_ME ||
                type == HitsplatID.DAMAGE_ME_CYAN ||
                type == HitsplatID.DAMAGE_ME_ORANGE ||
                type == HitsplatID.DAMAGE_ME_YELLOW ||
                type == HitsplatID.DAMAGE_ME_WHITE;
    }

    /**
     * Check if we should track this NPC
     */
    private boolean shouldTrackNPC(NPC npc)
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

        return true;
    }
}