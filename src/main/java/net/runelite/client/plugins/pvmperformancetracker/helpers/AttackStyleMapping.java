package net.runelite.client.plugins.pvmperformancetracker.helpers;

import net.runelite.client.plugins.pvmperformancetracker.enums.eNPC;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.SpotanimID;

/**
 * Maps NPC animation and projectile IDs to attack styles.
 *
 * Two categories of animations are tracked:
 *
 * 1. DIRECT ATTACKS — animations where the NPC's hitsplat lands exactly 1 tick
 *    after the animation fires (all melee). Stored in animationDelays.
 *    Projectile-based attacks use registerProjectile() instead because their
 *    travel time depends on distance at spawn time.
 *
 * 2. MECHANICS — animations that spawn delayed hazards (e.g. Vardorvis axes)
 *    where damage can land on any tick after the animation. These are NOT
 *    scheduled as direct hits. Instead, a "mechanic active" flag is set in
 *    NpcAttackTracker so any hitsplat that doesn't match a scheduled direct-hit
 *    tick can be attributed to the mechanic.
 *
 * IDs sourced from RuneLite's AnimationID.java and SpotanimID.java.
 * Entries marked "TODO - verify" were matched via non-1:1 name mapping
 * and should be confirmed by in-game logging.
 *
 * To add a new boss:
 *   1. Add its NPC ID(s) to eNPC
 *   2. Add a section in initializeCommonBossMappings() below
 */
public class AttackStyleMapping
{
    private static final Map<Integer, Map<Integer, String>> animationMappings = new HashMap<>();
    private static final Map<Integer, Map<Integer, Integer>> animationDelays  = new HashMap<>();
    private static final Map<Integer, Map<Integer, String>> projectileMappings = new HashMap<>();
    private static final Map<Integer, Set<Integer>>         mechanicAnimations = new HashMap<>();
    private static final Map<Integer, Map<Integer, String>> mechanicStyles     = new HashMap<>();
    private static final Map<Integer, Map<Integer, int[]>> linkedMechanicBossIds = new HashMap<>();
    private static final Map<Integer, Map<Integer, String>> linkedMechanicStyles  = new HashMap<>();

    // -----------------------------------------------------------------------
    // Registration API
    // -----------------------------------------------------------------------

    public static void registerScheduledDirectAttack(int npcId, int animId, String style, int delayTicks)
    {
        animationMappings.computeIfAbsent(npcId, k -> new HashMap<>()).put(animId, style);
        animationDelays.computeIfAbsent(npcId, k -> new HashMap<>()).put(animId, delayTicks);
    }

    public static void registerMechanic(int npcId, int animOrSpotId, String style)
    {
        mechanicAnimations.computeIfAbsent(npcId, k -> new HashSet<>()).add(animOrSpotId);
        mechanicStyles.computeIfAbsent(npcId, k -> new HashMap<>()).put(animOrSpotId, style);
    }

    public static void registerProjectile(int npcId, int projectileId, String attackStyle)
    {
        projectileMappings.computeIfAbsent(npcId, k -> new HashMap<>()).put(projectileId, attackStyle);
    }

    /**
     * Register a mechanic that is animated by a SEPARATE sub-NPC rather than the boss itself.
     *
     * Example: Vardorvis axes — the axe NPC (not Vardorvis) fires animation 10365.
     * When that animation fires, we need to flag a mechanic on Vardorvis's tracker entry,
     * not on the axe NPC's entry — because resolveIncomingHit() is called with Vardorvis's index.
     *
     * @param subNpcId   The NPC ID of the sub-NPC that fires the animation (e.g. axe NPC)
     * @param animId     The animation ID the sub-NPC plays
     * @param style      The mechanic style string (e.g. "axes")
     * @param bossNpcIds The boss NPC IDs that own this mechanic (e.g. eNPC.VARDORVIS)
     */
    public static void registerLinkedMechanic(int subNpcId, int animId, String style, int[] bossNpcIds)
    {
        linkedMechanicBossIds.computeIfAbsent(subNpcId, k -> new HashMap<>()).put(animId, bossNpcIds);
        linkedMechanicStyles.computeIfAbsent(subNpcId, k -> new HashMap<>()).put(animId, style);
    }

    // -----------------------------------------------------------------------
    // Query API
    // -----------------------------------------------------------------------

    public static boolean isMechanicAnimation(int npcId, int animId)
    {
        Set<Integer> set = mechanicAnimations.get(npcId);
        return set != null && set.contains(animId);
    }

    public static boolean isDirectAttackAnimation(int npcId, int animId)
    {
        Map<Integer, String> map = animationMappings.get(npcId);
        return map != null && map.containsKey(animId);
    }

    public static String getStyleFromAnimation(int npcId, int animId)
    {
        Map<Integer, String> map = animationMappings.get(npcId);
        return map != null ? map.get(animId) : null;
    }

    public static int getDirectAttackDelay(int npcId, int animId)
    {
        Map<Integer, Integer> map = animationDelays.get(npcId);
        if (map == null) return -1;
        Integer delay = map.get(animId);
        return delay != null ? delay : -1;
    }

    public static String getMechanicStyle(int npcId, int animId)
    {
        Map<Integer, String> map = mechanicStyles.get(npcId);
        return map != null ? map.get(animId) : null;
    }

    public static String getStyleFromProjectile(int npcId, int projectileId)
    {
        Map<Integer, String> map = projectileMappings.get(npcId);
        return map != null ? map.get(projectileId) : null;
    }

    /** Returns true if the given sub-NPC animation is a linked mechanic. */
    public static boolean isLinkedMechanicAnimation(int subNpcId, int animId)
    {
        Map<Integer, int[]> map = linkedMechanicBossIds.get(subNpcId);
        return map != null && map.containsKey(animId);
    }

    /** Returns the boss NPC IDs that own this linked mechanic, or null. */
    public static int[] getLinkedMechanicBossIds(int subNpcId, int animId)
    {
        Map<Integer, int[]> map = linkedMechanicBossIds.get(subNpcId);
        return map != null ? map.get(animId) : null;
    }

    /** Returns the style string for a linked mechanic animation. */
    public static String getLinkedMechanicStyle(int subNpcId, int animId)
    {
        Map<Integer, String> map = linkedMechanicStyles.get(subNpcId);
        return map != null ? map.get(animId) : null;
    }
    /**
     * Returns true if we have ANY mapping for this NPC ID —
     * direct attacks, mechanics, projectiles, or linked mechanics.
     * Used by AnimationListener to filter out irrelevant NPCs cheaply.
     */
    public static boolean hasMappings(int npcId)
    {
        return animationMappings.containsKey(npcId)
                || projectileMappings.containsKey(npcId)
                || mechanicAnimations.containsKey(npcId)
                || linkedMechanicBossIds.containsKey(npcId);  // <- add this line to existing hasMappings
    }



    // -----------------------------------------------------------------------
    // Boss mappings
    // -----------------------------------------------------------------------

    public static void initializeCommonBossMappings()
    {
        // ===================================================================
        // Desert Treasure II
        // ===================================================================

        // Vardorvis — melee + axe mechanic + head-scream magic/ranged projectile
        // Anim: NPC_VARDORVIS_01_MELEE_01=10340, NPC_VARDORVIS_AXE_01_ATTACK_START=10365
        // Spot: VFX_VARDORVIS_HEAD_PROJECTILE_MAGIC_01=2520, *_RANGED_01=2521
        for (int id : eNPC.VARDORVIS)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_VARDORVIS_01_MELEE_01, "slash", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_VARDORVIS_01_DASH_01, "spikes", 3);
            registerMechanic(id, AnimationID.NPC_VARDORVIS_01_ENTANGLE_START, "strangle");
            registerMechanic    (id, AnimationID.NPC_VARDORVIS_AXE_01_ATTACK_START, "axes");
            registerProjectile  (id, SpotanimID.VFX_VARDORVIS_HEAD_PROJECTILE_MAGIC_01,  "magic");
            registerProjectile  (id, SpotanimID.VFX_VARDORVIS_HEAD_PROJECTILE_RANGED_01,  "ranged");


            registerLinkedMechanic(12225, AnimationID.NPC_VARDORVIS_AXE_01_ATTACK_START, "axes", eNPC.VARDORVIS);
        }

        // Duke Sucellus — melee + magic projectile + gaze mechanic + vents
        // Anim: NPC_DUKE_SUCELLUS01_ATTACK_MELEE_01=10176, _SIGHT_ATTACK_01=10180
        // Spot: VFX_DUKE_SUCELLUS_ATTACK_MAGIC_PROJECTILE_01=2434, PROJANIM_DUKE_SPIT_01=2436
        for (int[] group : new int[][]{eNPC.DUKE_SUCELLUS_AWAKENED, eNPC.DUKE_SUCELLUS_POST_QUEST, eNPC.DUKE_SUCELLUS_QUEST}){
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.NPC_DUKE_SUCELLUS01_ATTACK_MELEE_01, "crush", 1);
                registerProjectile  (id, SpotanimID.VFX_DUKE_SUCELLUS_ATTACK_MAGIC_PROJECTILE_01,  "magic");
                registerProjectile  (id, SpotanimID.PROJANIM_DUKE_SPIT_01,  "magic");
                registerMechanic    (id, AnimationID.NPC_DUKE_SUCELLUS01_SIGHT_ATTACK_01, "gaze");
            }
        }

        // The Leviathan — melee (head + tail) + ranged/magic/melee projectiles + rock mechanic
        // Anim: NPC_LEVIATHAN_01_MELEE_01=10283, _02=10284, TAIL_MELEE=10301-10303
        // Spot: VFX_LEVIATHAN_01_PROJECTILE_RANGED_01=2487, _MAGIC_01=2489, _MELEE_01=2488
        for (int id : eNPC.THE_LEVIATHAN)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_LEVIATHAN_01_MELEE_01, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_LEVIATHAN_01_MELEE_02, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_LEVIATHAN_TAIL01_MELEE01, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_LEVIATHAN_TAIL01_MELEEVARIANT01, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_LEVIATHAN_TAIL01_MELEEVARIANT02, "melee", 1);
            registerProjectile  (id, SpotanimID.VFX_LEVIATHAN_01_PROJECTILE_SPOTANIM_RANGED_01,  "ranged");
            registerProjectile  (id, SpotanimID.VFX_LEVIATHAN_01_PROJECTILE_RANGED_01,  "ranged");
            registerProjectile  (id, SpotanimID.VFX_LEVIATHAN_01_PROJECTILE_SPOTANIM_MAGIC_01,  "magic");
            registerProjectile  (id, SpotanimID.VFX_LEVIATHAN_01_PROJECTILE_MAGIC_01,  "magic");
            registerProjectile  (id, SpotanimID.VFX_LEVIATHAN_01_PROJECTILE_MELEE_01,  "melee");  // shockwave
            registerMechanic    (id, AnimationID.NPC_LEVIATHAN_01_SPIT_START, "rocks");
        }

        // The Whisperer — melee + magic/ranged projectiles + screech + leeches mechanic
        // Anim: NPC_WHISPERER_01_ATTACK_MELEE_01=10234, SCREECH_START=10250, SUMMON_LEECHES=10254
        // Spot: PROJ_WHISPERER_01_MAGIC_01=2445, PROJ_WHISPERER_01_RANGED_01=2444
        for (int id : eNPC.THE_WHISPERER)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_WHISPERER_01_ATTACK_MELEE_01, "melee",   1);
            registerProjectile  (id, SpotanimID.PROJ_WHISPERER_01_RANGED_01,  "ranged");
            registerProjectile  (id, SpotanimID.PROJ_WHISPERER_01_MAGIC_01,  "magic");
            registerMechanic    (id, AnimationID.NPC_WHISPERER_01_ATTACK_SCREECH_01_START, "screech");
            registerMechanic    (id, AnimationID.NPC_WHISPERER_01_SUMMON_LEECHES_01, "leeches");
        }

        // ===================================================================
        // Dragon Slayer II
        // ===================================================================

        // Vorkath — melee + ranged/magic/acid/dragonfire projectiles
        // Anim: DS2_VORKATH_ATTACK_MELEE=7951
        // Spot: VORKATH_RANGED_TRAVEL=1477, _MAGIC_TRAVEL=1479, _AREA_TRAVEL=1481, _ACID_TRAVEL=1483
        for (int id : eNPC.VORKATH_AWAKENED)
        {
            registerScheduledDirectAttack(id, AnimationID.DS2_VORKATH_ATTACK_MELEE, "melee",      1);
            registerProjectile  (id, SpotanimID.VORKATH_RANGED_TRAVEL, "ranged");
            registerProjectile  (id, SpotanimID.VORKATH_RANGED_IMPACT, "ranged");       // VORKATH_RANGED_IMPACT
            registerProjectile  (id, SpotanimID.VORKATH_MAGIC_TRAVEL, "magic");
            registerProjectile  (id, SpotanimID.VORKATH_MAGIC_IMPACT, "magic");        // VORKATH_MAGIC_IMPACT
            registerProjectile  (id, SpotanimID.VORKATH_AREA_TRAVEL, "dragonfire");
            registerProjectile  (id, SpotanimID.VORKATH_AREA_SMALL_TRAVEL, "dragonfire");   // VORKATH_AREA_SMALL_TRAVEL
            registerProjectile  (id, SpotanimID.VORKATH_AREA_TRAVEL_SMALL, "dragonfire");   // VORKATH_AREA_TRAVEL_SMALL
            registerProjectile  (id, SpotanimID.VORKATH_ACID_TRAVEL, "acid");
            registerProjectile  (id, SpotanimID.VORKATH_SPAWN_TRAVEL, "ranged");       // VORKATH_SPAWN_TRAVEL (zombified spawn)
        }

        // Galvek — melee + multi-element projectiles
        // Anim: GALVEK_GROUNDED_SLASH=7900, GALVEK_GROUNDED_BREATH=7901
        // Spot: GALVEK_RANGED_TRAVEL=1489, GALVEK_MAGIC_TRAVEL=1490, GALVEK_EARTH_PROJ=1493,
        //       GALVEK_FIRE_PROJ=1495, GALVEK_WIND_PROJ=1496, GALVEK_WATER_PROJ=1497
        for (int id : eNPC.GALVEK_MONSTER)
        {
            registerScheduledDirectAttack(id, AnimationID.GALVEK_GROUNDED_SLASH, "melee",      1);
            registerScheduledDirectAttack(id, AnimationID.GALVEK_GROUNDED_BREATH, "dragonfire", 1);
            registerProjectile  (id, SpotanimID.GALVEK_RANGED_TRAVEL, "ranged");        // wind
            registerProjectile  (id, SpotanimID.GALVEK_MAGIC_TRAVEL, "magic");         // water
            registerProjectile  (id, SpotanimID.GALVEK_EARTH_PROJ, "magic");         // earth
            registerProjectile  (id, SpotanimID.GALVEK_FIRE_PROJ, "dragonfire");    // fire
            registerProjectile  (id, SpotanimID.GALVEK_WIND_PROJ, "ranged");        // wind alt
            registerProjectile  (id, SpotanimID.GALVEK_WATER_PROJ, "magic");         // water alt
            registerMechanic    (id, AnimationID.GALVEK_GROUNDED_STOMP, "stomp");
        }

        // Elvarg (Dragon Slayer I)
        // Anim: DRAGONSLAYER_ELVARG_FIRE=6642, DRAGONSLAYER_ELVARG_FIRE2=6643
        for (int id : eNPC.ELVARG)      
        { 
            registerScheduledDirectAttack(id, AnimationID.DRAGONSLAYER_ELVARG_FIRE, "dragonfire", 1); 
            registerScheduledDirectAttack(id, AnimationID.DRAGONSLAYER_ELVARG_FIRE2, "dragonfire", 1); 
        }
        for (int id : eNPC.ELVARG_HARD) 
        { 
            registerScheduledDirectAttack(id, AnimationID.DRAGONSLAYER_ELVARG_FIRE, "dragonfire", 1); 
            registerScheduledDirectAttack(id, AnimationID.DRAGONSLAYER_ELVARG_FIRE2, "dragonfire", 1); 
        }

        // ===================================================================
        // Inferno / Fight Caves
        // ===================================================================

        // TzTok-Jad — melee + magic/ranged (animation-based, no projectile ID needed)
        // Anim: JALTOKJAD_ATTACK_MELEE=7590, _MAGIC=7592, _RANGED=7593
        for (int id : eNPC.TZTOK_JAD)     { registerJad(id); }
        for (int id : eNPC.TZTOK_JAD_REK) { registerJad(id); }
        for (int id : eNPC.JALTOK_JAD)    { registerJad(id); }

        // TzKal-Zuk — melee + ranged projectile shield barrage
        // Anim: ZUK_ATTACK=7566
        // Spot: INFERNO_ZUK_PROJECTILE=1375, _MID=2381, _SMALL=2261, _GIGANTIC=3294
        for (int id : eNPC.TZKAL_ZUK)
        {
            registerScheduledDirectAttack(id, AnimationID.ZUK_ATTACK, "melee", 1);
            registerProjectile  (id, SpotanimID.INFERNO_ZUK_PROJECTILE, "ranged");
            registerProjectile  (id, SpotanimID.INFERNO_ZUK_PROJECTILE_SMALL, "ranged");
            registerProjectile  (id, SpotanimID.INFERNO_ZUK_PROJECTILE_MID, "ranged");
            registerProjectile  (id, SpotanimID.INFERNO_ZUK_PROJECTILE_MID_SHORT, "ranged");
            registerProjectile  (id, SpotanimID.INFERNO_ZUK_PROJECTILE_GIGANTIC, "ranged");
        }

        // Jal-Xil — melee + ranged
        // Anim: JALXIL_ATTACK_MELEE=7604, JALXIL_ATTACK_RANGED=7605
        for (int id : eNPC.JAL_XIL)
        {
            registerScheduledDirectAttack(id, AnimationID.JALXIL_ATTACK_MELEE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.JALXIL_ATTACK_RANGED, "ranged", 1);
        }

        // Jal-Ak — melee/magic/ranged blob (3 attack styles)
        // Anim: JALAK_ATTACK_MELEE=7582, JALAK_ATTACK_MAGIC=7581, JALAK_ATTACK_RANGED=7583
        for (int id : eNPC.JAL_AK)
        {
            registerScheduledDirectAttack(id, AnimationID.JALAK_ATTACK_MAGIC, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.JALAK_ATTACK_MELEE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.JALAK_ATTACK_RANGED, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.JALAKXIL_ATTACK_MELEE, "melee", 1); // sub-blob JALAKXIL_ATTACK_MELEE
        }

        // ===================================================================
        // Theatre of Blood
        // ===================================================================

        // Maiden of Sugadinti / Essyllt — blood magic + spawns mechanic
        // Anim: MAIDEN_ATTACK_BLOOD=8091, MAIDEN_ATTACK_SPECIAL=8092
        // Spot: ESSYLLT_SHOVE=1697
        for (int id : eNPC.ESSYLLT)      { registerMaiden(id); }
        for (int id : eNPC.ESSYLLT_HARD) { registerMaiden(id); }

        // Pestilent Bloat — stomp melee + falling flesh mechanic
        // Anim: TOB_BLOAT_SWINGING_CHAIN=8087
        // Spot: TOB_BLOAT_FALLING_FLESH1-4=1570-1573
        for (int id : eNPC.PESTILENT_BLOAT)
        {
            registerScheduledDirectAttack(id, AnimationID.TOB_BLOAT_SWINGING_CHAIN, "melee", 1);
            registerMechanic    (id, SpotanimID.TOB_BLOAT_FALLING_FLESH1, "flesh");
            registerMechanic    (id, SpotanimID.TOB_BLOAT_FALLING_FLESH2, "flesh");
            registerMechanic    (id, SpotanimID.TOB_BLOAT_FALLING_FLESH3, "flesh");
            registerMechanic    (id, SpotanimID.TOB_BLOAT_FALLING_FLESH4, "flesh");
        }

        // Nylocas Vasilias — ranged projectile (boss swaps melee/range/magic forms)
        // Spot: TOB_NYLOCAS_RANGEDPROJECTILE_SIZE1=1559, _SIZEMID=1560, _SIZE2=1561
        for (int id : eNPC.NYLOCAS_VASILIAS)
        {
            registerProjectile(id, SpotanimID.TOB_NYLOCAS_RANGEDPROJECTILE_SIZE1, "ranged");
            registerProjectile(id, SpotanimID.TOB_NYLOCAS_RANGEDPROJECTILE_SIZEMID, "ranged");
            registerProjectile(id, SpotanimID.TOB_NYLOCAS_RANGEDPROJECTILE_SIZE2, "ranged");
        }

        // Sotetseg — melee + magic/ranged projectile + maze ball mechanic
        // Anim: TOB_SOTETSEG_ATTACK_MELEE=8138, TOB_SOTETSEG_ATTACK_RANGED=8139
        // Spot: TOB_SOTETSEG_MAGING=1606, TOB_SOTETSEG_RANGING=1607, TOB_SOTETSEG_SHAREDATTACK=1604
        for (int id : eNPC.SOTETSEG)
        {
            registerScheduledDirectAttack(id, AnimationID.TOB_SOTETSEG_ATTACK_MELEE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.TOB_SOTETSEG_ATTACK_RANGED, "ranged", 1);
            registerProjectile  (id, SpotanimID.TOB_SOTETSEG_MAGING, "magic");
            registerProjectile  (id, SpotanimID.TOB_SOTETSEG_RANGING, "ranged");
            registerMechanic    (id, SpotanimID.TOB_SOTETSEG_SHAREDATTACK, "maze");
        }

        // Xarpus — ranged spit (phase 2) + guano exhumed mechanic
        // Anim: TOB_XARPUS_ATTACK_RANGED=8059
        // Spot: TOB_XARPUS_ACIDSPIT=1555, TOB_XARPUS_GUANO=1557
        for (int id : eNPC.XARPUS_PHASE_2_3)
        {
            registerScheduledDirectAttack(id, AnimationID.TOB_XARPUS_ATTACK_RANGED, "ranged", 1);
            registerProjectile  (id, SpotanimID.TOB_XARPUS_ACIDSPIT, "ranged");
            registerMechanic    (id, SpotanimID.TOB_XARPUS_GUANO, "guano");
        }

        // Verzik Vitur — all phases: magic bolt (P1), blood/lightning/ranged (P2), magic+ranged (P3)
        // Spot: VERZIK_PHASE1_PROJECTILE=1580, VERZIK_PHASE2_RANGED=1583, VERZIK_PHASE2_BLOODPROJ=1591,
        //       VERZIK_PHASE3_MAGEPROJ=1594, VERZIK_PHASE3_RANGEPROJ=1593, VERZIK_ACIDBOMB_PROJANIM=1598
        for (int id : eNPC.VERZIK_VITUR_NORMAL) { registerVerzik(id); }
        for (int id : eNPC.VERZIK_VITUR_ENTRY)  { registerVerzik(id); }
        for (int id : eNPC.VERZIK_VITUR_HARD)   { registerVerzik(id); }

        // ===================================================================
        // Chambers of Xeric
        // ===================================================================

        // Great Olm — all projectile-based; acid and crystal bomb = mechanic
        // Spot: OLM_WEAK_MAGE_PROJ=1341, OLM_WEAK_RANGE_PROJ=1343, OLM_WEAK_MELEE_PROJ=1345,
        //       OLM_FIREBREATH_TRAVEL=1339, OLM_ACID_SPIT=1354, OLM_CRYSTAL_BOMB_TRAVEL=1352
        for (int id : eNPC.GREAT_OLM_HEAD)
        {
            registerProjectile(id, SpotanimID.OLM_FIREBREATH_TRAVEL, "magic");      // fire
            registerProjectile(id, SpotanimID.OLM_WEAK_MAGE_PROJ, "magic");
            registerProjectile(id, SpotanimID.OLM_WEAK_MAGE_IMPACT, "magic");
            registerProjectile(id, SpotanimID.OLM_WEAK_RANGE_PROJ, "ranged");
            registerProjectile(id, SpotanimID.OLM_WEAK_RANGE_IMPACT, "ranged");
            registerProjectile(id, SpotanimID.OLM_WEAK_MELEE_PROJ, "melee");
            registerProjectile(id, SpotanimID.OLM_WEAK_MELEE_IMPACT, "melee");
            registerMechanic  (id, AnimationID.OLM_HEAD_ATTACK_ACID_FRONT, "acid");
            registerMechanic  (id, AnimationID.OLM_HEAD_ATTACK_ACID_RIGHT, "acid");
            registerMechanic  (id, AnimationID.OLM_HEAD_ATTACK_ACID_LEFT, "acid");
            registerMechanic  (id, AnimationID.OLM_SHOCKWAVE, "shockwave");
            registerMechanic  (id, SpotanimID.OLM_CRYSTAL_BOMB_TRAVEL, "crystalbomb");
        }

        // Tekton — pure melee (all 6 animation IDs)
        // Anim: TEKTON_ATTACK_STAB=7482, TEKTON_SLASH=7483, TEKTON_HAMMER_CRUSH=7484,
        //       enraged: 7493, 7494, 7492
        for (int id : eNPC.TEKTON)
        {
            registerScheduledDirectAttack(id, AnimationID.TEKTON_ATTACK_STAB, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.TEKTON_SLASH, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.TEKTON_HAMMER_CRUSH, "melee", 1);
        }
        for (int id : eNPC.TEKTON_ENRAGED)
        {
            registerScheduledDirectAttack(id, AnimationID.TEKTON_HAMMER_CRUSH_ENRAGED, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.TEKTON_ATTACK_STAB_ENRAGED, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.TEKTON_SLASH_ENRAGED, "melee", 1);
        }

        // Vespula — melee + poison portal projectile
        // Anim: VESPULA_ATTACK_MELEE_FLYING=7454
        // Spot: RAIDS_VESPULA_POISON=1364, RAIDS_VESPULA_PORTAL_ATTACK=1366
        for (int id : eNPC.VESPULA)
        {
            registerScheduledDirectAttack(id, AnimationID.VESPULA_ATTACK_MELEE_FLYING, "melee", 1);
            registerProjectile  (id, SpotanimID.RAIDS_VESPULA_POISON, "magic");
            registerProjectile  (id, SpotanimID.RAIDS_VESPULA_PORTAL_ATTACK, "magic");
        }

        // ===================================================================
        // Tombs of Amascut
        // ===================================================================

        // Ba-Ba — melee + boulder ranged + rock fall mechanic
        // Anim: NPC_BABA_ATTACK_MELEE=9743
        // Spot: TOA_BABA_RANGED_TRAVEL=2244, TOA_BABA_BALLS_TRAVEL=2245,
        //       TOA_BABA_ROCK_FALL=2250-2252 (mechanic)
        for (int id : eNPC.BA_BA)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_BABA_ATTACK_MELEE, "melee",    1);
            registerProjectile  (id, SpotanimID.TOA_BABA_RANGED_TRAVEL, "ranged");
            registerProjectile  (id, SpotanimID.TOA_BABA_BALLS_TRAVEL, "ranged");
            registerMechanic    (id, SpotanimID.TOA_BABA_ROCK_FALL, "rockfall");
            registerMechanic    (id, SpotanimID.TOA_BABA_ROCK_FALL_FASTER, "rockfall");
            registerMechanic    (id, SpotanimID.TOA_BABA_ROCK_FALL_FASTEST, "rockfall");
        }

        // Akkha — melee + magic/ranged projectiles + stomp mechanic
        // Anim: NPC_AKKHA_ATTACK_MELEE_SPEAR=9770, _SWORD=9771, STOMP_SPEAR=9788, _SWORD=9789
        // Spot: AKKHA_MAGIC_TRAVEL=2253, AKKHA_RANGED_TRAVEL=2255
        for (int id : eNPC.AKKHA)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_AKKHA_ATTACK_MELEE_SPEAR, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_AKKHA_ATTACK_MELEE_SWORD, "melee", 1);
            registerProjectile  (id, SpotanimID.AKKHA_MAGIC_TRAVEL, "magic");
            registerProjectile  (id, SpotanimID.AKKHA_MAGIC_IMPACT, "magic");
            registerProjectile  (id, SpotanimID.AKKHA_RANGED_TRAVEL, "ranged");
            registerMechanic    (id, AnimationID.NPC_AKKHA_ATTACK_STOMP_SPEAR, "stomp");
            registerMechanic    (id, AnimationID.NPC_AKKHA_ATTACK_STOMP_SWORD, "stomp");
        }

        // Zebak — melee + magic/ranged projectiles + flood mechanic
        // Anim: NPC_ZEBAK01_ATTACK_MELEE=9620, _02=9621
        // Spot: ZEBAK_MAGE_PROJANIM_INITIAL=2176, ZEBAK_RANGE_PROJANIM_INITIAL=2178
        for (int id : eNPC.ZEBAK_MONSTER)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_ZEBAK01_ATTACK_MELEE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_ZEBAK02_ATTACK_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.ZEBAK_MAGE_PROJANIM_INITIAL, "magic");
            registerProjectile  (id, SpotanimID.ZEBAK_MAGE_PROJANIM_INITIAL_ENRAGED, "magic");
            registerProjectile  (id, SpotanimID.ZEBAK_RANGE_PROJANIM_INITIAL, "ranged");
            registerProjectile  (id, SpotanimID.ZEBAK_RANGE_PROJANIM_INITIAL_ENRAGED, "ranged");
            registerMechanic    (id, SpotanimID.ZEBAK_SAFESPOT_TRAVEL, "flood");
        }

        // Tumeken's Warden — melee + prayer attack projectiles + pyramid + entomb
        for (int id : eNPC.TUMEKEN_S_WARDEN_PHASE_2) { registerWarden(id); }
        for (int id : eNPC.TUMEKEN_S_WARDEN_PHASE_3) { registerWarden(id); }

        // Elidinis' Warden — same as Tumeken (same prayer attack system)
        for (int id : eNPC.ELIDINIS_WARDEN_PHASE_2) { registerWarden(id); }
        for (int id : eNPC.ELIDINIS_WARDEN_PHASE_3) { registerWarden(id); }

        // ===================================================================
        // God Wars Dungeon 2 (Nex)
        // ===================================================================

        // Nex — melee (3 anims) + 5 phase projectiles + siphon mechanic
        // Anim: NEX_ATTACK=9180, NEX_DASH_ATTACK=9178, NEX_SMASH_ATTACK=9186
        // Spot: NEX_SMOKE_ATTACK_PROJ=1997, NEX_SHADOW=1999, NEX_BLOOD=2000,
        //       NEX_ICE_ATTACK_PROJ=2004, NEX_FINALE=2007
        for (int id : eNPC.NEX)
        {
            registerScheduledDirectAttack(id, AnimationID.NEX_DASH_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NEX_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NEX_SMASH_ATTACK, "melee", 1);
            registerProjectile  (id, SpotanimID.NEX_SMOKE_ATTACK_PROJ, "magic"); // smoke phase
            registerProjectile  (id, SpotanimID.NEX_SHADOW_ATTACK_PROJ, "magic"); // shadow phase
            registerProjectile  (id, SpotanimID.NEX_BLOOD_ATTACK_PROJ, "magic"); // blood phase
            registerProjectile  (id, SpotanimID.NEX_ICE_ATTACK_PROJ, "magic"); // ice phase
            registerProjectile  (id, SpotanimID.NEX_FINALE_ATTACK_PROJ, "magic"); // zaros phase
            registerMechanic    (id, AnimationID.NEX_BLOOD_SIPHON, "siphon");
            registerMechanic    (id, AnimationID.NEX_SUMMON, "summon");
        }

        // ===================================================================
        // Wilderness Bosses
        // ===================================================================

        // Callisto — melee + ranged projectile + trap mechanic
        // Anim: NPC_CALLISTO_ATTACK_MELEE01=10012, CALLISTO_ATTACK_MELEE=10020
        // Spot: FX_CALLISTO_RANGED_PROJECTILE=2350
        for (int id : eNPC.CALLISTO)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_CALLISTO_ATTACK_MELEE01, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.CALLISTO_ATTACK_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.FX_CALLISTO_RANGED_PROJECTILE,  "ranged");
            registerProjectile  (id, SpotanimID.FX_CALLISTO_RANGED_IMPACT,  "ranged");
            registerMechanic    (id, SpotanimID.FX_CALLISTO_TRAP,  "trap");
        }
        // Artio (Calvarion-tier Callisto)
        for (int id : eNPC.ARTIO)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_CALLISTO_ATTACK_MELEE01, "melee", 1);
            registerProjectile  (id, SpotanimID.FX_CALLISTO_RANGED_PROJECTILE,  "ranged");
            registerMechanic    (id, SpotanimID.FX_CALLISTO_TRAP,  "trap");
        }

        // Vet'ion — melee + magic projectile
        // Anim: NPC_VETION_ATTACK_MELEE_01=9971, _02=9972
        // Spot: FX_VETION_ATTACK_MAGIC_01=2344, SPELLS_VETION01_TRAVEL=2337
        for (int id : eNPC.VET_ION)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_VETION_ATTACK_MELEE_01, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_VETION_ATTACK_MELEE_02, "melee", 1);
            registerProjectile  (id, SpotanimID.SPELLS_VETION01_TRAVEL, "magic");
            registerProjectile  (id, SpotanimID.SPELLS_VETION01_TRAVEL02, "magic"); // SPELLS_VETION01_TRAVEL02
            registerProjectile  (id, SpotanimID.FX_VETION_ATTACK_MAGIC_01, "magic");
            registerProjectile  (id, SpotanimID.FX_VETION_ATTACK_MAGIC_02, "magic");
        }
        // Calvar'ion (Vet'ion reskin)
        for (int id : eNPC.CALVAR_ION)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_VETION_ATTACK_MELEE_01, "melee", 1); // TODO - verify same IDs
            registerProjectile  (id, SpotanimID.SPELLS_VETION01_TRAVEL, "magic");
            registerProjectile  (id, SpotanimID.FX_VETION_ATTACK_MAGIC_01, "magic");
        }

        // Venenatis — melee + ranged/magic/web projectiles
        // Anim: NPC_VENENATIS_MELEE_01=9991
        // Spot: FX_VENENATIS_RANGED_PROJECTILE=2356, _MAGIC_PROJECTILE=2358, _WEB_PROJECTILE=2360
        for (int id : eNPC.VENENATIS)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_VENENATIS_MELEE_01, "melee", 1);
            registerProjectile  (id, SpotanimID.FX_VENENATIS_RANGED_PROJECTILE, "ranged");
            registerProjectile  (id, SpotanimID.FX_VENENATIS_MAGIC_PROJECTILE, "magic");
            registerMechanic    (id, SpotanimID.FX_VENENATIS_WEB_PROJECTILE, "web");
        }
        for (int id : eNPC.SPINDEL) // Spindel = Venenatis reskin; shares spider boss attack style
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_VENENATIS_MELEE_01, "melee", 1); // TODO - verify
            registerProjectile  (id, SpotanimID.FX_VENENATIS_RANGED_PROJECTILE, "ranged");    // TODO - verify
            registerProjectile  (id, SpotanimID.FX_VENENATIS_MAGIC_PROJECTILE, "magic");     // TODO - verify
        }

        // ===================================================================
        // Slayer / Skilling Bosses
        // ===================================================================

        // Alchemical Hydra — all projectile-based
        // Spot: HYDRABOSS_MAGIC_PROJ=1662, HYDRABOSS_RANGED_PROJ=1663, HYDRABOSS_FIRE_TRAVEL=1667,
        //       HYDRABOSS_LIGHTNING_TRAVEL=1665, HYDRABOSS_SHOCKWAVE=1666
        for (int id : eNPC.ALCHEMICAL_HYDRA)
        {
            registerProjectile(id, SpotanimID.HYDRABOSS_MAGIC_PROJ, "magic");
            registerProjectile(id, SpotanimID.HYDRABOSS_RANGED_PROJ, "ranged");
            registerProjectile(id, SpotanimID.HYDRABOSS_LIGHTNING_TRAVEL, "magic");  // lightning
            registerProjectile(id, SpotanimID.HYDRABOSS_SHOCKWAVE, "magic");  // shockwave
            registerProjectile(id, SpotanimID.HYDRABOSS_FIRE_TRAVEL, "dragonfire");
            registerProjectile(id, SpotanimID.HYDRABOSS_FIRE, "dragonfire");
            registerMechanic  (id, AnimationID.HYDRABOSS_POOL_LANDED, "pools");  // HYDRABOSS_POOL_LANDED
        }

        // Cerberus — melee + ranged + fire breath + lava pools mechanic
        // Anim: CERBERUS_BITE=4491, CERBERUS_ATTACK_RANGE=4490, CERBERUS_FIRE_BREATH=4492,
        //       CERBERUS_SPECIAL_ATTACK_SPRAY=4493, CERBERUS_SPECIAL_ATTACK_FLAME=4501
        for (int id : eNPC.CERBERUS)
        {
            registerScheduledDirectAttack(id, AnimationID.CERBERUS_ATTACK_RANGE, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.CERBERUS_BITE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.CERBERUS_FIRE_BREATH, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.CERBERUS_SPECIAL_ATTACK_SPRAY, "magic", 1);
            registerMechanic    (id, AnimationID.CERBERUS_SPECIAL_ATTACK_FLAME, "flames");
        }

        // Grotesque Guardians — Dawn (ranged) + Dusk (melee/ranged) + lightning mechanic
        // Anim: GG_DAWN_ATTACK_SLASH=7769, GG_DAWN_ATTACK_RANGED=7770
        //       GG_DUSK_ATTACK_SLASH=7786, GG_DUSK_ATTACK_SWEEP=7788, GG_DUSK_ENRAGE_ATTACK_RANGED=7801
        for (int id : eNPC.DAWN)
        {
            registerScheduledDirectAttack(id, AnimationID.GG_DAWN_ATTACK_SLASH, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.GG_DAWN_ATTACK_RANGED, "ranged", 1);
            registerMechanic    (id, AnimationID.GG_DAWN_ATTACK_SPECIAL, "special");
        }
        for (int id : eNPC.DUSK)
        {
            registerScheduledDirectAttack(id, AnimationID.GG_DUSK_ATTACK_SLASH_DEFENSIVE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.GG_DUSK_ATTACK_SLASH, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.GG_DUSK_ATTACK_SWEEP_DEFENSIVE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.GG_DUSK_ATTACK_SWEEP, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.GG_DUSK_ENRAGE_ATTACK_SLASH, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.GG_DUSK_ENRAGE_ATTACK_RANGED, "ranged",1);
            registerMechanic    (id, AnimationID.GG_DUSK_ATTACK_SPECIAL, "special");
            registerMechanic    (id, AnimationID.GG_DUSK_ENRAGE_ATTACK_SPECIAL, "special");
            registerMechanic    (id, AnimationID.GG_DUSK_ATTACK_FORCEFIELD, "forcefield");
        }

        // Gargoyles
        // Anim: GARGOYLE_ATTACK=1517, GARGOYLE_MARBLE_ATTACK=7811, GARGOYLE_MARBLE_ATTACK_RANGED=7815
        // Spot: MARBLE_GARGOYLE_RANGEDSTUN_PROJECTILE=1453
        for (int id : eNPC.MARBLE_GARGOYLE)
        {
            registerScheduledDirectAttack(id, AnimationID.GARGOYLE_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.GARGOYLE_MARBLE_ATTACK, "melee", 1);
            registerProjectile  (id, SpotanimID.MARBLE_GARGOYLE_RANGEDSTUN_PROJECTILE, "ranged");
        }

        // Drake — melee + ranged + fire breath
        // Anim: DRAKE_MELEE=8275, DRAKE_RANGED=8276, SUPERIOR_DRAKE_MELEE_SPECIAL=8762
        // Spot: DRAKE_RANGE_PROJ=1636, DRAKE_BURN_PROJ=1637
        for (int id : eNPC.DRAKE)
        {
            registerScheduledDirectAttack(id, AnimationID.DRAKE_MELEE, "melee",      1);
            registerScheduledDirectAttack(id, AnimationID.DRAKE_RANGED, "ranged",     1);
            registerProjectile  (id, SpotanimID.DRAKE_RANGE_PROJ, "ranged");
            registerProjectile  (id, SpotanimID.DRAKE_BURN_PROJ, "dragonfire");
        }
        for (int id : eNPC.GUARDIAN_DRAKE)
        {
            registerScheduledDirectAttack(id, AnimationID.DRAKE_MELEE, "melee",      1);
            registerScheduledDirectAttack(id, AnimationID.SUPERIOR_DRAKE_MELEE_SPECIAL, "melee",      1);
            registerProjectile  (id, SpotanimID.DRAKE_BURN_PROJ, "dragonfire");
        }

        // Wyrm — melee + magic + ranged projectile
        // Anim: WYRM_ATTACK_MELEE=8270, WYRM_ATTACK_MAGIC=8271
        // Spot: WYRM_RANGE_PROJ=1634
        for (int id : eNPC.WYRM)
        {
            registerScheduledDirectAttack(id, AnimationID.WYRM_ATTACK_MELEE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.WYRM_ATTACK_MAGIC, "magic", 1);
            registerProjectile  (id, SpotanimID.WYRM_RANGE_PROJ, "ranged");
        }

        // Skeletal Wyvern + fossil island wyverns (all share same attack animations)
        // Anim: WYVERN_SKELETON_BITE=2985, WYVERN_SKELETON_TAIL_SWIPE=2986
        // Spot: WYVERN_SKELETON_TRAVEL_ICEBALL=500, WYVERN_SKELETON_TRAVEL_BREATH=501
        for (int[] group : new int[][]{eNPC.SKELETAL_WYVERN, eNPC.ANCIENT_WYVERN,
                eNPC.LONG_TAILED_WYVERN, eNPC.SPITTING_WYVERN, eNPC.TALONED_WYVERN})
        {
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.WYVERN_SKELETON_BITE, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.WYVERN_SKELETON_TAIL_SWIPE, "melee", 1);
                registerProjectile  (id, SpotanimID.WYVERN_SKELETON_LAUNCH_ICEBALL,  "magic");
                registerProjectile  (id, SpotanimID.WYVERN_SKELETON_TRAVEL_ICEBALL,  "magic");
                registerProjectile  (id, SpotanimID.WYVERN_SKELETON_TRAVEL_BREATH,  "magic");
                registerProjectile  (id, SpotanimID.WYVERN_SKELETON_IMPACT_BREATH,  "magic");
                registerProjectile  (id, SpotanimID.WYVERN_SKELETON_TRAVEL_BREATH_ANCIENT, "magic");
            }
        }

        // Basilisk Knight — melee + magic + entomb mechanic
        // Anim: BASILISK_KNIGHT_MELEE=8499, BASILISK_KNIGHT_MAGIC=8500
        // Spot: BASILISK_KNIGHT_MAGIC_TRAVEL=1735, BASILISK_KNIGHT_ENTOMB_TRAVEL=1744
        for (int id : eNPC.BASILISK_KNIGHT)
        {
            registerScheduledDirectAttack(id, AnimationID.BASILISK_KNIGHT_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.BASILISK_KNIGHT_MAGIC_TRAVEL, "magic");
            registerMechanic    (id, SpotanimID.BASILISK_KNIGHT_ENTOMB_TRAVEL, "entomb");
        }

        // The Jormungand — melee + magic + entomb
        // Anim: JORMUNGAND_MELEE=8510, JORMUNGAND_MAGIC=8511
        // Spot: JORMUNGAND_MAGIC_TRAVEL=1737, JORMUNGAND_ENTOMB_PROJ=1742
        for (int id : eNPC.THE_JORMUNGAND)
        {
            registerScheduledDirectAttack(id, AnimationID.JORMUNGAND_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.JORMUNGAND_MAGIC_TRAVEL, "magic");
            registerMechanic    (id, SpotanimID.JORMUNGAND_ENTOMB_PROJ, "entomb");
        }

        // Sarachnis — ranged spit + web projectile
        // Spot: SARACHNIS_RANGEPROJ=1686, SARACHNIS_WEB_PROJ=1687
        for (int id : eNPC.SARACHNIS)
        {
            registerProjectile(id, SpotanimID.SARACHNIS_RANGEPROJ, "ranged");
            registerProjectile(id, SpotanimID.SARACHNIS_WEB_PROJ, "magic");  // typeless web
        }

        // Hespori — ranged + magic + vine mechanic
        // Anim: HESPORI_ATTACK_RANGED=8224, HESPORI_ATTACK_SPECIAL=8223
        // Spot: HESPORI_MAGIC_PROJ=1640, HESPORI_RANGE_PROJ=1639, HESPORI_VINE_PROJ=1642
        for (int id : eNPC.HESPORI)
        {
            registerScheduledDirectAttack(id, AnimationID.HESPORI_ATTACK_RANGED, "ranged", 1);
            registerProjectile  (id, SpotanimID.HESPORI_RANGE_PROJ, "ranged");
            registerProjectile  (id, SpotanimID.HESPORI_MAGIC_PROJ, "magic");
            registerMechanic    (id, AnimationID.HESPORI_ATTACK_SPECIAL, "vines");
            registerMechanic    (id, SpotanimID.HESPORI_VINE_PROJ, "vines");
        }

        // Dark Beast — melee + ranged projectile
        // Anim: DARK_BEAST_UPDATE_ATTACK=2731
        // Spot: CRYSTAL_DARK_BEAST_RANGE_TRAVEL=1705
        for (int id : eNPC.DARK_BEAST)
        {
            registerScheduledDirectAttack(id, AnimationID.DARK_BEAST_UPDATE_ATTACK, "melee", 1);
            registerProjectile  (id, SpotanimID.CRYSTAL_DARK_BEAST_RANGE_TRAVEL, "ranged");
            registerProjectile  (id, SpotanimID.CRYSTAL_DARK_BEAST_RANGE_TRAVEL_HM, "ranged"); // HM version
        }

        // Smoke Devil / Nuclear — magic cloud projectile
        // Spot: SMOKE_DEVIL_SMOKE_PROJ=644
        for (int id : eNPC.SMOKE_DEVIL)         { registerProjectile(id, SpotanimID.SMOKE_DEVIL_SMOKE_PROJ, "magic"); }
        for (int id : eNPC.NUCLEAR_SMOKE_DEVIL) { registerProjectile(id, SpotanimID.SMOKE_DEVIL_SMOKE_PROJ, "magic"); }

        // Bloodveld variants — magic-tagged melee
        // Anim: BLOODVELD_ATTACK=1552, BLOODVELD_ATTACK_MOVE=8651
        for (int[] group : new int[][]{eNPC.BLOODVELD, eNPC.MUTATED_BLOODVELD,
                eNPC.INSATIABLE_BLOODVELD, eNPC.INSATIABLE_MUTATED_BLOODVELD})
        {
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.BLOODVELD_ATTACK, "magic", 1);
                registerScheduledDirectAttack(id, AnimationID.BLOODVELD_ATTACK_MOVE, "magic", 1);
            }
        }

        // Jelly variants — melee
        // Anim: JELLY_ATTACK=1586
        for (int[] group : new int[][]{eNPC.JELLY_REGULAR, eNPC.JELLY_WILDERNESS_SLAYER_CAVE,
                eNPC.CHILLED_JELLY, eNPC.VITREOUS_JELLY, eNPC.VITREOUS_WARPED_JELLY,
                eNPC.VITREOUS_CHILLED_JELLY, eNPC.WARPED_JELLY})
        {
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.JELLY_ATTACK, "melee", 1);
            }
        }

        // Pyrefiend — melee + magic cast
        // Anim: PYREFIEND_ATTACK=1582, PYREFIEND_CASTING=7820
        for (int id : eNPC.PYREFIEND)
        {
            registerScheduledDirectAttack(id, AnimationID.PYREFIEND_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.PYREFIEND_CASTING, "magic", 1);
        }

        // Banshees — melee scream
        for (int id : eNPC.BANSHEE)           { registerScheduledDirectAttack(id, AnimationID.BANSHEE_ATTACK, "melee", 1); }
        for (int id : eNPC.SCREAMING_BANSHEE) { registerScheduledDirectAttack(id, AnimationID.SCREAMING_BANSHEE_ATTACK, "melee", 1); }

        // Torcher — melee + fireball projectile
        // Anim: TORCHER_ATTACK=3882
        // Spot: TORCHER_FIREBALL_TRAVEL_SPOTANIM=647
        for (int id : eNPC.TORCHER)
        {
            registerScheduledDirectAttack(id, AnimationID.TORCHER_ATTACK, "melee", 1);
            registerProjectile  (id, SpotanimID.TORCHER_FIREBALL_LAUNCH_SPOTANIM,  "magic");
            registerProjectile  (id, SpotanimID.TORCHER_FIREBALL_TRAVEL_SPOTANIM,  "magic");
        }

        // Marble Gargoyle — melee + ranged (already registered above)

        // Lava / Magma Strykewyrm — melee + ranged + burrow mechanic
        // Anim: NPC_STRYKEWYRM_LAVA01_MELEE01=13654, _RANGE01=13655,
        //       _BURROW_ATTACK01-03=13656-13658
        for (int[] group : new int[][]{eNPC.LAVA_STRYKEWYRM, eNPC.MAGMA_STRYKEWYRM})
        {
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.NPC_STRYKEWYRM_LAVA01_MELEE01, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.NPC_STRYKEWYRM_LAVA01_RANGE01, "ranged", 1);
                registerMechanic    (id, AnimationID.NPC_STRYKEWYRM_LAVA01_BURROW_ATTACK01, "burrow");
                registerMechanic    (id, AnimationID.NPC_STRYKEWYRM_LAVA01_BURROW_ATTACK02, "burrow");
                registerMechanic    (id, AnimationID.NPC_STRYKEWYRM_LAVA01_BURROW_ATTACK03, "burrow");
            }
        }

        // Elder Aquanite — melee
        // Anim: SLAYER_AQUANITE_ATTACK=13637, SLAYER_SUP_AQUANITE_ATTACK=13644
        for (int id : eNPC.ELDER_AQUANITE)
        {
            registerScheduledDirectAttack(id, AnimationID.SLAYER_AQUANITE_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.SLAYER_SUP_AQUANITE_ATTACK, "melee", 1);
        }

        // ===================================================================
        // Dragon-Type NPCs
        // ===================================================================

        // All chromatic/metal dragons share BDRAG_ATTACK=25 (melee)
        for (int[] group : new int[][]{eNPC.BLACK_DRAGON, eNPC.BLUE_DRAGON_NORMAL,
                eNPC.BLUE_DRAGON_TASK_ONLY, eNPC.BLUE_DRAGON_RUINS_OF_TAPOYAUIK,
                eNPC.BRONZE_DRAGON, eNPC.GREEN_DRAGON, eNPC.RED_DRAGON,
                eNPC.BRUTAL_BLACK_DRAGON, eNPC.BRUTAL_BLUE_DRAGON,
                eNPC.BRUTAL_GREEN_DRAGON, eNPC.BRUTAL_RED_DRAGON})
        {
            for (int id : group) { registerScheduledDirectAttack(id, AnimationID.BDRAG_ATTACK, "melee", 1); }
        }

        // Adamant Dragon — melee + poison ball
        // Spot: ADAMANT_DRAGON_POISONBALL=1486
        for (int id : eNPC.ADAMANT_DRAGON)
        {
            registerScheduledDirectAttack(id, AnimationID.BDRAG_ATTACK,   "melee",      1);
            registerProjectile  (id, SpotanimID.ADAMANT_DRAGON_POISONBALL, "dragonfire");
        }

        // Rune Dragon — melee + electrovortex
        // Spot: RUNE_DRAGON_ELECTOVORTEX=1488
        for (int id : eNPC.RUNE_DRAGON)
        {
            registerScheduledDirectAttack(id, AnimationID.BDRAG_ATTACK,   "melee", 1);
            registerProjectile  (id, SpotanimID.RUNE_DRAGON_ELECTOVORTEX, "magic");
        }

        // ===================================================================
        // Quest Bosses
        // ===================================================================

        // Abyssal Sire
        // Anim: SIRE_ATTACK_DOUBLE_HOOK=5751, SIRE_ATTACK_DOUBLE_WHIPS=5755,
        //       SIRE_ATTACK_RIGHT_WHIP=5369, SIRE_RIGHT_HOOK=5366
        //       SIRE_ATTACK_MIASMA=4531 (mechanic)
        for (int id : eNPC.ABYSSAL_SIRE)
        {
            registerScheduledDirectAttack(id, AnimationID.SIRE_RIGHT_HOOK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.SIRE_ATTACK_RIGHT_WHIP, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.SIRE_ATTACK_DOUBLE_HOOK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.SIRE_ATTACK_DOUBLE_WHIPS, "melee", 1);
            registerMechanic    (id, AnimationID.SIRE_ATTACK_MIASMA, "miasma");
            registerMechanic    (id, AnimationID.SIRE_ATTACK_MIASMA_TWO, "miasma");
        }

        // Corporeal Beast
        // Anim: CORP_DOUBLE_STOMP=1686
        // Spot: CORP_SPIRIT_BEAST_STRONG_PROJ=316, _MID=315, _WEAK=314, CORP_GROUND_STOMP=318
        for (int id : eNPC.CORPOREAL_BEAST)
        {
            registerScheduledDirectAttack(id, AnimationID.CORP_DOUBLE_STOMP, "melee", 1);
            registerProjectile  (id, SpotanimID.CORP_SPIRIT_BEAST_WEAK_PROJ,  "magic");
            registerProjectile  (id, SpotanimID.CORP_SPIRIT_BEAST_MID_PROJ,  "magic");
            registerProjectile  (id, SpotanimID.CORP_SPIRIT_BEAST_STRONG_PROJ,  "magic");
            registerMechanic    (id, SpotanimID.CORP_GROUND_STOMP,  "stomp");
        }

        // Kalphite Queen
        // Anim: KALPHITE_QUEEN_ATTACK_MANDIBLES=1177, KALPHITE_QUEEN_ATTACK_CLAWS=1178,
        //       KALPHITE_QUEEN_RANGED_ATTACK=1250, KALPHITE_QUEEN_LIGHTNING=1170
        for (int id : eNPC.KALPHITE_QUEEN)
        {
            registerScheduledDirectAttack(id, AnimationID.KALPHITE_QUEEN_ATTACK_MANDIBLES, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.KALPHITE_QUEEN_ATTACK_CLAWS, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.KALPHITE_QUEEN_RANGED_ATTACK, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.KALPHITE_QUEEN_LIGHTNING, "magic", 1);
        }

        // Demonic Gorilla — all 3 styles (prayer switch boss)
        // Anim: DEMONIC_GORILLA_MAGIC=7225, DEMONIC_GORILLA_PUNCH=7226,
        //       DEMONIC_GORILLA_RANGE=7227, DEMONIC_GORILLA_SMASH_CHEST=7228
        for (int id : eNPC.DEMONIC_GORILLA)
        {
            registerScheduledDirectAttack(id, AnimationID.DEMONIC_GORILLA_MAGIC, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.DEMONIC_GORILLA_PUNCH, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.DEMONIC_GORILLA_RANGE, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.DEMONIC_GORILLA_SMASH_CHEST, "melee", 1);
        }
        for (int id : eNPC.TORTURED_GORILLA) // shares animations
        {
            registerScheduledDirectAttack(id, AnimationID.DEMONIC_GORILLA_MAGIC, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.DEMONIC_GORILLA_PUNCH, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.DEMONIC_GORILLA_RANGE, "ranged", 1);
        }

        // Araxxor — melee (normal + enraged + slow) + ranged/magic projectiles + acid mechanic
        // Anim: NPC_ARAXXOR_01_ATTACK_MELEE_01=11480, _ENRAGED_01=11487, _SLOW_MELEE_01=11483
        // Spot: ARAXXOR_RANGED_PROJECTILE=1621, ARAXXOR_MAGIC_PROJECTILE=1622
        for (int id : eNPC.ARAXXOR_IN_COMBAT)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_ARAXXOR_01_ATTACK_MELEE_01, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_ARAXXOR_01_ATTACK_SLOW_MELEE_01, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_ARAXXOR_01_ATTACK_MELEE_ENRAGED_01, "melee", 1);
            registerProjectile  (id, SpotanimID.ARAXXOR_RANGED_PROJECTILE,  "ranged");
            registerProjectile  (id, SpotanimID.ARAXXOR_MAGIC_PROJECTILE,  "magic");
            registerMechanic    (id, AnimationID.NPC_ARAXXOR_01_ATTACK_ACID_LEAK_01, "acid");
            registerMechanic    (id, AnimationID.NPC_ARAXXOR_01_ATTACK_ACID_SPRAY_01, "acid");
        }

        // Phantom Muspah
        // Anim: NPC_MUSPAH_ATTACK_MELEE_01=9920
        // Spot: PROJECTILE_MUSPAH_ATTACK_MAGIC_01=2327, _RANGED_01=2329
        for (int id : eNPC.PHANTOM_MUSPAH)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_MUSPAH_ATTACK_MELEE_01, "melee", 1);
            registerProjectile  (id, SpotanimID.PROJECTILE_MUSPAH_ATTACK_MAGIC_01, "magic");
            registerProjectile  (id, SpotanimID.PROJECTILE_MUSPAH_ATTACK_RANGED_01, "ranged");
            registerMechanic    (id, AnimationID.NPC_MUSPAH_ATTACK_SUMMON_01, "summon");
            registerMechanic    (id, AnimationID.NPC_MUSPAH_ATTACK_EXPLOSION_01, "explosion");
        }

        // The Nightmare (Sisterhood Sanctuary)
        // Anim: NIGHTMARE_ATTACK_MELEE=8594, _MAGIC=8595, _RANGED=8596
        // Spot: NIGHTMARE_MAGIC_TRAVEL=1764, NIGHTMARE_RANGED_TRAVEL=1766
        for (int id : eNPC.THE_NIGHTMARE)     { registerNightmare(id); }
        for (int id : eNPC.PHOSANI_S_NIGHTMARE) { registerNightmare(id); }

        // Crystalline / Corrupted Hunllef (Gauntlet)
        // Anim: HUNLLEF_ATTACK_MELEE=8420, HUNLLEF_ATTACK_SPECIAL=8418
        // Spot: CRYSTAL_HUNLLEF_MAGIC_TRAVEL=1707/1708, _RANGE_TRAVEL=1711/1712
        for (int id : eNPC.CRYSTALLINE_HUNLLEF)
        {
            registerScheduledDirectAttack(id, AnimationID.HUNLLEF_ATTACK_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.CRYSTAL_HUNLLEF_MAGIC_TRAVEL, "magic");
            registerProjectile  (id, SpotanimID.CRYSTAL_HUNLLEF_RANGE_TRAVEL, "ranged");
            registerProjectile  (id, SpotanimID.CRYSTAL_HUNLLEF_PRAYER_TRAVEL, "ranged"); // prayer travel
            registerMechanic    (id, AnimationID.HUNLLEF_ATTACK_SPECIAL, "tiles");
        }
        for (int id : eNPC.CORRUPTED_HUNLLEF)
        {
            registerScheduledDirectAttack(id, AnimationID.HUNLLEF_ATTACK_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.CRYSTAL_HUNLLEF_MAGIC_TRAVEL_HM, "magic");
            registerProjectile  (id, SpotanimID.CRYSTAL_HUNLLEF_RANGE_TRAVEL_HM, "ranged");
            registerProjectile  (id, SpotanimID.CRYSTAL_HUNLLEF_PRAYER_TRAVEL_HM, "ranged");
            registerMechanic    (id, AnimationID.HUNLLEF_ATTACK_SPECIAL, "tiles");
        }

        // Barrelchest (Great Brain Robbery)
        // Anim: BRAIN_BARRELCHEST_NORMAL_ATTACK=5894, BRAIN_BARRELCHEST_SPECIAL_ATTACK=5895
        for (int id : eNPC.BARRELCHEST)
        {
            registerScheduledDirectAttack(id, AnimationID.BRAIN_BARRELCHEST_NORMAL_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.BRAIN_BARRELCHEST_SPECIAL_ATTACK, "melee", 1);
        }

        // Dharok the Wretched (Barrows)
        // Anim: BARROW_DHAROK_SLASH=2066, BARROW_DHAROK_CRUSH=2067
        for (int id : eNPC.DHAROK_THE_WRETCHED)
        {
            registerScheduledDirectAttack(id, AnimationID.BARROW_DHAROK_SLASH, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.BARROW_DHAROK_CRUSH, "melee", 1);
        }

        // Penance Queen (Barbarian Assault)
        // Anim: BARBASSAULT_PENANCE_QUEEN_ATTACK=5084
        // Spot: BARBASSAULT_PENANCE_QUEEN_RANGE_ATTACK_TRAVEL=871
        for (int id : eNPC.PENANCE_QUEEN)
        {
            registerScheduledDirectAttack(id, AnimationID.BARBASSAULT_PENANCE_QUEEN_ATTACK, "melee", 1);
            registerProjectile  (id, SpotanimID.BARBASSAULT_PENANCE_QUEEN_RANGE_ATTACK_TRAVEL,  "ranged");
        }

        // Experiment No. 2 (Creature of Fenkenstrain)
        // Anim: GRIM_EXPERIMENT_NO2_ATTACK=6513
        // Spot: GRIM_EXPERIMENT_NO2_PROJECTILE_ANIM=1078
        for (int id : eNPC.EXPERIMENT_NO_2)
        {
            registerScheduledDirectAttack(id, AnimationID.GRIM_EXPERIMENT_NO2_ATTACK, "melee", 1);
            registerProjectile  (id, SpotanimID.GRIM_EXPERIMENT_NO2_PROJECTILE_ANIM, "magic");
        }

        // Headless Beast (A Taste of Hope)
        // Anim: BEAR_HEADLESS_BEAST_ATTACK=2267, BEAR_HEADLESS_BEAST_STOMP=8835
        for (int id : eNPC.HEADLESS_BEAST)
        {
            registerScheduledDirectAttack(id, AnimationID.BEAR_HEADLESS_BEAST_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.BEAR_HEADLESS_BEAST_STOMP, "melee", 1); }
        for (int id : eNPC.HEADLESS_BEAST_HARD)
        {
            registerScheduledDirectAttack(id, AnimationID.BEAR_HEADLESS_BEAST_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.BEAR_HEADLESS_BEAST_STOMP, "melee", 1); }

        // Agrith-Naar (Shadow of the Storm)
        // Anim: AGRITH_NANA_ATTACK=3501
        // Spot: AGRITH_NANA_RANGED=600
        for (int id : eNPC.AGRITH_NAAR)
        {
            registerScheduledDirectAttack(id, AnimationID.AGRITH_NANA_ATTACK, "melee", 1);
            registerProjectile(id, SpotanimID.AGRITH_NANA_RANGED, "magic"); }
        for (int id : eNPC.AGRITH_NAAR_HARD)
        {
            registerScheduledDirectAttack(id, AnimationID.AGRITH_NANA_ATTACK, "melee", 1);
            registerProjectile(id, SpotanimID.AGRITH_NANA_RANGED, "magic"); }

        // Dessourt (Desert Treasure I)
        // Anim: DESSOURT_MELEE_ATTACK=3508, DESSOURT_TOT_ATTACK=3507
        for (int id : eNPC.DESSOURT)
        {
            registerScheduledDirectAttack(id, AnimationID.DESSOURT_TOT_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.DESSOURT_MELEE_ATTACK, "melee", 1); }
        for (int id : eNPC.DESSOURT_HARD)
        {
            registerScheduledDirectAttack(id, AnimationID.DESSOURT_TOT_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.DESSOURT_MELEE_ATTACK, "melee", 1);
        }

        // Chaos Elemental (Wilderness)
        // Anim: CHAOSELEMENTAL_ATTACK=3146, CHAOSELEMENTAL_CASTING=3149,
        //       CHAOSELEMENTAL_DISCORD_CASTING=3154, CHAOSELEMENTAL_MADNESS_CASTING=3150
        for (int id : eNPC.CHAOS_ELEMENTAL)
        {
            registerScheduledDirectAttack(id, AnimationID.CHAOSELEMENTAL_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.CHAOSELEMENTAL_CASTING, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.CHAOSELEMENTAL_MADNESS_CASTING, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.CHAOSELEMENTAL_DISCORD_CASTING, "magic", 1);
        }

        // Demonic Gorilla / Kruk (Monkey Madness II)
        // Anim: KRUK_TORTURED_APE_PUNCH=7239, _MAGIC=7238, _RANGE=7240
        for (int id : eNPC.KRUK)
        {
            registerScheduledDirectAttack(id, AnimationID.KRUK_TORTURED_APE_MAGIC, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.KRUK_TORTURED_APE_PUNCH, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.KRUK_TORTURED_APE_RANGE, "ranged", 1);
        }

        // The Inadequacy (Dream Mentor)
        // Anim: DREAM_INADEQUACY_ATTACK_FRONT/BACK/LEFT/RIGHT=6318-6321, _RANGED=6325
        for (int[] group : new int[][]{eNPC.THE_INADEQUACY, eNPC.THE_INADEQUACY_HARD})
        {
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.DREAM_INADEQUACY_ATTACK_FRONT, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.DREAM_INADEQUACY_ATTACK_BACK, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.DREAM_INADEQUACY_ATTACK_LEFT, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.DREAM_INADEQUACY_ATTACK_RIGHT, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.DREAM_INADEQUACY_RANGED_ATTACK, "ranged", 1);
            }
        }

        // Giant Scarab (Contact! / TOA path)
        // Anim: SCARAB_ATTACK=1948, NPC_SCARAB_ATTACK=9587
        // Spot: CONTACT_SCARAB_RANGED=2081, TOA_SCARAB_RANGED_PROJECTILE01=2152
        for (int[] group : new int[][]{eNPC.GIANT_SCARAB, eNPC.GIANT_SCARAB_HARD})
        {
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.SCARAB_ATTACK, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.NPC_SCARAB_ATTACK, "melee", 1);
                registerProjectile  (id, SpotanimID.CONTACT_SCARAB_RANGED, "ranged");
                registerProjectile  (id, SpotanimID.TOA_SCARAB_RANGED_PROJECTILE01, "ranged");
            }
        }

        // Giant Roc (My Arm's Big Adventure)
        // Anim: MYARM_ROC_PECK_ATTACK=5024, MYARM_ROC_FLAP_ATTACK=5023, MYARM_ROC_ROCK_ATTACK=5025
        for (int[] group : new int[][]{eNPC.GIANT_ROC, eNPC.GIANT_ROC_HARD})
        {
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.MYARM_ROC_FLAP_ATTACK, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.MYARM_ROC_PECK_ATTACK, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.MYARM_ROC_ROCK_ATTACK, "ranged", 1);
            }
        }

        // Sea Troll Queen (Slug Menace)
        // Anim: SWAN_SEATROLL_ATTACK=3985
        for (int id : eNPC.SEA_TROLL_QUEEN) { registerScheduledDirectAttack(id, AnimationID.SWAN_SEATROLL_ATTACK, "melee", 1); }

        // Giant Sea Snake (Royal Trouble)
        // Anim: ROYAL_SEA_SNAKE_ATTACK=3538, ROYAL_SEA_SNAKE_MOTHER_RANGED_ATTACK=4041
        for (int id : eNPC.GIANT_SEA_SNAKE)
        {
            registerScheduledDirectAttack(id, AnimationID.ROYAL_SEA_SNAKE_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.ROYAL_SEA_SNAKE_MOTHER_RANGED_ATTACK, "ranged", 1);
        }
        for (int id : eNPC.MOTHER) // Sea Snake Mother
        {
            registerScheduledDirectAttack(id, AnimationID.ROYAL_SEA_SNAKE_MOTHER_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.ROYAL_SEA_SNAKE_MOTHER_RANGED_ATTACK, "ranged", 1);
        }

        // Mutant Tarn (Lair of Tarn Razorlor)
        // Anim: LOTR_TARN_RAZORLOR_MUTANT_ATTACK=5617
        for (int id : eNPC.MUTANT_TARN) { registerScheduledDirectAttack(id, AnimationID.LOTR_TARN_RAZORLOR_MUTANT_ATTACK, "melee", 1); }

        // Galvek, Elvarg handled above.

        // Killerwatt — melee + magic cast
        // Anim: KILLERWATT_BIPED_ATTACK=3163, KILLERWATT_BIPED_CAST=3164
        for (int id : eNPC.KILLERWATT)
        {
            registerScheduledDirectAttack(id, AnimationID.KILLERWATT_BIPED_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.KILLERWATT_BIPED_CAST, "magic", 1);
        }

        // Abomination (A Porcine of Interest quest)
        // Anim: MYQ4_ABOMINATION_MELEE=8025, MYQ4_ABOMINATION_RANGE=8026
        // Spot: MYQ4_ABOMINATION_PROJ_TRAVEL=1533
        for (int id : eNPC.ABOMINATION)
        {
            registerScheduledDirectAttack(id, AnimationID.MYQ4_ABOMINATION_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.MYQ4_ABOMINATION_PROJ_TRAVEL, "ranged");
        }

        // Sourhog (Misthalin Mystery)
        // Anim: SOURHOG_ATTACK_MELEE=8769
        // Spot: SOURHOG_SPIT_TRAVEL=1817
        for (int id : eNPC.SOURHOG)
        {
            registerScheduledDirectAttack(id, AnimationID.SOURHOG_ATTACK_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.SOURHOG_SPIT_TRAVEL, "ranged");
        }

        // Scion (Barrows — Doomion/Othainian/Holthion)
        // Anim: ABYSSAL_SCION_ATTACK_MELEE=7126, ABYSSAL_SCION_ATTACK_RANGED=7127
        for (int id : eNPC.SCION)
        {
            registerScheduledDirectAttack(id, AnimationID.ABYSSAL_SCION_ATTACK_MELEE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.ABYSSAL_SCION_ATTACK_RANGED, "ranged", 1);
        }

        // The Mimic (Elite Clue)
        // Anim: MIMIC_MELEE=8308, MIMIC_CHARGE_RANGED=8309
        for (int id : eNPC.THE_MIMIC)
        {
            registerScheduledDirectAttack(id, AnimationID.MIMIC_MELEE, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.MIMIC_CHARGE_RANGED, "ranged", 1);
        }

        // Judge of Yama / Yama (Fort Forinthry)
        // Anim: JUDGEOFYAMA_ATTACK=9043, NPC_YAMA01_MELEE01=12146
        // Spot: VFX_NPC_YAMA_MAGIC_FIRE_SPOTANIM01=3246, _SHADOW=3243
        for (int id : eNPC.JUDGE_OF_YAMA)
        {
            registerScheduledDirectAttack(id, AnimationID.JUDGEOFYAMA_ATTACK, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.JUDGEOFYAMA_ATTACK_SLOW,"melee", 1); // JUDGEOFYAMA_ATTACK_SLOW
        }
        for (int id : eNPC.YAMA)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_YAMA01_MELEE01, "melee", 1);
            registerProjectile  (id, SpotanimID.VFX_NPC_YAMA_MAGIC_SHADOW_SPOTANIM01,  "magic");
            registerProjectile  (id, SpotanimID.VFX_NPC_YAMA_MAGIC_FIRE_SPOTANIM01,  "magic");
            registerMechanic    (id, SpotanimID.VFX_YAMA_SHADOW_SPIKE_PROJECTILE_01,  "spike");
            registerMechanic    (id, SpotanimID.VFX_YAMA_FLAMING_ROCK_PROJECTILE_01,  "rockfall");
            registerMechanic    (id, AnimationID.NPC_YAMA_SUMMON01, "summon");
        }

        // ===================================================================
        // Varlamore content
        // ===================================================================

        // Amoxliatl — melee + ice projectile + summon mechanic
        // Anim: AMOXLIATL_ATTACK=11530
        // Spot: AMOXLIATL_ATTACK_PROJ=2934, VFX_AMOXLIATL_ICE_BLOCK_PROJECTILE_01=2936
        for (int id : eNPC.AMOXLIATL)
        {
            registerScheduledDirectAttack(id, AnimationID.AMOXLIATL_ATTACK, "melee", 1);
            registerProjectile  (id, SpotanimID.AMOXLIATL_ATTACK_PROJ,  "magic");
            registerProjectile  (id, SpotanimID.VFX_AMOXLIATL_ICE_BLOCK_PROJECTILE_01,  "magic");
            registerMechanic    (id, AnimationID.AMOXLIATL_SUMMON, "summon");
        }

        // Shellbane Gryphon
        // Anim: GRYPHON_BOSS_MELEE_ATTACK01=12552, GRYPHON_BOSS_RANGED01=12554, GRYPHON_BOSS_SPIT01=12553
        for (int id : eNPC.SHELLBANE_GRYPHON)
        {
            registerScheduledDirectAttack(id, AnimationID.GRYPHON_BOSS_MELEE_ATTACK01, "melee", 1);
            registerScheduledDirectAttack(id, AnimationID.GRYPHON_BOSS_RANGED01, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.GRYPHON_BOSS_SPIT01, "magic", 1);
        }

        // Branda the Fire Queen
        // Anim: FIRE_GIANT_BRANDA_QUEEN_ATTACK_MELEE01=11974
        // Spot: VFX_FIRE_GIANT_BRANDA_QUEEN_ATTACK_AOE_PROJ01=3209
        for (int id : eNPC.BRANDA_THE_FIRE_QUEEN)
        {
            registerScheduledDirectAttack(id, AnimationID.FIRE_GIANT_BRANDA_QUEEN_ATTACK_MELEE01, "melee", 1);
            registerProjectile  (id, SpotanimID.VFX_FIRE_GIANT_BRANDA_QUEEN_ATTACK_AOE_PROJ01,  "magic");
            registerMechanic    (id, AnimationID.FIRE_GIANT_BRANDA_QUEEN_ATTACK_SUMMON01, "summon");
        }

        // Eldric the Ice King
        // Anim: ICE_GIANT_ELDRIC_KING_ATTACK_MELEE01=11973
        // Spot: VFX_ICE_GIANT_ELDRIC_KING_ATTACK_AOE_PROJ01=3210, _MELEE01_PROJ01=3211
        for (int id : eNPC.ELDRIC_THE_ICE_KING)
        {
            registerScheduledDirectAttack(id, AnimationID.ICE_GIANT_ELDRIC_KING_ATTACK_MELEE01, "melee", 1);
            registerProjectile  (id, SpotanimID.VFX_ICE_GIANT_ELDRIC_KING_ATTACK_AOE_PROJ01,  "magic");
            registerProjectile  (id, SpotanimID.VFX_ICE_GIANT_ELDRIC_KING_MELEE01_PROJ01,  "melee");
            registerMechanic    (id, AnimationID.ICE_GIANT_ELDRIC_KING_ATTACK_SUMMON01, "summon");
        }

        // Manticore (Colosseum)
        // Anim: NPC_MANTICORE_01_TRIPLE_CHARGE=10868
        // Spot: VFX_MANTICORE_01_PROJECTILE_MAGIC_01=2681, _RANGED_01=2683, _MELEE_01=2685
        for (int id : eNPC.MANTICORE)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_MANTICORE_01_TRIPLE_CHARGE, "melee", 1);
            registerProjectile  (id, SpotanimID.VFX_MANTICORE_01_PROJECTILE_MAGIC_01,  "magic");
            registerProjectile  (id, SpotanimID.VFX_MANTICORE_01_PROJECTILE_RANGED_01,  "ranged");
            registerProjectile  (id, SpotanimID.VFX_MANTICORE_01_PROJECTILE_MELEE_01,  "melee");
        }

        // Minotaur (Colosseum)
        // Anim: NPC_MINOTAUR_BOSS_ATTACK_MELEE=10843
        // Spot: VFX_MINOTAUR_PROJECTILE_MAGIC_01=2950
        for (int id : eNPC.MINOTAUR)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_MINOTAUR_BOSS_ATTACK_MELEE, "melee", 1);
            registerProjectile  (id, SpotanimID.VFX_MINOTAUR_PROJECTILE_MAGIC_01,  "magic");
        }

        // ===================================================================
        // Miscellaneous / Slayer NPCs
        // ===================================================================

        // Balance Elemental (A Kingdom Divided)
        // Anim: NPC_BALANCE_ELEMENTAL_WATER_ATTACK01=11342, _AIR_=11348, _FIRE_=11354
        for (int id : eNPC.BALANCE_ELEMENTAL)
        {
            registerScheduledDirectAttack(id, AnimationID.NPC_BALANCE_ELEMENTAL_WATER_ATTACK01, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_BALANCE_ELEMENTAL_AIR_ATTACK01, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.NPC_BALANCE_ELEMENTAL_FIRE_ATTACK01, "magic", 1);
            registerProjectile  (id, SpotanimID.VFX_BALANCE_ELEMENTAL_WATER_ATTACK01_PROJANIM,  "magic");
            registerProjectile  (id, SpotanimID.VFX_BALANCE_ELEMENTAL_FIRE_ATTACK01_PROJANIM,  "magic");
            registerProjectile  (id, SpotanimID.VFX_BALANCE_ELEMENTAL_AIR_ATTACK_SPECIAL01_PROJANIM,  "ranged");
        }

        // Lizardman / Lizardman Shaman
        // Spot: LIZARDMAN_SPIT=1291, LIZARDSHAMAN_SPAWN_EXPLODE=1295 (mechanic)
        for (int id : eNPC.LIZARDMAN) { registerProjectile(id, SpotanimID.LIZARDMAN_SPIT, "ranged"); }
        for (int id : eNPC.LIZARDMAN_SHAMAN)
        {
            registerProjectile(id, SpotanimID.LIZARDSHAMAN_SPIT_ACID, "ranged");   // spit acid — TODO verify ID
            registerMechanic  (id, SpotanimID.LIZARDSHAMAN_SPAWN_EXPLODE, "spawn");
        }

        // Zygomite variants
        // Anim: ZYGOMITE_ATTACK=3326
        for (int[] group : new int[][]{eNPC.ZYGOMITE, eNPC.ANCIENT_ZYGOMITE})
        {
            for (int id : group) { registerScheduledDirectAttack(id, AnimationID.ZYGOMITE_ATTACK, "melee", 1); }
        }

        // Mutated Terrorbird / Warped Terrorbird
        // Anim: TERRORBIRD_ATTACK=322, TERRORBIRD_MOUNTED_ATTACK=327
        for (int[] group : new int[][]{eNPC.MUTATED_TERRORBIRD, eNPC.WARPED_TERRORBIRD})
        {
            for (int id : group)
            {
                registerScheduledDirectAttack(id, AnimationID.TERRORBIRD_ATTACK, "melee", 1);
                registerScheduledDirectAttack(id, AnimationID.TERRORBIRD_MOUNTED_ATTACK, "melee", 1);
            }
        }

        // Hydra (non-alchemical, all stages) — magic + ranged animations
        // Anim: HYDRA_STAGE_1_ATTACK_RANGED=8235, _MAGIC=8236 ... (stages 1-4)
        for (int id : eNPC.HYDRA)
        {
            registerScheduledDirectAttack(id, AnimationID.HYDRA_STAGE_1_ATTACK_RANGED, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.HYDRA_STAGE_1_ATTACK_MAGIC, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.HYDRA_STAGE_2_ATTACK_RANGED, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.HYDRA_STAGE_2_ATTACK_MAGIC, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.HYDRA_STAGE_3_ATTACK_RANGED, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.HYDRA_STAGE_3_ATTACK_MAGIC, "magic", 1);
            registerScheduledDirectAttack(id, AnimationID.HYDRA_STAGE_4_ATTACK_RANGED, "ranged", 1);
            registerScheduledDirectAttack(id, AnimationID.HYDRA_STAGE_4_ATTACK_MAGIC, "magic", 1);
        }

        // Dessout (Slayer) same entry as quest version — already registered above.

        // ===================================================================
        // Zulrah
        // ===================================================================
        // Spot IDs for Zulrah need in-game logging to confirm precisely.
        // Using approximate values known from community research:
        //   ZULRAH_RANGED_PROJ ≈ 1247, ZULRAH_MAGIC_PROJ ≈ 1249
        for (int id : eNPC.ZULRAH)
        {
            registerProjectile(id, SpotanimID.CERBERUS_SPECIAL_ATTACK_FLAME, "ranged"); // TODO - verify
            registerProjectile(id, SpotanimID.GUBLINCH_SNOW_SPELL, "magic");  // TODO - verify
        }
    }

    // -----------------------------------------------------------------------
    // Private helper methods to reduce code duplication
    // -----------------------------------------------------------------------

    private static void registerJad(int id)
    {
        // JALTOKJAD_ATTACK_MELEE=7590, _MAGIC=7592, _RANGED=7593
        registerScheduledDirectAttack(id, AnimationID.JALTOKJAD_ATTACK_MELEE, "melee", 1);
        registerScheduledDirectAttack(id, AnimationID.JALTOKJAD_ATTACK_MAGIC, "magic", 1);
        registerScheduledDirectAttack(id, AnimationID.JALTOKJAD_ATTACK_RANGED, "ranged", 1);
    }

    private static void registerMaiden(int id)
    {
        registerScheduledDirectAttack(id, AnimationID.MAIDEN_ATTACK_BLOOD, "magic", 1); // MAIDEN_ATTACK_BLOOD
        registerMechanic    (id, AnimationID.MAIDEN_ATTACK_SPECIAL, "spawns");    // MAIDEN_ATTACK_SPECIAL
    }

    private static void registerVerzik(int id)
    {
        registerProjectile(id, SpotanimID.VERZIK_PHASE1_PROJECTILE, "magic");       // P1 magic bolt
        registerProjectile(id, SpotanimID.VERZIK_PHASE2_RANGED, "ranged");      // P2 ranged
        registerProjectile(id, SpotanimID.VERZIK_PHASE2_BLOODPROJ, "magic");       // P2 blood proj
        registerProjectile(id, SpotanimID.VERZIK_PHASE3_MAGEPROJ, "magic");       // P3 mage
        registerProjectile(id, SpotanimID.VERZIK_PHASE3_RANGEPROJ, "ranged");      // P3 ranged
        registerMechanic  (id, SpotanimID.VERZIK_ACIDBOMB_PROJANIM, "acid");        // acid bomb
        registerMechanic  (id, SpotanimID.VERZIK_P3_WEB_PROJ, "web");         // P3 web proj
    }

    private static void registerWarden(int id)
    {
        // NPC_WARDENS_MELEE01=9659
        registerScheduledDirectAttack(id, AnimationID.NPC_WARDENS_MELEE01, "melee", 1);
        // prayer attacks (each style can be melee/ranged/magic depending on which prayer they drain)
        registerProjectile  (id, SpotanimID.TOA_WARDENS_PRAYER_MELEE_TRAVEL, "melee");     // TOA_WARDENS_PRAYER_MELEE_TRAVEL
        registerProjectile  (id, SpotanimID.TOA_WARDENS_PRAYER_RANGED_TRAVEL, "ranged");    // TOA_WARDENS_PRAYER_RANGED_TRAVEL
        registerProjectile  (id, SpotanimID.TOA_WARDENS_PRAYER_MAGIC_TRAVEL, "magic");     // TOA_WARDENS_PRAYER_MAGIC_TRAVEL
        registerProjectile  (id, SpotanimID.TOA_WARDENS_PRAYER_MAGIC_TRAVEL_SMALL, "magic");     // TOA_WARDENS_PRAYER_MAGIC_TRAVEL_SMALL
        registerMechanic    (id, AnimationID.TOA_WARDENS01_PYRAMID_ATTACK, "pyramid");   // TOA_WARDENS01_PYRAMID_ATTACK
        registerMechanic    (id, SpotanimID.TOA_WARDENS_ENTOMB_TRAVEL, "entomb");    // TOA_WARDENS_ENTOMB_TRAVEL
    }

    private static void registerNightmare(int id)
    {
        // Anim: NIGHTMARE_ATTACK_MELEE=8594
        // Spot: NIGHTMARE_MAGIC_TRAVEL=1764, NIGHTMARE_RANGED_TRAVEL=1766
        registerScheduledDirectAttack(id, AnimationID.NIGHTMARE_ATTACK_MELEE, "melee",   1);
        registerProjectile  (id, SpotanimID.NIGHTMARE_MAGIC_TRAVEL, "magic");
        registerProjectile  (id, SpotanimID.NIGHTMARE_MAGIC_IMPACT, "magic");  // NIGHTMARE_MAGIC_IMPACT
        registerProjectile  (id, SpotanimID.NIGHTMARE_RANGED_TRAVEL, "ranged");
        registerMechanic    (id, AnimationID.NIGHTMARE_ATTACK_RIFT, "rift");
        registerMechanic    (id, AnimationID.NIGHTMARE_ATTACK_RIFT_FAST, "rift");
        registerMechanic    (id, AnimationID.NIGHTMARE_ATTACK_SEGMENT, "segment");
        registerMechanic    (id, AnimationID.NIGHTMARE_ATTACK_SUMMON, "summon");
    }

    /** Clear all mappings (for testing). */
    public static void clearAllMappings()
    {
        animationMappings.clear();
        animationDelays.clear();
        projectileMappings.clear();
        mechanicAnimations.clear();
        mechanicStyles.clear();
        linkedMechanicBossIds.clear();
        linkedMechanicStyles.clear();
    }
}