# v0.1.1 Validation Status

CareerOps Share v0.1.1 reached the following validation state on 2026-08-29.

## Component validation

- PASS — `ShareParser.kt` compiles with the available Kotlin compiler.
- PASS — LinkedIn source detection.
- PASS — Indeed source detection.
- PASS — generic job-site hostname fallback.
- PASS — trailing URL punctuation cleanup.
- PASS — plain-text shares without a URL.
- PASS — lookalike-domain guard (`notlinkedin.com` is not classified as LinkedIn).
- PASS — Android XML resources/manifest are well-formed XML.
- PASS — compatibility `gradle-wrapper.jar` contains `org.gradle.wrapper.GradleWrapperMain`.
- PASS — compatibility wrapper end-to-end smoke test, including SHA-256 verification, extraction, and process launch.
- PASS — version/config assertions for v0.1.1, Gradle 9.5.0, `text/*` share target, and ChatGPT package routing.

## Android build validation

- PASS — Android Studio successfully produced `app-debug.apk`.
- PASS — build metadata reported application ID `com.careerops.share`.
- PASS — build metadata reported `versionCode = 2` and `versionName = 0.1.1`.
- PASS — build metadata reported debug variant and minimum dex SDK 26.

Validated APK SHA-256 from the successful local build:

```text
ff110f7f64a0abb0d650dc26e3dd6ba3502d382591c588c79e2add7379fe2eda
```

## Physical device validation

- PASS — APK installed and launched on a physical Android device.
- PASS — CareerOps Share appeared in the Android Sharesheet.
- PASS — shared job content was received by CareerOps Share.
- PASS — the prepared CareerOps payload was forwarded successfully to ChatGPT.

## CI / release automation

The repository includes:

- `.github/workflows/android-ci.yml` — builds and tests pushes and pull requests, then stores the debug APK as a temporary Actions artifact.
- `.github/workflows/android-release.yml` — on a `v*` tag, tests and builds the APK, verifies the tag matches `versionName`, calculates SHA-256, and publishes both files to GitHub Releases.

The first GitHub-hosted workflow run is a separate CI-environment acceptance gate and should be verified after this repository setup is committed.
