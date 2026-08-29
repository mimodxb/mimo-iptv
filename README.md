# Mimo IPTV

Production playlist URL:

https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv

Use that URL directly as an M3U playlist in TiviMate or another IPTV player.

## Build rules

- Aggregates large public IPTV sources at request time.
- Requires at least 3,000 usable unique entries before returning a playlist.
- Removes exact duplicate stream URLs.
- Rejects known webpage-only URLs such as YouTube, Twitch, Facebook, VK, OK and Rutube pages.
- Places Azerbaijan priority channels first and keeps backup streams for Real TV, Space TV and Xəzər TV.
- Preserves M3U metadata and group information from upstream sources.

## Azerbaijan priority set

Az TV, ATV Azerbaijan, ARB, ARB 24, Baku TV, CBC, CBC Sport, İctimai TV, İdman TV, Kanal S, Mədəniyyət TV, MTV Azerbaijan, Real TV, Space TV, Xəzər TV and Naxçıvan TV.

The playlist is generated dynamically so upstream source updates do not require changing the URL in the IPTV player.
