package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.helpers.CombatSessionManager;
import net.runelite.client.plugins.pvmperformancetracker.model.CombatSession;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class AnimationListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    // Common attack animations
    private static final Set<Integer> MELEE_ANIMATIONS = new HashSet<>();
    private static final Set<Integer> RANGED_ANIMATIONS = new HashSet<>();
    private static final Set<Integer> MAGIC_ANIMATIONS = new HashSet<>();

    static
    {
        // Melee animations
        MELEE_ANIMATIONS.add(422);  // Unarmed punch
        MELEE_ANIMATIONS.add(423);  // Unarmed kick
        MELEE_ANIMATIONS.add(401);  // Bronze-Rune swords
        MELEE_ANIMATIONS.add(406);  // Dragon longsword
        MELEE_ANIMATIONS.add(407);  // Dragon dagger
        MELEE_ANIMATIONS.add(428);  // Staff bash
        MELEE_ANIMATIONS.add(440);  // Pickaxe
        MELEE_ANIMATIONS.add(1658); // Whip
        MELEE_ANIMATIONS.add(1378); // Dragon scimitar
        MELEE_ANIMATIONS.add(7514); // Godsword
        MELEE_ANIMATIONS.add(8145); // Rapier
        MELEE_ANIMATIONS.add(4230); // Scythe

        // Ranged animations
        RANGED_ANIMATIONS.add(426);  // Shortbow
        RANGED_ANIMATIONS.add(5061); // Blowpipe
        RANGED_ANIMATIONS.add(7617); // Crossbow
        RANGED_ANIMATIONS.add(7552); // Ballista
        RANGED_ANIMATIONS.add(8291); // Bow of faerdhinen

        // Magic animations
        MAGIC_ANIMATIONS.add(1162); // Standard spell
        MAGIC_ANIMATIONS.add(1167); // Ancient spell
        MAGIC_ANIMATIONS.add(1978); // Powered staff
        MAGIC_ANIMATIONS.add(8532); // Trident
        MAGIC_ANIMATIONS.add(7855); // Sanguinesti staff
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

        // Only track local player
        if (!player.equals(client.getLocalPlayer()))
        {
            return;
        }

        int animationId = player.getAnimation();

        // Check if this is an attack animation
        if (isAttackAnimation(animationId))
        {
            handleAttackAnimation(animationId);
        }
    }

    private boolean isAttackAnimation(int animationId)
    {
        return MELEE_ANIMATIONS.contains(animationId) ||
                RANGED_ANIMATIONS.contains(animationId) ||
                MAGIC_ANIMATIONS.contains(animationId);
    }

    private void handleAttackAnimation(int animationId)
    {
        CombatSessionManager sessionManager = plugin.getSessionManager();

        // Get or create session
        CombatSession session = sessionManager.getCurrentSession();

        if (session == null && plugin.getConfig().autoStartCombat())
        {
            // Only auto-start if player is actually targeting an NPC
            if (plugin.getDamageCalculator().isInCombat())
            {
                sessionManager.startNewSession();
                session = sessionManager.getCurrentSession();
            }
        }

        if (session != null && session.isActive())
        {
            // Increment attack counter
            session.incrementAttacks();
            session.updateLastActivity();

            log.debug("Recorded attack animation: {}", animationId);
        }
    }

    /**
     * Add custom attack animation ID
     */
    public static void addMeleeAnimation(int animationId)
    {
        MELEE_ANIMATIONS.add(animationId);
    }

    public static void addRangedAnimation(int animationId)
    {
        RANGED_ANIMATIONS.add(animationId);
    }

    public static void addMagicAnimation(int animationId)
    {
        MAGIC_ANIMATIONS.add(animationId);
    }
}