# Third-party components

AndroidX Compose, Compose for TV, TV Provider, Lifecycle and Media3: Apache License 2.0, Android Open Source Project.

OkHttp: Apache License 2.0, Square and contributors.

Coil: Apache License 2.0, Coil contributors.

ZXing core 3.5.3: Apache License 2.0, ZXing authors. Generates local pairing QR codes.

Bouncy Castle bcpkix/bcprov/bcutil 1.87: Bouncy Castle Licence (MIT-style), Legion of the Bouncy Castle. Generates the per-install, encrypted-at-rest HTTPS identity. Dependency licence/notice resources are retained in the APK.

Gson 2.15.0: Apache License 2.0, Google and contributors. Jsoup 1.24.1: MIT License, Jonathan Hedley and contributors. Host dependencies for user-provided TVBox modules.

The TVBox host interface and loopback proxy implementation in this project do not include user-supplied Spider modules. Such modules are downloaded only after a user imports their subscription. A separate worker process protects UI availability; it has the application UID and is not a permission sandbox.

TinyPinyin (io.github.biezhi:TinyPinyin:2.0.3.RELEASE): Apache License 2.0, promeG and biezhi contributors. The Maven Central repackaging is used because the original JCenter artifact is unavailable from the configured repositories.

Kotlin and kotlinx.coroutines: Apache License 2.0, JetBrains and contributors.

Default MacCMS station configuration preserves PCBOX's existing public station names and stable keys. Private subscriptions, personal credentials, protected module downloads and PC user databases are not bundled. Providers are third-party network services; successful configuration loading is not playback validation.

Playback uses Android platform decoders through Media3; Windows Flyleaf/FFmpeg binaries and Windows runtimes are not copied into this APK.
