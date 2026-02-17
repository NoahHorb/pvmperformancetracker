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

        String npcName = npc.getName();
        int npcId = npc.getId();
        int damage = hitsplat.getAmount();
        int currentTick = client.getTickCount();

        String playerName = determineHitsplatSource(npc);
        if (playerName == null)
        {
            log.debug("Skipping hitsplat to {}: could not determine source (not party member or local player)", npcName);
            return; // Not from party member or local player
        }
        log.debug("Processing hitsplat: {} damage to {} from {}", damage, npcName, playerName);

        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null)
        {
            return;
        }


        Fight currentFight = fightTracker.getCurrentFight();

        log.debug("Hitsplat check: currentFight={}, active={}, targetId={}, currentId={}",
                currentFight != null ? currentFight.getBossName() : "null",
                currentFight != null ? currentFight.isActive() : false,
                npcId,
                currentFight != null ? currentFight.getBossNpcId() : -1);

        if (currentFight == null || !currentFight.isActive())
        {
            // No active fight - start new one on this first hitsplat
            log.debug("Starting new fight on first hitsplat to {} (damage: {})", npcName, damage);
            fightTracker.startNewFight(npcName, npcId);
            currentFight = fightTracker.getCurrentFight();
        }
        else if (currentFight.getBossNpcId() != npcId)
        {
            // Different target than current fight
            log.debug("Different NPC - current: {}, new: {}", currentFight.getBossNpcId(), npcId);
            fightTracker.endCurrentFight();
            fightTracker.startNewFight(npcName, npcId);
            currentFight = fightTracker.getCurrentFight();
        }

        // Record the damage (even if 0)
        if (currentFight != null && currentFight.isActive())
        {
            fightTracker.addDamageDealt(playerName, damage, npcName);

            // Track this NPC for death detection
            if (plugin.getCombatEventListener() != null)
            {
                plugin.getCombatEventListener().recordDamageToNPC(npc);
            }

            log.debug("{} dealt {} damage to {} (fight: {})",
                    playerName, damage, npcName, currentFight.getBossName());
        }
        else
        {
            log.debug("NOT recording damage - fight is null or inactive");
        }
//
//        // Start a new fight if not already active
//        if (!fightTracker.hasActiveFight())
//        {
//            fightTracker.startNewFight(npcName, npcId);
//            log.debug("Fight started against {} (ID: {})", npcName, npcId);
//        }
//
//        // Verify we're hitting the correct boss
//        Fight currentFight = fightTracker.getCurrentFight();
//        if (currentFight == null || currentFight.getBossNpcId() != npcId)
//        {
//            return;
//        }
//
//        // Determine which player caused this hitsplat
//        String damageDealer = determineHitsplatSource(npc);
//        if (damageDealer != null)
//        {
//            PlayerStats stats = currentFight.getPlayerStats().get(damageDealer);
//            if (stats != null)
//            {
//                stats.addDamageDealt(damage, currentTick, npcName);
//            }
//        }
    }
    /**
     * Handle damage taken by player from NPCs
     */
    private void handleDamageToPlayer(Hitsplat hitsplat)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return;
        }

        FightTracker fightTracker = plugin.getFightTracker();
        if (fightTracker == null || !fightTracker.hasActiveFight())
        {
            return;
        }

        Fight currentFight = fightTracker.getCurrentFight();
        if (currentFight == null)
        {
            return;
        }

        int damage = hitsplat.getAmount();
        int currentTick = client.getTickCount();

        // Find the NPC that dealt this damage
        NPC attackingNpc = findAttackingNpc(currentFight.getBossNpcId());

        if (attackingNpc == null)
        {
            log.debug("Could not determine attacking NPC");
            return;
        }

        // Get NPC stats using the NPC OBJECT for variant detection
        NpcCombatStats npcStats = null;
        if (plugin.getNpcStatsProvider() != null && plugin.getNpcStatsProvider().isLoaded())
        {
            npcStats = plugin.getNpcStatsProvider().getNpcStats(attackingNpc);  // ← USE NPC OBJECT
        }

        // Determine attack style used
        String attackStyleUsed = null;

        if (npcStats != null)
        {
            // Check if NPC has multiple attack styles
            if (npcStats.hasMultipleAttackStyles())
            {
                // NPC has multiple styles - track animations/projectiles
                if (plugin.getNpcAttackTracker() != null)
                {
                    int npcIndex = attackingNpc.getIndex();
                    attackStyleUsed = plugin.getNpcAttackTracker().getBestKnownAttackStyle(npcIndex);

                    // Confirm this attack style
                    if (attackStyleUsed != null)
                    {
                        plugin.getNpcAttackTracker().confirmAttackStyle(npcIndex, attackStyleUsed);
                    }
                }

                // Fallback to primary if not tracked yet
                if (attackStyleUsed == null)
                {
                    attackStyleUsed = npcStats.getPrimaryAttackStyle();
                    log.debug("No tracked style yet, using primary: {}", attackStyleUsed);
                }
            }
            else
            {
                // NPC has single attack style - use it directly
                attackStyleUsed = npcStats.getPrimaryAttackStyle();
            }
        }

        // Classify the damage type
        DamageType damageType = classifyDamage(attackingNpc, damage, attackStyleUsed, npcStats);

        // Record damage taken
        String playerName = localPlayer.getName();
        PlayerStats stats = currentFight.getPlayerStats().get(playerName);
        if (playerName != null)
        {
            if (stats != null)
            {
                stats.addDamageTaken(damage, currentTick, damageType);
            }
        }

        log.debug("Player took {} damage (type: {}, style: {})",
                damage, damageType, attackStyleUsed != null ? attackStyleUsed : "unknown");

        // Calculate death probability if we have NPC stats
        if (npcStats != null && attackStyleUsed != null)
        {
            double deathProbability = calculateDeathProbability(attackingNpc, attackStyleUsed, npcStats);
            if (deathProbability > 0.0)
            {
                stats.addDeathChance(deathProbability);
                log.debug("Death probability: {}% at (rolled {})",
                        String.format("%.1f", deathProbability * 100), damage);
            }
        }
    }

    /**
     * Find the NPC that is attacking the player
     */
    private NPC findAttackingNpc(int bossNpcId)
    {
        // Find the boss NPC in the world
        for (NPC npc : client.getNpcs())
        {
            if (npc != null && npc.getId() == bossNpcId)
            {
                return npc;
            }
        }
        return null;
    }

    /**
     * Classify damage type
     */
    private DamageType classifyDamage(NPC attackingNpc, int damage, String attackStyleUsed, NpcCombatStats npcStats)
    {
        if (attackingNpc == null || npcStats == null)
        {
            return DamageType.UNKNOWN;
        }

        // If attack style is unknown, try to get primary style as fallback
        if (attackStyleUsed == null || attackStyleUsed.isEmpty())
        {
            attackStyleUsed = npcStats.getPrimaryAttackStyle();
            log.debug("Using fallback attack style: {}", attackStyleUsed);
        }

        // Check if player had the CORRECT prayer active for this specific attack style
        boolean hadCorrectPrayer = isPrayerActiveForStyle(attackStyleUsed);

        // If prayer was active and still took damage, it's UNAVOIDABLE
        // (either typeless damage, or damage that goes through prayer)
        if (hadCorrectPrayer && damage > 0)
        {
            return DamageType.UNAVOIDABLE;
        }

        // If prayer wasn't active but should have been, it's PRAYABLE
        if (!hadCorrectPrayer && attackStyleUsed != null)
        {
            // Check if this attack style is prayable (melee/ranged/magic)
            String normalized = attackStyleUsed.toLowerCase();
            if (normalized.equals("melee") || normalized.equals("slash") ||
                    normalized.equals("stab") || normalized.equals("crush") ||
                    normalized.equals("ranged") || normalized.equals("magic"))
            {
                return DamageType.PRAYABLE;
            }
        }

        // Otherwise it's AVOIDABLE (could have been dodged by positioning/movement)
        return damage == 0 ? DamageType.AVOIDABLE : DamageType.UNAVOIDABLE;
    }


    /**
     * Check if player has correct prayer for the attack style
     */
    private boolean isPrayerActiveForStyle(String attackStyle)
    {
        if (attackStyle == null)
        {
            return false;
        }

        String normalized = attackStyle.toLowerCase();

        // Check melee prayers
        if (normalized.equals("melee") || normalized.equals("slash") ||
                normalized.equals("stab") || normalized.equals("crush"))
        {
            return client.isPrayerActive(Prayer.PROTECT_FROM_MELEE);
        }

        // Check ranged prayer
        if (normalized.equals("ranged"))
        {
            return client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES);
        }

        // Check magic prayer
        if (normalized.equals("magic"))
        {
            return client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
        }

        return false;
    }

    /**
     * Calculate death probability
     */
    private double calculateDeathProbability(NPC attackingNpc, String attackStyleUsed, NpcCombatStats npcStats)
    {

        double deathProb = 0.0;
        if (attackingNpc == null || npcStats == null)
        {
            return deathProb;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return deathProb;
        }

        // Get current HP
        int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);

        // Check if player has the CORRECT prayer active for this attack style
        boolean hasCorrectPrayer = isPrayerActiveForStyle(attackStyleUsed);

        // Calculate death probability using combat formulas
        CombatFormulas combatFormulas = plugin.getCombatFormulas();
        if (combatFormulas != null)
        {
            deathProb = combatFormulas.calculateDeathProbability(
                    currentHp, npcStats, attackStyleUsed, hasCorrectPrayer);
        }
        return deathProb;
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

        if (plugin.getConfig().trackBossesOnly())
        {
            return plugin.getBossDetectionHelper() != null &&
                    plugin.getBossDetectionHelper().isBoss(npc);
        }

        return true;
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

        // Additional fallback: check if local player is in combat stance and this is the nearest hostile NPC
        if (localPlayer != null && localPlayer.getAnimation() != -1)
        {
            Actor localTarget = localPlayer.getInteracting();
            // If player has no current target but is animating, this might be the target
            if (localTarget == null || localTarget == target)
            {
                lastAttackedNPC = target;
                return localPlayer.getName();
            }
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
}