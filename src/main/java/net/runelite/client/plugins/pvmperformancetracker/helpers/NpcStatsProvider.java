package net.runelite.client.plugins.pvmperformancetracker.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
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
 * Loads and caches NPC combat stats from OSRSBox database
 */
@Slf4j
public class NpcStatsProvider
{
    private static final String OSRSBOX_MONSTERS_URL = "https://raw.githubusercontent.com/osrsbox/osrsbox-db/master/docs/monsters-complete.json";
    private static final String CACHE_FILE_NAME = "osrsbox-monsters.json";

    private final Gson gson;
    private final Path cacheDirectory;
    private final Map<Integer, NpcCombatStats> npcStatsCache;
    private boolean isLoaded = false;

    public NpcStatsProvider(File runeLiteDirectory)
    {
        // Use lenient Gson to handle missing/null fields
        this.gson = new GsonBuilder()
                .setLenient()
                .create();
        this.npcStatsCache = new HashMap<>();

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
        log.info("Downloading OSRSBox monster database...");
        downloadDatabase(cacheFile);
    }

    /**
     * Download the database from OSRSBox
     */
    private void downloadDatabase(Path cacheFile)
    {
        try
        {
            log.info("Downloading OSRSBox database from: {}", OSRSBOX_MONSTERS_URL);

            URL url = new URL(OSRSBOX_MONSTERS_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000); // 2 minute timeout for large file
            connection.setRequestProperty("User-Agent", "RuneLite-PvMTracker/1.0");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200)
            {
                log.error("Failed to download database: HTTP {}", responseCode);
                return;
            }

            // Get content length (may be -1 if unknown)
            long contentLength = connection.getContentLengthLong();
            log.info("Downloading {} bytes...", contentLength > 0 ? contentLength : "unknown size");

            // Download with larger buffer and explicit flushing
            try (InputStream in = connection.getInputStream();
                 BufferedInputStream bufferedIn = new BufferedInputStream(in, 65536);
                 OutputStream out = new BufferedOutputStream(Files.newOutputStream(cacheFile), 65536))
            {
                byte[] buffer = new byte[65536]; // 64KB buffer
                int bytesRead;
                long totalBytesRead = 0;
                int lastLoggedMB = 0;

                while ((bytesRead = bufferedIn.read(buffer)) != -1)
                {
                    out.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Log progress every MB
                    int currentMB = (int)(totalBytesRead / 1000000);
                    if (currentMB > lastLoggedMB)
                    {
                        log.info("Downloaded {} MB...", currentMB);
                        lastLoggedMB = currentMB;
                    }
                }

                // Ensure everything is written
                out.flush();

                log.info("Successfully downloaded OSRSBox database ({} bytes, {} MB)",
                        totalBytesRead, totalBytesRead / 1000000);

                // Verify minimum size (OSRSBox DB should be at least 5MB)
                if (totalBytesRead < 5000000)
                {
                    log.error("Download too small: got {} bytes, expected at least 5MB", totalBytesRead);
                    Files.delete(cacheFile);
                    return;
                }
            }

            // Load the downloaded data
            loadFromCache(cacheFile);
        }
        catch (Exception e)
        {
            log.error("Failed to download OSRSBox database", e);

            // Delete partial download
            try
            {
                if (Files.exists(cacheFile))
                {
                    Files.delete(cacheFile);
                }
            }
            catch (IOException deleteEx)
            {
                log.warn("Failed to delete partial download", deleteEx);
            }
        }
    }

    /**
     * Load database from cached file
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

            npcStatsCache.clear();
            int successCount = 0;
            int failCount = 0;

            // Iterate through each NPC entry
            for (Map.Entry<String, JsonElement> entry : root.entrySet())
            {
                try
                {
                    int id = Integer.parseInt(entry.getKey());

                    // Try to parse the NPC stats
                    try
                    {
                        NpcCombatStats stats = gson.fromJson(entry.getValue(), NpcCombatStats.class);
                        if (stats != null)
                        {
                            stats.setId(id);
                            npcStatsCache.put(id, stats);
                            successCount++;
                        }
                    }
                    catch (Exception parseEx)
                    {
                        // Skip this NPC if it can't be parsed
                        failCount++;
                        if (failCount <= 10) // Log first 10 failures for debugging
                        {
                            log.debug("Failed to parse NPC {}: {}", id, parseEx.getMessage());
                        }
                    }
                }
                catch (NumberFormatException e)
                {
                    log.warn("Invalid NPC ID: {}", entry.getKey());
                }
            }

            isLoaded = true;
            log.info("Loaded {} NPC entries from OSRSBox database ({} failed to parse)", successCount, failCount);
        }
        catch (Exception e)
        {
            log.error("Failed to load NPC stats from cache", e);
        }
    }

    /**
     * Get NPC combat stats by ID
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