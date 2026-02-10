package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.enums.AttackStyle;
import net.runelite.client.plugins.pvmperformancetracker.helpers.CombatSessionManager;
import net.runelite.client.plugins.pvmperformancetracker.helpers.DamageCalculator;
import net.runelite.client.plugins.pvmperformancetracker.model.CombatSession;
import net.runelite.client.plugins.pvmperformancetracker.model.DamageEntry;

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

        // Only track hitsplats on NPCs from the player
        if (!(target instanceof NPC))
        {
            return;
        }

        // Check if this hitsplat is from the local player
        if (!isPlayerHitsplat(hitsplat))
        {
            return;
        }

        NPC npc = (NPC) target;

        // Check if we should track this NPC
        if (!shouldTrackNPC(npc))
        {
            return;
        }

        // Get or create combat session
        CombatSessionManager sessionManager = plugin.getSessionManager();
        CombatSession session = sessionManager.getCurrentSession();

        if (session == null && plugin.getConfig().autoStartCombat())
        {
            sessionManager.startNewSession();
            session = sessionManager.getCurrentSession();
        }

        if (session == null || !session.isActive())
        {
            return;
        }

        // Process the hitsplat
        processHitsplat(hitsplat, npc, session);
    }

    private void processHitsplat(Hitsplat hitsplat, NPC npc, CombatSession session)
    {
        int damage = hitsplat.getAmount();

        // Get current weapon and attack style
        DamageCalculator calculator = plugin.getDamageCalculator();
        String weapon = calculator.getCurrentWeapon();
        AttackStyle attackStyle = calculator.getCurrentAttackStyle();

        String targetName = npc.getName();

        // Create damage entry
        DamageEntry entry = new DamageEntry(
                damage,
                targetName,
                weapon,
                attackStyle
        );

        // Add to session
        session.addDamage(entry);

        // Update session activity
        session.updateLastActivity();

        // Update panel
        if (plugin.getPanel() != null)
        {
            plugin.getPanel().updatePanel();
        }

        log.debug("Recorded damage: {} to {} with {} ({})",
                damage, targetName, weapon, attackStyle.getDisplayName());
    }

    private boolean isPlayerHitsplat(Hitsplat hitsplat)
    {
        // Check if this is a damage hitsplat (not heal, poison, etc.)
        if (hitsplat.getHitsplatType() != HitsplatID.DAMAGE_ME &&
                hitsplat.getHitsplatType() != HitsplatID.BLOCK_ME &&
                hitsplat.getHitsplatType() != HitsplatID.DAMAGE_ME_CYAN &&
                hitsplat.getHitsplatType() != HitsplatID.DAMAGE_ME_ORANGE &&
                hitsplat.getHitsplatType() != HitsplatID.DAMAGE_ME_YELLOW &&
                hitsplat.getHitsplatType() != HitsplatID.DAMAGE_ME_WHITE)
        {
            // Not a player damage hitsplat
            return false;
        }

        return true;
    }

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

        // If tracking all combat, return true
        if (plugin.getConfig().trackAllCombat())
        {
            return true;
        }

        return false;
    }
}