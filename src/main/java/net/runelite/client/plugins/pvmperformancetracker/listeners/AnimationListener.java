package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
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

        // Only track player animations
        if (!(actor instanceof Player))
        {
            return;
        }

        Player player = (Player) actor;
        int animationId = player.getAnimation();

        // Check if this is an attack animation
        if (isAttackAnimation(animationId))
        {
            handleAttackAnimation(player, animationId);
        }
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

        // Get weapon speed
        int weaponSpeed;
        if (player.equals(client.getLocalPlayer()))
        {
            // For local player, we can accurately get weapon speed
            WeaponSpeedHelper weaponHelper = plugin.getWeaponSpeedHelper();
            weaponSpeed = weaponHelper != null ? weaponHelper.getAdjustedWeaponSpeed() : 4;
        }
        else
        {
            // For party members, estimate based on animation
            weaponSpeed = estimateWeaponSpeedFromAnimation(animationId);
        }

        // Record the attack
        fightTracker.recordAttack(playerName, weaponSpeed);

        log.debug("{} attacked with animation {} (weapon speed: {} ticks)",
                playerName, animationId, weaponSpeed);
    }

    /**
     * Estimate weapon speed based on attack animation
     * Used for party members where we can't see their equipment
     */
    private int estimateWeaponSpeedFromAnimation(int animationId)
    {
        // Blowpipe - 2 ticks
        if (animationId == 5061)
        {
            return 2;
        }

        // Whip, scimitars, daggers - 4 ticks
        if (animationId == 1658 || animationId == 1378 || animationId == 407 || animationId == 422)
        {
            return 4;
        }

        // Scythe - 5 ticks
        if (animationId == 4230 || animationId == 8056)
        {
            return 5;
        }

        // Godswords, 2h - 6 ticks
        if (animationId == 7514 || animationId == 386)
        {
            return 6;
        }

        // Crossbow - 6 ticks
        if (animationId == 7617)
        {
            return 6;
        }

        // Ballista - 6 ticks
        if (animationId == 7552 || animationId == 7554 || animationId == 7555)
        {
            return 6;
        }

        // Bow - 5 ticks (average)
        if (animationId == 426 || animationId == 929 || animationId == 8291)
        {
            return 5;
        }

        // Magic - 5 ticks (most spells)
        if (animationId == 1162 || animationId == 1167 || animationId == 1978)
        {
            return 5;
        }

        // Trident, Sanguinesti - 4 ticks
        if (animationId == 8532 || animationId == 7855)
        {
            return 4;
        }

        // Default
        return 4;
    }

    /**
     * Add custom attack animation ID
     */
    public static void addAttackAnimation(int animationId)
    {
        ATTACK_ANIMATIONS.add(animationId);
        log.debug("Added custom attack animation: {}", animationId);
    }
}