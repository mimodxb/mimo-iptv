package tv.mimo.app;
public final class Source {
    public final String id, name, url, epg;
    public final boolean enabled;
    public Source(String id, String name, String url, String epg, boolean enabled) {
        this.id=id; this.name=name; this.url=url; this.epg=epg; this.enabled=enabled;
    }
}
