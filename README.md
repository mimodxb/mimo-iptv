# Mimo IPTV

## Live playlist

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv

The URL is unchanged because the existing TiviMate playlist can be refreshed in place. The function behind it was replaced with **v5 on 2026-08-29**.

## 2026-08-29 repair

The previous build was not acceptable as an end-to-end verification. This revision removes the specific broken Azerbaijan candidates instead of keeping them merely to satisfy a channel-name checklist.

### Azerbaijan changes

- **Real TV:** old failing primary removed; `https://str.yodacdn.net/real/playlist.m3u8` is now the primary because it was confirmed working in TiviMate.
- **ATV Azerbaijan:** retained from the TiviMate-confirmed working stream.
- **İctimai TV:** retained from the TiviMate-confirmed working stream.
- **Space TV:** old failing source replaced with a separately HLS-probed direct stream.
- **İdman TV:** old failing source replaced with a separately HLS-probed direct stream.
- **ARB 24:** replaced with a separately HLS-probed direct stream.
- **CBC / CBC Sport:** direct/probed streams used.
- **ARB:** omitted for now; the known candidate failed and no sufficiently reliable replacement was established.
- **MTV Azerbaijan:** omitted for now; webpage/wrapper candidates were not accepted as a reliable TiviMate stream.

The healthy Azerbaijan baseline also retains Az TV, EL TV, Kanal 35, Kanal S, Kapaz TV, KN Music TV, Mədəniyyət TV, Naxçıvan TV, Vilayət TV and Xəzər TV where the health source reports them online.

### General playlist validation

The production function now starts from the complete current health-passing playlist rather than claiming that manually injected URLs are health-verified. It rejects known webpage-only hosts and exact duplicate stream URLs.

A local reconstruction against the latest deployed source artifact used during this repair produced:

- **8,353 unique stream URLs**
- **8,344 health-source entries**
- **22 Azerbaijan entries**
- **0 exact duplicate URLs**
- **0 known YouTube/VK/Facebook/Twitch webpage URLs**

The live total can change as the upstream health source refreshes.

## EPG repair

The earlier `dearbulut.github.io/iptv/epg/guide.xml.gz` URL is no longer used; the deployed Pages artifact did not contain that EPG file.

The playlist now embeds these guide sources:

1. Azerbaijan guide generated for matching playlist IDs:
   `https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-az`
2. Global XMLTV guide:
   `https://epg.pw/xmltv/epg.xml.gz`
3. Additional iptv-org-ID-compatible guide:
   `https://raw.githubusercontent.com/StrangeDrVN/epg/public/guide.xml.gz`

The Azerbaijan guide maps current programme data for Az TV, İctimai TV and Xəzər TV where available, plus İdman TV when its source contains programme data. EPG coverage is not claimed for every playlist channel.

## Current architecture

- The live playlist currently remains on the existing Supabase endpoint so the already-added TiviMate playlist can be repaired without changing its URL.
- This GitHub repository is the documentation and verification record for the current revision.
- The earlier GitHub-only migration is **not** being represented as completed; GitHub Actions runner provisioning previously failed before any job steps executed.
