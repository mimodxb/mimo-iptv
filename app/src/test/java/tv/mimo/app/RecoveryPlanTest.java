package tv.mimo.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class RecoveryPlanTest {
    private Channel.Stream stream(String url){return new Channel.Stream(url,"mimo",Collections.emptyMap());}
    @Test public void sameBackendUrlIsNotRetriedForever(){RecoveryPlan p=new RecoveryPlan();List<Channel.Stream> c=Arrays.asList(stream("https://a/live"),stream("https://a/live"));assertNotNull(p.next(c));assertNull(p.next(c));assertEquals(1,p.attempts());}
    @Test public void backendCanRefreshOnlyOnce(){RecoveryPlan p=new RecoveryPlan();assertTrue(p.claimRefresh());assertFalse(p.claimRefresh());}
    @Test public void freshBackendChoicePrecedesExtraPlaylist(){RecoveryPlan p=new RecoveryPlan();p.next(Arrays.asList(stream("https://old/live")));List<Channel.Stream> refreshed=Arrays.asList(stream("https://new/live"),stream("https://old/live"),stream("https://extra/live"));assertEquals("https://new/live",p.next(refreshed).url);assertEquals("https://extra/live",p.next(refreshed).url);assertNull(p.next(refreshed));}
    @Test public void emptyDegradedSourceTerminates(){RecoveryPlan p=new RecoveryPlan();assertNull(p.next(Collections.emptyList()));assertTrue(p.claimRefresh());assertNull(p.next(Collections.emptyList()));assertFalse(p.claimRefresh());}
    @Test public void manyPlaylistAlternativesAreCapped(){RecoveryPlan p=new RecoveryPlan();List<Channel.Stream> sources=new ArrayList<>();for(int i=0;i<10;i++)sources.add(stream("https://a/"+i));for(int i=0;i<6;i++)assertNotNull(p.next(sources));assertNull(p.next(sources));assertEquals(6,p.attempts());}
}
