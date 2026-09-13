package tv.mimo.app;

import android.util.Log;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

/**
 * Internal secondary candidate source for Azerbaijan channels.
 * Fetched from iptv-org/iptv; merged as fallback-only into existing channels.
 * Never prevents the main Mimo catalogue from loading.
 */
public final class InternalFallbackFeed {
    private static final String TAG = "MIMO_DIAG";
    private static final String FEED_URL = "https://iptv-org.github.io/iptv/countries/az.m3u";
    private static final String CACHE_NAME = "internal_fallback_az.m3u";
    private static final long CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000; // 24 hours
    public static final Pattern TVG_ID_SUFFIX = Pattern.compile("tvg-id=\"([^\"]+?)@(?:SD|HD|FHD|UHD)\"");

    private final File cacheDir;
    private final File cacheFile;

    public InternalFallbackFeed(File filesDir) {
        cacheDir = new File(filesDir, "internal_fallback");
        if (!cacheDir.exists()) cacheDir.mkdirs();
        cacheFile = new File(cacheDir, CACHE_NAME);
    }

    /**
     * Load fallback channels. Returns empty list on any failure — never throws.
     * Reads from cache if fresh, otherwise fetches from network.
     */
    public List<Channel> load(boolean network) {
        try {
            String text = null;
            if (cacheFile.exists() && !isStale()) {
                text = readCache();
                if (text != null) Log.w(TAG, "  fallback: cache hit (" + text.length() + " chars)");
            }
            if (text == null && network) {
                text = fetchFeed();
                if (text != null) {
                    writeCache(text);
                    Log.w(TAG, "  fallback: network fetch OK (" + text.length() + " chars)");
                }
            }
            if (text == null && cacheFile.exists()) {
                text = readCache();
                if (text != null) Log.w(TAG, "  fallback: stale cache fallback (" + text.length() + " chars)");
            }
            if (text == null) {
                Log.w(TAG, "  fallback: no data available");
                return Collections.emptyList();
            }
            // Strip @SD/@HD/@FHD/@UHD suffixes from tvg-id so keys match MIMO's keys
            String cleaned = TVG_ID_SUFFIX.matcher(text).replaceAll("tvg-id=\"$1\"");
            List<Channel> parsed = M3uParser.parse(cleaned, "fallback", FEED_URL);
            Log.w(TAG, "  fallback: parsed " + parsed.size() + " channels");
            return parsed;
        } catch (Exception e) {
            Log.w(TAG, "  fallback: load failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Merge fallback alternatives into existing channels.
     * Rules:
     *  - Primary (Mimo) streams stay first in candidate order.
     *  - Only NEW URLs (by identity) are appended.
     *  - No duplicate channel cards are created.
     */
    public static void mergeInto(List<Channel> existing, List<Channel> fallbacks) {
        if (fallbacks == null || fallbacks.isEmpty()) return;
        Map<String, Channel> existingMap = new LinkedHashMap<>();
        for (Channel c : existing) existingMap.put(c.key, c);

        for (Channel fb : fallbacks) {
            Channel target = existingMap.get(fb.key);
            if (target == null) continue;
            // Inherit logo from fallback if primary has none
            if ((target.logo == null || target.logo.isEmpty()) && fb.logo != null && !fb.logo.isEmpty()) {
                target.logo = fb.logo;
            }
            for (Channel.Stream s : fb.streams) {
                if (target.streams.stream().noneMatch(x -> x.identity().equals(s.identity()))) {
                    target.streams.add(s);
                }
            }
        }
    }

    private boolean isStale() {
        return System.currentTimeMillis() - cacheFile.lastModified() > CACHE_MAX_AGE_MS;
    }

    private String readCache() {
        try (FileInputStream in = new FileInputStream(cacheFile)) {
            return Repository.readBounded(in, 4 * 1024 * 1024);
        } catch (IOException e) {
            return null;
        }
    }

    private void writeCache(String text) {
        File temp = new File(cacheFile.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            if (!temp.renameTo(cacheFile)) {
                try (FileOutputStream out2 = new FileOutputStream(cacheFile)) {
                    out2.write(text.getBytes(StandardCharsets.UTF_8));
                }
                temp.delete();
            }
        } catch (IOException ignored) { }
    }

    private String fetchFeed() {
        try {
            return Repository.fetch(FEED_URL, 4 * 1024 * 1024);
        } catch (IOException e) {
            Log.w(TAG, "  fallback: fetch failed: " + e.getMessage());
            return null;
        }
    }
}
