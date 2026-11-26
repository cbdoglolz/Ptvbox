#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

: "${ANDROID_HOME:?ANDROID_HOME is required}"
: "${PTVBOX_KEYSTORE:?PTVBOX_KEYSTORE is required}"
: "${PTVBOX_STORE_PASSWORD:?PTVBOX_STORE_PASSWORD is required}"
: "${PTVBOX_KEY_PASSWORD:?PTVBOX_KEY_PASSWORD is required}"

version="$(sed -n 's/.*versionName = "\([0-9.]*\)".*/\1/p' app/build.gradle.kts)"
if [[ ! "$version" =~ ^[0-9]\.[0-9]\.[0-9]$ ]]; then
  echo "Invalid decimal version: $version" >&2
  exit 1
fi

ndk="$(find "$ANDROID_HOME/ndk" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
test -n "$ndk"
case "$(uname -s)" in
  Linux*) prebuilt="linux-x86_64"; clang_suffix="" ;;
  MINGW*|MSYS*) prebuilt="windows-x86_64"; clang_suffix=".cmd" ;;
  *) echo "Unsupported build host: $(uname -s)" >&2; exit 1 ;;
esac
ndk_bin="$ndk/toolchains/llvm/prebuilt/$prebuilt/bin"

rm -rf dist
mkdir -p dist "app/build/armhost-libs/arm64-v8a" "app/build/armhost-libs/armeabi-v7a"

chmod +x gradlew
./gradlew :app:testDebugUnitTest :app:assembleRelease --no-configuration-cache
cp app/build/outputs/apk/release/app-release.apk "dist/Ptvbox-$version.apk"

"$ndk_bin/aarch64-linux-android26-clang$clang_suffix" -shared -fPIC \
  -Wl,-soname,libptvbox_host.so \
  -o app/build/armhost-libs/arm64-v8a/libptvbox_host.so \
  app/src/main/cpp/spider_host.c
"$ndk_bin/armv7a-linux-androideabi26-clang$clang_suffix" -shared -fPIC \
  -Wl,-soname,libptvbox_host.so \
  -o app/build/armhost-libs/armeabi-v7a/libptvbox_host.so \
  app/src/main/cpp/spider_host.c

./gradlew :app:assembleRelease -PptvboxArmHost=true --no-configuration-cache
cp app/build/outputs/apk/release/app-release.apk "dist/Ptvbox-$version-ARM.apk"

sha256sum dist/*.apk
