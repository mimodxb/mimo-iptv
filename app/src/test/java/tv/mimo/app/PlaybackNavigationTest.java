package tv.mimo.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackNavigationTest {
    @Test public void next_wraps(){assertEquals(1,PlaybackActivity.nextIndex(0,5));}
    @Test public void next_last_to_first(){assertEquals(0,PlaybackActivity.nextIndex(4,5));}
    @Test public void next_single(){assertEquals(0,PlaybackActivity.nextIndex(0,1));}
    @Test public void next_empty(){assertEquals(-1,PlaybackActivity.nextIndex(0,0));}
    @Test public void next_middle(){assertEquals(3,PlaybackActivity.nextIndex(2,5));}
    @Test public void prev_wraps(){assertEquals(4,PlaybackActivity.prevIndex(0,5));}
    @Test public void prev_first_to_last(){assertEquals(3,PlaybackActivity.prevIndex(4,5));}
    @Test public void prev_single(){assertEquals(0,PlaybackActivity.prevIndex(0,1));}
    @Test public void prev_empty(){assertEquals(-1,PlaybackActivity.prevIndex(0,0));}
    @Test public void prev_middle(){assertEquals(1,PlaybackActivity.prevIndex(2,5));}
    @Test public void next_two_items(){assertEquals(1,PlaybackActivity.nextIndex(0,2));assertEquals(0,PlaybackActivity.nextIndex(1,2));}
    @Test public void prev_two_items(){assertEquals(1,PlaybackActivity.prevIndex(0,2));assertEquals(0,PlaybackActivity.prevIndex(1,2));}
}
