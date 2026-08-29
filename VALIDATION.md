# v0.1.1 Validation Status

Validated in the generation environment on 2026-08-28:

- PASS — `ShareParser.kt` compiles with the available Kotlin compiler.
- PASS — LinkedIn source detection.
- PASS — Indeed source detection.
- PASS — generic job-site hostname fallback.
- PASS — trailing URL punctuation cleanup.
- PASS — plain-text shares without a URL.
- PASS — lookalike-domain guard (`notlinkedin.com` is not classified as LinkedIn).
- PASS — Android XML resources/manifest are well-formed XML.
- PASS — compatibility `gradle-wrapper.jar` contains `org.gradle.wrapper.GradleWrapperMain`.
- PASS — compatibility wrapper end-to-end smoke test against a local dummy Gradle distribution, including SHA-256 verification, extraction, and process launch.
- PASS — version/config assertions for v0.1.1, Gradle 9.5.0, `text/*` share target, and ChatGPT package routing.

## Not validated here

A real `assembleDebug` Android build was **not** run because this execution environment does not contain the Android SDK and cannot resolve external build dependencies from the shell. Therefore this bundle does not include an APK and should be treated as **build-ready source with local component validation**, not a device-tested release.

The first acceptance gate on a machine with Android Studio / SDK 36 is:

```text
gradlew.bat assembleDebug
```

Expected output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Then install on-device and execute the manual Sharesheet checklist in `README.md`.
