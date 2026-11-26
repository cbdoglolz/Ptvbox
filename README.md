# Ptvbox

Private Android TV client workspace for the PCBOX feature set.

Status: signed ATV build **1.0.0** / versionCode 24. This release hardens private-source notice suppression in both the app and `:spider` processes, verifies the three reported plugin notices are hidden, and plays a real private-subscription candidate for 40 seconds without those notices. It retains 0.9.0 live danmaku preview, category source routing, Chinese anime metadata enrichment, cross-site IDs and detail caching. See the [1.0.0 report](docs/reports/Ptvbox-v1.0.0-Private-Source-Notice-Hardening.md) and [checklist](PROGRESS-CHECKLIST.md). Physical-TV acceptance, complete source coverage, cross-platform migration and automated source maintenance remain incomplete.

Start any handoff or development session from [PROJECT-CONTINUITY.md](PROJECT-CONTINUITY.md). It records the current architecture, version, tools, release state, verified/unverified work, risks and the exact next resume point. Cross-project procedures are defined in `C:\Users\cbdog\.codex\PROJECT-CONTINUITY.md`.

Version names use decimal places with one digit per component: after `0.9.9` comes `1.0.0`; two-digit components such as `0.10.0` are rejected. Tagged releases are built by GitHub Actions and attached to a draft GitHub Release containing only the signed universal and ARM APKs.

For ARM-only user Jar modules in BlueStacks, use `scripts/Build.ps1 -ArmHost` and the ARM APK; the normal build retains x86 support. Both variants share the same signing identity and data. Modules, personal configuration and credentials are never packaged. QR configuration requires a direct LAN path; emulator NAT needs an explicit PC port forward.

Stack: Kotlin, Compose for TV, Media3, local SQLite, Android Keystore, D-pad navigation, system voice input with keyboard fallback, and cached-catalog pinyin-initial search.

Shared with PCBOX: station identities and schemas, metadata/season/episode matching contracts, signed component update manifest, and regression fixtures. Windows binaries, WinUI, Windows HDR calls, DPAPI credentials and Node.exe are not Android binaries.

Build with JDK 21 and the Android SDK using `./gradlew :app:testDebugUnitTest :app:assembleDebug`. Windows local signed delivery uses `scripts/Build.ps1`; its stable signing key and DPAPI password stay under ignored `.tools`. Tagged `vX.Y.Z` releases run `scripts/Build-GitHub.sh`, attach only the two signed APKs to a draft GitHub Release, and do not upload logs or test evidence. Generated Releases must stay drafts for the owner to publish.

ATV home integration provides Watch Next and optional recommendation channels: Douban series/movies, Netflix-tagged series/movies, TMDB series/movies when configured, and recent/popular animation. The TV launcher controls channel visibility; use the system authorization buttons in Settings or the launcher's Customize Home Screen. Channel refresh retains last-good data on failures.

Do not commit personal subscriptions, API keys, login sessions, keystores or viewing history.

Approved staged design: [PCBOX 1.1.4 visual and behavior baseline with TV controls](docs/plans/PCBOX-1.1.4-ATV-Redesign-Plan.md). 0.5.0 resource-stage progress remains subject to owner review; later resource and advanced playback stages require separate acceptance.

Danmaku integration: [弹弹play开放弹幕网络](https://www.dandanplay.com/), owner-provided credentials/custom compatible services, public relay and Bilibili fallback. No borrowed developer credentials are included.
