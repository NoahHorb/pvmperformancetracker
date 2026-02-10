package net.runelite.client.plugins.pvmperformancetracker.model;

import lombok.Data;
import net.runelite.client.plugins.pvmperformancetracker.enums.AttackStyle;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class CombatSession
{
    private final String sessionId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean active;

    private String primaryTarget;
    private final Set<String> targets = new HashSet<>();

    private long lastActivityTimestamp;
    private long combatStartTimestamp;
    private long combatEndTimestamp;

    private int totalDamage;
    private int maxHit;
    private int totalAttacks;
    private int successfulHits;

    private final List<DamageEntry> damageEntries = new ArrayList<>();
    private final Map<AttackStyle, Integer> damageByStyle = new ConcurrentHashMap<>();
    private final Map<String, Integer> damageByWeapon = new ConcurrentHashMap<>();
    private final Map<String, Integer> damageByTarget = new ConcurrentHashMap<>();

    public CombatSession()
    {
        this.sessionId = UUID.randomUUID().toString();
        this.startTime = LocalDateTime.now();
        this.combatStartTimestamp = System.currentTimeMillis();
        this.lastActivityTimestamp = System.currentTimeMillis();
        this.active = true;

        // Initialize attack style map
        for (AttackStyle style : AttackStyle.values())
        {
            damageByStyle.put(style, 0);
        }
    }

    public void addDamage(DamageEntry entry)
    {
        damageEntries.add(entry);
        totalDamage += entry.getDamage();

        if (entry.getDamage() > maxHit)
        {
            maxHit = entry.getDamage();
        }

        // Update last activity
        lastActivityTimestamp = System.currentTimeMillis();

        // Track by attack style
        AttackStyle style = entry.getAttackStyle();
        damageByStyle.merge(style, entry.getDamage(), Integer::sum);

        // Track by weapon
        if (entry.getWeapon() != null && !entry.getWeapon().isEmpty())
        {
            damageByWeapon.merge(entry.getWeapon(), entry.getDamage(), Integer::sum);
        }

        // Track by target
        if (entry.getTarget() != null && !entry.getTarget().isEmpty())
        {
            damageByTarget.merge(entry.getTarget(), entry.getDamage(), Integer::sum);
            targets.add(entry.getTarget());

            // Set primary target if not set
            if (primaryTarget == null)
            {
                primaryTarget = entry.getTarget();
            }
        }
    }

    public void incrementAttacks()
    {
        totalAttacks++;
        lastActivityTimestamp = System.currentTimeMillis();
    }

    public void incrementSuccessfulHits()
    {
        successfulHits++;
    }

    public double getDPS()
    {
        long durationMs = getDurationMillis();
        if (durationMs == 0)
        {
            return 0.0;
        }
        return (totalDamage / (durationMs / 1000.0));
    }

    public double getAccuracyPercentage()
    {
        if (totalAttacks == 0)
        {
            return 0.0;
        }
        return (successfulHits / (double) totalAttacks) * 100.0;
    }

    public long getDurationMillis()
    {
        if (active)
        {
            return System.currentTimeMillis() - combatStartTimestamp;
        }
        else
        {
            return combatEndTimestamp - combatStartTimestamp;
        }
    }

    public long getDurationSeconds()
    {
        return getDurationMillis() / 1000;
    }

    public long getSecondsSinceLastActivity()
    {
        return (System.currentTimeMillis() - lastActivityTimestamp) / 1000;
    }

    public void endSession()
    {
        this.active = false;
        this.endTime = LocalDateTime.now();
        this.combatEndTimestamp = System.currentTimeMillis();
    }

    public Map<String, Integer> getDamageByAttackStyle()
    {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<AttackStyle, Integer> entry : damageByStyle.entrySet())
        {
            if (entry.getValue() > 0)
            {
                result.put(entry.getKey().getDisplayName(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, Integer> getDamageByWeapon()
    {
        return new HashMap<>(damageByWeapon);
    }

    public Map<String, Integer> getDamageByTarget()
    {
        return new HashMap<>(damageByTarget);
    }

    public List<DamageEntry> getDamageEntries()
    {
        return new ArrayList<>(damageEntries);
    }

    public boolean isTimedOut(int timeoutSeconds)
    {
        return getSecondsSinceLastActivity() >= timeoutSeconds;
    }

    public void updateLastActivity()
    {
        this.lastActivityTimestamp = System.currentTimeMillis();
    }

    public LocalDateTime getTimestamp()
    {
        return startTime;
    }
}