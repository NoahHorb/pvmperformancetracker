package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.enums.DamageType;
import net.runelite.client.plugins.pvmperformancetracker.helpers.CombatFormulas;
import net.runelite.client.plugins.pvmperformancetracker.helpers.FightTracker;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;
import net.runelite.client.plugins.pvmperformancetracker.models.NpcCombatStats;
import net.runelite.client.plugins.pvmperformancetracker.models.PlayerStats;
import net.runelite.client.plugins.pvmperformancetracker.party.PartyStatsManager;

@Slf4j
public class HitsplatListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    // Track the last NPC the local player attacked (for hitsplat attribution)
    private NPC lastAttackedNPC;

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
            log.debug("Skipping NPC: not tracked");
            return;
        }

        // Only track player damage hitsplats
        if (!isPlayerDamageHitsplat(hitsplat))
        {
            log.debug("Skipping hitsplat: not player damage type");
            return;
        }

        int damage = hitsplat.getAmount();
        String targetName = npc.getName();
        int targetId = npc.getId();

        // Try to determine which player dealt this damage (PARTY MEMBERS ONLY)
        String playerName = determineHitsplatSource(npc);

        if (playerName == null)
        {
            log.debug("Skipping hitsplat to {}: could not determine source (not party member or local player)", targetName);
            return; // Not from party member or local player
        }

        log.debug("Processing hitsplat: {} damage to {} from {}", damage, targetName, playerName);

        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null)
        {
            return;
        }

        // START FIGHT ON FIRST HITSPLAT
        Fight currentFight = fightTracker.getCurrentFight();

        log.debug("Hitsplat check: currentFight={}, active={}, targetId={}, currentId={}",
                currentFight != null ? currentFight.getBossName() : "null",
                currentFight != null ? currentFight.isActive() : false,
                targetId,
                currentFight != null ? currentFight.getBossNpcId() : -1);

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
            log.debug("Different NPC - current: {}, new: {}", currentFight.getBossNpcId(), targetId);
            fightTracker.endCurrentFight();
            fightTracker.startNewFight(targetName, targetId);
            currentFight = fightTracker.getCurrentFight();
        }

        // Record the damage (even if 0)
        if (currentFight != null && currentFight.isActive())
        {
            fightTracker.addDamageDealt(playerName, damage, targetName);

            // Track this NPC for death detection
            if (plugin.getCombatEventListener() != null)
            {
                plugin.getCombatEventListener().recordDamageToNPC(npc);
            }

            log.debug("{} dealt {} damage to {} (fight: {})",
                    playerName, damage, targetName, currentFight.getBossName());
        }
        else
        {
            log.debug("NOT recording damage - fight is null or inactive");
        }
    }

    /**
     * Handle damage taken by the local player
     */
    private void handleDamageToPlayer(Hitsplat hitsplat)
    {
        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null || !fightTracker.hasActiveFight())
        {
            return; // No active fight to track
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return;
        }

        Fight currentFight = fightTracker.getCurrentFight();
        if (currentFight == null || !currentFight.isActive())
        {
            return;
        }

        int damage = hitsplat.getAmount();
        int currentTick = fightTracker.getCurrentTick();

        // Get local player stats
        PlayerStats playerStats = currentFight.getLocalPlayerStats();
        if (playerStats == null)
        {
            return;
        }

        // Get current HP (before hit)
        int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);

        // Calculate death probability BEFORE this hit
        double deathProbability = calculateDeathProbability(currentHp, currentFight, damage);
        if (deathProbability > 0.0)
        {
            playerStats.addDeathChance(deathProbability);
            log.debug("Death probability: {}% at {} HP", deathProbability * 100, currentHp);
        }

        // Classify the damage type
        DamageType damageType = classifyDamage(currentFight, hitsplat);

        // Record the damage
        playerStats.addDamageTaken(damage, damageType, currentTick);

        log.debug("Player took {} {} damage (HP: {} -> {})",
                damage, damageType, currentHp, currentHp - damage);
    }

    /**
     * Calculate probability of death from this hit
     */
    private double calculateDeathProbability(int currentHp, Fight fight, int damageAmount)
    {
        if (currentHp <= 0 || damageAmount == 0)
        {
            return 0.0;
        }

        // Get NPC stats
        NpcCombatStats npcStats = null;
        if (plugin.getNpcStatsProvider() != null && plugin.getNpcStatsProvider().isLoaded())
        {
            npcStats = plugin.getNpcStatsProvider().getNpcStats(fight.getBossNpcId());
        }

        if (npcStats == null)
        {
            // Fallback: simple calculation based on damage dealt
            // If damage >= current HP, there was death risk
            if (damageAmount >= currentHp)
            {
                // Rough estimate: assume 50% hit chance, uniform damage distribution
                return 0.5 * (damageAmount - currentHp + 1.0) / (damageAmount + 1.0);
            }
            return 0.0;
        }

        // Check if prayer is active
        boolean isPrayerActive = isPrayerActive(npcStats);

        // Use combat formulas to calculate death probability
        CombatFormulas formulas = plugin.getCombatFormulas();
        if (formulas != null)
        {
            return formulas.calculateDeathProbability(currentHp, npcStats, isPrayerActive);
        }

        return 0.0;
    }

    /**
     * Check if player has appropriate protection prayer active
     */
    private boolean isPrayerActive(NpcCombatStats npcStats)
    {
        String attackType = npcStats.getPrimaryAttackType();
        if (attackType == null)
        {
            return false;
        }

        // Check if relevant protection prayer is active
        switch (attackType.toLowerCase())
        {
            case "melee":
                return client.isPrayerActive(Prayer.PROTECT_FROM_MELEE);
            case "ranged":
                return client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES);
            case "magic":
                return client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
            default:
                return false;
        }
    }

    /**
     * Classify damage as Avoidable, Prayable, or Unavoidable
     */
    private DamageType classifyDamage(Fight fight, Hitsplat hitsplat)
    {
        // Get NPC stats
        NpcCombatStats npcStats = null;
        if (plugin.getNpcStatsProvider() != null && plugin.getNpcStatsProvider().isLoaded())
        {
            npcStats = plugin.getNpcStatsProvider().getNpcStats(fight.getBossNpcId());
        }

        if (npcStats == null)
        {
            return DamageType.UNKNOWN;
        }

        // Check if appropriate prayer was active
        boolean hadCorrectPrayer = isPrayerActive(npcStats);

        // If prayer was active and still took damage, it's UNAVOIDABLE
        if (hadCorrectPrayer && hitsplat.getAmount() > 0)
        {
            return DamageType.UNAVOIDABLE;
        }

        // If prayer wasn't active but should have been, it's PRAYABLE
        if (!hadCorrectPrayer)
        {
            return DamageType.PRAYABLE;
        }

        // Otherwise it's AVOIDABLE (could have been dodged by positioning/movement)
        return DamageType.AVOIDABLE;
    }

    /**
     * Determine which player caused the hitsplat
     * ONLY RETURNS: local player OR party members (NOT random nearby players)
     */
    private String determineHitsplatSource(NPC target)
    {
        Player localPlayer = client.getLocalPlayer();

        // Check if local player is attacking this NPC
        if (localPlayer != null && localPlayer.getInteracting() == target)
        {
            lastAttackedNPC = target; // Track for fallback
            return localPlayer.getName();
        }

        // Fallback: if local player recently attacked this NPC (getInteracting can be null between attacks)
        if (localPlayer != null && target == lastAttackedNPC)
        {
            return localPlayer.getName();
        }

        // Check party members ONLY (if party tracking is enabled)
        PartyStatsManager partyManager = plugin.getPartyStatsManager();
        if (partyManager != null && partyManager.isPartyTrackingEnabled())
        {
            // Get party members who are nearby
            for (Player player : partyManager.getNearbyPartyMembers())
            {
                if (player.getInteracting() == target)
                {
                    return player.getName();
                }
            }
        }

        // NO FALLBACK to random players - if we can't confirm it's local player or party member, return null
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