[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$sdk=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{throw 'ANDROID_HOME is required'}
$ndk=Get-ChildItem (Join-Path $sdk 'ndk') -Directory | Sort-Object Name -Descending | Select-Object -First 1
if(-not $ndk){throw 'Install an Android NDK to build the optional ARM host marker'}
$bin=Join-Path $ndk.FullName 'toolchains/llvm/prebuilt/windows-x86_64/bin'
foreach($entry in @(@('arm64-v8a','aarch64-linux-android26-clang.cmd'),@('armeabi-v7a','armv7a-linux-androideabi26-clang.cmd'))){
    $folder=Join-Path $root "app/build/armhost-libs/$($entry[0])"
    New-Item -ItemType Directory -Force $folder | Out-Null
    & (Join-Path $bin $entry[1]) -shared -fPIC '-Wl,-soname,libptvbox_host.so' -o (Join-Path $folder 'libptvbox_host.so') (Join-Path $root 'app/src/main/cpp/spider_host.c')
    if($LASTEXITCODE -ne 0){throw 'ARM host marker compilation failed'}
}
