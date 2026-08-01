param(
    [Parameter(Mandatory = $true)]
    [string]$Tag
)

$ErrorActionPreference = "Stop"

$versionCode = (Select-String -Path "app/build.gradle.kts" -Pattern 'versionCode\s*=\s*(\d+)' | Select-Object -First 1).Matches.Groups[1].Value
$versionName = (Select-String -Path "app/build.gradle.kts" -Pattern 'versionName\s*=\s*"([^"]+)"' | Select-Object -First 1).Matches.Groups[1].Value

if ([string]::IsNullOrWhiteSpace($versionCode) -or [string]::IsNullOrWhiteSpace($versionName)) {
    throw "Could not read versionCode/versionName from app/build.gradle.kts"
}
if ($Tag -ne "v$versionName") {
    throw "Tag $Tag does not match versionName $versionName"
}

$properties = "app/keystores/keystore.properties"
if (!(Test-Path -LiteralPath $properties)) {
    throw "Missing $properties. Configure local release signing before preflight."
}

& .\gradlew.bat :app:assembleModernReleaseSigned --no-daemon
if (!$?) {
    throw "Signed release build failed"
}

$apk = Get-ChildItem -Path "app/build/outputs/apk" -Filter "*.apk" -File -Recurse |
    Where-Object { $_.FullName -match "modern[\\/]release-signed" } |
    Select-Object -First 1
if ($null -eq $apk) {
    throw "Signed modern APK was not produced"
}

$buildTools = Get-ChildItem -Path "$env:ANDROID_HOME/build-tools" -Directory |
    Sort-Object Name -Descending |
    Select-Object -First 1
$apksigner = Join-Path $buildTools.FullName "apksigner.bat"
if (!(Test-Path -LiteralPath $apksigner)) {
    throw "apksigner was not found under ANDROID_HOME"
}

& $apksigner verify --verbose $apk.FullName
if (!$?) {
    throw "APK signature verification failed"
}

$sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk.FullName).Hash.ToLowerInvariant()
Write-Output "Release preflight passed"
Write-Output "Tag: $Tag"
Write-Output "Version code: $versionCode"
Write-Output "APK: $($apk.FullName)"
Write-Output "SHA-256: $sha256"
