package net.runelite.client.plugins.pvmperformancetracker.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.plugins.pvmperformancetracker.models.NpcCombatStats;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * downloads and caches NPC combat stats from custom NPC database
 */
@Slf4j
public class NpcStatsProvider
{
    // URL to your GitHub-hosted database
    private static final String NPC_DATABASE_URL = "https://raw.githubusercontent.com/NoahHorb/scraperwiki/refs/heads/master/npc_database.json";
    private static final String CACHE_FILE_NAME = "npc-database.json";
    private final Client client;

    private BossVariantHelper bossVariantHelper;
    private final Gson gson;
    private final Path cacheDirectory;
    private final Map<Integer, NpcCombatStats> npcStatsCache;
    private final Map<String, NpcCombatStats> npcStatsByKey;
    private boolean isLoaded = false;

    public NpcStatsProvider(File runeLiteDirectory, Client client)
    {
        // Use lenient Gson to handle missing/null fields
        this.gson = new GsonBuilder()
                .setLenient()
                .create();
        this.npcStatsCache = new HashMap<>();
        this.npcStatsByKey = new HashMap<>();
        this.client = client;
        // Cache in RuneLite's config directory
        this.cacheDirectory = Paths.get(runeLiteDirectory.getAbsolutePath(), "pvmperformancetracker");

        try
        {
            Files.createDirectories(cacheDirectory);
        }
        catch (IOException e)
        {
            log.error("Failed to create cache directory", e);
        }
    }

    /**
     * Initialize the provider - download or load cached database
     */
    public void initialize()
    {
        this.bossVariantHelper = new BossVariantHelper();
        Path cacheFile = cacheDirectory.resolve(CACHE_FILE_NAME);

        // Check if cache exists and is recent (less than 7 days old)
        if (Files.exists(cacheFile))
        {
            try
            {
                long lastModified = Files.getLastModifiedTime(cacheFile).toMillis();
                long daysSinceUpdate = (System.currentTimeMillis() - lastModified) / (1000 * 60 * 60 * 24);

                if (daysSinceUpdate < 7)
                {
                    log.info("Loading NPC stats from cache (age: {} days)", daysSinceUpdate);
                    loadFromCache(cacheFile);
                    return;
                }
                else
                {
                    log.info("Cache is {} days old, downloading fresh data", daysSinceUpdate);
                }
            }
            catch (IOException e)
            {
                log.warn("Failed to check cache file age", e);
            }
        }

        // Download fresh data
        downloadDatabase(cacheFile);
    }

    /**
     * Download database from GitHub
     */
    private void downloadDatabase(Path cacheFile)
    {
        log.info("Downloading NPC database from: {}", NPC_DATABASE_URL);

        try
        {
            URL url = new URL(NPC_DATABASE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                // Read response
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
                {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        response.append(line).append("\n");
                    }

                    // Save to cache
                    Files.write(cacheFile, response.toString().getBytes(StandardCharsets.UTF_8));
                    log.info("Downloaded and cached NPC database");

                    // Load from cache
                    loadFromCache(cacheFile);
                }
            }
            else
            {
                log.error("Failed to download NPC database. HTTP code: {}", responseCode);

                // Try to load old cache if download failed
                if (Files.exists(cacheFile))
                {
                    log.info("Falling back to existing cache");
                    loadFromCache(cacheFile);
                }
            }
        }
        catch (Exception e)
        {
            log.error("Failed to download NPC database", e);

            // Try to load old cache if download failed
            if (Files.exists(cacheFile))
            {
                log.info("Falling back to existing cache");
                loadFromCache(cacheFile);
            }
        }
    }

    /**
     * Load NPC stats from cached file
     */
    private void loadFromCache(Path cacheFile)
    {
        try
        {
            // Read entire file as string first
            String jsonContent = new String(Files.readAllBytes(cacheFile), StandardCharsets.UTF_8);

            // Parse JSON manually to handle errors gracefully
            JsonElement jsonElement;
            try
            {
                jsonElement = new JsonParser().parse(jsonContent);
            }
            catch (Exception parseException)
            {
                log.error("Cache file is corrupted, deleting and re-downloading: {}", parseException.getMessage());

                // Delete corrupted cache
                try
                {
                    Files.delete(cacheFile);
                    log.info("Deleted corrupted cache file");
                }
                catch (IOException deleteEx)
                {
                    log.warn("Failed to delete corrupted cache", deleteEx);
                }

                // Try to download fresh data
                downloadDatabase(cacheFile);
                return;
            }

            if (!jsonElement.isJsonObject())
            {
                log.error("Invalid JSON format - expected object");
                return;
            }

            JsonObject root = jsonElement.getAsJsonObject();

            // Check if there's an "npcs" wrapper object (new format)
            JsonObject npcsObject;
            if (root.has("npcs") && root.get("npcs").isJsonObject())
            {
                npcsObject = root.getAsJsonObject("npcs");
                log.debug("Found 'npcs' wrapper in database");
            }
            else
            {
                // Fallback: treat root as the npcs object (old format compatibility)
                npcsObject = root;
                log.debug("No 'npcs' wrapper found, using root object");
            }

            npcStatsCache.clear();
            npcStatsByKey.clear(); // Clear the variant key cache too
            int successCount = 0;
            int failCount = 0;

            // Iterate through each NPC entry in the npcs object
            for (Map.Entry<String, JsonElement> entry : npcsObject.entrySet())
            {
                String entryKey = entry.getKey();

                // Skip metadata entries
                if (entryKey.equals("_metadata"))
                {
                    continue;
                }

                try
                {
                    // Try to parse the NPC stats
                    NpcCombatStats stats = gson.fromJson(entry.getValue(), NpcCombatStats.class);
                    if (stats != null)
                    {
                        // Extract the numeric ID from the key (e.g., "8059_Post_quest_Awakened" -> 8059)
                        // Or from the stats.id field if it's already set
                        int numericId = extractNumericId(entryKey, stats);

                        if (numericId > 0)
                        {
                            // Store by numeric ID (for fallback when variant not found)
                            npcStatsCache.put(numericId, stats);

                            // ALSO store by full variant key (e.g., "12223_Awakened")
                            npcStatsByKey.put(entryKey, stats);

                            successCount++;

                            log.debug("Loaded NPC: {} (numeric ID: {})", entryKey, numericId);

                            // If the ID field contains multiple IDs (comma-separated), cache for all
                            if (stats.getId() != null && stats.getId().contains(","))
                            {
                                String[] ids = stats.getId().split(",");
                                for (String idStr : ids)
                                {
                                    try
                                    {
                                        int additionalId = Integer.parseInt(idStr.trim());
                                        if (additionalId != numericId) // Don't duplicate the primary ID
                                        {
                                            npcStatsCache.put(additionalId, stats);
                                            log.debug("Added alternate ID {} for {}", additionalId, stats.getName());
                                        }
                                    }
                                    catch (NumberFormatException nfe)
                                    {
                                        log.debug("Skipping invalid alternate ID: {}", idStr);
                                    }
                                }
                            }
                        }
                        else
                        {
                            log.debug("Could not extract numeric ID from key: {}", entryKey);
                            failCount++;
                        }
                    }
                }
                catch (Exception parseEx)
                {
                    // Skip this NPC if it can't be parsed
                    failCount++;
                    if (failCount <= 10) // Log first 10 failures for debugging
                    {
                        log.debug("Failed to parse NPC {}: {}", entryKey, parseEx.getMessage());
                    }
                }
            }

            isLoaded = true;
            log.info("Loaded {} NPC entries from database ({} failed to parse)", successCount, failCount);
        }
        catch (Exception e)
        {
            log.error("Failed to load NPC stats from cache", e);
        }
    }

    /**
     * Extract numeric NPC ID from the entry key or stats object
     * Key format examples: "8059_Post_quest_Awakened", "2042_Serpentine_"
     *
     * @param entryKey The JSON key (e.g., "8059_Post_quest_Awakened")
     * @param stats The parsed NPC stats object
     * @return Numeric ID, or -1 if not found
     */
    private int extractNumericId(String entryKey, NpcCombatStats stats)
    {
        // First try: Parse the ID from the key (format: "ID_Version_Phase")
        if (entryKey != null && entryKey.contains("_"))
        {
            String idPart = entryKey.split("_")[0];
            try
            {
                return Integer.parseInt(idPart);
            }
            catch (NumberFormatException e)
            {
                // Fall through to next method
            }
        }

        // Second try: Get the first ID from the stats.id field
        if (stats.getId() != null)
        {
            // Handle comma-separated IDs (e.g., "8059,8061")
            String idStr = stats.getId().contains(",")
                    ? stats.getId().split(",")[0].trim()
                    : stats.getId();

            try
            {
                return Integer.parseInt(idStr);
            }
            catch (NumberFormatException e)
            {
                log.debug("Could not parse ID from stats: {}", stats.getId());
            }
        }

        // Could not extract ID
        return -1;
    }

    /**
     * Get NPC combat stats by NPC object (variant-aware)
     * This is the primary method that should be used
     */
    public NpcCombatStats getNpcStats(NPC npc)
    {
        if (!isLoaded || npc == null)
        {
            log.warn("NPC stats provider not loaded yet or NPC is null");
            return null;
        }

        // Get variant-specific key using BossVariantHelper
        String variantKey = bossVariantHelper != null ?
                bossVariantHelper.getNpcDatabaseKey(npc) :
                String.valueOf(npc.getId());

        // Try variant-specific lookup first
        NpcCombatStats stats = npcStatsByKey.get(variantKey);

        if (stats != null)
        {
            log.debug("Found variant-specific stats for: {}", variantKey);
            return stats;
        }

        // Fallback to numeric ID lookup
        int npcId = npc.getId();
        stats = npcStatsCache.get(npcId);

        if (stats != null)
        {
            log.debug("Using base stats for NPC ID: {} (no variant found for {})", npcId, variantKey);
        }
        else
        {
            log.debug("No stats found for NPC ID: {}, variant: {}", npcId, variantKey);
        }

        return stats;
    }

    /**
     * Get NPC combat stats by ID (fallback method)
     * Use getNpcStats(NPC) instead when possible for variant support
     */
    public NpcCombatStats getNpcStats(int npcId)
    {
        if (!isLoaded)
        {
            log.warn("NPC stats provider not loaded yet");
            return null;
        }

        return npcStatsCache.get(npcId);
    }

    /**
     * Check if provider is loaded and ready
     */
    public boolean isLoaded()
    {
        return isLoaded;
    }

    /**
     * Get number of cached NPCs
     */
    public int getCachedNpcCount()
    {
        return npcStatsCache.size();
    }
}