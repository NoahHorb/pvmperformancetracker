package net.runelite.client.plugins.pvmperformancetracker.helpers;

import net.runelite.client.plugins.pvmperformancetracker.model.CombatSession;
import net.runelite.client.plugins.pvmperformancetracker.model.DamageEntry;

import java.util.*;
import java.util.stream.Collectors;

public class CombatStatisticsHelper
{
    /**
     * Calculate average DPS across multiple sessions
     */
    public double calculateAverageDPS(List<CombatSession> sessions)
    {
        if (sessions == null || sessions.isEmpty())
        {
            return 0.0;
        }

        return sessions.stream()
                .mapToDouble(CombatSession::getDPS)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate total damage across multiple sessions
     */
    public int calculateTotalDamage(List<CombatSession> sessions)
    {
        if (sessions == null || sessions.isEmpty())
        {
            return 0;
        }

        return sessions.stream()
                .mapToInt(CombatSession::getTotalDamage)
                .sum();
    }

    /**
     * Calculate average accuracy across multiple sessions
     */
    public double calculateAverageAccuracy(List<CombatSession> sessions)
    {
        if (sessions == null || sessions.isEmpty())
        {
            return 0.0;
        }

        return sessions.stream()
                .mapToDouble(CombatSession::getAccuracyPercentage)
                .average()
                .orElse(0.0);
    }

    /**
     * Find the highest DPS session
     */
    public CombatSession findHighestDPSSession(List<CombatSession> sessions)
    {
        if (sessions == null || sessions.isEmpty())
        {
            return null;
        }

        return sessions.stream()
                .max(Comparator.comparingDouble(CombatSession::getDPS))
                .orElse(null);
    }

    /**
     * Find the highest damage session
     */
    public CombatSession findHighestDamageSession(List<CombatSession> sessions)
    {
        if (sessions == null || sessions.isEmpty())
        {
            return null;
        }

        return sessions.stream()
                .max(Comparator.comparingInt(CombatSession::getTotalDamage))
                .orElse(null);
    }

    /**
     * Get sessions filtered by target
     */
    public List<CombatSession> getSessionsByTarget(List<CombatSession> sessions, String target)
    {
        if (sessions == null || target == null)
        {
            return Collections.emptyList();
        }

        return sessions.stream()
                .filter(s -> target.equalsIgnoreCase(s.getPrimaryTarget()))
                .collect(Collectors.toList());
    }

    /**
     * Get unique targets from sessions
     */
    public Set<String> getUniqueTargets(List<CombatSession> sessions)
    {
        if (sessions == null || sessions.isEmpty())
        {
            return Collections.emptySet();
        }

        return sessions.stream()
                .map(CombatSession::getPrimaryTarget)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Calculate damage distribution percentages
     */
    public Map<String, Double> calculateDamageDistribution(CombatSession session)
    {
        Map<String, Double> distribution = new HashMap<>();

        if (session == null || session.getTotalDamage() == 0)
        {
            return distribution;
        }

        Map<String, Integer> damageByStyle = session.getDamageByAttackStyle();
        int totalDamage = session.getTotalDamage();

        for (Map.Entry<String, Integer> entry : damageByStyle.entrySet())
        {
            double percentage = (entry.getValue() / (double) totalDamage) * 100.0;
            distribution.put(entry.getKey(), percentage);
        }

        return distribution;
    }

    /**
     * Get top damage dealers (weapons) from a session
     */
    public List<Map.Entry<String, Integer>> getTopDamageWeapons(CombatSession session, int limit)
    {
        if (session == null)
        {
            return Collections.emptyList();
        }

        return session.getDamageByWeapon().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calculate hits per minute
     */
    public double calculateHitsPerMinute(CombatSession session)
    {
        if (session == null || session.getDurationSeconds() == 0)
        {
            return 0.0;
        }

        double durationMinutes = session.getDurationSeconds() / 60.0;
        return session.getSuccessfulHits() / durationMinutes;
    }

    /**
     * Calculate average hit
     */
    public double calculateAverageHit(CombatSession session)
    {
        if (session == null || session.getSuccessfulHits() == 0)
        {
            return 0.0;
        }

        return session.getTotalDamage() / (double) session.getSuccessfulHits();
    }

    /**
     * Get damage timeline (useful for graphing)
     */
    public Map<Long, Integer> getDamageTimeline(CombatSession session, int intervalSeconds)
    {
        Map<Long, Integer> timeline = new TreeMap<>();

        if (session == null || session.getDamageEntries().isEmpty())
        {
            return timeline;
        }

        long sessionStart = session.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        for (DamageEntry entry : session.getDamageEntries())
        {
            long entryTime = entry.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long interval = (entryTime - sessionStart) / (intervalSeconds * 1000);

            timeline.merge(interval, entry.getDamage(), Integer::sum);
        }

        return timeline;
    }

    /**
     * Compare two sessions and generate comparison statistics
     */
    public Map<String, Object> compareSession(CombatSession session1, CombatSession session2)
    {
        Map<String, Object> comparison = new HashMap<>();

        if (session1 == null || session2 == null)
        {
            return comparison;
        }

        comparison.put("dps_difference", session1.getDPS() - session2.getDPS());
        comparison.put("damage_difference", session1.getTotalDamage() - session2.getTotalDamage());
        comparison.put("accuracy_difference", session1.getAccuracyPercentage() - session2.getAccuracyPercentage());
        comparison.put("duration_difference", session1.getDurationSeconds() - session2.getDurationSeconds());

        return comparison;
    }
}