package tv.mimo.app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public final class M3uParser {
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    public static Map<String,String> attributes(String line) {
        Map<String,String> out = new HashMap<>();
        Matcher m = ATTRIBUTE.matcher(line);
        while (m.find()) out.put(m.group(1).toLowerCase(Locale.ROOT), m.group(2) != null ? m.group(2) : m.group(3));
        return out;
    }
    public static boolean isHttp(String value) {
        if (value != null && (value.startsWith("http://") || value.startsWith("https://"))) return true;
        try { URI u = new URI(value); return u.getHost() != null && ("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme())); }
        catch (Exception e) { return false; }
    }
    public static List<Channel> parse(String content, String sourceId, String baseUrl) throws IOException {
        String clean = content.replace("\uFEFF", "").trim();
        if (!clean.startsWith("#EXTM3U") || clean.contains("#EXT-X-TARGETDURATION") || clean.contains("#EXT-X-STREAM-INF"))
            throw new IOException("Enter an M3U channel playlist, not a video or web page.");
        Map<String,Channel> channels = new LinkedHashMap<>();
        Channel pending = null;
        Map<String,String> headers = new LinkedHashMap<>();
        URI baseUri = null;
        try { baseUri = URI.create(baseUrl); } catch (Exception ignored) {}
        for (String raw : clean.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.startsWith("#EXTINF:")) {
                Map<String,String> a = attributes(line);
                int comma = metadataComma(line);
                String name = comma < 0 ? a.getOrDefault("tvg-name", "Channel") : line.substring(comma + 1).trim();
                pending = new Channel(a.getOrDefault("tvg-id", ""), name, a.getOrDefault("tvg-country", ""),
                    a.getOrDefault("group-title", ""), a.getOrDefault("tvg-logo", ""), sourceId);
                headers = new LinkedHashMap<>();
            } else if (line.startsWith("#EXTVLCOPT:http-user-agent=")) headers.put("User-Agent", line.substring(line.indexOf('=') + 1));
            else if (line.startsWith("#EXTVLCOPT:http-referrer=")) headers.put("Referer", line.substring(line.indexOf('=') + 1));
            else if (!line.isEmpty() && !line.startsWith("#") && pending != null) {
                String url = line;
                int pipe = url.indexOf('|');
                if (pipe >= 0) {
                    for (String field : url.substring(pipe + 1).split("&")) {
                        String[] pair = field.split("=", 2);
                        if (pair.length == 2 && (pair[0].equalsIgnoreCase("User-Agent") || pair[0].equalsIgnoreCase("Referer")))
                            try { headers.put(pair[0].equalsIgnoreCase("Referer") ? "Referer" : "User-Agent", URLDecoder.decode(pair[1], "UTF-8")); } catch (Exception ignored) { }
                    }
                    url = url.substring(0, pipe);
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try { if (baseUri != null) url = baseUri.resolve(url).toString(); } catch (Exception ignored) { }
                }
                if (isHttp(url)) {
                    Channel existing = channels.get(pending.key);
                    if (existing == null) { existing = pending; channels.put(pending.key, existing); }
                    Channel.Stream stream = new Channel.Stream(url, sourceId, headers);
                    if (existing.streams.stream().noneMatch(s -> s.identity().equals(stream.identity()))) existing.streams.add(stream);
                }
                pending = null;
            }
        }
        if (channels.isEmpty()) throw new IOException("No supported HTTP/HTTPS channels in this playlist.");
        return new ArrayList<>(channels.values());
    }
    static int metadataComma(String line) {
        char quote = 0;
        for (int i=0; i<line.length(); i++) {
            char c = line.charAt(i);
            if (quote == 0 && (c == '\"' || c == '\'')) quote = c;
            else if (c == quote) quote = 0;
            else if (c == ',' && quote == 0) return i;
        }
        return -1;
    }
    public static List<Channel> merge(List<Channel> input) {
        Map<String,Channel> out = new LinkedHashMap<>();
        for (Channel c : input) {
            Channel existing = out.get(c.key);
            if (existing == null) { out.put(c.key, c); continue; }
            for (Channel.Stream s : c.streams)
                if (existing.streams.stream().noneMatch(x -> x.identity().equals(s.identity()))) existing.streams.add(s);
        }
        List<Channel> result = new ArrayList<>(out.values());
        result.sort(Comparator.comparing((Channel c) -> !c.priority()).thenComparing(c -> !c.key.equals("xezertv.az"))
            .thenComparing(c -> c.name.toLowerCase(Locale.ROOT)));
        return result;
    }
}
