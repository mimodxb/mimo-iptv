package tv.mimo.app;

import android.content.*;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

public final class Repository {
    public static final String PLAYLIST = "https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv";
    public static final String EPG = "https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-merged";
    private final SharedPreferences prefs;
    private final File cache;
    public Repository(Context context) {
        prefs = context.getSharedPreferences("mimo", Context.MODE_PRIVATE);
        cache = new File(context.getFilesDir(), "playlists");
        if (!cache.exists()) cache.mkdirs();
    }
    public List<Source> sources() {
        List<Source> result = new ArrayList<>();
        String saved = prefs.getString("sources", null);
        if (saved == null) {
            result.add(new Source("mimo", "Mimo IPTV", PLAYLIST, EPG, true));
            return result;
        }
        try {
            JSONArray arr = new JSONArray(saved);
            for (int i=0; i<arr.length(); i++) {
                JSONObject j=arr.getJSONObject(i);
                result.add(new Source(j.getString("id"),j.getString("name"),j.getString("url"),j.optString("epg"),j.optBoolean("enabled",true)));
            }
        } catch (JSONException ignored) { }
        return result;
    }
    public void saveSources(List<Source> sources) {
        JSONArray arr = new JSONArray();
        for (Source s : sources) try {
            arr.put(new JSONObject().put("id",s.id).put("name",s.name).put("url",s.url).put("epg",s.epg).put("enabled",s.enabled));
        } catch (JSONException ignored) { }
        prefs.edit().putString("sources",arr.toString()).apply();
        // Removed or edited sources must never reappear from stale caches.
        Set<String> keep = new HashSet<>();
        for (Source s : sources) keep.add(cacheFile(s).getName());
        File[] files = cache.listFiles();
        if (files != null) for (File f : files) if (!keep.contains(f.getName())) f.delete();
    }
    public Set<String> favorites() { return new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet())); }
    public boolean toggleFavorite(String key) {
        Set<String> favorites = favorites();
        boolean added = !favorites.remove(key);
        if (added) favorites.add(key);
        prefs.edit().putStringSet("favorites",favorites).apply();
        return added;
    }
    private File cacheFile(Source s) {
        String key = UUID.nameUUIDFromBytes((s.id+":"+s.url).getBytes(StandardCharsets.UTF_8)).toString();
        return new File(cache,key+".m3u");
    }
    public static final class Catalog {
        public final List<Channel> channels;
        public final List<String> notices;
        public final Set<String> epgUrls;
        Catalog(List<Channel> channels,List<String> notices,Set<String> epgUrls) { this.channels=channels;this.notices=notices;this.epgUrls=epgUrls; }
    }
    public Catalog load(boolean network) {
        Log.w("MIMO_DIAG","load() network="+network+" start");
        List<Channel> all = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        Set<String> epgs = new LinkedHashSet<>();
        List<Source> ordered = sources();
        ordered.sort(Comparator.comparing(s -> !s.url.equals(PLAYLIST)));
        for (Source s : ordered) {
            if (!s.enabled) continue;
            if (M3uParser.isHttp(s.epg)) epgs.add(s.epg);
            File file=cacheFile(s);
            Log.w("MIMO_DIAG","  source="+s.id+" cache="+file.getAbsolutePath()+" exists="+file.exists()+" size="+file.length());
            String text=null;
            List<Channel> parsed=null;
            if (network) try {
                Log.w("MIMO_DIAG","  fetching "+s.url);
                text=fetch(s.url,16*1024*1024);
                Log.w("MIMO_DIAG","  fetch returned "+text.length()+" chars, first100="+text.substring(0,Math.min(100,text.length())).replace("\n","\\n"));
                parsed=M3uParser.parse(text,s.id,s.url);
                Log.w("MIMO_DIAG","  network parse OK channels="+parsed.size());
                File temp = new File(file.getPath()+".tmp");
                try(FileOutputStream out = new FileOutputStream(temp)) {out.write(text.getBytes(StandardCharsets.UTF_8));}
                if (!temp.renameTo(file)) {
                    try(FileOutputStream out=new FileOutputStream(file)){out.write(text.getBytes(StandardCharsets.UTF_8));}
                    temp.delete();
                }
            } catch(Exception e) { text=null; parsed=null; Log.w("MIMO_DIAG","  fetch/validate FAILED: "+e.getClass().getSimpleName()+": "+e.getMessage()); notices.add(s.name+": refresh unavailable"+(file.exists()?"; using saved playlist.":".")); }
            if (parsed==null && text==null && file.exists()) try(FileInputStream in = new FileInputStream(file)) {text=readBounded(in,16*1024*1024);Log.w("MIMO_DIAG","  cache read OK "+text.length()+" chars");}catch(IOException ignored){Log.w("MIMO_DIAG","  cache read FAILED");}
            else if (parsed==null && text==null) Log.w("MIMO_DIAG","  no cache file, text still null");
            if (parsed==null && text!=null) try {
                parsed=M3uParser.parse(text,s.id,s.url);
                Log.w("MIMO_DIAG","  cache parse OK channels="+parsed.size());
            } catch(IOException e) {Log.w("MIMO_DIAG","  parse FAILED: "+e.getMessage()); notices.add(s.name+": invalid playlist.");}
            if (parsed!=null) all.addAll(parsed);
            if (text!=null) try {
                String first=text.replace("\uFEFF","").trim().split("\\r?\\n",2)[0];
                Map<String,String> attrs=M3uParser.attributes(first);
                String url=attrs.getOrDefault("x-tvg-url",attrs.getOrDefault("url-tvg",""));
                for(String epg:url.split(",")) if(M3uParser.isHttp(epg.trim())) epgs.add(epg.trim());
            } catch(Exception ignored) {}
        }
        Log.w("MIMO_DIAG","  all.size="+all.size());
        boolean mimoEnabled=ordered.stream().anyMatch(s->s.enabled&&s.url.equals(PLAYLIST));
        if(mimoEnabled) {
            if(all.stream().noneMatch(c->c.key.equals("xezertv.az"))) all.add(new Channel("XezerTV.az","Xəzər TV","AZ","Azerbaijan","","mimo"));
            if(all.stream().noneMatch(c->c.key.equals("spacetv.az"))) all.add(new Channel("SpaceTV.az","Space TV","AZ","Azerbaijan","","mimo"));
        }
        List<Channel> merged=M3uParser.merge(all);
        Log.w("MIMO_DIAG","load() network="+network+" done merged="+merged.size()+" notices="+notices);
        return new Catalog(merged,notices,epgs);
    }
    /** Refresh only the authoritative backend when recovering a priority channel. No embedded backup URLs. */
    public List<Channel.Stream> refreshPriority(String key) throws IOException {
        Source backend=null;
        for(Source s:sources()) if(s.enabled && s.url.equals(PLAYLIST)) {backend=s;break;}
        if(backend==null) return Collections.emptyList();
        for(Channel c:M3uParser.parse(fetch(backend.url,16*1024*1024),backend.id,backend.url))
            if(c.key.equals(key)) return c.streams;
        return Collections.emptyList();
    }
    public static String fetch(String url,int limit) throws IOException {
        if(!M3uParser.isHttp(url)) throw new IOException("Only HTTP and HTTPS URLs are supported.");
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(12000);connection.setReadTimeout(25000);
        connection.setRequestProperty("User-Agent","MIMO-TV/0.1 AndroidTV");
        connection.setRequestProperty("Accept-Encoding","gzip");
        connection.setUseCaches(false);
        try {
            int code=connection.getResponseCode();
            if(code<200 || code>=300) throw new IOException("HTTP "+code);
            try(InputStream raw=connection.getInputStream(); PushbackInputStream peek=new PushbackInputStream(raw,2)) {
                byte[] prefix=new byte[2]; int n=peek.read(prefix); if(n>0) peek.unread(prefix,0,n);
                boolean gzip=n==2 && (prefix[0]&255)==31 && (prefix[1]&255)==139;
                InputStream in=gzip?new GZIPInputStream(peek):peek;
                return readBounded(in,limit);
            }
        } finally {connection.disconnect();}
    }
    static String readBounded(InputStream in,int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
        while((n=in.read(buf))!=-1) {if(out.size()+n>limit) throw new IOException("Feed exceeds the phase-one size limit.");out.write(buf,0,n);}
        return out.toString("UTF-8");
    }
}
