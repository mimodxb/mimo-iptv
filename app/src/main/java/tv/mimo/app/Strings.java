package tv.mimo.app;

import java.util.*;

/** Lightweight runtime English / Azerbaijani translations. */
public final class Strings {
    private static final Map<String, String[]> M = new LinkedHashMap<>();

    // key → { English, Azerbaijani }
    static {
        // Navigation
        M.put("nav_home",      new String[]{"Home", "Ana səhifə"});
        M.put("nav_search",    new String[]{"Search", "Axtarış"});
        M.put("nav_favorites", new String[]{"Favorites", "Sevimlilər"});
        M.put("nav_guide",     new String[]{"Guide", "Bələdçi"});
        M.put("nav_settings",  new String[]{"Settings", "Parametrlər"});

        // Home
        M.put("home_headline",  new String[]{"A little closer to home.", "Evə bir az daha yaxın."});
        M.put("home_hero_label",new String[]{"FROM MIMO'S COLLECTIVE", "MIMO KOLEKTİVİNDƏN"});
        M.put("home_hero_title",new String[]{"Familiar voices.\nOne place to watch.", "Tanıdık səslər.\nBir izləmə yeri."});
        M.put("home_hero_sub",  new String[]{"Azerbaijan first. A world of channels beyond.", "Azərbaycan birinci. Kənarda kanallar dünyası."});
        M.put("home_footer",    new String[]{"MIMO'S COLLECTIVE\nMade for your living room", "MIMO KOLEKTİVİ\nOtağınız üçün hazırlanıb"});

        // Search
        M.put("search_headline", new String[]{"Find your channel.", "Kanalınızı tapın."});
        M.put("search_hint",     new String[]{"Channel name or country", "Kanal adı və ya ölkə"});

        // Favorites
        M.put("fav_headline",  new String[]{"Your favorites.", "Sevimliləriniz."});
        M.put("fav_empty",     new String[]{"No favorites yet — hold OK on a channel to save it.", "Hələ sevimli yoxdur — kanalı saxlamaq üçün OK saxlayın."});

        // Guide
        M.put("guide_headline",  new String[]{"What's on.", "Nə var."});
        M.put("guide_load",      new String[]{"↻  Load / refresh programme guide", "↻  Proqram bələdçisini yüklə yenilə"});
        M.put("guide_loading",   new String[]{"Loading guide…", "Bələdçi yüklənir…"});
        M.put("guide_none",      new String[]{"No XMLTV guide configured. Add one in playlist settings.", "XMLTV bələdçi konfiqurasiya olunmayıb. Playlist parametrlərində əlavə edin."});
        M.put("guide_channels",  new String[]{"channels with listings", "siyahılı kanallar"});
        M.put("guide_some_fail", new String[]{". Some guides could not be loaded.", ". Bəzi bələdçilər yüklənə bilmədi."});
        M.put("guide_times",     new String[]{". Times shown in your TV's time zone.", ". Vaxtlar TV-nizin saat qurşağına görə göstərilir."});

        // Settings
        M.put("settings_headline", new String[]{"Make yourself at home.", "Özünüzü evdə hiss edin."});
        M.put("settings_sub",      new String[]{"Playlists, programme guides, and Mimo's Collective.", "Pleylistlər, proqram bələdçiləri və Mimo Kolektivi."});
        M.put("settings_playlists",new String[]{"YOUR PLAYLISTS", "SİZNİN PLEYLİSTLƏRİNİZ"});
        M.put("settings_add",      new String[]{"+  Add playlist", "+  Pleylist əlavə et"});
        M.put("settings_refresh",  new String[]{"↻  Refresh all playlists", "↻  Bütün pleylistləri yenilə"});
        M.put("settings_refreshing",new String[]{"Refreshing…", "Yenilənir…"});
        M.put("settings_diag",     new String[]{"Check Xəzər / Space backend status", "Xəzər / Space backend statusunu yoxla"});
        M.put("settings_restore",  new String[]{"Restore Mimo IPTV", "Mimo IPTV-ni bərpa et"});
        M.put("settings_about",    new String[]{"About", "Haqqında"});
        M.put("settings_lang",     new String[]{"Language", "Dil"});
        M.put("settings_en",       new String[]{"English", "İngiliscə"});
        M.put("settings_az",       new String[]{"Azerbaijani", "Azərbaycanca"});
        M.put("settings_dev",      new String[]{"Developer diagnostics", "İnkişafetdirici diaqnostikası"});

        // Playback
        M.put("play_opening",    new String[]{"Opening channel…", "Kanal açılır…"});
        M.put("play_connecting", new String[]{"Connecting…", "Qoşulur…"});
        M.put("play_live",       new String[]{"Live TV", "Canlı TV"});
        M.put("play_buffering",  new String[]{"Buffering…", "Buferlənir…"});
        M.put("play_paused",     new String[]{"Paused", "Dayandırılıb"});
        M.put("play_ended",      new String[]{"The live stream ended.", "Canlı yayım bitdi."});
        M.put("play_unavailable",new String[]{"This channel is unavailable. Automatic recovery finished. Choose Retry or Back.", "Bu kanal mövcud deyil. Avtomatik bərpa başa çatdı. Yenidən cəhd edin və ya Geri seçin."});
        M.put("play_refreshing", new String[]{"Refreshing Mimo's backend for another source…", "Mimo-nun backend-i yenilənir…"});
        M.put("play_source",     new String[]{"Source", "Mənbə"});
        M.put("play_retry",      new String[]{"↻  Retry", "↻  Yenidən cəhd"});
        M.put("play_back",       new String[]{"‹  Back", "‹  Geri"});
        M.put("play_pause",      new String[]{"Pause", "Dayandır"});
        M.put("play_fav_add",    new String[]{"♡  Favorite", "♡  Sevimli"});
        M.put("play_fav_done",   new String[]{"♥  Saved", "♥  Saxlanıldı"});
        M.put("play_resume",     new String[]{"Play", "Davam et"});
        M.put("play_prev",       new String[]{"‹  Prev", "‹  Əvvəlki"});
        M.put("play_next",       new String[]{"Next  ›", "Növbəti  ›"});
        M.put("play_last",       new String[]{"Last  ⇄", "Sonuncu  ⇄"});

        // About
        M.put("about_title",     new String[]{"MIMO TV", "MIMO TV"});
        M.put("about_creator",   new String[]{"by Movsum Mirzazada", "Movsum Mirzazada tərəfindən"});
        M.put("about_collective",new String[]{"Mimo's Collective", "Mimo Kolektivi"});
        M.put("about_desc",      new String[]{"A little closer to home.", "Evə bir az daha yaxın."});
        M.put("about_version",   new String[]{"Version 0.1.0", "Versiya 0.1.0"});

        // Common
        M.put("channels",       new String[]{"channels", "kanallar"});
        M.put("no_channels",    new String[]{"No channels found. Check your playlists in Settings.", "Kanal tapılmadı. Parametrlərdə pleylistlərinizi yoxlayın."});
        M.put("available",      new String[]{"available entries", "mənbə mövcuddur"});
        M.put("refreshing",     new String[]{"Refreshing your playlists…", "Pleylistləriniz yenilənir…"});
        M.put("together",       new String[]{"Your channels, together.", "Kanallarınız bir yerdə."});
        M.put("source_unavail", new String[]{"Source unavailable", "Mənbə mövcud deyil"});
        M.put("watch_live",     new String[]{"Watch live", "Canlı izlə"});
        M.put("added_fav",      new String[]{"Added to favorites", "Sevimlilərə əlavə edildi"});
        M.put("removed_fav",    new String[]{"Removed from favorites", "Sevimlilərdən silindi"});
        M.put("edit_playlist",  new String[]{"Edit playlist", "Pleylisti redaktə et"});
        M.put("disable",        new String[]{"Disable", "Deaktiv et"});
        M.put("enable",         new String[]{"Enable", "Aktiv et"});
        M.put("remove",         new String[]{"Remove", "Sil"});
        M.put("cancel",         new String[]{"Cancel", "Ləğv et"});
        M.put("save",           new String[]{"Save", "Saxla"});
        M.put("close",          new String[]{"Close", "Bağla"});
        M.put("playlist_name",  new String[]{"Playlist name", "Pleylist adı"});
        M.put("m3u_url",        new String[]{"M3U URL (http or https)", "M3U URL (http və ya https)"});
        M.put("epg_url",        new String[]{"XMLTV guide URL (optional)", "XMLTV bələdçi URL (ixtiyari)"});
        M.put("add_playlist",   new String[]{"Add playlist", "Pleylist əlavə et"});
        M.put("remove_confirm", new String[]{"Remove", "Sil"});
        M.remove("remove_confirm");
        M.put("remove_playlist",new String[]{"Remove %s?", "%s silinsin?"});
        M.put("remove_playlist_msg",new String[]{"Removes this playlist from MIMO TV on this television.", "Bu pleylisti bu TV-dən silir."});
        M.put("enter_name",     new String[]{"Enter a playlist name", "Pleylist adı daxil edin"});
        M.put("enter_url",      new String[]{"Enter a full HTTP or HTTPS playlist URL", "Tam HTTP və ya HTTPS pleylist URL daxil edin"});
        M.put("enter_epg",      new String[]{"Enter an HTTP or HTTPS guide URL", "HTTP və ya HTTPS bələdçi URL daxil edin"});
        M.put("already_added",  new String[]{"This playlist is already added", "Bu pleylist artıq əlavə olunub"});
        M.put("hint_dpad",      new String[]{"D-pad  Move     OK  Watch     Hold OK  Favorite     Back  Return", "D-pad  Hərəkət     OK  İzle     OK Saxla  Sevimli     Geri  Qayıt"});
        M.put("live_tv",        new String[]{"●  LIVE TV", "●  CANLI TV"});
        M.put("about_detail",   new String[]{
            "Native Android TV · Media3 playback\nFavorites and playlists are saved on this TV.\nHTTP streams are supported for legacy broadcasters.\nGuide coverage depends on your sources.",
            "Yerli Android TV · Media3 playback\nSevimlilər və pleylistlər bu TV-də saxlanılır.\nHTTP stream-ləri köhnə yayımçılar üçün dəstəklənir.\nBələdçi əhatəsi mənbələrinizdən asılıdır."
        });
    }

    /** Get translation for key in given language code ("en" or "az"). */
    public static String get(String key, String lang) {
        String[] pair = M.get(key);
        if (pair == null) return key;
        return "az".equals(lang) ? pair[1] : pair[0];
    }

    /** Get translation using a two-arg format (for %s substitution). */
    public static String get(String key, String lang, Object... args) {
        return String.format(get(key, lang), args);
    }

    /** Return all registered keys (for testing). */
    public static Set<String> keys() { return Collections.unmodifiableSet(M.keySet()); }
}
