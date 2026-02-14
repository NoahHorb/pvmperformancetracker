package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks NPC animations and projectiles to determine which specific attack is being used
 * This is crucial for accurate max hit calculation for death probability
 */
@Slf4j
public class NpcAttackTracker
{
    // Stores the most recent attack type for each NPC
    private final Map<Integer, AttackInfo> npcLastAttack = new HashMap<>();

    /**
     * Record an NPC animation
     */
    public void recordNpcAnimation(NPC npc, int animationId)
    {
        if (npc == null)
        {
            return;
        }

        int npcIndex = npc.getIndex();
        int npcId = npc.getId();

        AttackInfo attackInfo = npcLastAttack.getOrDefault(npcIndex, new AttackInfo());
        attackInfo.setNpcId(npcId);
        attackInfo.setAnimationId(animationId);
        attackInfo.setTimestamp(System.currentTimeMillis());

        npcLastAttack.put(npcIndex, attackInfo);

        log.debug("NPC {} (index {}) animation: {}", npcId, npcIndex, animationId);
    }

    /**
     * Record an NPC projectile
     */
    public void recordNpcProjectile(NPC npc, int projectileId)
    {
        if (npc == null)
        {
            return;
        }

        int npcIndex = npc.getIndex();
        int npcId = npc.getId();

        AttackInfo attackInfo = npcLastAttack.getOrDefault(npcIndex, new AttackInfo());
        attackInfo.setNpcId(npcId);
        attackInfo.setProjectileId(projectileId);
        attackInfo.setTimestamp(System.currentTimeMillis());

        npcLastAttack.put(npcIndex, attackInfo);

        log.debug("NPC {} (index {}) projectile: {}", npcId, npcIndex, projectileId);
    }

    /**
     * Get the most recent attack info for an NPC
     * Returns null if no recent attack (>5 seconds old)
     */
    public AttackInfo getRecentAttack(int npcIndex)
    {
        AttackInfo info = npcLastAttack.get(npcIndex);

        if (info == null)
        {
            return null;
        }

        // Only consider attacks within last 5 seconds
        long age = System.currentTimeMillis() - info.getTimestamp();
        if (age > 5000)
        {
            return null;
        }

        return info;
    }

    /**
     * Clear old attack data
     */
    public void cleanup()
    {
        long now = System.currentTimeMillis();
        npcLastAttack.entrySet().removeIf(entry ->
                (now - entry.getValue().getTimestamp()) > 10000); // Remove >10s old
    }

    /**
     * Information about an NPC's attack
     */
    @Data
    public static class AttackInfo
    {
        private int npcId;
        private int animationId = -1;
        private int projectileId = -1;
        private long timestamp;
    }
}