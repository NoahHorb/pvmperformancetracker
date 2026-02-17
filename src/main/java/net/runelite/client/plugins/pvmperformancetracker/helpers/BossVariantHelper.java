package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;

/**
 * Helper class to distinguish between boss variants that share the same NPC ID
 * Returns keys in the format: "ID_Variant_" (e.g., "12223_Awakened_", "12223_Post_quest_")
 * Needs manual curation
 * notable boss: DT2, Doom
 */
@Slf4j
public class BossVariantHelper
{

    public BossVariantHelper()
    {
    }

    /**
     * Get the appropriate database key for an NPC, accounting for variants
     * Returns keys in the format used by the database: "ID_Variant_"
     *
     * @param npc The NPC to check
     * @return Database key string (e.g., "12223_Awakened_", "12223_Post_quest_", "12224_Quest_")
     */
    public String getNpcDatabaseKey(NPC npc)
    {
        if (npc == null)
        {
            return null;
        }

        int npcId = npc.getId();
        String npcName = npc.getName();

        // Vardorvis has 4 variants
        if (isVardorvis(npcId))
        {
            return getVardorvisVariant(npc);
        }

        // Other DT2 Bosses - distinguish Awakened from Normal
        if (isDT2Boss(npcId))
        {
            boolean isAwakened = isAwakenedDT2Boss(npc);
            if (isAwakened)
            {
                return npcId + "_Awakened_";
            }
            else
            {
                return npcId + "_Post_quest_";  // Assume post-quest for non-awakened
            }
        }

        // For non-variant bosses, just return the ID as a string
        return String.valueOf(npcId);
    }

    /**
     * Check if NPC is Vardorvis (any variant)
     */
    private boolean isVardorvis(int npcId)
    {
        // Vardorvis Quest: 12224, 12228, 12425
        // Vardorvis Post-quest/Awakened: 12223, 12426
        // Vardorvis Armageddon: 13656
        return npcId == 12223 || npcId == 12224 || npcId == 12228 ||
                npcId == 12425 || npcId == 12426 || npcId == 13656;
    }

    /**
     * Get the specific Vardorvis variant based on NPC ID and combat level
     */
    private String getVardorvisVariant(NPC npc)
    {
        int npcId = npc.getId();
        int combatLevel = npc.getCombatLevel();

        // Armageddon variant (different ID)
        if (npcId == 13656)
        {
            log.debug("Detected Vardorvis Armageddon (ID: {}, combat: {})", npcId, combatLevel);
            return "13656_Armageddon_";
        }

        // Quest variant (different ID)
        if (npcId == 12224 || npcId == 12228 || npcId == 12425)
        {
            log.debug("Detected Vardorvis Quest (ID: {}, combat: {})", npcId, combatLevel);
            return "12224_Quest_";
        }

        // ID 12223 or 12426 - could be Post-quest OR Awakened
        // Distinguish by combat level:
        // - Post-quest: 784
        // - Awakened: 1136
        if (combatLevel >= 1000)
        {
            log.debug("Detected Vardorvis Awakened (ID: {}, combat: {})", npcId, combatLevel);
            return "12223_Awakened_";
        }
        else if (combatLevel >= 700)
        {
            log.debug("Detected Vardorvis Post-quest (ID: {}, combat: {})", npcId, combatLevel);
            return "12223_Post_quest_";
        }
        else
        {
            // Fallback - probably Quest variant with wrong ID
            log.warn("Unknown Vardorvis variant (ID: {}, combat: {}), defaulting to Post-quest", npcId, combatLevel);
            return "12223_Post_quest_";
        }
    }

    /**
     * Check if NPC is a DT2 boss (excluding Vardorvis which has its own method)
     */
    private boolean isDT2Boss(int npcId)
    {
        // Duke Sucellus: 12166, 12167
        // The Leviathan: 12215
        // The Whisperer: 12205, 12206, 12207
        return npcId == 12166 || npcId == 12167 ||  // Duke
                npcId == 12215 ||  // Leviathan
                npcId == 12205 || npcId == 12206 || npcId == 12207;  // Whisperer
    }

    /**
     * Determine if a DT2 boss (non-Vardorvis) is Awakened or Normal
     */
    private boolean isAwakenedDT2Boss(NPC npc)
    {
        int npcId = npc.getId();
        int combatLevel = npc.getCombatLevel();

        if (npcId == 12166 || npcId == 12167) // Duke Sucellus
        {
            // Awakened Duke: ~893
            // Normal Duke: ~636
            if (combatLevel >= 750)
            {
                log.debug("Detected Awakened Duke Sucellus (combat level: {})", combatLevel);
                return true;
            }
            else
            {
                log.debug("Detected Normal Duke Sucellus (combat level: {})", combatLevel);
                return false;
            }
        }

        if (npcId == 12215) // Leviathan
        {
            // Awakened Leviathan: ~815
            // Normal Leviathan: ~624
            if (combatLevel >= 700)
            {
                log.debug("Detected Awakened Leviathan (combat level: {})", combatLevel);
                return true;
            }
            else
            {
                log.debug("Detected Normal Leviathan (combat level: {})", combatLevel);
                return false;
            }
        }

        if (npcId == 12205 || npcId == 12206 || npcId == 12207) // Whisperer
        {
            // Awakened Whisperer: ~886
            // Normal Whisperer: ~645
            if (combatLevel >= 750)
            {
                log.debug("Detected Awakened Whisperer (combat level: {})", combatLevel);
                return true;
            }
            else
            {
                log.debug("Detected Normal Whisperer (combat level: {})", combatLevel);
                return false;
            }
        }

        return false;  // Default to Normal
    }

    /**
     * Get a human-readable name for the boss variant
     */
    public String getVariantDisplayName(NPC npc)
    {
        if (npc == null || npc.getName() == null)
        {
            return null;
        }

        String baseName = npc.getName();

        if (isVardorvis(npc.getId()))
        {
            int combatLevel = npc.getCombatLevel();
            if (combatLevel >= 1000)
            {
                return baseName + " - Awakened";
            }
            else if (combatLevel >= 700)
            {
                return baseName + " - Post-quest";
            }
            else if (combatLevel >= 550)
            {
                return baseName + " - Quest";
            }
            else
            {
                return baseName + " - Armageddon";
            }
        }

        if (isDT2Boss(npc.getId()))
        {
            boolean isAwakened = isAwakenedDT2Boss(npc);
            return isAwakened ? (baseName + " - Awakened") : (baseName + " - Post-quest");
        }

        return baseName;
    }

    /**
     * Check if two NPCs are the same variant
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