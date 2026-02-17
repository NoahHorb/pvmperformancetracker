package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

/**
 * Helper class to distinguish between boss variants that share the same NPC ID
 * Examples: Awakened vs Normal DT2 bosses, different Zulrah phases, etc.
 */
@Slf4j
public class BossVariantHelper
{
    private final Client client;

    public BossVariantHelper(Client client)
    {
        this.client = client;
    }

    /**
     * Get the appropriate database key for an NPC, accounting for variants
     * This handles cases where Normal and Awakened bosses share IDs
     *
     * @param npc The NPC to check
     * @return Database key string (e.g., "12223_Awakened" or "12223_Normal")
     */
    public String getNpcDatabaseKey(NPC npc)
    {
        if (npc == null)
        {
            return null;
        }

        int npcId = npc.getId();
        String npcName = npc.getName();

        // DT2 Bosses - distinguish Awakened from Normal
        if (isDT2Boss(npcId))
        {
            boolean isAwakened = isAwakenedDT2Boss(npc);
            String variant = isAwakened ? "Awakened" : "Normal";
            return npcId + "_" + variant;
        }

        // Zulrah - distinguish phases
        if (isZulrah(npcId))
        {
            String phase = getZulrahPhase(npc);
            return npcId + "_" + phase;
        }

        // Default: just return the ID as string
        return String.valueOf(npcId);
    }

    /**
     * Check if NPC is a DT2 boss
     */
    private boolean isDT2Boss(int npcId)
    {
        // Vardorvis: 12223
        // Duke Sucellus: 12166, 12167
        // The Leviathan: 12215
        // The Whisperer: 12205, 12206, 12207
        return npcId == 12223 ||  // Vardorvis
                npcId == 12166 || npcId == 12167 ||  // Duke
                npcId == 12215 ||  // Leviathan
                npcId == 12205 || npcId == 12206 || npcId == 12207;  // Whisperer
    }

    /**
     * Check if NPC is Zulrah
     */
    private boolean isZulrah(int npcId)
    {
        return npcId == 2042 || npcId == 2043 || npcId == 2044;
    }

    /**
     * Determine if a DT2 boss is Awakened or Normal
     *
     * Detection methods:
     * 1. Check max HP (Awakened has higher HP)
     * 2. Check location (Awakened is in different area)
     * 3. Check combat level
     */
    private boolean isAwakenedDT2Boss(NPC npc)
    {
        int npcId = npc.getId();

        // Method 1: Location-based detection
        WorldPoint location = npc.getWorldLocation();

        if (npcId == 12223) // Vardorvis
        {
            // Awakened Vardorvis location: Stranglewood (different coordinates)
            // Normal Vardorvis location: Below Stranglewood

            // Awakened is typically in a different Y-plane or region
            // Check the Y-coordinate or plane
            int plane = location.getPlane();
            int regionId = location.getRegionID();

            // Awakened Vardorvis is in region 4405 (Stranglewood ritual site)
            // Normal Vardorvis is in region 4404
            if (regionId == 4405)
            {
                return true;  // Awakened
            }

            // Additional check: Awakened is usually on plane 1, normal on plane 0
            // This may vary, so location is more reliable
        }

        // Method 2: HP-based detection (fallback)
        int maxHp = npc.getHealthRatio();
        int currentHp = npc.getHealthScale();

        // Awakened bosses have significantly higher HP
        // This is less reliable as a detection method

        // Method 3: Combat level (most reliable for some bosses)
        int combatLevel = npc.getCombatLevel();

        if (npcId == 12223) // Vardorvis
        {
            // Awakened Vardorvis: 703
            // Normal Vardorvis: 542
            if (combatLevel > 650)
            {
                return true;  // Awakened
            }
        }

        if (npcId == 12166 || npcId == 12167) // Duke Sucellus
        {
            // Awakened Duke: 893
            // Normal Duke: 636
            if (combatLevel > 750)
            {
                return true;
            }
        }

        if (npcId == 12215) // Leviathan
        {
            // Awakened Leviathan: 815
            // Normal Leviathan: 624
            if (combatLevel > 700)
            {
                return true;
            }
        }

        if (npcId == 12205 || npcId == 12206 || npcId == 12207) // Whisperer
        {
            // Awakened Whisperer: 886
            // Normal Whisperer: 645
            if (combatLevel > 750)
            {
                return true;
            }
        }

        return false;  // Default to Normal
    }

    /**
     * Get Zulrah's current phase
     */
    private String getZulrahPhase(NPC npc)
    {
        int npcId = npc.getId();

        switch (npcId)
        {
            case 2042:
                return "Serpentine";  // Green (Ranged)
            case 2043:
                return "Magma";       // Red (Melee + Mage)
            case 2044:
                return "Tanzanite";   // Blue (Mage + Ranged)
            default:
                return "Unknown";
        }
    }

    /**
     * Get a human-readable name for the boss variant
     */
    public String getVariantDisplayName(NPC npc)
    {
        if (npc == null)
        {
            return null;
        }

        String baseName = npc.getName();

        if (isDT2Boss(npc.getId()))
        {
            boolean isAwakened = isAwakenedDT2Boss(npc);
            return baseName + (isAwakened ? " - Awakened" : "");
        }

        if (isZulrah(npc.getId()))
        {
            String phase = getZulrahPhase(npc);
            return baseName + " (" + phase + ")";
        }

        return baseName;
    }

    /**
     * Check if two NPCs are the same variant
     * Useful for verifying we're still fighting the same boss
     */
    public boolean isSameVariant(NPC npc1, NPC npc2)
    {
        if (npc1 == null || npc2 == null)
        {
            return false;
        }

        String key1 = getNpcDatabaseKey(npc1);
        String key2 = getNpcDatabaseKey(npc2);

        return key1 != null && key1.equals(key2);
    }
}