# Phase-one validation

Validated locally on 12 September 2026. This document distinguishes the compiled Android implementation, simulated Android tests, backend observations, and hardware work still outstanding.

## Confirmed

- Debug APK assembled from the supplied native source: package `tv.mimo.app`, version `0.1.0`, minimum SDK 23, target SDK 35.
- APK signature verification passed with v1 and v2 signing. Standard v1 META-INF metadata warnings were emitted; v2 APK verification passed.
- Packaged manifest declares the Android TV/Leanback launcher activity, required Leanback support, optional touchscreen, TV banner, and Internet access.
- **25 automated tests passed, zero failures, errors, or skipped tests.**

| Suite | Tests | Coverage |
|---|---:|---|
| CatalogTest | 13 | Unicode and quoted metadata, country classification, priority identities, cross-source merging, stream headers, relative URLs, invalid feeds, stable favorite IDs |
| RecoveryPlanTest | 5 | One backend refresh, no repeated URL loops, backend-first replacement, unavailable states, four-attempt cap |
| AndroidShellTest | 7 | Native shell startup and all section click handlers, stored source settings, favorites, empty source list, offline priority cards, XMLTV matching/time zones and declaration rejection |

Android screen tests use Robolectric with simulated Android API 28. They do not emulate a TCL video decoder, physical D-pad timing, or the visual output of the television.

The app’s actual Java playlist parser was also run against the fetched production M3U:

| Category | Parsed entries |
|---|---:|
| Azerbaijan | 21 |
| Russia | 389 |
| Turkey | 152 |
| Europe | 2,430 |
| World | 5,204 |
| Total | 8,196 |

Xəzər’s Unicode name, stable ID and selected HTTPS URL survived parsing. Space was absent. The app adds its unavailable priority card while Mimo IPTV is enabled, so the UI can have one more card than the delivered channel count.

Backend v7 diagnostics reported `DEGRADED`: Xəzər’s manifest passed; Space’s candidates failed. See `design/backend-check-2026-09-12.json` for the observed result. No backend configuration or stream override was changed.

The design preview’s inline JavaScript passed a syntax check. The derived brand emblem was visually inspected. Automated browser viewing of the local HTML was blocked by browser URL policy and was not retried through another route; the preview therefore has no browser visual-verification claim.

## Final build gate

Final command: `assembleDebug testDebugUnitTest lintDebug` — **BUILD SUCCESSFUL**, exit code 0. The final run completed after adding Android 12+ data-extraction rules; all 25 tests passed again.

Android lint: **0 errors, 23 warnings**. Remaining warnings are recorded verbatim in `validation/android-lint.txt`. They include pinned dependencies with newer releases, English UI string construction, fixed landscape orientation for TV, the square launcher emblem, and broad grid refresh notifications. One backup advisory requests an additional legacy full-backup rule; Android versions below 12 already have backup disabled by `allowBackup=false`. No lint errors were suppressed to pass the gate.

Private app data is excluded from Android 12+ cloud backup and device-transfer rules, in addition to `allowBackup=false`. The final APK was rechecked for its signature and manifest declarations. The source archive includes test result XML and verification reports; machine-specific SDK paths, caches, generated build directories, and signing keys are excluded.

## Still requires the TCL

- Actual HLS/DASH video and audio decoding; priority channels first.
- D-pad direction, long press, focus restoration, safe margins, and on-screen keyboard behaviour.
- Pause/resume, Home/background behaviour, interruptions and extended buffering.
- Merged guide delivery and real broadcaster programme coverage.
- Repeated offline/online operation with multiple user-provided playlists.

The compiled APK is suitable for phase-one testing, not a claim that Space or every upstream channel currently plays. It has not been installed on the user’s TV, published to GitHub, or submitted to an app store.
