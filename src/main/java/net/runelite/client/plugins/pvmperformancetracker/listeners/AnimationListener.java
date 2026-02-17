package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.enums.AnimationIds;
import net.runelite.client.plugins.pvmperformancetracker.helpers.FightTracker;
import net.runelite.client.plugins.pvmperformancetracker.helpers.WeaponSpeedHelper;
import net.runelite.client.plugins.pvmperformancetracker.party.PartyStatsManager;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class AnimationListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    // Common attack animations (expanded list)
    private static final Set<Integer> ATTACK_ANIMATIONS = new HashSet<>();

    static
    {
        // Melee animations
        ATTACK_ANIMATIONS.add(422);  // Unarmed punch
        ATTACK_ANIMATIONS.add(423);  // Unarmed kick
        ATTACK_ANIMATIONS.add(401);  // Bronze-Rune swords
        ATTACK_ANIMATIONS.add(406);  // Dragon longsword
        ATTACK_ANIMATIONS.add(407);  // Dragon dagger
        ATTACK_ANIMATIONS.add(428);  // Staff bash
        ATTACK_ANIMATIONS.add(440);  // Pickaxe
        ATTACK_ANIMATIONS.add(1658); // Whip
        ATTACK_ANIMATIONS.add(1378); // Dragon scimitar
        ATTACK_ANIMATIONS.add(7514); // Godsword
        ATTACK_ANIMATIONS.add(8145); // Rapier
        ATTACK_ANIMATIONS.add(4230); // Scythe
        ATTACK_ANIMATIONS.add(8056); // Scythe 2
        ATTACK_ANIMATIONS.add(390);  // Staff
        ATTACK_ANIMATIONS.add(386);  // 2h sword

        // Ranged animations
        ATTACK_ANIMATIONS.add(426);  // Shortbow
        ATTACK_ANIMATIONS.add(5061); // Blowpipe
        ATTACK_ANIMATIONS.add(7617); // Crossbow
        ATTACK_ANIMATIONS.add(7552); // Ballista
        ATTACK_ANIMATIONS.add(8291); // Bow of faerdhinen
        ATTACK_ANIMATIONS.add(929);  // Longbow
        ATTACK_ANIMATIONS.add(7554); // Heavy ballista
        ATTACK_ANIMATIONS.add(7555); // Light ballista

        // Magic animations
        ATTACK_ANIMATIONS.add(1162); // Standard spell
        ATTACK_ANIMATIONS.add(1167); // Ancient spell
        ATTACK_ANIMATIONS.add(1978); // Powered staff
        ATTACK_ANIMATIONS.add(8532); // Trident
        ATTACK_ANIMATIONS.add(7855); // Sanguinesti staff
        ATTACK_ANIMATIONS.add(9493); // Tumeken's shadow
    }

    public AnimationListener(PvMPerformanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }

    public void onAnimationChanged(AnimationChanged event)
    {
        if (!plugin.getConfig().enableTracking())
        {
            return;
        }

        Actor actor = event.getActor();

        // Track player animations
        if (actor instanceof Player)
        {
            handlePlayerAnimation((Player) actor);
        }
        // Track NPC animations
        else if (actor instanceof NPC)
        {
            handleNpcAnimation((NPC) actor);
        }
    }

    /**
     * Handle player animation changes
     */
    private void handlePlayerAnimation(Player player)
    {
        // CRITICAL: Only track local player OR party members
        if (!isPartyMemberOrLocal(player))
        {
            return; // Skip random nearby players
        }

        int animationId = player.getAnimation();

        // Check if this is an attack animation
        if (isAttackAnimation(animationId))
        {
            handleAttackAnimation(player, animationId);
        }
    }

    /**
     * Handle NPC animation changes
     * Tracks NPC attack animations to determine attack style
     */
    private void handleNpcAnimation(NPC npc)
    {
        FightTracker fightTracker = plugin.getFightTracker();

        // Only track if there's an active fight
        if (fightTracker == null || !fightTracker.hasActiveFight())
        {
            return;
        }

        // Only track the boss we're fighting
        int currentBossId = fightTracker.getCurrentFight().getBossNpcId();
        if (npc.getId() != currentBossId)
        {
            return;
        }

        int animationId = npc.getAnimation();

        // Record the NPC animation in the attack tracker
        if (plugin.getNpcAttackTracker() != null && animationId != -1)
        {
            plugin.getNpcAttackTracker().recordNpcAnimation(npc, animationId);
        }
    }

    /**
     * Check if player is local player or a party member
     */
    private boolean isPartyMemberOrLocal(Player player)
    {
        Player localPlayer = client.getLocalPlayer();

        // Check if this is the local player
        if (player.equals(localPlayer))
        {
            return true;
        }

        // Check if this is a party member
        PartyStatsManager partyManager = plugin.getPartyStatsManager();
        if (partyManager != null && partyManager.isPartyTrackingEnabled())
        {
            return partyManager.isPartyMember(player.getName());
        }

        return false; // Not local player and not in party
    }

    private boolean isAttackAnimation(int animationId)
    {
        return ATTACK_ANIMATIONS.contains(animationId);
    }

    private void handleAttackAnimation(Player player, int animationId)
    {
        FightTracker fightTracker = plugin.getFightTracker();

        // Only record if there's an active fight
        if (fightTracker == null || !fightTracker.hasActiveFight())
        {
            return;
        }

        String playerName = player.getName();
        if (playerName == null)
        {
            return;
        }

        // Get weapon speed using the animation ID for PERFECT accuracy
        int weaponSpeed;
        if (player.equals(client.getLocalPlayer()))
        {
            // For local player, use AnimationIds.getTicks() with actual weapon ID
            ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
            int weaponId = -1;

            if (equipment != null)
            {
                Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
                if (weapon != null)
                {
                    weaponId = weapon.getId();
                }
            }

            // Get EXACT ticks from AnimationIds
            weaponSpeed = AnimationIds.getTicks(animationId, weaponId);

            // Fallback if getTicks returns 0 (unknown animation)
            if (weaponSpeed == 0)
            {
                WeaponSpeedHelper weaponHelper = plugin.getWeaponSpeedHelper();
                weaponSpeed = weaponHelper != null ? weaponHelper.getCurrentWeaponSpeed() : 4;
            }
        }
        else
        {
            // For party members, estimate based on animation alone (weaponId unknown)
            weaponSpeed = AnimationIds.getTicks(animationId, -1);

            if (weaponSpeed == 0)
            {
                weaponSpeed = estimateWeaponSpeedFromAnimation(animationId);
            }
        }

        // Record the attack
        fightTracker.recordAttack(playerName, weaponSpeed);

        // Calculate expected damage for local player only (we have their equipment stats)
        if (player.equals(client.getLocalPlayer()))
        {
            calculateAndRecordExpectedDamage(fightTracker, playerName, animationId);
        }

        log.debug("{} attacked with animation {} (weapon speed: {} ticks)",
                playerName, animationId, weaponSpeed);
    }

    /**
     * Calculate and record expected damage for the attack
     */
    private void calculateAndRecordExpectedDamage(FightTracker fightTracker, String playerName, int animationId)
    {
        // Get NPC stats
        if (plugin.getNpcStatsProvider() == null || !plugin.getNpcStatsProvider().isLoaded())
        {
            return; // NPC database not loaded yet
        }

        int bossNpcId = fightTracker.getCurrentFight().getBossNpcId();
        var npcStats = plugin.getNpcStatsProvider().getNpcStats(bossNpcId);

        if (npcStats == null)
        {
            return; // No stats available for this NPC
        }

        // Determine attack style from animation
        String attackStyle = determineAttackStyle(animationId);

        // Calculate expected damage
        var combatFormulas = plugin.getCombatFormulas();
        if (combatFormulas != null)
        {
            double expectedDamage = combatFormulas.calculateExpectedDamage(npcStats, attackStyle);

            // Record it in player stats
            var currentFight = fightTracker.getCurrentFight();
            if (currentFight != null)
            {
                var playerStats = currentFight.getPlayerStats().get(playerName);
                if (playerStats != null)
                {
                    playerStats.addExpectedDamage(expectedDamage);
                    log.debug("Expected damage for {}: {} (style: {})", playerName, expectedDamage, attackStyle);
                }
            }
        }
    }

    /**
     * Determine attack style from animation ID and weapon ID
     * Uses AnimationIds class for accurate detection
     */
    private String determineAttackStyle(int animationId)
    {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
        {
            return "slash";
        }

        // Get equipped weapon ID
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        int weaponId = -1;

        if (equipment != null)
        {
            Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
            if (weapon != null)
            {
                weaponId = weapon.getId();
            }
        }

        // Use AnimationIds to get the attack style
        return AnimationIds.getAttackStyle(animationId, weaponId);
    }


    /**
     * Estimate weapon speed from animation ID for party members
     */
    private int estimateWeaponSpeedFromAnimation(int animationId)
    {
        // Blowpipe and darts
        if (animationId == 5061)
        {
            return 2;
        }

        // Shortbow, chinchompas
        if (animationId == 426 || animationId == 7618)
        {
            return 3;
        }

        // Most weapons
        if (animationId == 401 || animationId == 1658 || animationId == 7514 ||
                animationId == 8145 || animationId == 1162 || animationId == 1167)
        {
            return 4;
        }

        // Slower weapons (crossbows, etc.)
        if (animationId == 7617)
        {
            return 5;
        }

        // Godswords and 2h weapons
        if (animationId == 7514 || animationId == 386)
        {
            return 6;
        }

        // Default
        return 4;
    }
}