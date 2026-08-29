# Changelog

## 0.1.1

- Added pinned Gradle 9.5.0 wrapper/bootstrap files.
- Added SHA-256 verification for the Gradle distribution.
- Bumped Android app version to 0.1.1 / versionCode 2.
- Added direct **Send to ChatGPT** action using the official Android package id.
- Added fallback to the regular Sharesheet if ChatGPT is unavailable.
- Simplified the share intent filter to `text/*`.
- Added `singleTop` activity handling for repeated shares.
- Extracted URL/source/payload parsing into Android-independent `ShareParser.kt`.
- Added a 100,000-character shared-text safety cap.
- Added local parser smoke tests.
- Updated build/test documentation.
