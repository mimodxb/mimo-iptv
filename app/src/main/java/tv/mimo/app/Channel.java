package tv.mimo.app;

import java.util.*;

/** Stable identity is independent of signed or refreshed stream URLs. */
public final class Channel {
    public final String key, tvgId, name, country, group, logo;
    public final String cat;
    public final List<Stream> streams = new ArrayList<>();
    private static final java.util.regex.Pattern COMBINING = java.util.regex.Pattern.compile("\\p{M}");
    private static final java.util.regex.Pattern NON_ALNUM = java.util.regex.Pattern.compile("[^\\p{L}\\p{N}]");
    public Channel(String tvgId, String name, String country, String group, String logo, String source) {
        this.tvgId = tvgId; this.name = name; this.country = country; this.group = group; this.logo = logo;
        String normName = normalize(name);
        String normGroup = normalize(group);
        String priority = priorityId(tvgId, normName, country, normGroup);
        key = !priority.isEmpty() ? priority : !tvgId.isEmpty() ? tvgId.toLowerCase(Locale.ROOT)
            : source + ":" + normName + ":" + normGroup;
        String cu = country.toUpperCase(Locale.ROOT);
        if (cu.isEmpty() && tvgId.contains(".")) cu = tvgId.substring(tvgId.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        if (!priority.isEmpty() || cu.equals("AZ") || normGroup.contains("azerbaijan")) cat = "Azerbaijan";
        else if (cu.equals("RU") || normGroup.contains("russia")) cat = "Russia";
        else if (cu.equals("TR") || normGroup.contains("turkey") || normGroup.contains("turkiye")) cat = "Turkey";
        else if (EUROPE.contains(cu) || normGroup.contains("europe")) cat = "Europe";
        else cat = "World";
    }
    public boolean priority() { return key.equals("xezertv.az") || key.equals("spacetv.az"); }
    public static String normalize(String text) {
        String nfd = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        String noCombining = COMBINING.matcher(nfd).replaceAll("");
        String lower = noCombining.toLowerCase(Locale.ROOT).replace('\u0259','e').replace('\u0131','i');
        return NON_ALNUM.matcher(lower).replaceAll("");
    }
    static String priorityId(String id, String normName, String country, String normGroup) {
        String n = normName.replaceAll("(hd|sd|fhd|1080p|720p)$", "");
        String i = id.toLowerCase(Locale.ROOT);
        if (i.equals("xezertv.az") || i.equals("khazartv.az")) return "xezertv.az";
        if (i.equals("spacetv.az")) return "spacetv.az";
        boolean az = country.equalsIgnoreCase("AZ") || normGroup.contains("azerbaijan") || id.endsWith(".az");
        if (az && Arrays.asList("xezertv", "xazar", "xazartv", "khazartv", "xezer").contains(n)) return "xezertv.az";
        return az && n.equals("spacetv") ? "spacetv.az" : "";
    }
    public String category() { return cat; }
    private static final Set<String> EUROPE = new HashSet<>(Arrays.asList(
        "AL","AD","AT","BY","BE","BA","BG","HR","CY","CZ","DK","EE","FI","FR","DE","GR","HU","IS","IE","IT",
        "XK","LV","LI","LT","LU","MT","MD","MC","ME","NL","MK","NO","PL","PT","RO","SM","RS","SK","SI","ES","SE","CH","UA","GB","UK","VA"));
    public static final class Stream {
        public final String url, sourceId;
        public final Map<String,String> headers;
        public Stream(String url, String sourceId, Map<String,String> headers) {
            this.url = url; this.sourceId = sourceId; this.headers = new LinkedHashMap<>(headers);
        }
        public String identity() { return url + headers.toString(); }
    }
}
