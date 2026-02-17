package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.AnimationChanged;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.helpers.FightTracker;
import net.runelite.client.plugins.pvmperformancetracker.helpers.PlayerAttackStyleHelper;
import net.runelite.client.plugins.pvmperformancetracker.models.Fight;
import net.runelite.client.plugins.pvmperformancetracker.models.PlayerAttackStyle;
import net.runelite.client.plugins.pvmperformancetracker.party.PartyStatsManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Listens for animation changes from players and NPCs
 * - Tracks player attack animations to record DPS and expected damage
 * - Tracks NPC attack animations to determine attack styles (for multi-style bosses)
 */
@Slf4j
public class AnimationListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    // Common attack animations - used to filter which animations to track
    private static final Set<Integer> ATTACK_ANIMATIONS = new HashSet<>();

    static
    {
        // === MELEE ANIMATIONS ===
        ATTACK_ANIMATIONS.add(422);  // Unarmed punch
        ATTACK_ANIMATIONS.add(423);  // Unarmed kick
        ATTACK_ANIMATIONS.add(390);  // Dragon scimitar
        ATTACK_ANIMATIONS.add(401);  // Bronze-Rune swords
        ATTACK_ANIMATIONS.add(406);  // Dragon longsword/battleaxe
        ATTACK_ANIMATIONS.add(407);  // Dragon dagger
        ATTACK_ANIMATIONS.add(428);  // Staff bash / spear
        ATTACK_ANIMATIONS.add(440);  // Godsword slash
        ATTACK_ANIMATIONS.add(1658); // Abyssal whip
        ATTACK_ANIMATIONS.add(1665); // Godsword smash
        ATTACK_ANIMATIONS.add(386);  // 2h sword
        ATTACK_ANIMATIONS.add(395);  // Dragon 2h
        ATTACK_ANIMATIONS.add(381);  // Dragon dagger stab
        ATTACK_ANIMATIONS.add(400);  // Dragon longsword stab
        ATTACK_ANIMATIONS.add(7514); // Soulreaper axe
        ATTACK_ANIMATIONS.add(8145); // Zamorakian hasta
        ATTACK_ANIMATIONS.add(8056); // Scythe of vitur
        ATTACK_ANIMATIONS.add(4230); // Scythe variant
        ATTACK_ANIMATIONS.add(7004); // Osmumten's fang slash
        ATTACK_ANIMATIONS.add(7005); // Osmumten's fang stab
        ATTACK_ANIMATIONS.add(7045); // Inquisitor's mace
        ATTACK_ANIMATIONS.add(4503); // Granite maul
        ATTACK_ANIMATIONS.add(2078); // Saradomin sword
        ATTACK_ANIMATIONS.add(8288); // Dragon hunter lance
        ATTACK_ANIMATIONS.add(9544); // Voidwaker

        // === RANGED ANIMATIONS ===
        ATTACK_ANIMATIONS.add(426);  // Shortbow/longbow
        ATTACK_ANIMATIONS.add(929);  // Longbow
        ATTACK_ANIMATIONS.add(5061); // Blowpipe/chinchompas
        ATTACK_ANIMATIONS.add(7617); // Crossbow / Bow of faerdhinen
        ATTACK_ANIMATIONS.add(7552); // Twisted bow
        ATTACK_ANIMATIONS.add(7554); // Heavy ballista
        ATTACK_ANIMATIONS.add(7555); // Light ballista / blowpipe
        ATTACK_ANIMATIONS.add(8291); // Bow of faerdhinen variant
        ATTACK_ANIMATIONS.add(9206); // Zaryte crossbow
        ATTACK_ANIMATIONS.add(8267); // Ballista
        ATTACK_ANIMATIONS.add(10656); // Blazing blowpipe

        // === MAGIC ANIMATIONS ===
        ATTACK_ANIMATIONS.add(1162); // Standard spellbook cast
        ATTACK_ANIMATIONS.add(1167); // Trident of the seas/swamp
        ATTACK_ANIMATIONS.add(1978); // Ancient spellbook cast
        ATTACK_ANIMATIONS.add(8532); // Dawnbringer
        ATTACK_ANIMATIONS.add(7855); // Sanguinesti staff
        ATTACK_ANIMATIONS.add(9487); // Tumeken's shadow
        ATTACK_ANIMATIONS.add(9493); // Tumeken's shadow variant
    }

    public AnimationListener(PvMPerformanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }

    public void onAnimationChanged(AnimationChanged event)
    {
        if (!plugin.getConfig().enableTracking())
        {
            return;
        }

        Actor actor = event.getActor();

        // Track player animations
        if (actor instanceof Player)
        {
            handlePlayerAnimation((Player) actor);
        }
        // Track NPC animations
        else if (actor instanceof NPC)
        {
            handleNpcAnimation((NPC) actor);
        }
    }

    /**
     * Handle player animation changes
     * Only tracks local player and party members (not random nearby players)
     */
    private void handlePlayerAnimation(Player player)
    {
        // CRITICAL: Only track local player OR party members
        if (!isPartyMemberOrLocal(player))
        {
            return; // Skip random nearby players
        }

        int animationId = player.getAnimation();

        // Check if this is an attack animation
        if (isAttackAnimation(animationId))
        {
            handleAttackAnimation(player, animationId);
        }
    }

    /**
     * Handle NPC animation changes
     * Tracks NPC attack animations to determine attack style (for multi-style bosses)
     */
    private void handleNpcAnimation(NPC npc)
    {
        FightTracker fightTracker = plugin.getFightTracker();

        // Only track if there's an active fight
        if (fightTracker == null || !fightTracker.hasActiveFight())
        {
            return;
        }

        // Only track the boss we're fighting
        int currentBossId = fightTracker.getCurrentFight().getBossNpcId();
        if (npc.getId() != currentBossId)
        {
            return;
        }

        int animationId = npc.getAnimation();

        // Record the NPC animation in the attack tracker
        if (plugin.getNpcAttackTracker() != null && animationId != -1)
        {
            plugin.getNpcAttackTracker().recordNpcAnimation(npc, animationId);
        }
    }

    /**
     * Check if player is local player or a party member
     */
    private boolean isPartyMemberOrLocal(Player player)
    {
        Player localPlayer = client.getLocalPlayer();

        // Check if this is the local player
        if (player.equals(localPlayer))
        {
            return true;
        }

        // Check if this is a party member
        PartyStatsManager partyManager = plugin.getPartyStatsManager();
        if (partyManager != null && partyManager.isPartyTrackingEnabled())
        {
            return partyManager.isPartyMember(player.getName());
        }

        return false; // Not local player and not in party
    }

    /**
     * Check if animation ID is a known attack animation
     */
    private boolean isAttackAnimation(int animationId)
    {
        return ATTACK_ANIMATIONS.contains(animationId);
    }

    /**
     * Handle an attack animation from a player
     * Records the attack and calculates expected damage
     */
    private void handleAttackAnimation(Player player, int animationId)
    {
        FightTracker fightTracker = plugin.getFightTracker();

        // Only record if there's an active fight
        if (fightTracker == null || !fightTracker.hasActiveFight())
        {
            return;
        }

        String playerName = player.getName();
        if (playerName == null)
        {
            return;
        }

        // Get weapon ID (only available for local player)
        Integer weaponId = null;
        if (player.equals(client.getLocalPlayer()))
        {
            weaponId = getEquippedWeaponId();
        }

        // Get complete attack information using PlayerAttackStyleHelper
        PlayerAttackStyle attackStyle = PlayerAttackStyleHelper.getAttackStyle(animationId, weaponId);

        // Record the attack with weapon speed
        fightTracker.recordAttack(playerName, attackStyle.getWeaponSpeedTicks());

        // Calculate expected damage for local player only (we have their equipment stats)
        if (player.equals(client.getLocalPlayer()))
        {
            calculateAndRecordExpectedDamage(fightTracker, playerName, attackStyle);
        }

        log.debug("{} attacked with animation {} (style: {}, weapon speed: {} ticks)",
                playerName, animationId, attackStyle.getStyle(), attackStyle.getWeaponSpeedTicks());
    }

    /**
     * Get the currently equipped weapon ID for the local player
     * Returns null if no weapon or player not found
     */
    private Integer getEquippedWeaponId()
    {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null)
        {
            return null;
        }

        Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        if (weapon == null || weapon.getId() <= 0)
        {
            return null;
        }

        return weapon.getId();
    }

    /**
     * Calculate and record expected damage for the attack
     * Only called for local player since we need their equipment stats
     */
    private void calculateAndRecordExpectedDamage(FightTracker fightTracker, String playerName, PlayerAttackStyle attackStyle)
    {
        // Check if NPC database is loaded
        if (plugin.getNpcStatsProvider() == null || !plugin.getNpcStatsProvider().isLoaded())
        {
            return; // NPC database not loaded yet
        }

        Fight currentFight = fightTracker.getCurrentFight();
        if (currentFight == null)
        {
            return;
        }

        // CRITICAL: Get the actual NPC object for variant detection
        // We need to find the NPC in the world to distinguish Normal vs Awakened etc.
        int bossNpcId = currentFight.getBossNpcId();

        // Find the actual NPC object in the game world
        NPC bossNpc = null;
        for (NPC npc : client.getNpcs())
        {
            if (npc != null && npc.getId() == bossNpcId && npc.getName() != null)
            {
                // Found a matching NPC
                bossNpc = npc;
                break;
            }
        }

        if (bossNpc == null)
        {
            log.warn("Could not find boss NPC in world (ID: {})", bossNpcId);
            return;
        }

        // Use the NPC object for variant-aware stats lookup
        // This allows BossVariantHelper to distinguish between Normal/Awakened/Quest variants
        var npcStats = plugin.getNpcStatsProvider().getNpcStats(bossNpc);

        if (npcStats == null)
        {
            log.warn("No stats available for NPC: {} (ID: {})", bossNpc.getName(), bossNpcId);
            return;
        }

        // Calculate expected damage using combat formulas
        var combatFormulas = plugin.getCombatFormulas();
        if (combatFormulas != null)
        {
            double expectedDamage = combatFormulas.calculateExpectedDamage(npcStats, attackStyle.getStyle());

            // Record it in player stats
            var playerStats = currentFight.getPlayerStats().get(playerName);
            if (playerStats != null)
            {
                playerStats.addExpectedDamage(expectedDamage);
                log.debug("Expected damage for {}: {} (style: {})", playerName, expectedDamage, attackStyle.getStyle());
            }
        }
    }
}