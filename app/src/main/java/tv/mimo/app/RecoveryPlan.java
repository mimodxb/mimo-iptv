package tv.mimo.app;

import android.util.Log;
import java.util.*;

/** One backend refresh per session; every URL/header combination is tried at most once.
 *  Extended with stall detection for priority channels (Xəzər/Space). */
public final class RecoveryPlan {
    public static final int MAX_ATTEMPTS = 6;
    
    private final Set<String> attempted = new HashSet<>();
    private boolean refreshed;
    
    // Stall detection for priority channels
    private long playbackStartTime = 0;
    private int stallCount = 0;
    private long lastStallTime = 0;
    private static final int MAX_STALLS_BEFORE_FALLBACK = 3;
    private long lastPosition = -1;
    private long lastStallCheckTime = 0;
    private boolean currentlyPlaying = false;
    private boolean isPriorityChannel = false;
    
    /** Call when playback genuinely starts (STATE_READY + playing) for a priority channel */
    public void onPlaybackStarted(boolean isPriority) {
        playbackStartTime = System.currentTimeMillis();
        lastPosition = 0;
        lastStallCheckTime = System.currentTimeMillis();
        stallCount = 0;
        lastStallTime = 0;
        currentlyPlaying = true;
        isPriorityChannel = isPriority;
    }
    
    /** Call when playback is paused or stopped - suspends stall detection */
    public void onPlaybackPaused() {
        currentlyPlaying = false;
    }
    
    /** Call when playback resumes - restarts warm-up period */
    public void onPlaybackResumed() {
        if (currentlyPlaying) return; // Already playing
        playbackStartTime = System.currentTimeMillis();
        lastPosition = 0;
        lastStallCheckTime = System.currentTimeMillis();
        stallCount = 0;
        lastStallTime = 0;
        currentlyPlaying = true;
    }
    
    /** Call when playback stops completely */
    public void onPlaybackStopped() {
        currentlyPlaying = false;
        isPriorityChannel = false;
    }
    
    /** Call when switching to a new stream - resets all stall state */
    public void onStreamChanged(boolean isPriority) {
        playbackStartTime = 0;
        lastPosition = -1;
        lastStallCheckTime = 0;
        stallCount = 0;
        lastStallTime = 0;
        currentlyPlaying = false;
        isPriorityChannel = isPriority;
    }
    
    /** Call periodically with current playback position to detect stalls.
     *  Returns true if a stall is detected and fallback should be triggered.
     *  Only runs for priority channels that are currently playing. */
    public boolean checkForStall(long currentPositionMs) {
        // Only check for priority channels that are currently playing
        if (!isPriorityChannel || !currentlyPlaying) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        
        // Only check after playback has been running for a bit (10s warm-up)
        if (playbackStartTime == 0 || now - playbackStartTime < 10000) {
            lastPosition = currentPositionMs;
            lastStallCheckTime = now;
            return false;
        }
        
        // Check if position has advanced since last check
        if (currentPositionMs > lastPosition) {
            // Position advanced, reset stall tracking
            lastPosition = currentPositionMs;
            lastStallCheckTime = now;
            stallCount = 0; // Reset stall count on progress
            return false;
        }
        
        // No progress - check if we've been stalled long enough (3s threshold)
        if (now - lastStallCheckTime >= 3000) {
            stallCount++;
            lastStallTime = now;
            lastStallCheckTime = now;
            android.util.Log.w("MIMO_DIAG", "RecoveryPlan: Stall detected (" + stallCount + "/" + 3 + ")");
            
            if (stallCount >= 3) {
                android.util.Log.w("MIMO_DIAG", "RecoveryPlan: Max stalls reached, triggering fallback");
                return true;
            }
        }
        
        return false;
    }
    
    /** Check if we should trigger fallback due to stalls */
    public boolean shouldFallbackDueToStalls() {
        return stallCount >= 3;
    }
    
    public boolean claimRefresh() { if (refreshed) return false; refreshed = true; return true; }
    public int attempts() { return attempted.size(); }
    
    public Channel.Stream next(List<Channel.Stream> streams) {
        if (attempted.size() >= MAX_ATTEMPTS) return null;
        for (Channel.Stream stream : streams) if (attempted.add(stream.identity())) return stream;
        return null;
    }
}