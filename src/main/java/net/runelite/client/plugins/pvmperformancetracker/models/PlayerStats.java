package net.runelite.client.plugins.pvmperformancetracker.models;

import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import net.runelite.client.plugins.pvmperformancetracker.enums.DamageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks all performance metrics for a single player in a fight
 */
@Slf4j
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

    // Hits buffered this tick, waiting for combined death-chance calculation.
    private final List<PendingHit> pendingHits = new ArrayList<>();
    private int pendingHitTick = -1;  // Tick the pending hits belong to
    private int pendingHitHp   = -1;  // HP at the time the first hit of this tick arrived

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
     * Queue a hit for combined death probability calculation at end of tick.
     *
     * @param hit        The hit parameters
     * @param gameTick   The current game tick
     * @param currentHp  Player's HP at the moment this hit arrived (pre-damage)
     */
    public void queueDeathCalcHit(PendingHit hit, int gameTick, int currentHp)
    {
        if (pendingHitTick != gameTick)
        {
            // New tick — discard any stale hits from the previous tick.
            // The authoritative flush already happened (or will happen) in onGameTick/endCurrentFight.
            pendingHits.clear();
            pendingHitTick = gameTick;
            pendingHitHp   = currentHp;  // Capture HP on the first hit of this tick
        }
        // Note: do NOT update pendingHitHp on subsequent hits of the same tick —
        // we want the HP *before any of this tick's hits landed*, which is the
        // value captured on the first hit.
        pendingHits.add(hit);
    }

    /**
        * Flush pending hits and commit a single combined death-chance entry.
        * Uses the HP that was captured when the first hit of the tick arrived.
        *
        * Call from:
        *   - PvMPerformanceTrackerPlugin.onGameTick() — normal case
        *   - FightTracker.endCurrentFight() — fight ends mid-tick (e.g. death)
        *
        * @param forTick  Only flushes if pendingHitTick matches this value
     */
    public void flushPendingDeathCalc(int forTick)
    {
        if (pendingHits.isEmpty() || pendingHitTick != forTick || pendingHitHp == -1)
        {
            return;
        }

        double deathProbability;

        if (pendingHits.size() == 1)
        {
            deathProbability = calculateSingleHitDeathProbability(pendingHits.get(0), pendingHitHp);
        }
        else
        {
            deathProbability = calculateCombinedDeathProbability(pendingHits, pendingHitHp);
        }

        if (deathProbability > 0.0)
        {
            addDeathChance(deathProbability);
            log.debug("[PlayerStats] Flushed {} pending hit(s) for tick={} hp={}: deathChance={}%",
                    pendingHits.size(), forTick, pendingHitHp,
                    String.format("%.2f", deathProbability * 100));
        }

        pendingHits.clear();
        pendingHitHp = -1;
    }

    /**
     * Single-hit death probability — equivalent to the existing CombatFormulas path
     * but operating on pre-computed effective min/max.
     */
    private double calculateSingleHitDeathProbability(PendingHit h, int currentHp)
    {
        if (h.effectiveMaxHit < currentHp)
        {
            return 0.0;
        }

        boolean guaranteed = h.effectiveMinHit > 0;
        double effectiveHitChance = guaranteed ? 1.0 : h.hitChance;

        if (h.effectiveMinHit >= currentHp)
        {
            return effectiveHitChance;
        }

        int possibleHits = h.effectiveMaxHit - h.effectiveMinHit + 1;
        int lethalHits   = h.effectiveMaxHit - currentHp + 1;
        return effectiveHitChance * ((double) lethalHits / possibleHits);
    }

    /**
     * Combined death probability for N hits landing on the same tick.
     *
     * Algorithm:
     *   1. Build the probability distribution of the combined damage sum via convolution.
     *      Each hit i is uniform over [min_i, max_i], weighted by its hit-chance p_i.
     *      A miss contributes 0 damage with probability (1 - p_i).
     *   2. P(death) = sum of combined[k] for k >= currentHp.
     *
     * This is exact for discrete uniform distributions and O(N * maxSum) in time.
     *
     * Example (two axes, [7,17] each, hitChance=0.85, currentHp=30):
     *   combined distribution spans [0, 34].
     *   P(combined >= 30) = P(sum in {30,31,32,33,34}).
     */
    private double calculateCombinedDeathProbability(List<PendingHit> hits, int currentHp)
    {
        // Maximum possible combined damage
        int maxSum = 0;
        for (PendingHit h : hits)
        {
            maxSum += h.effectiveMaxHit;
        }

        if (maxSum < currentHp)
        {
            // Even all hits at maximum can't kill — no death risk
            return 0.0;
        }

        // combined[k] = probability that the sum of all hits equals exactly k
        // Start with a degenerate distribution: P(sum=0) = 1.0
        double[] combined = new double[maxSum + 1];
        combined[0] = 1.0;

        for (PendingHit h : hits)
        {
            double[] next = new double[maxSum + 1];
            int range = h.effectiveMaxHit - h.effectiveMinHit + 1;

            // Attacks with a minimum hit > 0 always land (guaranteed accuracy).
            // Attacks with no minimum hit (effectiveMinHit == 0) can miss.
            boolean guaranteed = h.effectiveMinHit > 0;
            double missProbability       = guaranteed ? 0.0 : (1.0 - h.hitChance);
            double hitProbabilityPerValue = guaranteed ? (1.0 / range) : (h.hitChance / range);

            for (int prevSum = 0; prevSum <= maxSum; prevSum++)
            {
                if (combined[prevSum] == 0.0) continue;

                // Case A: this hit MISSES — contributes 0 damage
                if (missProbability > 0.0)
                {
                    next[prevSum] += combined[prevSum] * missProbability;
                }

                // Case B: this hit LANDS — contributes uniform [min, max]
                for (int dmg = h.effectiveMinHit; dmg <= h.effectiveMaxHit; dmg++)
                {
                    int newSum = prevSum + dmg;
                    if (newSum <= maxSum)
                    {
                        next[newSum] += combined[prevSum] * hitProbabilityPerValue;
                    }
                }
            }

            combined = next;
        }

        // P(death) = P(combined sum >= currentHp)
        double deathProbability = 0.0;
        for (int k = currentHp; k <= maxSum; k++)
        {
            deathProbability += combined[k];
        }

        log.debug("[PlayerStats] Combined death calc: {} hits, currentHp={}, maxSum={}, P(death)={}%",
                hits.size(), currentHp, maxSum,
                String.format("%.2f", deathProbability * 100));

        return deathProbability;
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
    @Data
    public static class PendingHit
    {
        public final int    effectiveMinHit;
        public final int    effectiveMaxHit;
        public final double hitChance;       // NPC accuracy against player [0,1]
        public final String style;

        public PendingHit(int effectiveMinHit, int effectiveMaxHit, double hitChance, String style)
        {
            this.effectiveMinHit = effectiveMinHit;
            this.effectiveMaxHit = effectiveMaxHit;
            this.hitChance       = hitChance;
            this.style           = style;
        }
    }
}