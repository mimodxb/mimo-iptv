# Mimo IPTV

Production playlist URL:

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv

Use that URL directly as an M3U playlist in TiviMate or another IPTV player.

## Current verified build

Verified on 2026-08-29 by directly invoking the production endpoint.

- HTTP status: 200
- M3U header: valid `#EXTM3U`
- Content type: `application/x-mpegURL; charset=utf-8`
- Total channels: 4,178
- Entries from the latest health-passing source set: 4,166
- Azerbaijan entries: 25
- Required Azerbaijan channels missing: 0
- Obvious webpage-only / YouTube-style URLs: 0
- SHA-256: `39d79d06b3037433d65a9d2a905de08a86fcd739b5b1c0e7235ee4ede471e9c3`
- Main health-checked source: `https://dearbulut.github.io/iptv/playlists/online.m3u`

The endpoint URL itself did not change during this refresh. The playlist generated behind that URL was updated and re-verified.

## Current architecture

- The production playlist is generated and served by the Supabase Edge Function above.
- This GitHub repository currently stores the project documentation and verification record; it does not yet host the production `playlist.m3u` file itself.
- TiviMate therefore uses the Supabase URL shown above.

## Build rules

- Uses the latest health-passing IPTV source set as the main baseline.
- Adds and prioritizes Azerbaijan channels separately.
- Removes exact duplicate stream URLs.
- Rejects known webpage-only URLs such as YouTube, Twitch, Facebook, VK, OK and similar page URLs.
- Preserves M3U metadata and group information where available.
- Keeps the production URL stable when the generated playlist is refreshed.

## Azerbaijan priority set

The current verified output contains 25 Azerbaijan entries. The required priority set includes:

AzTV, ATV Azerbaijan, ARB, ARB 24, Baku TV, CBC Sport, İctimai TV, İdman TV, Mədəniyyət TV, MTV Azerbaijan, Real TV, Space TV and Xəzər TV.

Additional Azerbaijan channels and backup stream variants are retained where useful.
