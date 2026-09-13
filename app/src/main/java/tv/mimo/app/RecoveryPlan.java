package tv.mimo.app;
import java.util.*;

/** One backend refresh per session; every URL/header combination is tried at most once. */
public final class RecoveryPlan {
    public static final int MAX_ATTEMPTS = 6;
    private final Set<String> attempted = new HashSet<>();
    private boolean refreshed;
    public Channel.Stream next(List<Channel.Stream> streams) {
        if (attempted.size() >= MAX_ATTEMPTS) return null;
        for (Channel.Stream stream : streams) if (attempted.add(stream.identity())) return stream;
        return null;
    }
    public boolean claimRefresh() { if (refreshed) return false; refreshed = true; return true; }
    public int attempts() { return attempted.size(); }
}
