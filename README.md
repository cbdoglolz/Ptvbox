# Ptvbox

Private Android TV client workspace for the PCBOX feature set.

Status: architecture/bootstrap only. **No working Android client or APK is claimed yet.** Current implementation priority is the independent component maintenance pipeline.

Planned stack: Kotlin, Compose for TV, Android playback adapters, D-pad focus management, voice input with keyboard fallback, and a catalog-backed pinyin-initial search index.

Shared with PCBOX: station identities and schemas, metadata/season/episode matching contracts, signed component update manifest, and regression fixtures. Windows binaries, WinUI, Windows HDR calls, DPAPI credentials and Node.exe are not Android binaries.

Future delivery: a published PC source tag triggers an Android build against its recorded compatible source revision; scheduled checks are a fallback. APK signing identity remains stable; generated Releases stay drafts for the owner to publish. Until an Android implementation exists, no fake build-success workflow or version-only APK is generated.

Do not commit personal subscriptions, API keys, login sessions, keystores or viewing history.
