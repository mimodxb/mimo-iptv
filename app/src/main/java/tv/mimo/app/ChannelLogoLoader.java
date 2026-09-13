package tv.mimo.app;

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
    private static final ExecutorService exec = Executors.newFixedThreadPool(2);
    private static final Handler main = new Handler(Looper.getMainLooper());

    /** Placeholder silhouette for missing/failed logos. */
    static Bitmap placeholder(int w, int h) {
        Bitmap b = Bitmap.createBitmap(Math.max(w,1), Math.max(h,1), CFG);
        Canvas c = new Canvas(b);
        c.drawColor(TvStyle.PANEL);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(TvStyle.MUTED); p.setTextSize(Math.min(w,h)*0.4f); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("📺", w/2f, h*0.62f, p);
        return b;
    }

    /** Load URL into ImageView asynchronously. Falls back to placeholder. */
    public static void load(ImageView view, String url, int w, int h) {
        if (url == null || url.isEmpty()) { view.setImageBitmap(placeholder(w, h)); return; }
        Bitmap cached = cache.get(url);
        if (cached != null) { view.setImageBitmap(cached); return; }
        view.setImageBitmap(placeholder(w, h));
        exec.execute(() -> {
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
                        main.post(() -> view.setImageBitmap(scaled));
                        return;
                    }
                }
            } catch (Exception ignored) {}
            main.post(() -> view.setImageBitmap(placeholder(w, h)));
        });
    }

    /** Cancel pending loads (call in onDestroy). */
    public static void cancel() { exec.shutdownNow(); }
}
