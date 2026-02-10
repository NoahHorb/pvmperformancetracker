package net.runelite.client.plugins.pvmperformancetracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttackStyle
{
    MELEE("Melee"),
    RANGED("Ranged"),
    MAGIC("Magic"),
    UNKNOWN("Unknown");

    private final String displayName;

    public static AttackStyle fromString(String style)
    {
        if (style == null)
        {
            return UNKNOWN;
        }

        String upperStyle = style.toUpperCase();

        switch (upperStyle)
        {
            case "MELEE":
            case "STAB":
            case "SLASH":
            case "CRUSH":
                return MELEE;
            case "RANGED":
            case "RANGE":
            case "ACCURATE":
            case "RAPID":
            case "LONGRANGE":
                return RANGED;
            case "MAGIC":
            case "MAGE":
            case "SPELL":
            case "DEFENSIVE":
                return MAGIC;
            default:
                return UNKNOWN;
        }
    }
}