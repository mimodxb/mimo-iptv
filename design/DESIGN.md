# MIMO TV — Phase 01

A native Android TV home for Mimo’s Collective. Azerbaijan is the opening category; Russia, Turkey, Europe, and remaining World channels stay one remote action away. The application consumes the existing backend rather than packaging thousands of ageing stream URLs.

## Brand treatment

The original attachment was recovered from the referenced conversation and visually inspected. The supplied `/mnt/data/IMG-20260111-WA0000.jpg` path belonged to that conversation; the actual attachment was retrieved locally. An unchanged copy is included in `app/src/main/res/drawable-nodpi/mimo_original.jpg` and appears in Settings.

The original identity has an interwoven teal monogram, an upward botanical sprig, green leaves, and gold ribbon. The simplified emblem reduces leaf detail while retaining those elements. MIMO TV is a separate, legible horizontal wordmark; the original square image and small founder line are not shrunk into the TV navigation rail. An ivory tile gives the deep teal emblem sufficient contrast on the dark interface.

The derived emblem is an initial interpretation, not a replacement for the original master. It is used in the rail, home panel, launcher icon, and TV banner. The native launcher banner uses a code-native wordmark. The generated emblem was visually inspected before inclusion.

| Token | Value | Use |
|---|---|---|
| Background | `#091516` | Quiet, near-black teal |
| Surface | `#122627` | Channel cards |
| Highlight surface | `#133939` | Home panel and focus |
| Main text | `#EDF1E7` | Warm white, readable at a distance |
| Secondary text | `#A6BBB1` | Source and navigation hints |
| Focus | `#B8D977` | Visible outline, not colour alone |
| Brand accent | `#D9AE51` | Small labels and priority emphasis |

## Screen contract

- **Home:** persistent left rail, restrained brand panel, country tabs, a virtualized four-column channel grid. Xəzər then Space appear first in Azerbaijan. The layout budget targets a complete card row at 960 × 540 dp; the grid scrolls for additional rows. Hardware layout still needs checking.
- **Search:** one native keyboard field filters all enabled sources by name, country/category. The IME search action moves focus to results. Accent-insensitive matching supports `Xezer` for `Xəzər`.
- **Favorites:** a saved collection using channel identity, not changing stream URLs. Hold OK or press Menu on a card to toggle; a Favorite action is also available during playback.
- **Guide:** category-filtered now/next cards, loaded on demand from configured XMLTV feeds. Show “Programme information unavailable” when no matching data exists. This phase does not include a full horizontal timetable.
- **Settings:** add, edit, enable, disable, remove, and refresh multiple M3U sources; optional XMLTV URL per source. Existing Mimo playlist and EPG are preconfigured. Only the hostname is displayed in the source list, keeping URL tokens out of the main screen. Editing shows the full URL locally.
- **Playback:** full-screen Media3 video with Back, Play/Pause, Favorite, and Retry. Chrome hides after six seconds of playback; directional input brings it back. Remote media buttons work. Player resources are released when the activity stops.

## Remote interaction

| Input | Behaviour |
|---|---|
| D-pad | Move between native focusable controls |
| OK on a channel | Open playback |
| Hold OK / Menu on a card | Add/remove favorite |
| Left from first grid column | Return to active rail item |
| Back in playback | Return to originating channel |
| Back in another section | Home |
| Back on Home | Rail first; exit when Home already has focus |
| Media play/pause | Control video without touch |

The focus outline is accompanied by a surface and elevation change. Navigation is based on native Android focus handling; actual TCL key-repeat, overscan, IME, and focus behaviour still require device acceptance testing. Text uses Android scalable units. Cards are intentionally typographic in this phase; remote channel-logo downloading is deferred.

## Backend and failure states

The read-only GitHub README and live endpoints were inspected on 12 September 2026. The deployed v7 backend selects priority stream candidates. MIMO TV does not duplicate the backend’s candidate URLs or its HLS health algorithm.

1. Open the backend-selected stream first.
2. On priority error or a 25-second uninterrupted buffer, refresh the existing Mimo playlist once and resolve the stable channel ID again.
3. Try a newly selected backend URL, then alternate URLs present in the user’s enabled playlists.
4. Try each URL/header combination once, with a maximum of four playback attempts per session.
5. Finish in a visible unavailable state. Explicit Retry starts a new session.

Space remains a visible unavailable priority card when the backend omits it. This card triggers a fresh lookup, not playback of a fabricated URL. Cached playlists remain usable during source outages and are labelled as saved data.

Live diagnostic evidence from this phase: backend v7 reported **DEGRADED**, **8,196 channel entries**, **21 Azerbaijan entries**. Xəzər’s manifest passed; Space’s two candidates failed (timeout and DNS resolution). These are timestamped backend observations, not proof of current device playback.

## Preview boundary

Open `preview.html` for a local interactive design companion. Its categories and channel cards are illustrative; favorites and added playlists last only for that page session. It deliberately does not play streams. The native APK contains the actual persistent settings, playlist parsing, XMLTV loading, and Media3 playback.

Automated browser opening of the local HTML file was blocked by browser security policy. The browser preview was therefore not visually verified in this environment. Source syntax is checked separately. The native APK and tests are the implementation deliverable; the HTML is not an Android screenshot.

## Image-generation record

Tool: built-in image generation. Input: original Mimo’s Collective attachment. Final emblem: `app/src/main/res/drawable-nodpi/mimo_emblem.png`.

Prompt:

> Create a simplified app emblem derived faithfully from the attached Mimo's Collective logo. Preserve the recognizable interwoven M/W-like teal monogram silhouette, the central upward botanical sprig, and a single gold ribbon crossing its lower portion. Reduce the leaves to three broad clean shapes and remove tiny gradients/details so it reads on a television. Flat polished vector-like graphic, deep teal #073C42 main body, bright leaf green #93C83E leaves, warm gold #D9AE51 ribbon. Keep the recognizable identity of this exact attached logo, do not invent an unrelated letter M. No words, no typography, no tagline. One centered large emblem filling 78% of a square solid warm ivory #EEECE4 background. This is a brand asset for native Android TV MIMO TV. Output a single square icon, no mockup or device.

## Technical references

- [Android TV app requirements](https://developer.android.com/training/tv/get-started/create)
- [Media3 player setup and release](https://developer.android.com/media/media3/exoplayer/hello-world)
- [AGP 8.9 compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [Existing backend repository](https://github.com/mimodxb/mimo-iptv)
