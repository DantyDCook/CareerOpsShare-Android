# CareerOps Share for Android — v0.1.1

A small Android share-target app that adds **CareerOps Share** to the Android Sharesheet.

## What v0.1.1 does

1. Open a job post in LinkedIn, Indeed, Chrome, Glassdoor, etc.
2. Tap **Share**.
3. Select **CareerOps Share**.
4. The app captures the shared title/text/URL and prepares:

   `Analyze this job using CareerOps:`

   followed by the shared content.
5. Review or edit the payload.
6. Tap **Send to ChatGPT** to target the installed ChatGPT Android app directly.
7. If ChatGPT is not installed, the app falls back to the Android Sharesheet.

You can also copy the prepared payload or send it to another app.

## Privacy / permissions

v0.1.1 has no INTERNET permission, no storage permission, no GitHub token, and no OpenAI API key. It only receives text explicitly shared to it and forwards/copies the text when you tap a button.

## Build configuration

- Android Gradle Plugin: 9.3.0
- Gradle distribution: 9.5.0
- compileSdk / targetSdk: 36
- minSdk: 26
- Java: 17+
- Kotlin support: AGP 9 built-in Kotlin

### Wrapper note

This source bundle includes a small dependency-free compatibility `gradle-wrapper.jar` compiled from source in `gradle/wrapper/bootstrap-src/`. It exists because the build environment that produced this ZIP could not download Gradle's official wrapper JAR directly.

It downloads the pinned Gradle 9.5.0 binary distribution, verifies its SHA-256, unpacks it under `GRADLE_USER_HOME`, and launches Gradle. After your first successful Gradle run, replace it with Gradle's official generated wrapper files:

```bash
./gradlew wrapper --gradle-version 9.5.0
./gradlew wrapper --gradle-version 9.5.0
```

Commit the resulting official `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, and `gradle-wrapper.properties` if you put this project in Git.

## Build in Android Studio

1. Open the `CareerOpsShare` directory.
2. Use a JDK 17+ Gradle JDK.
3. Install Android SDK Platform 36 / Build Tools 36 if Android Studio prompts for them.
4. Sync Gradle.
5. Select the `app` run configuration and run on your Android device.

## Command-line build

With Android SDK 36 installed and `ANDROID_HOME` configured:

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

On Windows you can also run `build-debug.ps1` from PowerShell.

Install with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Manual test checklist

- Share a LinkedIn job URL and confirm **CareerOps Share** appears.
- Confirm source displays as LinkedIn and the URL is detected.
- Confirm the payload starts with `Analyze this job using CareerOps:`.
- Tap **Send to ChatGPT** and confirm ChatGPT opens with the shared text.
- Share an Indeed job and repeat.
- Share plain text with no URL and confirm it still prepares a payload.
- Test **Copy** and **Other app…**.

## Included local checks

`tools/ShareParserSmokeTest.kt` exercises LinkedIn, Indeed, generic URLs, punctuation trimming, and plain-text behavior without Android dependencies.

## v0.2 direction

Replace the ChatGPT forwarding transport with direct CareerOps ingestion (local gateway or authenticated HTTPS endpoint), while retaining this Android Sharesheet receiver as the phone-side entry point.
