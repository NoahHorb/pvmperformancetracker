package net.runelite.client.plugins.pvmperformancetracker.listeners;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.plugins.pvmperformancetracker.PvMPerformanceTrackerPlugin;
import net.runelite.client.plugins.pvmperformancetracker.helpers.CombatSessionManager;
import net.runelite.client.plugins.pvmperformancetracker.model.CombatSession;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ChatMessageListener
{
    private final PvMPerformanceTrackerPlugin plugin;
    private final Client client;

    // Patterns for detecting combat-related messages
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("You (hit|slam|smash|slash|stab) .*? for (\\d+) damage");
    private static final Pattern DEATH_PATTERN = Pattern.compile("You have defeated .*");
    private static final Pattern PLAYER_DEATH_PATTERN = Pattern.compile("Oh dear, you are dead!");
    private static final Pattern SUCCESSFUL_HIT_PATTERN = Pattern.compile("You (hit|slash|stab|smash)");

    public ChatMessageListener(PvMPerformanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
        this.client = plugin.getClient();
    }

    public void onChatMessage(ChatMessage event)
    {
        if (!plugin.getConfig().enableTracking())
        {
            return;
        }

        ChatMessageType type = event.getType();
        String message = event.getMessage();

        // Only process game messages and spam
        if (type != ChatMessageType.GAMEMESSAGE &&
                type != ChatMessageType.SPAM &&
                type != ChatMessageType.ENGINE)
        {
            return;
        }

        // Check for death message
        if (PLAYER_DEATH_PATTERN.matcher(message).find())
        {
            handlePlayerDeathMessage();
            return;
        }

        // Check for NPC defeat message
        if (DEATH_PATTERN.matcher(message).find())
        {
            handleNPCDefeatMessage(message);
            return;
        }

        // Check for successful hit (to track accuracy)
        if (SUCCESSFUL_HIT_PATTERN.matcher(message).find())
        {
            handleSuccessfulHit();
        }
    }

    private void handlePlayerDeathMessage()
    {
        log.debug("Player death detected from chat message");

        CombatSessionManager sessionManager = plugin.getSessionManager();

        if (plugin.getConfig().endOnDeath() && sessionManager.hasActiveSession())
        {
            sessionManager.endCurrentSession();
        }
    }

    private void handleNPCDefeatMessage(String message)
    {
        log.debug("NPC defeat detected: {}", message);

        CombatSessionManager sessionManager = plugin.getSessionManager();
        CombatSession session = sessionManager.getCurrentSession();

        if (session == null || !session.isActive())
        {
            return;
        }

        // Extract NPC name from message if possible
        // This is a fallback method, the ActorDeath event is more reliable
    }

    private void handleSuccessfulHit()
    {
        CombatSessionManager sessionManager = plugin.getSessionManager();
        CombatSession session = sessionManager.getCurrentSession();

        if (session != null && session.isActive())
        {
            session.incrementSuccessfulHits();
            session.updateLastActivity();
        }
    }
}