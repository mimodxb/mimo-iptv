# MIMO TV

Native Android TV and Android phone/tablet player for Mimo's Collective, by Movsum Mirzazada. One APK contains the TCL/TV remote interface and the touch interface. TiviMate is not required.

## Features

- Azerbaijan, Russia, Turkey, Europe, World and All channels; search, favorites and recently watched.
- TV D-pad navigation, channel surfing, last channel and visible focus; mobile bottom navigation and portrait/landscape playback.
- Media3 HLS/DASH playback, bounded recovery, and backend refresh for Xəzər/Space priority channels.
- Channel logos fitted inside their square cards without cropping or stretching; PNG/JPEG/WebP/GIF/SVG support, memory/disk caching and exact-ID logo enrichment.
- English/Azerbaijani interface, playlist-source editing, enabled/disabled sources, and XMLTV programme guide.
- Local preferences and cached playlists; no administrative API keys in the APK.

## Install

Download the APK from this repository's Releases. It works without a PC, ADB or developer mode. Open it on Android and allow installation from your chosen file-sharing app when Android asks. The TV launcher opens the remote interface; phones open the mobile interface.

The release candidate uses a private release signing key. An older developer/debug installation with the same package name may have a different signature; Android will reject an in-place update. Preserve your playlist URLs before removing an old developer build. Subsequent releases must retain the release signing key.

## Current acceptance status

Version 1.0.0-rc3 is a release candidate, not a claim of full physical-device acceptance. See [PROJECT_STATE.md](PROJECT_STATE.md) for the exact checkpoint and remaining blockers.

- Production playlist v9 preserves upstream logo metadata and supplies multiple verified Xəzər candidates.
- A current catalogue snapshot contained 8,239 stream entries. Image checks plus alternate-logo repairs verified raster-image responses for 7,544 entries; 695 remained unverified, including missing metadata, broken assets and rate-limited hosts. These counts are not a guarantee of every image on every device.
- Space TV has no verified working source in the tested candidates. Its priority card remains available for future backend recovery.
- Guide listings depend on the provider's exact XMLTV IDs and current schedules. Missing programmes are not invented.
- Both devices have been connected and tested; full channel acceptance remains incomplete. rc3 addresses a reproduced TCL certificate-trust failure and corrects AzTV/CBC/CBC Sport sources. See [TCL repair evidence](docs/tcl-rc3-repair.md).

## Backend

Playlist: https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv

Guide: https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-merged

Add `?check=1` to the playlist URL for diagnostics. The deployed playlist function source and tests are now version-controlled under `supabase/functions/mimo-iptv`. Its public access setting is preserved from the existing backend. Android uses the backend; it does not embed administrator credentials.

Logo metadata comes from upstream playlists and the public iptv-org catalogue. Channel names, marks and streams belong to their respective owners. The included JSON index supplies URL metadata, not ownership of those assets. Users can add their own playlist sources.

## Build and verify

Requires JDK 17 and Android SDK 35. Set `sdk.dir` in an untracked `local.properties` or configure the Android SDK environment.

```text
./gradlew assembleRelease testDebugUnitTest lintDebug
node --test supabase/functions/mimo-iptv/index.test.mjs
```

The unsigned release output is `app/build/outputs/apk/release/app-release-unsigned.apk`. Sign it with Android SDK `apksigner` using the privately retained release key. Never commit keystores or passwords. The Gradle wrapper and pinned dependencies are included.

The `design/` browser preview is a design reference; the shipped application is native Android.

