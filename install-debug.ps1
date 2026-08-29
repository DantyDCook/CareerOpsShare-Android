$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) {
    throw 'Debug APK not found. Run build-debug.ps1 first.'
}

$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    throw 'adb is not on PATH. Install Android SDK Platform Tools or run the app from Android Studio.'
}

& adb devices
& adb install -r $apk
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'CareerOps Share installed.'
