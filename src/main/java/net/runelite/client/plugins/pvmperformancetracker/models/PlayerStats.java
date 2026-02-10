package net.runelite.client.plugins.pvmperformancetracker.models;

import lombok.Data;
import net.runelite.client.plugins.pvmperformancetracker.enums.DamageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks all performance metrics for a single player in a fight
 */
@Data
public class PlayerStats
{
    private final String playerName;

    // Offensive Metrics
    private int damageDealt;
    private int totalAttacks;
    private int successfulHits;

    // Attacking Ticks Tracking
    private int totalCombatTicks;  // Ticks from first hitsplat to death
    private int totalAttackingTicks; // Sum of (attacks * weapon speed)
    private int attackingTicksLost; // Real-time counter of ticks lost
    private Integer lastAttackTick; // Tick of last attack
    private int currentWeaponSpeed; // Current weapon speed (updates each attack)

    // Defensive Metrics
    private int avoidableDamageTaken;
    private int prayableDamageTaken;
    private int unavoidableDamageTaken;

    // Detailed damage entries for analysis
    private final List<DamageInstance> damageDealtInstances = new ArrayList<>();
    private final List<DamageInstance> damageTakenInstances = new ArrayList<>();

    // Tracking state
    private boolean isLocalPlayer;
    private Integer firstDamageTick;
    private Integer lastDamageTick;

    public PlayerStats(String playerName)
    {
        this.playerName = playerName;
        this.damageDealt = 0;
        this.totalAttacks = 0;
        this.successfulHits = 0;
        this.totalCombatTicks = 0;
        this.totalAttackingTicks = 0;
        this.attackingTicksLost = 0;
        this.avoidableDamageTaken = 0;
        this.prayableDamageTaken = 0;
        this.unavoidableDamageTaken = 0;
        this.isLocalPlayer = false;
    }

    /**
     * Add damage dealt by this player
     */
    public void addDamageDealt(int damage, int tick, String target)
    {
        this.damageDealt += damage;

        if (damage > 0)
        {
            this.successfulHits++;
        }

        // Track first and last damage tick for combat duration
        if (firstDamageTick == null || tick < firstDamageTick)
        {
            firstDamageTick = tick;
        }
        if (lastDamageTick == null || tick > lastDamageTick)
        {
            lastDamageTick = tick;
        }

        // Initialize lastAttackTick on first damage if not set by animation
        // This ensures tick loss tracking works even if we miss the animation event
        if (lastAttackTick == null)
        {
            lastAttackTick = tick;
            currentWeaponSpeed = 4; // Default to 4-tick if unknown
        }

        // Store detailed instance
        DamageInstance instance = new DamageInstance(tick, damage, target);
        damageDealtInstances.add(instance);
    }

    /**
     * Add damage taken by this player
     */
    public void addDamageTaken(int damage, int tick, DamageType damageType, String source)
    {
        DamageInstance instance = new DamageInstance(tick, damage, source);
        instance.setDamageType(damageType);
        damageTakenInstances.add(instance);

        // Categorize damage
        switch (damageType)
        {
            case AVOIDABLE:
                avoidableDamageTaken += damage;
                break;
            case PRAYABLE:
                prayableDamageTaken += damage;
                break;
            case UNAVOIDABLE:
                unavoidableDamageTaken += damage;
                break;
            case UNKNOWN:
                // Count as unavoidable by default
                unavoidableDamageTaken += damage;
                break;
        }
    }

    /**
     * Record an attack attempt (for tick loss calculation)
     * Tick loss logic: After attack, player must wait {weapon speed} ticks
     * If they don't attack after that, increment ticks lost
     */
    public void recordAttack(int weaponSpeed, int currentTick)
    {
        // Before recording new attack, lock in any ticks lost since last attack
        updateTicksLostOnAttack(currentTick);

        this.totalAttacks++;
        this.totalAttackingTicks += weaponSpeed;

        // Update last attack tick and weapon speed
        lastAttackTick = currentTick;
        currentWeaponSpeed = weaponSpeed;
    }

    /**
     * Calculate DPS based on fight duration
     */
    public double calculateDPS(int fightDurationTicks)
    {
        if (fightDurationTicks == 0)
        {
            return 0.0;
        }

        // Convert ticks to seconds (600ms per tick = 0.6s)
        double durationSeconds = fightDurationTicks * 0.6;
        return damageDealt / durationSeconds;
    }

    /**
     * Calculate accuracy percentage
     */
    public double getAccuracyPercentage()
    {
        if (totalAttacks == 0)
        {
            return 0.0;
        }
        return (successfulHits / (double) totalAttacks) * 100.0;
    }

    /**
     * Calculate ticks lost in real-time during active fight
     * For ended fights, returns the finalized value
     * Logic: After each attack, wait {weapon speed} ticks.
     * If player hasn't attacked by then, start counting ticks lost.
     */
    public int calculateTicksLost(int currentTick, boolean fightActive)
    {
        // For ended fights, return the finalized value (don't keep accumulating)
        if (!fightActive)
        {
            return attackingTicksLost;
        }

        // For active fights, calculate real-time
        if (lastAttackTick == null)
        {
            return 0; // No attacks yet, no ticks lost
        }

        // Calculate how many ticks since last attack
        int ticksSinceLastAttack = currentTick - lastAttackTick;

        // If we're still within weapon speed cooldown, no ticks lost yet
        if (ticksSinceLastAttack <= currentWeaponSpeed)
        {
            return attackingTicksLost; // Return accumulated ticks lost
        }

        // We're past the cooldown - calculate how many ticks lost since cooldown ended
        int ticksOverCooldown = ticksSinceLastAttack - currentWeaponSpeed;

        // Return accumulated + current ticks lost
        return attackingTicksLost + ticksOverCooldown;
    }

    /**
     * Update ticks lost when a new attack happens
     * This "locks in" the ticks lost since last attack
     */
    public void updateTicksLostOnAttack(int currentTick)
    {
        if (lastAttackTick == null)
        {
            return;
        }

        int ticksSinceLastAttack = currentTick - lastAttackTick;

        // If we attacked after cooldown, add the extra ticks to accumulated ticks lost
        if (ticksSinceLastAttack > currentWeaponSpeed)
        {
            int ticksOverCooldown = ticksSinceLastAttack - currentWeaponSpeed;
            attackingTicksLost += ticksOverCooldown;
        }
        // If we attacked during cooldown, no ticks lost (good!)
    }

    /**
     * Get total damage taken (all types)
     */
    public int getTotalDamageTaken()
    {
        return avoidableDamageTaken + prayableDamageTaken + unavoidableDamageTaken;
    }

    /**
     * Finalize stats at end of fight
     */
    public void finalizeFight(int fightDurationTicks)
    {
        // Calculate combat ticks (first damage to last damage)
        if (firstDamageTick != null && lastDamageTick != null)
        {
            totalCombatTicks = lastDamageTick - firstDamageTick;
        }

        // Finalize ticks lost - lock in the final value
        if (lastAttackTick != null)
        {
            // Calculate any remaining ticks lost since last attack
            int ticksSinceLastAttack = fightDurationTicks - (lastAttackTick - firstDamageTick);

            if (ticksSinceLastAttack > currentWeaponSpeed)
            {
                int ticksOverCooldown = ticksSinceLastAttack - currentWeaponSpeed;
                attackingTicksLost += ticksOverCooldown;
            }
        }
    }

    /**
     * Merge stats from another PlayerStats (for Overall mode)
     */
    public void mergeStats(PlayerStats other)
    {
        this.damageDealt += other.damageDealt;
        this.totalAttacks += other.totalAttacks;
        this.successfulHits += other.successfulHits;
        this.totalCombatTicks += other.totalCombatTicks;
        this.totalAttackingTicks += other.totalAttackingTicks;
        this.attackingTicksLost += other.attackingTicksLost;
        this.avoidableDamageTaken += other.avoidableDamageTaken;
        this.prayableDamageTaken += other.prayableDamageTaken;
        this.unavoidableDamageTaken += other.unavoidableDamageTaken;

        this.damageDealtInstances.addAll(other.damageDealtInstances);
        this.damageTakenInstances.addAll(other.damageTakenInstances);
    }

    /**
     * Inner class to track individual damage instances
     */
    @Data
    public static class DamageInstance
    {
        private final int tick;
        private final int amount;
        private final String target;
        private DamageType damageType;

        public DamageInstance(int tick, int amount, String target)
        {
            this.tick = tick;
            this.amount = amount;
            this.target = target;
            this.damageType = DamageType.UNKNOWN;
        }
    }
}