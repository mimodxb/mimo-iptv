package tv.mimo.app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class InternalFallbackFeedTest {

    private Channel.Stream stream(String url, String source) {
        return new Channel.Stream(url, source, Collections.emptyMap());
    }

    private Channel channel(String tvgId, String name, String country, String... urls) {
        Channel c = new Channel(tvgId, name, country, "Azerbaijan", "", "mimo");
        for (String url : urls) c.streams.add(stream(url, "mimo"));
        return c;
    }

    @Test
    public void primaryMimoStreamStaysFirst() {
        List<Channel> existing = new ArrayList<>();
        existing.add(channel("IdmanTV.az", "Idman TV", "AZ",
            "http://213.239.195.222/azerbaijan/idman_stream_sd_2023/playlist.m3u8"));

        List<Channel> fallbacks = new ArrayList<>();
        // Simulates post-preprocessing: @SD suffix stripped by InternalFallbackFeed
        fallbacks.add(channel("IdmanTV.az", "Idman TV", "AZ",
            "https://live.itv.az/idman.m3u8"));

        InternalFallbackFeed.mergeInto(existing, fallbacks);

        assertEquals(1, existing.size());
        assertEquals(2, existing.get(0).streams.size());
        assertEquals("http://213.239.195.222/azerbaijan/idman_stream_sd_2023/playlist.m3u8",
            existing.get(0).streams.get(0).url);
        assertEquals("https://live.itv.az/idman.m3u8",
            existing.get(0).streams.get(1).url);
    }

    @Test
    public void secondaryStreamsMergeIntoSameChannel() {
        List<Channel> existing = new ArrayList<>();
        existing.add(channel("ARB24.az", "ARB 24", "AZ",
            "http://85.132.81.184:8080/arb24/live1/index.m3u8"));

        List<Channel> fallbacks = new ArrayList<>();
        fallbacks.add(channel("ARB24.az", "ARB 24", "AZ",
            "http://erlyvideo.izone.az:80/arb24/mono.m3u8"));

        InternalFallbackFeed.mergeInto(existing, fallbacks);

        assertEquals(1, existing.size());
        assertEquals(2, existing.get(0).streams.size());
        assertEquals("http://85.132.81.184:8080/arb24/live1/index.m3u8",
            existing.get(0).streams.get(0).url);
        assertEquals("http://erlyvideo.izone.az:80/arb24/mono.m3u8",
            existing.get(0).streams.get(1).url);
    }

    @Test
    public void duplicateUrlsAreRemoved() {
        List<Channel> existing = new ArrayList<>();
        existing.add(channel("XezerTV.az", "Xezer TV", "AZ",
            "https://www.xezerxeber.az/stream/index.m3u8"));

        List<Channel> fallbacks = new ArrayList<>();
        fallbacks.add(channel("XezerTV.az", "Xezer TV", "AZ",
            "https://www.xezerxeber.az/stream/index.m3u8"));

        InternalFallbackFeed.mergeInto(existing, fallbacks);

        assertEquals(1, existing.size());
        assertEquals(1, existing.get(0).streams.size());
    }

    @Test
    public void duplicateChannelCardsAreNotCreated() {
        List<Channel> existing = new ArrayList<>();
        existing.add(channel("XezerTV.az", "Xezer TV", "AZ", "https://a/live"));
        existing.add(channel("ARB24.az", "ARB 24", "AZ", "https://b/live"));

        List<Channel> fallbacks = new ArrayList<>();
        fallbacks.add(channel("XezerTV.az", "Xezer TV", "AZ", "https://c/live"));
        fallbacks.add(channel("ARB24.az", "ARB 24", "AZ", "https://d/live"));

        InternalFallbackFeed.mergeInto(existing, fallbacks);

        assertEquals(2, existing.size());
        assertEquals("xezertv.az", existing.get(0).key);
        assertEquals("arb24.az", existing.get(1).key);
    }

    @Test
    public void fallbackFeedFailureDoesNotBreakCatalog() {
        List<Channel> existing = new ArrayList<>();
        existing.add(channel("XezerTV.az", "Xezer TV", "AZ", "https://a/live"));
        existing.add(channel("ARB24.az", "ARB 24", "AZ", "https://b/live"));

        // Null fallback — should not crash
        InternalFallbackFeed.mergeInto(existing, null);
        assertEquals(2, existing.size());
        assertEquals(1, existing.get(0).streams.size());

        // Empty fallback — should not crash
        InternalFallbackFeed.mergeInto(existing, Collections.emptyList());
        assertEquals(2, existing.size());
        assertEquals(1, existing.get(0).streams.size());
    }

    @Test
    public void unrelatedRussiaTurkeyEuropeWorldChannelsUnaffected() {
        List<Channel> existing = new ArrayList<>();
        Channel ru = new Channel("russia1.ru", "Russia 1", "RU", "Russia", "", "mimo");
        ru.streams.add(stream("https://ru/live", "mimo"));
        Channel tr = new Channel("trt1.tr", "TRT 1", "TR", "Turkey", "", "mimo");
        tr.streams.add(stream("https://tr/live", "mimo"));
        Channel de = new Channel("ard.de", "ARD", "DE", "Europe", "", "mimo");
        de.streams.add(stream("https://de/live", "mimo"));
        Channel world = new Channel("bbcc.co.uk", "BBC", "GB", "World", "", "mimo");
        world.streams.add(stream("https://gb/live", "mimo"));

        existing.add(ru); existing.add(tr); existing.add(de); existing.add(world);

        List<Channel> fallbacks = new ArrayList<>();
        fallbacks.add(channel("XezerTV.az", "Xezer TV", "AZ", "https://az/live"));

        InternalFallbackFeed.mergeInto(existing, fallbacks);

        assertEquals(4, existing.size());
        assertEquals(1, existing.get(0).streams.size()); // Russia unchanged
        assertEquals(1, existing.get(1).streams.size()); // Turkey unchanged
        assertEquals(1, existing.get(2).streams.size()); // Europe unchanged
        assertEquals(1, existing.get(3).streams.size()); // World unchanged
    }

    @Test
    public void fallbackChannelNotInPrimaryIsSkipped() {
        List<Channel> existing = new ArrayList<>();
        existing.add(channel("XezerTV.az", "Xezer TV", "AZ", "https://a/live"));

        List<Channel> fallbacks = new ArrayList<>();
        fallbacks.add(channel("Unknown.az", "Unknown", "AZ", "https://unknown/live"));

        InternalFallbackFeed.mergeInto(existing, fallbacks);

        assertEquals(1, existing.size());
        assertEquals("xezertv.az", existing.get(0).key);
    }

    @Test
    public void multipleFallbackUrlsMergeCorrectly() {
        List<Channel> existing = new ArrayList<>();
        existing.add(channel("IdmanTV.az", "Idman TV", "AZ", "http://primary/live"));

        List<Channel> fallbacks = new ArrayList<>();
        Channel fb = new Channel("IdmanTV.az", "Idman TV", "AZ", "Azerbaijan", "", "fallback");
        fb.streams.add(stream("http://fallback1/live", "fallback"));
        fb.streams.add(stream("http://fallback2/live", "fallback"));
        fb.streams.add(stream("http://fallback3/live", "fallback"));
        fallbacks.add(fb);

        InternalFallbackFeed.mergeInto(existing, fallbacks);

        assertEquals(1, existing.size());
        assertEquals(4, existing.get(0).streams.size());
        assertEquals("http://primary/live", existing.get(0).streams.get(0).url);
        assertEquals("http://fallback1/live", existing.get(0).streams.get(1).url);
        assertEquals("http://fallback2/live", existing.get(0).streams.get(2).url);
        assertEquals("http://fallback3/live", existing.get(0).streams.get(3).url);
    }

    @Test
    public void recoveryPlanCanTraverseAllCandidates() {
        RecoveryPlan plan = new RecoveryPlan();
        List<Channel.Stream> candidates = new ArrayList<>();
        candidates.add(stream("http://primary/live", "mimo"));
        candidates.add(stream("http://fallback1/live", "fallback"));
        candidates.add(stream("http://fallback2/live", "fallback"));

        assertNotNull(plan.next(candidates));
        assertNotNull(plan.next(candidates));
        assertNotNull(plan.next(candidates));
        // MAX_ATTEMPTS is 6, but only 3 unique candidates
        assertNull(plan.next(candidates));
    }

    @Test
    public void suffixStrippingRegexWorks() {
        String input = "#EXTINF:-1 tvg-id=\"IdmanTV.az@SD\" tvg-country=\"AZ\",Idman TV";
        String expected = "#EXTINF:-1 tvg-id=\"IdmanTV.az\" tvg-country=\"AZ\",Idman TV";
        String cleaned = InternalFallbackFeed.TVG_ID_SUFFIX.matcher(input).replaceAll("tvg-id=\"$1\"");
        assertEquals(expected, cleaned);

        // Should not strip suffixes without @
        String input2 = "#EXTINF:-1 tvg-id=\"IdmanTV.az\" tvg-country=\"AZ\",Idman TV";
        String cleaned2 = InternalFallbackFeed.TVG_ID_SUFFIX.matcher(input2).replaceAll("tvg-id=\"$1\"");
        assertEquals(input2, cleaned2);

        // Should strip @HD and @FHD too
        String input3 = "tvg-id=\"Test.az@HD\"";
        assertEquals("tvg-id=\"Test.az\"", InternalFallbackFeed.TVG_ID_SUFFIX.matcher(input3).replaceAll("tvg-id=\"$1\""));

        String input4 = "tvg-id=\"Test.az@FHD\"";
        assertEquals("tvg-id=\"Test.az\"", InternalFallbackFeed.TVG_ID_SUFFIX.matcher(input4).replaceAll("tvg-id=\"$1\""));
    }
}
