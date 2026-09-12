package tv.mimo.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class CatalogTest {
    private List<Channel> parse(String body)throws Exception{return M3uParser.parse("\uFEFF#EXTM3U\r\n"+body,"mimo","https://example.org/list.m3u");}
    @Test public void unicodeNamesAndQuotedCommasSurvive()throws Exception{
        Channel c=parse("#EXTINF:-1 tvg-id=\"XezerTV.az\" group-title=\"News, Azerbaijan\" tvg-country=\"AZ\",Xəzər TV\r\nhttps://host/live.m3u8").get(0);
        assertEquals("Xəzər TV",c.name);assertEquals("Azerbaijan",c.category());assertEquals("xezertv.az",c.key);
    }
    @Test public void aliasesMergeIntoPriorityIdentity()throws Exception{
        List<Channel> channels=parse("#EXTINF:-1 tvg-country=\"AZ\",Xəzər TV\nhttps://a/x\n#EXTINF:-1 tvg-country=\"AZ\",Khazar TV\nhttps://b/x");
        assertEquals(1,channels.size());assertEquals(2,channels.get(0).streams.size());
    }
    @Test public void sameNameInOtherCountryIsNotPriority()throws Exception{
        assertFalse(parse("#EXTINF:-1 tvg-country=\"BR\",Space TV\nhttps://a/x").get(0).priority());
    }
    @Test public void headerDirectivesAndRelativeUrlsAreKept()throws Exception{
        Channel.Stream s=parse("#EXTINF:-1,Test\n#EXTVLCOPT:http-user-agent=TV Client\n#EXTVLCOPT:http-referrer=https://channel.org/\n/live.m3u8").get(0).streams.get(0);
        assertEquals("https://example.org/live.m3u8",s.url);assertEquals("TV Client",s.headers.get("User-Agent"));assertEquals("https://channel.org/",s.headers.get("Referer"));
    }
    @Test public void pipeHeadersAreDecoded()throws Exception{
        Channel.Stream s=parse("#EXTINF:-1,Test\nhttps://a/live|User-Agent=TV%20Client&Referer=https%3A%2F%2Fa%2F").get(0).streams.get(0);
        assertEquals("https://a/live",s.url);assertEquals("TV Client",s.headers.get("User-Agent"));
    }
    @Test public void duplicateUrlsDoNotCauseRetryLoops()throws Exception{
        assertEquals(1,parse("#EXTINF:-1 tvg-id=\"Test.az\",Test\nhttps://a/live\n#EXTINF:-1 tvg-id=\"Test.az\",Test\nhttps://a/live").get(0).streams.size());
    }
    @Test public void countryCanComeFromTvgId()throws Exception{
        assertEquals("Russia",parse("#EXTINF:-1 tvg-id=\"One.ru\",One\nhttps://a/live").get(0).category());
        assertEquals("Turkey",parse("#EXTINF:-1 tvg-country=\"TR\",One\nhttps://a/live").get(0).category());
        assertEquals("Europe",parse("#EXTINF:-1 tvg-country=\"DE\",One\nhttps://a/live").get(0).category());
    }
    @Test(expected=java.io.IOException.class)public void rejectsHtml()throws Exception{M3uParser.parse("<html>Sign in</html>","s","https://example.org");}
    @Test(expected=java.io.IOException.class)public void rejectsHlsMasterAsChannelPlaylist()throws Exception{parse("#EXT-X-STREAM-INF:BANDWIDTH=100\nhttps://a/live");}
    @Test(expected=java.io.IOException.class)public void rejectsFileUrls()throws Exception{parse("#EXTINF:-1,Secret\nfile:///data/private");}
    @Test public void unrelatedMissingIdChannelsStaySourceScoped()throws Exception{
        List<Channel> all=parse("#EXTINF:-1,News\nhttps://a/live");
        all.addAll(M3uParser.parse("#EXTM3U\n#EXTINF:-1,News\nhttps://b/live","other","https://other.org"));assertEquals(2,M3uParser.merge(all).size());
    }
    @Test public void refreshedUrlsDoNotChangeFavoriteIdentity()throws Exception{
        assertEquals(parse("#EXTINF:-1 tvg-id=\"SpaceTV.az\",Space TV\nhttps://a/live?token=one").get(0).key,
            parse("#EXTINF:-1 tvg-id=\"SpaceTV.az\",Space TV\nhttps://a/live?token=two").get(0).key);
    }
    @Test public void mergingSourcesPreservesPrimaryAndAlternatives()throws Exception{
        List<Channel> all=parse("#EXTINF:-1 tvg-id=\"SpaceTV.az\",Space TV\nhttps://backend/live");
        all.addAll(M3uParser.parse("#EXTM3U\n#EXTINF:-1 tvg-id=\"SpaceTV.az\",Space\nhttps://extra/live","extra","https://extra.org"));
        List<Channel> merged=M3uParser.merge(all);assertEquals(1,merged.size());assertEquals("https://backend/live",merged.get(0).streams.get(0).url);assertEquals(2,merged.get(0).streams.size());
    }
}
