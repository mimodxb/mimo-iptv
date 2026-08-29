# Mimo IPTV

## Final TiviMate endpoints

Playlist URL:

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv

Single EPG URL:

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-merged

The playlist URL remains unchanged so the existing TiviMate playlist can be refreshed in place.

## Production state — 2026-08-29

### Playlist

`mimo-iptv` is deployed as **version 6** and ACTIVE. It starts from the current Dearbulut health-source playlist, removes obvious webpage-only URLs and exact duplicate stream URLs, and applies the Azerbaijan repair overrides.

The playlist header references only the single merged EPG endpoint above.

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

The broken Dearbulut `epg/guide.xml.gz` deployment is not used.

`mimo-epg-merged` is deployed as Supabase **version 18**, implementation **4.1**, and ACTIVE. It provides one XMLTV endpoint for TiviMate and combines behind that URL:

- EPG.PW Lite global XMLTV for broad international programme data;
- the current StrangeDrVN public `guide.xml` as additional iptv-org-style `tvg-id` coverage;
- custom Azerbaijan EPG.PW data rewritten to `AzTV.az`, `IctimaiTV.az`, `XezerTV.az`, and `IdmanTV.az` when programme data is available.

The large international XMLTV documents are streamed through the Edge Function rather than being loaded completely into memory. Their individual XML declarations and `<tv>` wrappers are stripped and the function emits one outer XMLTV document. Responses are cacheable for twelve hours.

The function supports `?check=1` for compact upstream/structural diagnostics.

The current StrangeDrVN `public` branch contains `channels.xml`, `guide.xml`, `guide.xml.gz`, and `guide.json`; its guide is supplemental to EPG.PW Lite rather than the sole international dependency.

EPG coverage is not claimed for every playlist channel. Automatic guide assignment still depends on compatible XMLTV/channel identifiers or player-side name matching.

## Validation boundary

Supabase reports both production Edge Functions ACTIVE and the deployed source has been inspected. The current assistant environment cannot directly fetch the public Supabase project hostname after deployment because DNS/external fetch restrictions block that route. Deployment-state verification is therefore not represented as an independently observed end-to-end HTTP response.

Likewise, HTTP success, valid M3U/XMLTV structure, an upstream health check, or successful deployment is not proof that every live stream plays on every device or network.

## Architecture

- Supabase Edge Functions are the production delivery layer.
- GitHub is the documentation and verification record for the deployed configuration.
- The earlier GitHub-only migration is not represented as completed because the attempted GitHub Actions runner did not execute its job steps.
