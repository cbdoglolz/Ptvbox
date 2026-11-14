# Implementation order

1. Consume the same signed component schema as PCBOX, with platform compatibility and explicit activation.
2. Establish a buildable TV project and run D-pad search → details → playback → subtitles → resume on the target box.
3. Build a feature parity matrix for discovery, source groups, source ranking, live/sports, online/local/embedded subtitles, danmaku, network storage, audio/HDR, history and settings. Hardware-specific output is verified separately.
4. Add system voice input plus a catalog-backed pinyin-initial index, preserving aliases and exact season/episode matching.
5. Bind verified PC source tags to Android CI. Changes without an Android implementation are marked pending rather than passed off as full parity. APK updates require stable package/signature and increasing versionCode.

Remote-friendly behavior includes focus restoration, clear selected states, return to the original source detail, long-press adjustments, and accessible QR/auth dialogs. Credentials stay device-local.
