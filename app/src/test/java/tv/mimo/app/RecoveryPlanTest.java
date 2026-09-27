package tv.mimo.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class RecoveryPlanTest {
    @Test public void recurringBufferingTriggersBoundedFallback(){
        RecoveryPlan p=new RecoveryPlan();p.onStreamChanged(true);
        assertFalse(p.recordBuffering(0));p.onPlaybackStarted(true);
        assertFalse(p.recordBuffering(1000));assertFalse(p.recordBuffering(90000));assertTrue(p.recordBuffering(180000));
        p.onStreamChanged(true);assertFalse(p.recordBuffering(190000));
    }
    @Test public void isolatedInterruptionsAndOrdinaryChannelsDoNotTriggerEarlyFallback(){
        RecoveryPlan p=new RecoveryPlan();p.onPlaybackStarted(true);
        assertFalse(p.recordBuffering(0));assertFalse(p.recordBuffering(301000));assertFalse(p.recordBuffering(602000));
        p.onStreamChanged(false);p.onPlaybackStarted(false);
        for(int i=0;i<5;i++)assertFalse(p.recordBuffering(i));
    }
    private Channel.Stream stream(String url){return new Channel.Stream(url,"mimo",Collections.emptyMap());}
    @Test public void sameBackendUrlIsNotRetriedForever(){RecoveryPlan p=new RecoveryPlan();List<Channel.Stream> c=Arrays.asList(stream("https://a/live"),stream("https://a/live"));assertNotNull(p.next(c));assertNull(p.next(c));assertEquals(1,p.attempts());}
    @Test public void backendCanRefreshOnlyOnce(){RecoveryPlan p=new RecoveryPlan();assertTrue(p.claimRefresh());assertFalse(p.claimRefresh());}
    @Test public void freshBackendChoicePrecedesExtraPlaylist(){RecoveryPlan p=new RecoveryPlan();p.next(Arrays.asList(stream("https://old/live")));List<Channel.Stream> refreshed=Arrays.asList(stream("https://new/live"),stream("https://old/live"),stream("https://extra/live"));assertEquals("https://new/live",p.next(refreshed).url);assertEquals("https://extra/live",p.next(refreshed).url);assertNull(p.next(refreshed));}
    @Test public void emptyDegradedSourceTerminates(){RecoveryPlan p=new RecoveryPlan();assertNull(p.next(Collections.emptyList()));assertTrue(p.claimRefresh());assertNull(p.next(Collections.emptyList()));assertFalse(p.claimRefresh());}
    @Test public void manyPlaylistAlternativesAreCapped(){RecoveryPlan p=new RecoveryPlan();List<Channel.Stream> sources=new ArrayList<>();for(int i=0;i<10;i++)sources.add(stream("https://a/"+i));for(int i=0;i<6;i++)assertNotNull(p.next(sources));assertNull(p.next(sources));assertEquals(6,p.attempts());}
}
