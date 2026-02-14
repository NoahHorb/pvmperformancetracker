package net.runelite.client.plugins.pvmperformancetracker.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Database of NPC-specific attacks with their max hits
 * This is crucial for accurate death probability calculations
 *
 * PHILOSOPHY:
 * - For most NPCs: Use OSRSBox general max hit (already implemented)
 * - For bosses with special attacks: Use this curated database
 * - Users can add custom entries via JSON files in .runelite/pvmperformancetracker/npc-attacks/
 */
@Slf4j
public class NpcAttackDatabase
{
    private final Map<Integer, NpcAttackSet> npcAttacks = new HashMap<>();
    private final Gson gson;

    public NpcAttackDatabase()
    {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        initializeDatabase();
        loadCustomAttacks();
    }

    /**
     * Get the attack data for a specific NPC attack
     * Returns null if no specific data exists (caller should use OSRSBox default)
     */
    public AttackData getAttackData(int npcId, int animationId, int projectileId)
    {
        NpcAttackSet attackSet = npcAttacks.get(npcId);
        if (attackSet == null)
        {
            return null; // No specific data - use OSRSBox default
        }

        // Try to match by animation first
        if (animationId != -1)
        {
            AttackData data = attackSet.animationAttacks.get(animationId);
            if (data != null)
            {
                log.debug("Found specific attack data for NPC {} animation {}: max={}, min={}",
                        npcId, animationId, data.getMaxHit(), data.getMinHit());
                return data;
            }
        }

        // Try to match by projectile
        if (projectileId != -1)
        {
            AttackData data = attackSet.projectileAttacks.get(projectileId);
            if (data != null)
            {
                log.debug("Found specific attack data for NPC {} projectile {}: max={}, min={}",
                        npcId, projectileId, data.getMaxHit(), data.getMinHit());
                return data;
            }
        }

        // Return default (create AttackData with default max hit, 0 min hit)
        return new AttackData(attackSet.getDefaultMaxHit(), "Default attack");
    }

    /**
     * Legacy method - returns just max hit
     * @deprecated Use getAttackData() instead to get min hit too
     */
    @Deprecated
    public Integer getAttackMaxHit(int npcId, int animationId, int projectileId)
    {
        AttackData data = getAttackData(npcId, animationId, projectileId);
        return data != null ? data.getMaxHit() : null;
    }

    /**
     * Load custom attack definitions from user JSON files
     * Users can add their own boss data in .runelite/pvmperformancetracker/npc-attacks/
     */
    private void loadCustomAttacks()
    {
        try
        {
            // Check for custom attacks directory
            Path customDir = Paths.get(System.getProperty("user.home"), ".runelite", "pvmperformancetracker", "npc-attacks");

            if (!Files.exists(customDir))
            {
                Files.createDirectories(customDir);
                log.info("Created custom NPC attacks directory: {}", customDir);
                return;
            }

            // Load all JSON files in the directory
            Files.list(customDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(this::loadCustomAttackFile);

        }
        catch (IOException e)
        {
            log.warn("Failed to load custom NPC attacks", e);
        }
    }

    private void loadCustomAttackFile(Path filePath)
    {
        try (FileReader reader = new FileReader(filePath.toFile()))
        {
            Type type = new TypeToken<Map<Integer, NpcAttackSet>>(){}.getType();
            Map<Integer, NpcAttackSet> customAttacks = gson.fromJson(reader, type);

            if (customAttacks != null)
            {
                npcAttacks.putAll(customAttacks);
                log.info("Loaded {} custom NPC attack sets from {}", customAttacks.size(), filePath.getFileName());
            }
        }
        catch (Exception e)
        {
            log.error("Failed to load custom attack file: {}", filePath, e);
        }
    }

    /**
     * Initialize the database with curated boss attack data
     * Only includes bosses with multiple different attacks that have different max hits
     */
    private void initializeDatabase()
    {
        log.info("Initializing NPC Attack Database with curated boss data...");

        // DT2 BOSSES - These have very different max hits for different attacks
        addVardorvis();
        addDukeSucellus();
        addLeviathan();
        addWhisperer();

        // TOMBS OF AMASCUT - Multiple attack types per boss
        addToABosses();

        // CHAMBERS OF XERIC - Special attacks differ significantly
        addCoxBosses();

        // THEATRE OF BLOOD - Phase-dependent and special attacks
        addTobBosses();

        // GOD WARS DUNGEON - Melee vs ranged vs magic
        addGwdBosses();

        // WILDERNESS BOSSES - Special mechanics
        addWildernessBosses();

        // SLAYER BOSSES - Special attacks
        addSlayerBosses();

        // INFERNO - Different Jads, Zuk phases
        addInferno();

        // NEX - Phase changes
        addNex();

        log.info("Loaded {} NPC attack sets", npcAttacks.size());
    }

    // ===== BOSS DEFINITIONS =====
    // Only include bosses where different attacks have SIGNIFICANTLY different max hits
    // For most NPCs, OSRSBox max hit is sufficient

    private void addVardorvis()
    {
        for (int id : new int[]{12223, 12224, 12225})
        {
            NpcAttackSet attacks = new NpcAttackSet(id, "Vardorvis", 30);
            attacks.addAnimation(9492, 30, "Melee swipe");
            attacks.addAnimation(9493, 65, "Axe throw special"); // MUCH higher!
            attacks.addAnimation(9495, 15, "Spike attack through prayer");
            npcAttacks.put(id, attacks);
        }
    }

    private void addDukeSucellus()
    {
        for (int id : new int[]{12166, 12167, 12168})
        {
            NpcAttackSet attacks = new NpcAttackSet(id, "Duke Sucellus", 41);
            attacks.addAnimation(10812, 41, "Melee");
            attacks.addProjectile(2514, 32, "Magic");
            attacks.addAnimation(10814, 10, "Siphon unavoidable");
            npcAttacks.put(id, attacks);
        }
    }

    private void addLeviathan()
    {
        for (int id : new int[]{12214, 12215, 12216})
        {
            NpcAttackSet attacks = new NpcAttackSet(id, "The Leviathan", 50);
            attacks.addProjectile(2487, 50, "Ranged");
            attacks.addProjectile(2488, 80, "Lightning special"); // Much higher!
            npcAttacks.put(id, attacks);
        }
    }

    private void addWhisperer()
    {
        for (int id : new int[]{12205, 12206, 12207})
        {
            NpcAttackSet attacks = new NpcAttackSet(id, "The Whisperer", 23);
            attacks.addAnimation(9493, 23, "Melee");
            attacks.addAnimation(9495, 15, "Screech typeless");
            npcAttacks.put(id, attacks);
        }
    }

    private void addToABosses()
    {
        // Ba-Ba
        NpcAttackSet baba = new NpcAttackSet(11779, "Ba-Ba", 14);
        baba.addAnimation(9675, 14, "Melee slam");
        baba.addAnimation(9676, 20, "Boulder throw");
        npcAttacks.put(11779, baba);

        // Kephri
        for (int id : new int[]{11719, 11720, 11721})
        {
            NpcAttackSet kephri = new NpcAttackSet(id, "Kephri", 13);
            kephri.addAnimation(9547, 13, "Melee");
            kephri.addAnimation(9549, 15, "Dung special");
            npcAttacks.put(id, kephri);
        }

        // Akkha - Different styles have different max hits
        for (int id : new int[]{11789, 11790, 11791, 11792})
        {
            NpcAttackSet akkha = new NpcAttackSet(id, "Akkha", 15);
            akkha.addAnimation(9766, 15, "Melee");
            akkha.addProjectile(2140, 22, "Magic");
            akkha.addProjectile(2141, 22, "Ranged");
            npcAttacks.put(id, akkha);
        }
    }

    private void addCoxBosses()
    {
        // Olm - Different attacks
        for (int id : new int[]{7554, 7555})
        {
            NpcAttackSet olm = new NpcAttackSet(id, "Olm Head", 32);
            olm.addProjectile(1339, 32, "Magic");
            olm.addProjectile(1340, 32, "Ranged");
            olm.addAnimation(7568, 13, "Fire breath unavoidable");
            npcAttacks.put(id, olm);
        }

        // Tekton - Regular vs anvil
        for (int id : new int[]{7541, 7542, 7543})
        {
            NpcAttackSet tekton = new NpcAttackSet(id, "Tekton", 30);
            tekton.addAnimation(7492, 30, "Melee");
            tekton.addAnimation(7493, 50, "Anvil smash special");
            npcAttacks.put(id, tekton);
        }
    }

    private void addTobBosses()
    {
        // Verzik P3 - Melee much higher than ranged
        for (int id : new int[]{8371, 8372, 8373})
        {
            NpcAttackSet verzikP3 = new NpcAttackSet(id, "Verzik P3", 50);
            verzikP3.addProjectile(1585, 50, "Ranged");
            verzikP3.addAnimation(8117, 69, "Melee special"); // Very high!
            npcAttacks.put(id, verzikP3);
        }
    }

    private void addGwdBosses()
    {
        // Bandos - Melee vs ranged
        NpcAttackSet bandos = new NpcAttackSet(6260, "General Graardor", 60);
        bandos.addAnimation(7060, 60, "Melee");
        bandos.addProjectile(1200, 44, "Ranged");
        npcAttacks.put(6260, bandos);

        // K'ril - Melee vs magic
        NpcAttackSet kril = new NpcAttackSet(6203, "K'ril Tsutsaroth", 49);
        kril.addAnimation(6948, 49, "Melee");
        kril.addProjectile(1227, 32, "Magic");
        npcAttacks.put(6203, kril);

        // Zilyana - Melee vs magic
        NpcAttackSet zilyana = new NpcAttackSet(6247, "Commander Zilyana", 32);
        zilyana.addAnimation(6964, 32, "Melee");
        zilyana.addProjectile(1221, 22, "Magic");
        npcAttacks.put(6247, zilyana);
    }

    private void addWildernessBosses()
    {
        // Callisto - Just melee (no special tracking needed, but good example)
        // Most wilderness bosses have single attack types
    }

    private void addSlayerBosses()
    {
        // Cerberus - Regular vs ghost special
        for (int id : new int[]{5862, 5863, 5864})
        {
            NpcAttackSet cerberus = new NpcAttackSet(id, "Cerberus", 23);
            cerberus.addAnimation(4490, 23, "Melee");
            cerberus.addProjectile(1242, 30, "Magic");
            cerberus.addProjectile(1245, 30, "Ranged");
            cerberus.addAnimation(4494, 50, "Ghost special"); // Much higher!
            npcAttacks.put(id, cerberus);
        }
    }

    private void addInferno()
    {
        // TzKal-Zuk
        NpcAttackSet zuk = new NpcAttackSet(7706, "TzKal-Zuk", 60);
        zuk.addProjectile(1521, 60, "Ranged");
        zuk.addProjectile(1522, 115, "Mage attack"); // Very high!
        npcAttacks.put(7706, zuk);
    }

    private void addNex()
    {
        // Nex - Different phases
        NpcAttackSet nex = new NpcAttackSet(11278, "Nex", 31);
        nex.addProjectile(1953, 31, "Magic smoke");
        nex.addProjectile(1954, 35, "Ice");
        nex.addProjectile(1957, 44, "Blood siphon");
        npcAttacks.put(11278, nex);
    }

    /**
     * Represents a set of attacks for a specific NPC
     */
    @Data
    public static class NpcAttackSet
    {
        private final int npcId;
        private final String npcName;
        private final int defaultMaxHit;

        private final Map<Integer, AttackData> animationAttacks = new HashMap<>();
        private final Map<Integer, AttackData> projectileAttacks = new HashMap<>();

        public void addAnimation(int animationId, int maxHit, String description)
        {
            animationAttacks.put(animationId, new AttackData(maxHit, description));
        }

        public void addAnimation(int animationId, int maxHit, int minHit, String description)
        {
            animationAttacks.put(animationId, new AttackData(maxHit, minHit, description));
        }

        public void addProjectile(int projectileId, int maxHit, String description)
        {
            projectileAttacks.put(projectileId, new AttackData(maxHit, description));
        }

        public void addProjectile(int projectileId, int maxHit, int minHit, String description)
        {
            projectileAttacks.put(projectileId, new AttackData(maxHit, minHit, description));
        }

        public Integer getMaxHitByAnimation(int animationId)
        {
            AttackData data = animationAttacks.get(animationId);
            return data != null ? data.getMaxHit() : null;
        }

        public Integer getMinHitByAnimation(int animationId)
        {
            AttackData data = animationAttacks.get(animationId);
            return data != null ? data.getMinHit() : null;
        }

        public Integer getMaxHitByProjectile(int projectileId)
        {
            AttackData data = projectileAttacks.get(projectileId);
            return data != null ? data.getMaxHit() : null;
        }

        public Integer getMinHitByProjectile(int projectileId)
        {
            AttackData data = projectileAttacks.get(projectileId);
            return data != null ? data.getMinHit() : null;
        }
    }

    @Data
    public static class AttackData
    {
        private final int maxHit;
        private final int minHit; // Minimum hit (0 for most attacks)
        private final String description;

        // Constructor for attacks with min hit
        public AttackData(int maxHit, int minHit, String description)
        {
            this.maxHit = maxHit;
            this.minHit = minHit;
            this.description = description;
        }

        // Constructor for normal attacks (min hit = 0)
        public AttackData(int maxHit, String description)
        {
            this.maxHit = maxHit;
            this.minHit = 0;
            this.description = description;
        }
    }
}