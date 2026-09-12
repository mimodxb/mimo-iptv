# MIMO TV

Phase-one native Android TV application for Mimo's Collective. The app uses the existing Mimo IPTV backend, opens in Azerbaijan, and supports Russia, Turkey, Europe, and remaining World channels.

## Included

- Native TV launcher entry, landscape shell, remote/D-pad navigation, visible focus outlines, and dark teal/green/gold styling.
- Home/category channel grid, accent-insensitive search, persistent favorites, and a now/next EPG entry point.
- Multiple M3U playlists: add, edit, enable/disable, remove, refresh, and persist locally. XMLTV guide URL per source; M3U-declared guide URLs are also recognized.
- Media3/ExoPlayer playback for HTTP/HTTPS media, HLS and DASH modules, audio focus, media-key support, buffer/error handling, and lifecycle cleanup.
- Xəzər and Space are priority identities. On failure the app refreshes Mimo's backend selection once, then tries available playlist alternatives. No backend candidate URLs are copied into the APK. Automatic recovery stops after four distinct stream/header combinations.
- Offline playlist caches with an explicit saved-data notice. A failed or invalid refresh cannot replace a valid cached playlist.
- Derived TV emblem and unchanged original logo in Settings.
- A separate [interactive design preview](design/preview.html) and [design specification](design/DESIGN.md).

## Existing Backend (Production)

### Endpoints

| Purpose | Address |
|---|---|
| Playlist | `https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv` |
| XMLTV | `https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-merged` |
| Diagnostics | Playlist address with `?check=1` |
| Documentation | [mimodxb/mimo-iptv](https://github.com/mimodxb/mimo-iptv) |

These endpoints are preconfigured. The app does not need an embedded Supabase administration key. No backend deployment or GitHub publication is part of this phase.

### Production State — 2026-09-11

#### Playlist

`mimo-iptv` is deployed as **version 7** and Supabase reports it **ACTIVE**.

The function starts from the Dearbulut health-source playlist for broad international coverage, removes webpage-only URLs and duplicate stream URLs, then applies Azerbaijan-specific repairs. Russian, Turkish, European and other international channels therefore remain supplied by the large upstream health-filtered playlist rather than a small hand-maintained list.

Azerbaijan entries are also deduplicated by channel identity (`tvg-id`) where available, not only by exact URL/name.

#### Priority Azerbaijan Validation

Xəzər TV and Space TV are treated as priority channels.

On each playlist build, version 7 performs a direct HLS manifest probe and chooses the first candidate that actually responds as HLS.

**Xəzər TV — `XezerTV.az`**

Candidates, in order:

1. `https://www.xezerxeber.az/stream/index.m3u8`
2. `https://xezerxeber.az/stream/main_stream.m3u8`

The first candidate is also reported HTTP 200 by a current public IPTV directory updated 2026-09-07.

**Space TV — `SpaceTV.az`**

Candidates, in order:

1. `http://213.239.195.222/azerbaijan/space_stream_sd_2023/playlist.m3u8`
2. `https://streams.livetv.az/azerbaijan/space_stream/playlist.m3u8`

The first candidate has recent independent HLS probe evidence showing an HLS stream with H.264 video and AAC audio. Space remains more intermittent than Xəzər, so version 7 does not blindly assume one permanent URL.

If neither priority candidate passes the live probe, version 7 does not falsely mark it as verified; the diagnostic endpoint reports the priority channel as degraded and the upstream health-source entry may remain available if present.

#### Other Azerbaijan Repairs

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

#### Diagnostics

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

#### Single Merged EPG

`mimo-epg-merged` remains the single XMLTV endpoint for TiviMate. It combines broad international programme data with additional public XMLTV coverage and Azerbaijan-specific ID rewriting where programme data is available.

EPG coverage is not claimed for every playlist channel; assignment still depends on compatible XMLTV/channel identifiers or player-side matching.

#### Validation Boundary

Supabase reports production `mimo-iptv` version 7 ACTIVE and the deployed source has been inspected after deployment.

The assistant execution environment used for this deployment cannot directly resolve the public Supabase hostname, so an end-to-end request to the production `?check=1` endpoint was not independently fetched from that environment. Xəzər and Space have separate current public stream evidence, and version 7 now performs its own live HLS checks at playlist-generation time.

Final device playback still depends on the stream host, network/geographic access, Android/player handling, and whether the broadcaster changes its live URL.

#### Architecture

- Supabase Edge Functions are the production delivery layer.
- GitHub is the documentation and verification record.
- The existing TiviMate playlist URL remains unchanged.

---

## Build

Open this directory in Android Studio, or use JDK 17 and Android SDK platform/build tools 35:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

On macOS/Linux:

```sh
sh ./gradlew assembleDebug testDebugUnitTest lintDebug
```

Point `ANDROID_HOME` to your SDK, or create a local `local.properties` with `sdk.dir=...`. The source archive excludes machine-specific paths and build caches. Gradle 8.11.1 and its distribution checksum are pinned in the wrapper. AGP 8.9.2 and Media3 1.6.1 are deliberately pinned as a compatible baseline; they are not claimed to be the latest releases.

Native UI uses Java Android Views and RecyclerView; it does not embed the HTML preview. Minimum Android version is 6.0 / API 23. Package ID: `tv.mimo.app`. Build output: `app/build/outputs/apk/debug/app-debug.apk`.

## Try on your TCL

The provided APK is a debug preview build. Install it through your existing Android TV sideloading workflow. If using an already-authorized ADB connection:

```powershell
adb install -r MIMO-TV-0.1.0-debug.apk
adb shell am start -n tv.mimo.app/.MainActivity
```

No TV IP address, pairing, or device authorization is assumed. This phase does not connect to or install onto your TV.

The archive does not contain the private debug signing key. A build made on another computer may use a different debug key and may require uninstalling the preview before installation, which clears its local settings. Production signing and an upgrade path are not part of this phase.

Use D-pad to move, OK to watch, hold OK or press Menu to favorite a card, and Back to return. Playback also provides Favorite and Retry buttons. Play/Pause media keys are supported.

## Acceptance Checks on the Television

1. Launch from the Android TV home screen and confirm branding, text size, and safe screen margins.
2. Move through all five sections with the remote. Scroll to a lower channel row, play a channel, then press Back and confirm focus returns to that card.
3. Search `Xezer`; confirm Xəzər appears. Save it as a favorite, restart the app, and confirm the favorite persists.
4. Test Xəzər first. Test Space next and record whether the backend has recovered or still reports unavailable. Then test several other Azerbaijan channels and one Russian, Turkish, and European stream.
5. Add a second authorized M3U playlist, restart, disable/re-enable it, and remove it. Confirm its channels follow the enabled setting.
6. Load the guide. Verify real programme titles/time zone where XMLTV data exists and honest unavailable states elsewhere.
7. Pause/resume, press the TV Home button, and return. Confirm playback stops in the background and restarts cleanly.
8. Disconnect the network, refresh, and confirm saved-playlist/error messaging. Restore the network and retry.

## Phase-One Boundaries

- Not a production release or Play Store submission. Release signing, deployment, and real-device acceptance are later steps.
- The browser design companion is illustrative and does not play streams. Automated browser opening was blocked by browser policy; it was not visually verified.
- Device decoding, geoblocking, upstream uptime, TCL focus behaviour, and live EPG coverage remain unverified on hardware.
- No DRM, catch-up, recording, external storage import, Xtream API, background playback, full timetable grid, or downloaded channel-logo cache in this phase. Cards display readable channel names.
- Playlist and EPG fetches are capped at 16 MiB and 32 MiB respectively; excessive or invalid feeds show errors. M3U supports quoted metadata, relative HTTP(S) streams, VLC user-agent/referrer options, and URL-pipe headers. Vendor-specific `#EXTHTTP` JSON and DRM directives are not implemented.
- Favorites/source URLs/cache are in Android private app storage; OS cloud backup is disabled. Full URL tokens remain visible in the local edit form. User-supplied HTTP playlists and legacy streams use cleartext by design; TLS certificate checking remains enabled for HTTPS.

See [VALIDATION.md](VALIDATION.md) for the exact checks performed and remaining limits.