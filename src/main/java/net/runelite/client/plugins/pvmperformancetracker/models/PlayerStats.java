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

    // Expected Damage Metrics (calculated from combat formulas)
    private double expectedDamageDealt;
    private int expectedDamageCalculations; // Count for averaging

    // Attacking Ticks Tracking
    private int totalCombatTicks;  // Ticks from first hitsplat to death
    private int totalAttackingTicks; // Sum of (attacks * weapon speed)
    private int attackingTicksLost; // Real-time counter of ticks lost
    private Integer lastAttackTick; // Tick of last attack
    private int currentWeaponSpeed; // Current weapon speed (updates each attack)

    // Base stats for Overall mode (locked-in values from completed fights)
    private int baseDamageDealt;
    private int baseTotalAttacks;
    private int baseSuccessfulHits;
    private int baseAttackingTicksLost;
    private double baseExpectedDamageDealt;
    private int baseExpectedDamageCalculations;
    private int baseChancesOfDeath;
    private double baseCumulativeDeathChance;  // Compounding survival formula across fights

    // Defensive Metrics
    private int damageTaken; // Total damage taken
    private int avoidableDamageTaken;
    private int prayableDamageTaken;
    private int unavoidableDamageTaken;

    // Death Chance Tracking
    private int chancesOfDeath; // Count of hits with death probability > 0%
    private double cumulativeDeathChance; // Compounding probability of death

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
        this.expectedDamageDealt = 0.0;
        this.expectedDamageCalculations = 0;
        this.totalCombatTicks = 0;
        this.totalAttackingTicks = 0;
        this.attackingTicksLost = 0;
        this.baseDamageDealt = 0;
        this.baseTotalAttacks = 0;
        this.baseSuccessfulHits = 0;
        this.baseAttackingTicksLost = 0;
        this.baseExpectedDamageDealt = 0.0;
        this.baseExpectedDamageCalculations = 0;
        this.damageTaken = 0;
        this.avoidableDamageTaken = 0;
        this.prayableDamageTaken = 0;
        this.unavoidableDamageTaken = 0;
        this.chancesOfDeath = 0;
        this.cumulativeDeathChance = 0.0;
        this.baseChancesOfDeath = 0;
        this.baseCumulativeDeathChance = 0.0;
        this.isLocalPlayer = false;
        this.currentWeaponSpeed = 4; // Default
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
    public void addDamageTaken(int damage, int tick, DamageType damageType)
    {
        this.damageTaken += damage;
        switch (damageType)
        {
            case AVOIDABLE:
                this.avoidableDamageTaken += damage;
                break;
            case PRAYABLE:
                this.prayableDamageTaken += damage;
                break;
            case UNAVOIDABLE:
                this.unavoidableDamageTaken += damage;
                break;
            default:
                // UNKNOWN - don't categorize
                break;
        }

        DamageInstance instance = new DamageInstance(tick, damage, "Self");
        instance.setDamageType(damageType);
        damageTakenInstances.add(instance);
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
        // Changed from > to >= so tick loss counts immediately after weapon speed
        if (ticksSinceLastAttack >= currentWeaponSpeed)
        {
            int ticksOverCooldown = ticksSinceLastAttack - currentWeaponSpeed;
            attackingTicksLost += ticksOverCooldown;
        }
        // If we attacked during cooldown, no ticks lost (good!)
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

        // If we're still within weapon speed cooldown, no new ticks lost
        // Changed from <= to < so it increments immediately after weapon speed
        if (ticksSinceLastAttack < currentWeaponSpeed)
        {
            return attackingTicksLost; // Return accumulated ticks lost
        }

        // We're past the cooldown - calculate how many ticks lost since cooldown ended
        int ticksOverCooldown = ticksSinceLastAttack - currentWeaponSpeed;

        // Return accumulated + current ticks lost
        return attackingTicksLost + ticksOverCooldown;
    }

    /**
     * Calculate DPS
     */
    public double calculateDPS(int durationTicks)
    {
        if (durationTicks == 0)
        {
            return 0.0;
        }

        double durationSeconds = durationTicks * 0.6;
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
        if (lastAttackTick != null && firstDamageTick != null)
        {
            // Calculate any remaining ticks lost since last attack
            int fightEndTick = firstDamageTick + fightDurationTicks;
            int ticksSinceLastAttack = fightEndTick - lastAttackTick;

            // Changed from > to >= so tick loss counts immediately after weapon speed
            if (ticksSinceLastAttack >= currentWeaponSpeed)
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
        this.expectedDamageDealt += other.expectedDamageDealt;
        this.expectedDamageCalculations += other.expectedDamageCalculations;
        this.damageTaken += other.damageTaken;
        this.avoidableDamageTaken += other.avoidableDamageTaken;
        this.prayableDamageTaken += other.prayableDamageTaken;
        this.unavoidableDamageTaken += other.unavoidableDamageTaken;
        this.chancesOfDeath += other.chancesOfDeath;

        // Merge cumulative death chance probabilities
        double survivalProb = (1.0 - this.cumulativeDeathChance) * (1.0 - other.cumulativeDeathChance);
        this.cumulativeDeathChance = 1.0 - survivalProb;

        this.damageDealtInstances.addAll(other.damageDealtInstances);
        this.damageTakenInstances.addAll(other.damageTakenInstances);
    }

    /**
     * Sync Overall stats with current fight for real-time display.
     * Overall shows: base (locked-in from previous fights) + current (active fight).
     */
    public void syncWithCurrentFight(PlayerStats currentStats, int currentTick)
    {
        // Offensive stats — base + current
        this.damageDealt               = baseDamageDealt + currentStats.getDamageDealt();
        this.totalAttacks              = baseTotalAttacks + currentStats.getTotalAttacks();
        this.successfulHits            = baseSuccessfulHits + currentStats.getSuccessfulHits();
        this.expectedDamageDealt       = baseExpectedDamageDealt + currentStats.getExpectedDamageDealt();
        this.expectedDamageCalculations = baseExpectedDamageCalculations + currentStats.getExpectedDamageCalculations();

        // Tick loss: base (finalized) + current (real-time)
        int currentTickLoss = currentStats.calculateTicksLost(currentTick, true);
        this.attackingTicksLost = baseAttackingTicksLost + currentTickLoss;

        // Weapon state for display
        this.lastAttackTick    = currentStats.getLastAttackTick();
        this.currentWeaponSpeed = currentStats.getCurrentWeaponSpeed();

        // Defensive damage totals — base + current
        this.damageTaken          = currentStats.getDamageTaken();
        this.avoidableDamageTaken  = currentStats.getAvoidableDamageTaken();
        this.prayableDamageTaken   = currentStats.getPrayableDamageTaken();
        this.unavoidableDamageTaken = currentStats.getUnavoidableDamageTaken();

        // Death probability — compound base (previous fights) with current fight.
        // This produces the correct cumulative probability across all sessions.
        // e.g. base=5%, current=3.5% → displayed = 1 - (0.95 * 0.965) = 8.33%
        this.chancesOfDeath = baseChancesOfDeath + currentStats.getChancesOfDeath();
        double baseSurvival    = 1.0 - baseCumulativeDeathChance;
        double currentSurvival = 1.0 - currentStats.getCumulativeDeathChance();
        this.cumulativeDeathChance = 1.0 - (baseSurvival * currentSurvival);
    }

    /**
     * Lock current fight stats into Overall's base values
     * Called when a fight ends
     */
    /**
     * Lock current fight stats into Overall's base values.
     * Called when a fight ends.
     */
    public void lockInFightStats(PlayerStats currentStats)
    {
        // Offensive stats — simple addition
        baseDamageDealt              += currentStats.getDamageDealt();
        baseTotalAttacks             += currentStats.getTotalAttacks();
        baseSuccessfulHits           += currentStats.getSuccessfulHits();
        baseAttackingTicksLost       += currentStats.getAttackingTicksLost();
        baseExpectedDamageDealt      += currentStats.getExpectedDamageDealt();
        baseExpectedDamageCalculations += currentStats.getExpectedDamageCalculations();

        // Death probability — compound using survival formula so that
        // e.g. 3.5% + 3.5% = 6.88%, not 7.0%.
        // baseCumulativeDeathChance represents all previous fights combined.
        // We merge the current fight's death chance into it.
        baseChancesOfDeath += currentStats.getChancesOfDeath();
        double baseSurvival    = 1.0 - baseCumulativeDeathChance;
        double currentSurvival = 1.0 - currentStats.getCumulativeDeathChance();
        baseCumulativeDeathChance = 1.0 - (baseSurvival * currentSurvival);

        // Defensive damage totals — also lock in
        // (These don't have separate base fields yet; if you add them later,
        //  follow the same pattern. For now we keep them on the display fields.)

        // Update displayed values to match base (no current fight active now)
        this.damageDealt               = baseDamageDealt;
        this.totalAttacks              = baseTotalAttacks;
        this.successfulHits            = baseSuccessfulHits;
        this.attackingTicksLost        = baseAttackingTicksLost;
        this.expectedDamageDealt       = baseExpectedDamageDealt;
        this.expectedDamageCalculations = baseExpectedDamageCalculations;
        this.chancesOfDeath            = baseChancesOfDeath;
        this.cumulativeDeathChance     = baseCumulativeDeathChance;

        // Reset weapon state since no current fight
        this.lastAttackTick = null;
    }

    /**
     * Add expected damage (calculated from combat formulas)
     */
    public void addExpectedDamage(double expectedDamage)
    {
        this.expectedDamageDealt += expectedDamage;
        this.expectedDamageCalculations++;
    }

    /**
     * Get average expected damage per attack
     */
    public double getAverageExpectedDamage()
    {
        if (expectedDamageCalculations == 0)
        {
            return 0.0;
        }
        return expectedDamageDealt / expectedDamageCalculations;
    }

    /**
     * Calculate expected DPS
     */
    public double getExpectedDps(int fightDurationTicks)
    {
        if (fightDurationTicks == 0)
        {
            return 0.0;
        }
        double seconds = fightDurationTicks * 0.6; // 0.6 seconds per tick
        return expectedDamageDealt / seconds;
    }

    /**
     * Add damage taken with classification
     */
    public void addDamageTaken(int damage, DamageType damageType, int tick)
    {
        this.damageTaken += damage;

        switch (damageType)
        {
            case AVOIDABLE:
                this.avoidableDamageTaken += damage;
                break;
            case PRAYABLE:
                this.prayableDamageTaken += damage;
                break;
            case UNAVOIDABLE:
                this.unavoidableDamageTaken += damage;
                break;
        }

        DamageInstance instance = new DamageInstance(tick, damage, "Self");
        instance.setDamageType(damageType);
        damageTakenInstances.add(instance);
    }

    /**
     * Add a chance of death from a hit
     * Updates both the count and cumulative probability
     */
    public void addDeathChance(double deathProbability)
    {
        if (deathProbability > 0.0)
        {
            this.chancesOfDeath++;

            // Calculate cumulative probability: 1 - (1 - current) * (1 - new)
            double survivalProbability = 1.0 - this.cumulativeDeathChance;
            double newSurvivalProbability = survivalProbability * (1.0 - deathProbability);
            this.cumulativeDeathChance = 1.0 - newSurvivalProbability;
        }
    }

    /**
     * Get cumulative death chance as a percentage
     */
    public double getDeathChancePercentage()
    {
        return cumulativeDeathChance * 100.0;
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