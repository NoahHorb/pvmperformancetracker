package net.runelite.client.plugins.pvmperformancetracker.model;

import lombok.Data;
import net.runelite.client.plugins.pvmperformancetracker.enums.AttackStyle;

import java.time.LocalDateTime;

@Data
public class DamageEntry
{
    private final int damage;
    private final String target;
    private final String weapon;
    private final AttackStyle attackStyle;
    private final LocalDateTime timestamp;
    private final String attackName;
    private final int attackAnimation;

    public DamageEntry(int damage, String target, String weapon, AttackStyle attackStyle)
    {
        this(damage, target, weapon, attackStyle, null, -1);
    }

    public DamageEntry(int damage, String target, String weapon, AttackStyle attackStyle, String attackName, int attackAnimation)
    {
        this.damage = damage;
        this.target = target;
        this.weapon = weapon;
        this.attackStyle = attackStyle;
        this.timestamp = LocalDateTime.now();
        this.attackName = attackName;
        this.attackAnimation = attackAnimation;
    }

    public boolean isSuccessfulHit()
    {
        return damage > 0;
    }
}