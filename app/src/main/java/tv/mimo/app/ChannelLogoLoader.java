package tv.mimo.app;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/** Lightweight async channel-logo loader with in-memory LRU cache. No external dependencies. */
public final class ChannelLogoLoader {
    private static final int MAX_CACHE = 60;
    private static final int TIMEOUT_MS = 8000;
    private static final Bitmap.Config CFG = Bitmap.Config.RGB_565;
    private static final LruCache<String, Bitmap> cache = new LruCache<>(MAX_CACHE);
    private static volatile ExecutorService exec;
    private static final Handler main = new Handler(Looper.getMainLooper());

    private static ExecutorService exec() {
        if (exec == null || exec.isShutdown()) {
            synchronized (ChannelLogoLoader.class) {
                if (exec == null || exec.isShutdown()) exec = Executors.newFixedThreadPool(2);
            }
        }
        return exec;
    }

    /** Placeholder silhouette for missing/failed logos. */
    public static Bitmap placeholder(int w, int h) {
        Bitmap b = Bitmap.createBitmap(Math.max(w,1), Math.max(h,1), CFG);
        Canvas c = new Canvas(b);
        c.drawColor(TvStyle.PANEL);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(TvStyle.MUTED); p.setTextSize(Math.min(w,h)*0.4f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("\u25A3", w/2f, h*0.62f, p);
        return b;
    }

    /** Load URL into ImageView asynchronously. Falls back to placeholder.
     *  Uses tag-based reuse protection: a delayed response is only applied if
     *  the ImageView's tag still matches the requested URL.
     *  Preserves aspect ratio using FIT_CENTER-style scaling.
     *  @param view Target ImageView (will be set to FIT_CENTER scaleType)
     *  @param url Logo URL
     *  @param widthDp Target width in dp
     *  @param heightDp Target height in dp
     *  @param context Context for density conversion
     */
    public static void load(ImageView view, String url, int widthDp, int heightDp, Context context) {
        if (url == null || url.isEmpty()) { 
            view.setImageBitmap(placeholder(dp(context, widthDp), dp(context, heightDp))); 
            view.setTag(null); 
            return; 
        }
        // Ensure FIT_CENTER to preserve aspect ratio
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setAdjustViewBounds(true);
        
        int targetW = dp(context, widthDp);
        int targetH = dp(context, heightDp);
        Bitmap cached = cache.get(url);
        if (cached != null) { 
            view.setImageBitmap(cached); 
            view.setTag(url); 
            return; 
        }
        view.setImageBitmap(placeholder(dp(context, widthDp), dp(context, heightDp)));
        view.setTag(url);
        final String tag = url;
        exec().execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(TIMEOUT_MS); conn.setReadTimeout(TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", "MIMO-TV/0.1 AndroidTV");
                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    Bitmap raw = BitmapFactory.decodeStream(in);
                    in.close();
                    if (raw != null) {
                        // Scale preserving aspect ratio - fit within target bounds
                        Bitmap scaled = scalePreservingAspectRatio(raw, targetW, targetH);
                        if (scaled != raw) raw.recycle();
                        cache.put(url, scaled);
                        main.post(() -> { if (tag.equals(view.getTag())) view.setImageBitmap(scaled); });
                        return;
                    }
                }
            } catch (Exception ignored) {}
            main.post(() -> { if (tag.equals(view.getTag())) view.setImageBitmap(placeholder(dp(context, widthDp), dp(context, heightDp))); });
        });
    }

    /** Backward-compatible overload for existing callers */
    public static void load(ImageView view, String url, int w, int h) {
        // Use a default context - will be overridden by the new overload in updated callers
        if (view.getContext() != null) {
            load(view, url, w, h, view.getContext());
        } else {
            // Fallback to old behavior if no context
            loadLegacy(view, url, w, h);
        }
    }

    /** Legacy implementation for backward compatibility */
    private static void loadLegacy(ImageView view, String url, int w, int h) {
        if (url == null || url.isEmpty()) { view.setImageBitmap(placeholder(w, h)); view.setTag(null); return; }
        Bitmap cached = cache.get(url);
        if (cached != null) { view.setImageBitmap(cached); view.setTag(url); return; }
        view.setImageBitmap(placeholder(w, h));
        view.setTag(url);
        final String tag = url;
        exec().execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(TIMEOUT_MS); conn.setReadTimeout(TIMEOUT_MS);
                conn.setRequestProperty("User-Agent", "MIMO-TV/0.1 AndroidTV");
                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    Bitmap raw = BitmapFactory.decodeStream(in);
                    in.close();
                    if (raw != null) {
                        Bitmap scaled = Bitmap.createScaledBitmap(raw, w, h, true);
                        if (scaled != raw) raw.recycle();
                        cache.put(url, scaled);
                        main.post(() -> { if (tag.equals(view.getTag())) view.setImageBitmap(scaled); });
                        return;
                    }
                }
            } catch (Exception ignored) {}
            main.post(() -> { if (tag.equals(view.getTag())) view.setImageBitmap(placeholder(w, h)); });
        });
    }

    /** Scale bitmap preserving aspect ratio, fitting within target bounds (FIT_CENTER equivalent) */
    private static Bitmap scalePreservingAspectRatio(Bitmap src, int targetW, int targetH) {
        if (src == null) return null;
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        if (srcW <= 0 || srcH <= 0) return src;
        
        float scale = Math.min((float) targetW / srcW, (float) targetH / srcH);
        int newW = Math.max(1, Math.round(srcW * scale));
        int newH = Math.max(1, Math.round(srcH * scale));
        
        if (newW == srcW && newH == srcH) return src;
        
        Bitmap scaled = Bitmap.createScaledBitmap(src, newW, newH, true);
        return scaled;
    }

    /** Convert dp to pixels */
    public static int dp(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    /** Cancel pending loads for Activity destruction. Does NOT shut down the
     *  shared executor — it persists across Activity recreations. */
    public static void cancel() { /* no-op: executor is shared and persistent */ }
}