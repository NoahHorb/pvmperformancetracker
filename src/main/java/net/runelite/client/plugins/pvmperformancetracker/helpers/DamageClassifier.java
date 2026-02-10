package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.plugins.pvmperformancetracker.enums.DamageType;

import java.util.HashMap;
import java.util.Map;

/**
 * Classifies damage as Avoidable, Prayable, or Unavoidable
 * Based on NPC ID, animation ID, projectile ID, and player prayer state
 */
@Slf4j
public class DamageClassifier
{
    private final Client client;

    // Boss-specific damage classification rules
    private final Map<Integer, BossMechanicRules> bossMechanics = new HashMap<>();

    public DamageClassifier(Client client)
    {
        this.client = client;
        initializeBossMechanics();
    }

    /**
     * Classify damage taken by a player
     */
    public DamageType classifyDamage(int npcId, int animationId, int projectileId, Prayer activePrayer, int damage)
    {
        // Check if we have specific rules for this boss
        BossMechanicRules rules = bossMechanics.get(npcId);

        if (rules != null)
        {
            return rules.classifyDamage(animationId, projectileId, activePrayer, damage);
        }

        // Default classification based on general patterns
        return classifyGenericDamage(animationId, projectileId, activePrayer);
    }

    /**
     * Generic damage classification when no boss-specific rules exist
     */
    private DamageType classifyGenericDamage(int animationId, int projectileId, Prayer activePrayer)
    {
        // If no projectile and no animation, likely unavoidable chip damage
        if (projectileId == -1 && animationId == -1)
        {
            return DamageType.UNAVOIDABLE;
        }

        // If projectile exists, it's potentially avoidable
        if (projectileId != -1)
        {
            return DamageType.AVOIDABLE;
        }

        // Default to unknown for further analysis
        return DamageType.UNKNOWN;
    }

    /**
     * Initialize boss-specific mechanic rules
     * Currently supporting ToA, CoX, ToB
     */
    private void initializeBossMechanics()
    {
        // Tombs of Amascut - Ba-Ba
        bossMechanics.put(11779, createBaBaMechanics());

        // Tombs of Amascut - Kephri
        bossMechanics.put(11719, createKephriMechanics());

        // Tombs of Amascut - Akkha
        bossMechanics.put(11789, createAkkhaMechanics());

        // Tombs of Amascut - Zebak
        bossMechanics.put(11730, createZebakMechanics());

        // Tombs of Amascut - Wardens
        bossMechanics.put(11750, createWardensMechanics());
        bossMechanics.put(11752, createWardensMechanics());

        // Add more bosses as needed...
        // CoX bosses
        // ToB bosses
        // GWD bosses
    }

    /**
     * Ba-Ba damage classification
     */
    private BossMechanicRules createBaBaMechanics()
    {
        BossMechanicRules rules = new BossMechanicRules("Ba-Ba");

        // Boulder slam (avoidable)
        rules.addAvoidableMechanic(9675); // Slam animation

        // Falling rocks (avoidable)
        rules.addAvoidableProjectile(2204);

        // Melee attacks (prayable)
        rules.addPrayableMechanic(9672, Prayer.PROTECT_FROM_MELEE);

        return rules;
    }

    /**
     * Kephri damage classification
     */
    private BossMechanicRules createKephriMechanics()
    {
        BossMechanicRules rules = new BossMechanicRules("Kephri");

        // Dung attack (avoidable)
        rules.addAvoidableProjectile(2135);

        // Swarm (unavoidable when shield is down)
        rules.addUnavoidableMechanic(9579);

        return rules;
    }

    /**
     * Akkha damage classification
     */
    private BossMechanicRules createAkkhaMechanics()
    {
        BossMechanicRules rules = new BossMechanicRules("Akkha");

        // Melee attacks (prayable)
        rules.addPrayableMechanic(9752, Prayer.PROTECT_FROM_MELEE);

        // Ranged attacks (prayable)
        rules.addPrayableMechanic(9754, Prayer.PROTECT_FROM_MISSILES);

        // Magic attacks (prayable)
        rules.addPrayableMechanic(9756, Prayer.PROTECT_FROM_MAGIC);

        // Shadow orbs (avoidable)
        rules.addAvoidableProjectile(2208);

        return rules;
    }

    /**
     * Zebak damage classification
     */
    private BossMechanicRules createZebakMechanics()
    {
        BossMechanicRules rules = new BossMechanicRules("Zebak");

        // Water attack (avoidable)
        rules.addAvoidableProjectile(2193);

        // Wave (avoidable)
        rules.addAvoidableMechanic(9613);

        return rules;
    }

    /**
     * Wardens damage classification
     */
    private BossMechanicRules createWardensMechanics()
    {
        BossMechanicRules rules = new BossMechanicRules("Wardens");

        // Skull attacks (avoidable)
        rules.addAvoidableProjectile(2175);

        // Pyramid projectiles (avoidable)
        rules.addAvoidableProjectile(2176);

        // Energy siphon (unavoidable when in range)
        rules.addUnavoidableMechanic(9729);

        return rules;
    }

    /**
     * Inner class to hold boss-specific rules
     */
    private static class BossMechanicRules
    {
        private final String bossName;
        private final Map<Integer, DamageType> animationRules = new HashMap<>();
        private final Map<Integer, DamageType> projectileRules = new HashMap<>();
        private final Map<Integer, Prayer> prayableAnimations = new HashMap<>();

        public BossMechanicRules(String bossName)
        {
            this.bossName = bossName;
        }

        public void addAvoidableMechanic(int animationId)
        {
            animationRules.put(animationId, DamageType.AVOIDABLE);
        }

        public void addAvoidableProjectile(int projectileId)
        {
            projectileRules.put(projectileId, DamageType.AVOIDABLE);
        }

        public void addPrayableMechanic(int animationId, Prayer correctPrayer)
        {
            prayableAnimations.put(animationId, correctPrayer);
        }

        public void addUnavoidableMechanic(int animationId)
        {
            animationRules.put(animationId, DamageType.UNAVOIDABLE);
        }

        public DamageType classifyDamage(int animationId, int projectileId, Prayer activePrayer, int damage)
        {
            // Check projectile rules first
            if (projectileId != -1 && projectileRules.containsKey(projectileId))
            {
                return projectileRules.get(projectileId);
            }

            // Check if this is a prayable attack
            if (prayableAnimations.containsKey(animationId))
            {
                Prayer correctPrayer = prayableAnimations.get(animationId);

                // If player had correct prayer, it's unavoidable (they did everything right)
                if (activePrayer == correctPrayer)
                {
                    return DamageType.UNAVOIDABLE;
                }
                else
                {
                    // They didn't have correct prayer, so it's prayable damage
                    return DamageType.PRAYABLE;
                }
            }

            // Check animation rules
            if (animationRules.containsKey(animationId))
            {
                return animationRules.get(animationId);
            }

            // Default to unknown
            return DamageType.UNKNOWN;
        }
    }
}