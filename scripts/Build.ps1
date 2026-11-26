[CmdletBinding()]
param([switch]$DebugOnly,[switch]$ArmHost)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Push-Location $root
$names=@('PTVBOX_KEYSTORE','PTVBOX_STORE_PASSWORD','PTVBOX_KEY_PASSWORD')
$prior=@{};foreach($name in $names){$prior[$name]=[Environment]::GetEnvironmentVariable($name,'Process')}
try {
    New-Item -ItemType Directory -Force .tools,artifacts | Out-Null
    if(-not $DebugOnly -and -not $env:PTVBOX_KEYSTORE){
        if(-not $IsWindows -and $env:OS -ne 'Windows_NT'){throw 'Set signing environment variables before a release build on this OS'}
        $store=Join-Path $root '.tools/ptvbox-release.jks'
        $secretPath=Join-Path $root '.tools/signing-password.dpapi'
        if(-not(Test-Path -LiteralPath $store)){
            if(Test-Path -LiteralPath $secretPath){throw 'Signing key missing; refusing to silently change update identity'}
            $bytes=New-Object byte[] 32;[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
            $password=[Convert]::ToBase64String($bytes)
            ConvertTo-SecureString -String $password -AsPlainText -Force | ConvertFrom-SecureString | Set-Content -LiteralPath $secretPath
            $env:PTVBOX_STORE_PASSWORD=$password;$env:PTVBOX_KEY_PASSWORD=$password
            & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $store -alias ptvbox -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Ptvbox, O=Ptvbox' -storepass:env PTVBOX_STORE_PASSWORD -keypass:env PTVBOX_KEY_PASSWORD
            if($LASTEXITCODE -ne 0){throw 'Signing key generation failed'}
        }else{
            if(-not(Test-Path -LiteralPath $secretPath)){throw 'Signing password missing; restore original signing backup'}
            $secure=(Get-Content -LiteralPath $secretPath -Raw).Trim() | ConvertTo-SecureString
            $env:PTVBOX_STORE_PASSWORD=[Net.NetworkCredential]::new('',$secure).Password
            $env:PTVBOX_KEY_PASSWORD=$env:PTVBOX_STORE_PASSWORD
        }
        $env:PTVBOX_KEYSTORE=$store
    }
    $variant=if($DebugOnly){'Debug'}else{'Release'}
    $extra=@();if($ArmHost){& "$PSScriptRoot/Build-ArmHost.ps1";if($LASTEXITCODE -ne 0){throw 'ARM build failed'};$extra+= '-PptvboxArmHost=true'}
    & ./gradlew.bat ':app:testDebugUnitTest' ":app:assemble$variant" --console=plain --no-configuration-cache @extra
    if($LASTEXITCODE -ne 0){throw 'Build or regression tests failed'}
    $version=[regex]::Match((Get-Content app/build.gradle.kts -Raw),'versionName = "([0-9.]+(?:-c[0-9]+)?)"').Groups[1].Value
    $isCandidate=$version -match '-c[0-9]+$'
    $baseVersion=$version -replace '-c[0-9]+$',''
    $relFolder=if($isCandidate){"artifacts/candidate-v$baseVersion"}else{"artifacts/release-v$version"}
    $folder=Join-Path $root $relFolder;New-Item -ItemType Directory -Force $folder | Out-Null
    if($isCandidate){Get-ChildItem -LiteralPath $folder -Filter '*.apk' -ErrorAction SilentlyContinue | Where-Object {$_.Name -notlike "Ptvbox-$version*"} | Remove-Item -Force -ErrorAction SilentlyContinue}
    else{$candDir=Join-Path $root "artifacts/candidate-v$baseVersion";if(Test-Path -LiteralPath $candDir){Remove-Item -LiteralPath $candDir -Recurse -Force -ErrorAction SilentlyContinue}}
    $suffix=if($DebugOnly){'-QA'}elseif($ArmHost){'-ARM'}else{''};$file="Ptvbox-$version$suffix.apk"
    $source=if($DebugOnly){'app/build/outputs/apk/debug/app-debug.apk'}else{'app/build/outputs/apk/release/app-release.apk'}
    Copy-Item -LiteralPath $source -Destination (Join-Path $folder $file)
    $hash=(Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $folder $file)).Hash.ToLowerInvariant()
    $sums=Join-Path $folder 'SHA256SUMS.txt';$lines=if(Test-Path $sums){@(Get-Content $sums | Where-Object {(-not $isCandidate -or $_ -match ('  Ptvbox-'+[regex]::Escape($version))) -and $_ -notmatch ('  '+[regex]::Escape($file)+'$')})}else{@()};@($lines)+"$hash  $file" | Set-Content -LiteralPath $sums -Encoding ascii
    Write-Output "Built: $folder/$file"
}finally{foreach($name in $names){[Environment]::SetEnvironmentVariable($name,$prior[$name],'Process')};Pop-Location}
