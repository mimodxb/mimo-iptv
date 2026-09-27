## 2026-09-27: TCL Azerbaijan regression repair (rc3)

- Installed signed versionCode 102 on TCL, preserving settings.
- Reproduced Real TV TLS failure on TCL Android 9: GlobalSign Root R46 is missing from firmware trust store. Added the fingerprint-verified official root only for yodacdn.net. Certificate and hostname validation remain enabled. No trust-all code.
- Corrected public playlist @SD/@HD/@FHD/@UHD identity normalization so country grouping and alternate merging work.
- Backend deployed revision 11: corrected AzTV /aztv/ (404) to /azertv/; replaced CBC and CBC Sport with streams discovered on their official live pages. CBC requires the public player's Referer and User-Agent directives.
- Build, 66 unit tests and lint passed. Initial sandbox test run could not open local HTTP test sockets; same tests passed with local sockets permitted.
- Real TV no longer reports TLS failure on TCL and initializes audio/video decoders. Physical video/audio confirmation is pending; captured hardware video is black and is not acceptance evidence.
- Space TV remains unresolved. Alvin/Kapaz sustained playback and remaining named channels still require device acceptance. Do not redo bulk logo audit.
- APK: outputs/MIMO-TV-1.0.0-rc3/MIMO-TV-1.0.0-rc3.apk. Signing key unchanged and private.

Certificate source: https://secure.globalsign.com/cacert/rootr46.crt
SHA-256: 4FA3126D8D3A11D1C4855A4F807CBAD6CF919D3A5A88B03BEA2C6372D93C40C9
CBC official page: https://cbctv.az/info/pages/tv-live
CBC Sport official page: https://www.cbcsport.tv/live/
