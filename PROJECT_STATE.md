# MIMO TV — PROJECT STATE

**Last updated:** 12 September 2026
**Updated by:** OpenCode
**Reason:** Initial canonical project-state creation from existing audit plus independently verified external corrections.

---

## 1. PROJECT IDENTITY

| Item | Value |
|---|---|
| Name | MIMO TV |
| Package ID | `tv.mimo.app` |
| Local path | `C:\Master Folder All Projects - Claude Assisting\MIMO-TV` |
| Version | `0.1.0` (versionCode 1) |
| Language | Java |
| UI | Native Android Views + RecyclerView |
| Playback | Media3/ExoPlayer 1.6.1 (HLS + DASH) |
| Min SDK | 23 (Android 6.0) |
| Target SDK | 35 (Android 15) |
| Device | Android TV (TCL), Leanback launcher |

---

## 2. FINAL PRODUCT INTENT

MIMO TV is a native Android TV IPTV application for Mimo's Collective. It operates entirely by TV remote / D-pad. It delivers live channels from the existing Mimo IPTV Supabase backend, organized into Azerbaijan, Russia, Turkey, Europe, and World categories. Features include persistent favorites, accent-insensitive search, multiple M3U playlist support with per-source XMLTV guide URLs, on-demand programme now/next display, full Media3 playback with automatic recovery, offline caching, and priority identity handling for Xəzər TV and Space TV. The intended final product also includes Azerbaijani/English language switching, an About/creator section with photo and contact information, and "by Movsum Mirzazada" branding.

---

## 3. CURRENT ANDROID APP STATE

**VERIFIED LOCALLY** — 9 Java source files, 3 test files.

**Implemented in the real Android app:**
- TV launcher shell, landscape, Leanback intent filter
- D-pad navigation with visible focus outlines, left-from-grid returns to rail
- Home screen with hero section and category tabs
- Five categories: Azerbaijan, Russia, Turkey, Europe, World
- Channel loading from network with offline cache fallback
- Full M3U parser (attributes, VLC options, pipe headers, relative URLs)
- HLS and DASH playback via Media3/ExoPlayer
- Search (accent-insensitive, filters name + category + country)
- Favorites (persistent via SharedPreferences, toggle by hold OK or Menu)
- XMLTV EPG parser; on-demand guide loading with now/next display
- Multiple playlist management (add, edit, enable/disable, remove, refresh)
- Settings page with source management, brand section, Mimo IPTV restore
- Unavailable-channel placeholder cards for missing priority channels
- Priority identity for Xəzər TV and Space TV with alias merging
- Network/offline handling with cache fallback and error notices
- RecoveryPlan: max 4 playback attempts, one backend refresh, URL+header deduplication
- Backend diagnostic check button (`?check=1`)

**NOT implemented in the Android app:**
- Azerbaijani language / language toggle
- About page / creator section
- "by Movsum Mirzazada" branding
- Channel logo display on cards
- Production APK signing

**design/preview.html is a browser design prototype only.** It does not play streams. Its channels are hardcoded JavaScript objects. Do not confuse preview.html features with the real Android app.

---

## 4. UI / DESIGN STATE

**VERIFIED LOCALLY**

The Android app uses a dark teal/green/gold color scheme (`#091516` background, `#B8D977` focus, `#D9AE51` accent). Layout is a persistent left rail (brand, navigation, signature) with a main content area showing a four-column channel grid. Cards are typographic (channel name, category, priority label). Playback is full-screen with auto-hiding chrome.

Design tokens and screen contracts are documented in `design/DESIGN.md`.

**Features approved in design but existing only in preview.html (not yet in Android):**
- Azerbaijani language system and language toggle
- About page (biography, photo, signature, contact information)
- "by Movsum Mirzazada" byline in the sidebar
- Design assets: `design/assets/about-photo.png`, `design/assets/about-signature.png`

---

## 5. BACKEND STATE

**VERIFIED EXTERNALLY** (HTTP fetch + independent GitHub verification)

| Endpoint | URL | Version |
|---|---|---|
| Playlist | `https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-iptv` | 7 |
| EPG (merged) | `https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-merged` | 21 |
| EPG (AZ) | `https://nkuhaupwlxadvihnnned.supabase.co/functions/v1/mimo-epg-az` | 1 |
| Diagnostics | Playlist URL with `?check=1` | — |

**Production Edge Function source:** The Edge Function source is currently stored/deployed in Supabase and is not currently version-controlled in the mimodxb/mimo-iptv GitHub repository. The GitHub repository (https://github.com/mimodxb/mimo-iptv) currently contains documentation only.

The Android app preconfigures the Mimo IPTV playlist and EPG endpoints. No embedded Supabase admin key is required. No backend deployment is part of phase one.

---

## 6. CHANNEL STATE

**HISTORICAL / SAVED DIAGNOSTIC** — `design/backend-check-2026-09-12.json` (12 September 2026)

| Category | Count |
|---|---:|
| Azerbaijan | 21 |
| Russia | 389 |
| Turkey | 152 |
| Europe | 2,430 |
| World | 5,204 |
| **Total** | **8,196** |

Backend status: `DEGRADED`

**Xəzər TV:** Working — HLS manifest check passed (HTTP 200). Priority identity with alias merging (`xezertv.az`, `khazartv.az`, normalized name variants).

**Space TV:** Not working — two backend candidates both failed. Candidate 1: "signal aborted". Candidate 2: DNS resolution failure for `streams.livetv.az`. Space TV is absent from the delivered playlist. The Android app inserts an unavailable priority placeholder card (`Repository.java:101`).

**Proposed new Space TV candidate:** `http://109.205.166.68/server124/space_tv/index.m3u8` — NOT present in the local Android project. Also independently verified as NOT present in the currently deployed Supabase `mimo-iptv` version 7 source. Do not add or test this URL without coordinating backend and app changes together.

---

## 7. EPG STATE

**VERIFIED LOCALLY** (code) + **EXTERNAL** (HTTP fetch)

Android side: `Epg.java` implements a full XMLTV parser using `XmlPullParser`. Wired correctly — `Repository.java:13` defines the EPG URL; `loadGuide()` fetches on demand; `programme()` displays now/next.

Backend data:
- `mimo-epg-merged` (v21): Returns valid XMLTV with ~848 channels. Mostly Malaysian/Indian/international channels. Only 1 of 12 known Azerbaijani playlist tvg-ids (`IdmanTV.az`) found in the EPG data.
- `mimo-epg-az` (v1): Returns HTTP 502 with body `EPG build error: Value is not a valid ByteString`. Not functional.

**Known problem:** Near-total mismatch between playlist tvg-id values (`ATV.az`, `IctimaiTV.az`, etc.) and EPG channel IDs. The app will show "Programme information unavailable" for most Azerbaijani channels.

---

## 8. LANGUAGE / ABOUT STATE

**VERIFIED LOCALLY**

Azerbaijani language, language switching, the About page, creator photo/signature, and "by Movsum Mirzazada" branding currently exist **only in `design/preview.html`**. They have NOT been ported to the real Android application.

The Android `app/src/main/res/values/strings.xml` contains only `<string name="app_name">MIMO TV</string>`. All UI text is hardcoded in English within Java source files. No `values-az/` directory exists.

---

## 9. BUILD / TEST STATE

**VERIFIED LOCALLY**

| Item | Result |
|---|---|
| Build command | `assembleDebug testDebugUnitTest lintDebug` |
| Build result | **BUILD SUCCESSFUL** (exit code 0) |
| Date | 12 September 2026 |
| Tests | **25/25 passed**, 0 failures, 0 errors, 0 skipped |
| Lint | **0 errors, 23 warnings** (SetTextI18n, GradleDependency, fixed orientation, launcher icon shape) |
| APK signing | v1 JAR + v2 APK Signature Scheme verified (debug key, self-signed) |
| APK file | `app\build\outputs\apk\debug\app-debug.apk` — 7.78 MB, generated 12 Sep 2026 |
| Package ID | `tv.mimo.app` (verified via `apk-metadata.txt`) |
| Hardware test | **DONE** — APK installed and launched on TCL BeyondTV via ADB (12 Sep 2026). Full acceptance testing pending. |

Test suites: CatalogTest (13), RecoveryPlanTest (5), AndroidShellTest (7). All results saved in `validation/`.

---

## 10. CURRENT BLOCKERS / PENDING WORK

### P0 — Blocks core use
1. Install APK on TCL TV and test playback, D-pad, focus — requires TCL/ADB
2. Fix Space TV stream — both backend candidates failed; needs backend change in Supabase
3. Test Xəzər TV on real hardware — manifest check passed but device decoding unverified

### P1 — Before family testing
4. Add Space TV candidate URL to Edge Function — requires Supabase access
5. Resolve `mimo-epg-az` 502 error — requires Supabase access
6. Fix EPG channel ID mismatch — requires Supabase access + user decision

### P2 — Before public release
7. Extract hardcoded strings to `strings.xml` and create `values-az/strings.xml` — local code
8. Port About page and "by Movsum Mirzazada" from preview.html to Android — local code
9. Add channel logo display on cards — local code, UI/polish task
10. Production APK signing — user decision

---

## 11. NEXT ACTION

1. ~~Generate a fresh debug APK using the existing Android build (`assembleDebug`).~~ DONE
2. ~~Install that APK on the TCL Android TV through ADB.~~ DONE (TCL BeyondTV, 12 Sep 2026)
3. Run the 8-step acceptance checklist documented in `README.md` (lines 62–71).

This requires physical TV access and an authorized ADB connection. Do NOT automatically execute these steps — they require the owner to connect the TV.

---

## 12. SOURCE CONTROL STATE

**VERIFIED LOCALLY**

The local Android project at `C:\Master Folder All Projects - Claude Assisting\MIMO-TV` is **NOT a Git repository**. No `.git/` directory exists.

The external GitHub repository at https://github.com/mimodxb/mimo-iptv currently contains documentation only (README.md). It does not contain the Supabase Edge Function source code.

---

## 13. ACCESS BOUNDARIES

**What OpenCode can verify locally:**
- All Android Java source code, resources, manifests, build files
- Test results and lint output in `validation/`
- Design files in `design/`
- HTTP responses from public Supabase endpoints (playlist, EPG)

**What requires independent verification:**
- Supabase Edge Function source code and deployment configuration
- GitHub repository contents beyond README
- TCL TV hardware behavior, D-pad focus, video decoding
- ADB connectivity and APK installation
- Whether the proposed Space TV URL actually works for HLS playback
- Supabase dashboard access, function versioning, logs

**Do not claim access to GitHub, Supabase, TV hardware, or another external system unless that access was actually used.**

---

## 14. AGENT CONTINUITY RULES

1. Read PROJECT_STATE.md before starting any project work.
2. Do not repeat a full project audit unless PROJECT_STATE.md is missing, clearly corrupted, or the owner explicitly requests an audit.
3. Verify only the specific information needed for the current task.
4. Never treat conversation memory as stronger evidence than current project files or direct system verification.
5. Never claim access to GitHub, Supabase, TV hardware, or another external system unless that access was actually used.
6. After completing a material project change, update PROJECT_STATE.md with the verified result.
7. Do not mark planned work as completed.
8. Record unresolved contradictions instead of guessing.
9. Keep PROJECT_STATE.md concise. Do not paste logs, large code blocks, or full conversation histories into it.
10. Do not create additional competing status/handover files unless explicitly requested.
11. `opencodereport.txt` remains supporting audit evidence; PROJECT_STATE.md is the canonical current-state file.

---

## 15. LAST UPDATED

| Field | Value |
|---|---|
| Date | 12 September 2026 |
| Updated by | OpenCode |
| Reason | Debug APK installed on TCL BeyondTV via ADB; first app launch confirmed; acceptance testing pending |
| Next scheduled review | When a material project change is completed (e.g., first TV install, backend fix, language port) |
