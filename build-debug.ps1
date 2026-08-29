$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

Write-Host 'CareerOps Share v0.1.1 - debug build'
Write-Host 'Using Java:'
& java -version

if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
    Write-Warning 'ANDROID_HOME / ANDROID_SDK_ROOT is not set. Android Studio may still provide the SDK when building inside the IDE.'
}

& .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) {
    throw "Gradle completed but the expected APK was not found: $apk"
}

Write-Host ''
Write-Host "APK ready: $apk"
