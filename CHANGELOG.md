# Changelog

## 0.2.0 — development

- Added `CareerOpsRequest` schema v1.0 as the transport-neutral handoff contract.
- Added CareerOps action choices for Analyze, Analyze + Build & Store, and Analyze + Build & Store + Cover Letter.
- Added `DestinationProfile` and `TransportType` abstractions.
- Converted ChatGPT from a hard-coded UI path into a destination profile.
- Added Android app and system chooser transport implementations.
- Added a disabled future CareerOps Gateway / HTTP transport boundary without enabling networking.
- Added local action/destination persistence with `SharedPreferences`.
- Added LinkedIn job-ID extraction and canonical URL generation.
- Added Indeed `jk` extraction and canonical URL generation.
- Added safe tracking/share-parameter cleanup.
- Preserved raw shared content independently of canonical URL normalization.
- Added structured text rendering compatible with the existing CareerOps chat trigger.
- Added JSON rendering for future API/webhook/gateway integrations.
- Added **Copy request JSON**.
- Added JVM unit tests and expanded the standalone parser smoke test.
- Added architecture flowcharts and request-schema documentation.
- Bumped Android version to 0.2.0 / versionCode 3.
- Kept v0.2 local-only with no `INTERNET` permission.

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
