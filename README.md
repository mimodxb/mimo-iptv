# Mimo IPTV

## Final TiviMate endpoints

Playlist URL:

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv

Single EPG URL:

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-merged

The playlist URL remains unchanged so the existing TiviMate playlist can be refreshed in place.

## Production state — 2026-08-29

### Playlist

`mimo-iptv` is deployed as **version 6**. It starts from the current Dearbulut health-source playlist, removes obvious webpage-only URLs and exact duplicate stream URLs, and applies the Azerbaijan repair overrides.

The playlist header now references only the single merged EPG endpoint above.

### Azerbaijan repairs

- Real TV: `https://str.yodacdn.net/real/playlist.m3u8` — promoted to primary because it was confirmed working in TiviMate.
- ATV Azerbaijan: `https://lives.atv.az:5443/ATV_TV_STREAM/streams/atvcanli.m3u8` — retained from TiviMate-confirmed playback.
- İctimai TV: `https://live.itv.az/itv.m3u8` — retained from TiviMate-confirmed playback.
- Space TV: `http://213.239.195.222/azerbaijan/space_stream_sd_2023/playlist.m3u8` — independent HLS probe evidence; not represented as user-confirmed playback.
- İdman TV: `http://213.239.195.222/azerbaijan/idman_stream_sd_2023/playlist.m3u8` — independent HLS probe evidence; not represented as user-confirmed playback.
- ARB 24: `http://85.132.81.184:8080/arb24/live1/index.m3u8` — independent HLS probe evidence; not represented as user-confirmed playback.
- CBC: `https://stream.cbctv.az:5443/LiveApp/streams/cbctv.m3u8`.
- CBC Sport: `http://213.239.195.222/azerbaijan/cbc_sport_stream_hd_2023/playlist.m3u8`.
- Baku TV: `https://rtmp.baku.tv/hls/bakutv.m3u8`.
- ARB primary: omitted until a reliable direct stream is established.
- MTV Azerbaijan: omitted until a reliable direct TiviMate-compatible stream is established.

The upstream Azerbaijan baseline remains available for channels reported online by the health source.

## Single merged EPG

The previous Dearbulut `epg/guide.xml.gz` URL is not used because that file was absent from the inspected deployment.

`mimo-epg-merged` is deployed as **version 4**. It exposes one XMLTV URL for TiviMate and combines:

- EPG.PW Lite global XMLTV as broad international guide data;
- the current StrangeDrVN/iptv-org-compatible XMLTV guide to improve matching for iptv-org-style `tvg-id` values;
- custom Azerbaijan EPG.PW channel data rewritten to the playlist IDs `AzTV.az`, `IctimaiTV.az`, `XezerTV.az`, and `IdmanTV.az` when programme data is available.

The function streams the large global XMLTV sources into one `<tv>` document instead of loading the complete guides into memory at once. Responses are cacheable for twelve hours.

EPG coverage is not claimed for every playlist channel. Automatic guide assignment still depends on the playlist `tvg-id` matching an XMLTV channel ID. The merged endpoint is intended to maximize coverage while preserving the single-URL requirement.

## Validation boundary

A successful HTTP response, valid M3U/XMLTV structure, upstream health check, or successful deployment is not represented as proof that every live stream plays on every device or network. Stream playback and EPG matching are separate checks.

## Architecture

- Supabase Edge Functions are the production delivery layer.
- GitHub is the documentation and verification record for the deployed configuration.
- The earlier GitHub-only migration is not represented as completed because the attempted GitHub Actions runner did not execute its job steps.
