# Mimo IPTV

## TiviMate endpoints

Playlist URL:

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv

Single EPG URL:

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-merged

The playlist URL remains unchanged, so the existing TiviMate playlist can be refreshed in place.

## Production state — 2026-09-11

### Playlist

`mimo-iptv` is deployed as **version 7** and Supabase reports it **ACTIVE**.

The function starts from the Dearbulut health-source playlist for broad international coverage, removes webpage-only URLs and duplicate stream URLs, then applies Azerbaijan-specific repairs. Russian, Turkish, European and other international channels therefore remain supplied by the large upstream health-filtered playlist rather than a small hand-maintained list.

Azerbaijan entries are also deduplicated by channel identity (`tvg-id`) where available, not only by exact URL/name.

### Priority Azerbaijan validation

Xəzər TV and Space TV are treated as priority channels.

On each playlist build, version 7 performs a direct HLS manifest probe and chooses the first candidate that actually responds as HLS.

#### Xəzər TV — `XezerTV.az`

Candidates, in order:

1. `https://www.xezerxeber.az/stream/index.m3u8`
2. `https://xezerxeber.az/stream/main_stream.m3u8`

The first candidate is also reported HTTP 200 by a current public IPTV directory updated 2026-09-07.

#### Space TV — `SpaceTV.az`

Candidates, in order:

1. `http://213.239.195.222/azerbaijan/space_stream_sd_2023/playlist.m3u8`
2. `https://streams.livetv.az/azerbaijan/space_stream/playlist.m3u8`

The first candidate has recent independent HLS probe evidence showing an HLS stream with H.264 video and AAC audio. Space remains more intermittent than Xəzər, so version 7 does not blindly assume one permanent URL.

If neither priority candidate passes the live probe, version 7 does not falsely mark it as verified; the diagnostic endpoint reports the priority channel as degraded and the upstream health-source entry may remain available if present.

### Other Azerbaijan repairs

- ATV Azerbaijan: `https://lives.atv.az:5443/ATV_TV_STREAM/streams/atvcanli.m3u8`
- İctimai TV: `https://live.itv.az/itv.m3u8`
- Real TV: `https://str.yodacdn.net/real/playlist.m3u8`
- AzTV: `https://str.yodacdn.net/aztv/index.m3u8`
- Mədəniyyət TV: `https://str.yodacdn.net/medeniyyettele/index.m3u8`
- İdman TV: `http://213.239.195.222/azerbaijan/idman_stream_sd_2023/playlist.m3u8`
- ARB 24: `http://85.132.81.184:8080/arb24/live1/index.m3u8`
- CBC: `https://stream.cbctv.az:5443/LiveApp/streams/cbctv.m3u8`
- CBC Sport: `http://213.239.195.222/azerbaijan/cbc_sport_stream_hd_2023/playlist.m3u8`
- Baku TV: `https://rtmp.baku.tv/hls/bakutv.m3u8`

The upstream Azerbaijan baseline remains available for additional channels reported healthy by the global source.

### Diagnostics

Use:

`https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv?check=1`

The response reports:

- playlist version and channel count;
- Azerbaijan channel count;
- selected Xəzər and Space URLs;
- per-candidate HLS probe results;
- Azerbaijan entries and validation labels;
- playlist SHA-256.

`PASS` means the general playlist checks pass and both priority channels passed their live HLS probes. `DEGRADED` means the playlist can still be produced but at least one priority channel did not pass its live probe at that moment.

## Single merged EPG

`mimo-epg-merged` remains the single XMLTV endpoint for TiviMate. It combines broad international programme data with additional public XMLTV coverage and Azerbaijan-specific ID rewriting where programme data is available.

EPG coverage is not claimed for every playlist channel; assignment still depends on compatible XMLTV/channel identifiers or player-side matching.

## Validation boundary

Supabase reports production `mimo-iptv` version 7 ACTIVE and the deployed source has been inspected after deployment.

The assistant execution environment used for this deployment cannot directly resolve the public Supabase hostname, so an end-to-end request to the production `?check=1` endpoint was not independently fetched from that environment. Xəzər and Space have separate current public stream evidence, and version 7 now performs its own live HLS checks at playlist-generation time.

Final device playback still depends on the stream host, network/geographic access, Android/player handling, and whether the broadcaster changes its live URL.

## Architecture

- Supabase Edge Functions are the production delivery layer.
- GitHub is the documentation and verification record.
- The existing TiviMate playlist URL remains unchanged.
