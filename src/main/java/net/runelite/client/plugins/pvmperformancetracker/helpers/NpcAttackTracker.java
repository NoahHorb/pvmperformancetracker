package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.client.plugins.pvmperformancetracker.models.AttackStyleMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks NPC attacks and determines which attack style was used
 * Maps animation/projectile IDs to attack styles for accurate max hit tracking
 */
@Slf4j
public class NpcAttackTracker
{
    // Track the last attack style used by each NPC (by index)
    private final Map<Integer, PendingNpcAttack> pendingAttacks = new HashMap<>();

    // Track confirmed attack styles from hitsplats
    @Getter
    private final Map<Integer, String> lastConfirmedAttackStyle = new HashMap<>();

    /**
     * Record an NPC animation
     * This is called when we see an NPC perform an attack animation
     */
    public void recordNpcAnimation(NPC npc, int animationId)
    {
        if (npc == null)
        {
            return;
        }

        int npcId = npc.getId();
        int npcIndex = npc.getIndex();

        // Try to determine attack style from animation
        String attackStyle = AttackStyleMapping.getStyleFromAnimation(npcId, animationId);

        if (attackStyle != null)
        {
            // Store as pending attack
            PendingNpcAttack pending = new PendingNpcAttack(npcId, npcIndex, attackStyle, animationId, -1);
            pendingAttacks.put(npcIndex, pending);

            log.debug("NPC {} (index {}) used animation {} -> style: {}",
                    npc.getName(), npcIndex, animationId, attackStyle);
        }
        else
        {
            log.debug("NPC {} (index {}) used unmapped animation: {}",
                    npc.getName(), npcIndex, animationId);
        }
    }

    /**
     * Record an NPC projectile
     * This is called when we see an NPC spawn a projectile
     */
    public void recordNpcProjectile(NPC npc, int projectileId)
    {
        if (npc == null)
        {
            return;
        }

        int npcId = npc.getId();
        int npcIndex = npc.getIndex();

        // Try to determine attack style from projectile
        String attackStyle = AttackStyleMapping.getStyleFromProjectile(npcId, projectileId);

        if (attackStyle != null)
        {
            // Update or create pending attack
            PendingNpcAttack pending = pendingAttacks.get(npcIndex);

            if (pending != null)
            {
                // Update existing pending attack with projectile info
                pending.setProjectileId(projectileId);
                pending.setAttackStyle(attackStyle); // Projectile takes priority
            }
            else
            {
                // Create new pending attack from projectile
                pending = new PendingNpcAttack(npcId, npcIndex, attackStyle, -1, projectileId);
                pendingAttacks.put(npcIndex, pending);
            }

            log.debug("NPC {} (index {}) spawned projectile {} -> style: {}",
                    npc.getName(), npcIndex, projectileId, attackStyle);
        }
        else
        {
            log.debug("NPC {} (index {}) spawned unmapped projectile: {}",
                    npc.getName(), npcIndex, projectileId);
        }
    }

    /**
     * Confirm an attack style when damage is dealt
     * This is called when a hitsplat appears on the player
     */
    public void confirmAttackStyle(int npcIndex, String attackStyle)
    {
        if (attackStyle != null)
        {
            lastConfirmedAttackStyle.put(npcIndex, attackStyle);

            // Clear pending attack for this NPC
            pendingAttacks.remove(npcIndex);

            log.debug("Confirmed attack style for NPC index {}: {}", npcIndex, attackStyle);
        }
    }

    /**
     * Get the attack style for a pending attack from an NPC
     * Returns null if no pending attack found
     */
    public String getPendingAttackStyle(int npcIndex)
    {
        PendingNpcAttack pending = pendingAttacks.get(npcIndex);
        return pending != null ? pending.getAttackStyle() : null;
    }

    /**
     * Get the last confirmed attack style for an NPC
     * Returns null if no confirmed style found
     */
    public String getLastConfirmedStyle(int npcIndex)
    {
        return lastConfirmedAttackStyle.get(npcIndex);
    }

    /**
     * Get the best known attack style for an NPC
     * Prioritizes: pending attack > last confirmed > null
     */
    public String getBestKnownAttackStyle(int npcIndex)
    {
        // Check for pending attack first (most recent)
        String pending = getPendingAttackStyle(npcIndex);
        if (pending != null)
        {
            return pending;
        }

        // Fall back to last confirmed
        return getLastConfirmedStyle(npcIndex);
    }

    /**
     * Clear all tracking data
     */
    public void clear()
    {
        pendingAttacks.clear();
        lastConfirmedAttackStyle.clear();
    }

    /**
     * Clear tracking for a specific NPC
     */
    public void clearNpc(int npcIndex)
    {
        pendingAttacks.remove(npcIndex);
        lastConfirmedAttackStyle.remove(npcIndex);
    }

    /**
     * Inner class to track pending NPC attacks
     */
    @Getter
    private static class PendingNpcAttack
    {
        private final int npcId;
        private final int npcIndex;
        private String attackStyle;
        private int animationId;
        private int projectileId;

        public PendingNpcAttack(int npcId, int npcIndex, String attackStyle,
                                int animationId, int projectileId)
        {
            this.npcId = npcId;
            this.npcIndex = npcIndex;
            this.attackStyle = attackStyle;
            this.animationId = animationId;
            this.projectileId = projectileId;
        }

        public void setAttackStyle(String attackStyle)
        {
            this.attackStyle = attackStyle;
        }

        public void setProjectileId(int projectileId)
        {
            this.projectileId = projectileId;
        }
    }
}