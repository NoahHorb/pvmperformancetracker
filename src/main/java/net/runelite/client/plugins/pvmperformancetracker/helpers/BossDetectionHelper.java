package net.runelite.client.plugins.pvmperformancetracker.helpers;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
/**
 * This is only used for boss only tracking - Might remove
 */
@Slf4j
public class BossDetectionHelper
{
    // Common boss names
    private static final Set<String> BOSS_NAMES = new HashSet<>(Arrays.asList(
            // Godwars Dungeon
            "General Graardor", "Kree'arra", "Commander Zilyana", "K'ril Tsutsaroth",
            "Nex",

            // Wilderness Bosses
            "Callisto", "Venenatis", "Vet'ion", "Chaos Elemental", "Crazy archaeologist",
            "Chaos Fanatic", "Scorpia", "King Black Dragon",

            // Chambers of Xeric (Cox/Raids 1)
            "Great Olm", "Tekton", "Vespula", "Vanguard", "Vasa Nistirio", "Muttadile",
            "Mystics", "Shamans", "Skeletal Mystic",

            // Theatre of Blood (ToB/Raids 2)
            "Maiden of Sugadinti", "Pestilent Bloat", "Nylocas Vasilias", "Sotetseg",
            "Xarpus", "Verzik Vitur",

            // Tombs of Amascut (ToA/Raids 3)
            "Apmeken", "Ba-Ba", "Kephri", "Akkha", "Zebak", "Tumeken's Warden",
            "Elidinis' Warden", "Wardens",

            // Slayer Bosses
            "Cerberus", "Thermonuclear smoke devil", "Kraken", "Abyssal Sire",
            "Grotesque Guardians", "Alchemical Hydra",

            // Nightmare
            "The Nightmare", "Phosani's Nightmare",

            // Corporeal Beast
            "Corporeal Beast",

            // Dagannoth Kings
            "Dagannoth Prime", "Dagannoth Rex", "Dagannoth Supreme",

            // Kalphite Queen
            "Kalphite Queen",

            // Giant Mole
            "Giant Mole",

            // Barrows Brothers
            "Ahrim the Blighted", "Dharok the Wretched", "Guthan the Infested",
            "Karil the Tainted", "Torag the Corrupted", "Verac the Defiled",

            // Zulrah
            "Zulrah",

            // Vorkath
            "Vorkath",

            // Jad & Zuk
            "TzTok-Jad", "TzKal-Zuk", "JalTok-Jad",

            // Dragon bosses
            "Elvarg", "Galvek",

            // Demonic Gorillas
            "Demonic gorilla",

            // Skotizo
            "Skotizo",

            // Sarachnis
            "Sarachnis",

            // Zalcano
            "Zalcano",

            // Gauntlet
            "Crystalline Hunllef", "Corrupted Hunllef",

            // Phantom Muspah
            "Phantom Muspah",

            // Duke Sucellus
            "Duke Sucellus",

            // Vardorvis
            "Vardorvis",

            // The Leviathan
            "The Leviathan",

            // The Whisperer
            "The Whisperer",

            // Perilous Moons
            "Blue Moon",

            // Scurrius
            "Scurrius",

            // Araxxor (Fortis Colosseum)
            "Sol Heredit",

            // Obor
            "Obor",

            // Bryophyta
            "Bryophyta",

            // Hespori
            "Hespori",

            // Mimic
            "The Mimic",

            // Tempoross
            "Tempoross",

            // Wintertodt
            "Wintertodt"
    ));

    // Boss combat level thresholds
    private static final int MIN_BOSS_COMBAT_LEVEL = 100;

    /**
     * Check if an NPC is considered a boss
     */
    public boolean isBoss(NPC npc)
    {
        if (npc == null)
        {
            return false;
        }

        String npcName = npc.getName();
        if (npcName == null)
        {
            return false;
        }

        // Check if NPC name matches known bosses
        if (BOSS_NAMES.contains(npcName))
        {
            return true;
        }

        // Check for partial matches (some bosses have variations)
        for (String bossName : BOSS_NAMES)
        {
            if (npcName.contains(bossName) || bossName.contains(npcName))
            {
                return true;
            }
        }

        // Check combat level and size as fallback
        int combatLevel = npc.getCombatLevel();
        int size = npc.getComposition().getSize();

        // High combat level + large size often indicates a boss
        if (combatLevel >= MIN_BOSS_COMBAT_LEVEL && size >= 3)
        {
            return true;
        }

        // Very high combat level alone can indicate a boss
        if (combatLevel >= 300)
        {
            return true;
        }

        return false;
    }

    /**
     * Check if NPC is a raid boss
     */
    public boolean isRaidBoss(NPC npc)
    {
        if (npc == null || npc.getName() == null)
        {
            return false;
        }

        String name = npc.getName();

        // Chambers of Xeric
        if (name.contains("Olm") || name.contains("Tekton") || name.contains("Vespula") ||
                name.contains("Vanguard") || name.contains("Vasa") || name.contains("Muttadile"))
        {
            return true;
        }

        // Theatre of Blood
        if (name.contains("Maiden") || name.contains("Bloat") || name.contains("Nylocas") ||
                name.contains("Sotetseg") || name.contains("Xarpus") || name.contains("Verzik"))
        {
            return true;
        }

        // Tombs of Amascut
        if (name.contains("Apmeken") || name.contains("Ba-Ba") || name.contains("Kephri") ||
                name.contains("Akkha") || name.contains("Zebak") || name.contains("Warden"))
        {
            return true;
        }

        return false;
    }

    /**
     * Check if NPC is a Godwars boss
     */
    public boolean isGodwarsBoss(NPC npc)
    {
        if (npc == null || npc.getName() == null)
        {
            return false;
        }

        String name = npc.getName();
        return name.contains("Graardor") || name.contains("Kree'arra") ||
                name.contains("Zilyana") || name.contains("K'ril") || name.equals("Nex");
    }

    /**
     * Get boss category
     */
    public String getBossCategory(NPC npc)
    {
        if (npc == null || npc.getName() == null)
        {
            return "Unknown";
        }

        if (isRaidBoss(npc))
        {
            return "Raid";
        }

        if (isGodwarsBoss(npc))
        {
            return "Godwars Dungeon";
        }

        String name = npc.getName();

        if (name.contains("TzTok-Jad") || name.contains("TzKal-Zuk"))
        {
            return "Fight Caves/Inferno";
        }

        if (name.contains("Zulrah") || name.contains("Vorkath"))
        {
            return "Dragon/Snake Boss";
        }

        if (name.contains("Cerberus") || name.contains("Kraken") ||
                name.contains("Thermonuclear") || name.contains("Abyssal Sire") ||
                name.contains("Grotesque") || name.contains("Alchemical Hydra"))
        {
            return "Slayer Boss";
        }

        if (BOSS_NAMES.contains(name))
        {
            return "Boss";
        }

        return "Standard";
    }

    /**
     * Add a custom boss name to the detection list
     */
    public void addCustomBoss(String bossName)
    {
        if (bossName != null && !bossName.isEmpty())
        {
            BOSS_NAMES.add(bossName);
            log.debug("Added custom boss: {}", bossName);
        }
    }
}