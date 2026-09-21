# Changelog

## 0.3.0 — in development

- Started preset-driven routing architecture.
- Added `CareerOpsPreset` with stable ID/name, CareerOps action, destination, model preference, request profile, auto-forward behavior, and Direct Share visibility.
- Added built-in Quick Analyze, Build & Store, and Full Application presets.
- Added local preset persistence and v0.2 action/destination migration behavior.
- Added configurable default preset.
- Added explicit opt-in to skip the editor for normal shares and immediately use the default preset.
- Added Android Sharing Shortcuts / Direct Share publication for enabled presets.
- Added fast routing before editor UI construction when a Direct Share preset or enabled default auto-forward path is used.
- Added interactive fallback when an immediate route cannot be completed.
- Added Android-independent `CareerOpsRoutePlanner` and unit coverage.
- Added CareerOps Standard text and CareerOps JSON request profiles.
- Added model-preference metadata while explicitly keeping Android app model selection advisory rather than falsely claiming model enforcement.
- Bumped Android development version to 0.3.0 / versionCode 5.
- Kept v0.3 local-only: no `INTERNET` permission and no HTTP/Gateway transport implementation.
- Kept the v0.2.1 permanent signing identity and guarded release architecture unchanged.

## 0.2.1 — stable-signing transition

- Added a permanent release-signing architecture for GitHub-distributed APKs.
- Added environment-driven Gradle release signing with no credentials stored in source control.
- Added a guard that prevents `assembleRelease` / `bundleRelease` from silently proceeding without release signing configuration.
- Added `Android Release Candidate` workflow for signed, non-published physical-device testing.
- Changed the guarded release workflow from `assembleDebug` to signed `assembleRelease`.
- Added Android `apksigner` verification and signer SHA-256 reporting.
- Added signed APK SHA-256 and signing-certificate record release assets.
- Added four GitHub Actions signing-secret contracts.
- Added PowerShell helper for generating/backing/configuring the permanent PKCS12 key.
- Added signing-material ignore rules.
- Added `docs/SIGNING.md` and updated the release-finalization flow for signed release candidates.
- Bumped Android version to 0.2.1 / versionCode 4 for the first permanent-signing release.
- No application runtime behavior changes.

## 0.2.0

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
