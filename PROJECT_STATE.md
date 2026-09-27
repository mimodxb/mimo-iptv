# MIMO TV checkpoint — 27 September 2026

## Latest follow-up: rc2 installed on Samsung

Settings contrast corrected and visually verified on SM-S928B. Owner approved replacing the old signature; installation succeeded. Playback recovery now detects repeated short buffering and the mobile stall detector starts correctly. Release build, lint and 65 tests pass. Evening Xəzər stability and TCL testing remain open; Space candidates still fail.

## Continue here; do not restart the audit

Integrated `feature/mobile-integration` and `feature/tv-library-navigation` into `release/tv-mobile-completion-20260927`.

Implemented: bounded aspect-preserving image loader, SVG decoding, verified alternate-logo index, mobile Home/category/Favorites navigation, source CRUD, English/Azerbaijani selection, actual XMLTV guide, serialized catalogue loading, landscape layout correction and player rotation correction. Production Supabase playlist v9 is deployed and publicly verified: upstream logo metadata survives, Xəzər exposes both official media-probed candidates, Space correctly reports degraded.

Final validation PASS: assembleRelease, assembleDebug, 63 Android tests, lintDebug, and 3 backend tests. Signed release APK verified by apksigner. The large production XMLTV guide (56 MB, 73,979 programmes, dates 26 Sep–4 Oct) now streams through the parser rather than loading as one String.

## Remaining acceptance blockers

1. Device testing: Samsung SM-S928B is connected, but installation was rejected because the existing app has a different signing key. Await explicit owner permission to replace the old installation; do not uninstall silently. TCL 192.168.100.6:5555 did not respond. Await its current IP. Verify video/audio, remote controls, rotation and logos on the actual devices.
2. Space TV: all existing and additionally checked public candidates failed. A working source is required; do not claim repaired from manifest success alone.
3. Logo audit: 7,544 / 8,239 stream entries had verified raster responses after 140 alternate-logo repairs. 695 remained unverified. Most HTTP failures were Wikimedia rate limiting. Do not rerun the full bulk audit; use the saved unresolved list for targeted follow-up.
4. Physical acceptance and complete logo coverage remain open. Do not label the project 100% complete or overwrite these blockers with a generic PASS.

## Local evidence and signing

Workspace: `work/mimo-current`. Evidence: sibling `work/current-audit`, including build logs, live backend v9 response, logo HTTP results, repair summary, and exact unresolved entries. Private release key: sibling `work/mimo-signing/mimo-release.jks`; its password is encrypted to the Windows user in `password.dpapi`. Preserve both outside GitHub. Deliverables go in `outputs`.

Future work should use the checkpoint and only resolve the blockers above. Do not repeat repository initialization, broad Notion discovery, or the catalogue-wide logo scan.



Latest checkpoint: [rc3 TCL regression repair](docs/tcl-rc3-repair.md). Read this before repeating diagnostics.
