# Validation Status

## v0.3.0 — Direct Share / Quick Share / UI candidate

Primary release branch: `feature/v0.3.0-direct-share-presets`  
Validated stacked UI/semantics branch: `feature/v0.3.0-ui-shell-insets`

Expected Android version:

- `versionName = 0.3.0`
- `versionCode = 5`

CareerOps intake request contract:

- `schema_version = 1.0`
- Android application version and CareerOps request-schema version are independent.
- `docs/REQUEST_SCHEMA.md` defines the current v1.0 request contract.
- CareerOps-Engine currently has no competing intake-request schema under its `schemas/` tree.
- CareerOps-Engine issue #20 tracks centralizing intake-schema/version governance before future contract changes.

Expected permanent signer certificate SHA-256:

```text
e77ad35df3c7444a8573693e4d83892a871c06cedebb7c21214f3fa55a9158d9
```

### Automated validation

- PASS — first v0.3 implementation Android CI run #22 completed successfully.
- PASS — Android CI #27 completed successfully on routing implementation commit `61e8b08062eb0b42693da690f024ff7bb711d8f7` before initial signed-RC device testing.
- PASS — stacked UI shell first compiled/tested successfully in Android CI #39.
- PASS — theme implementation compiled/tested successfully in Android CI #41.
- PASS — Android CI #52 completed successfully on exact two-slot/default-separation implementation head `5fb95fd81ccb604d089cee2e9c6a954dd9ab26bf`.
- PASS — Gradle unit tests cover preset-driven route planning and JSON/text request-profile selection.
- PASS — unit coverage added for `DirectShareSlots` normalization and independent `RegularShareDefaults` session conversion.
- PASS — debug APK compiled with Android SDK 36 / JDK 17 in CI.
- PENDING — final/current-head CI after this validation-documentation commit.
- PASS — final combined v0.3 branch Android CI #56 completed successfully on exact signed candidate `5b48c102b4ed6f98d37aefbcfcef2f1838c47ccf`.
- PASS — Android Release Candidate run #9 completed successfully on exact candidate `5b48c102b4ed6f98d37aefbcfcef2f1838c47ccf`; artifact `careerops-share-v0.3.0-signed-rc` (ID `10589064268`) recorded with workflow artifact digest `sha256:581f82a0660c15b3186b926b138f4ddd5dd6e26c0c4ff3a2fda284bd670dbb33` and signer SHA-256 `e77ad35df3c7444a8573693e4d83892a871c06cedebb7c21214f3fa55a9158d9`.

### Physical-device upgrade validation

- PASS — signed v0.3.0 RC installed over the stable-signed v0.2.1 app using the Android package-installer update path.
- PASS — no uninstall/signature migration was required between v0.2.1 and v0.3.0.
- PASS — later signed v0.3 UI/semantics RCs also upgraded in place over the already-installed v0.3 candidate.
- PASS — app launched normally after each in-place update.

### Regular Share / Review

- PASS — ordinary **CareerOps Share** path works on the physical device.
- PASS — regular/non-Direct-Share routing still works after the v0.3 upgrades.
- PASS — regular Share now initializes from independent regular-share defaults rather than from a Quick Share profile.
- PASS — persistent regular-share defaults can be changed/saved from Settings.
- PASS — changing the review action from **Analyze** to **Build & Store** immediately regenerates the prepared request with `ANALYZE_BUILD_STORE`.
- PASS — review-time route selection remains usable after the default/profile separation.
- PENDING — explicitly re-verify that **Skip editor for normal shares** disabled always opens the interactive editor.
- PASS — LinkedIn source/job-ID/canonical-URL regression passed on the final combined candidate.
- PASS — Indeed source/`jk`/canonical-URL regression passed on the final combined candidate.
- PASS — CareerOps JSON request-profile / copy smoke test passed on the final combined candidate.
- PASS — Android chooser (`Other app…`) smoke test passed on the final combined candidate; the prepared request remained intact and could be handed off successfully.

### Quick Share profile behavior

- PASS — Quick Analyze Direct Share works on the physical device.
- PASS — a second saved Quick Share can be activated and works.
- PASS — Quick Share configurations remain independent from regular app defaults.
- PASS — Direct Share Slot 1 is visible and editable.
- PASS — Direct Share Slot 2 is visible and editable.
- PASS — both slots can be populated from the saved built-in profile library.
- PASS — **Clear Direct Share** removes the active CareerOps Direct Share shortcuts.
- PASS — clearing Direct Share does not prevent the saved Quick Share profile from being selected again.
- PASS — restoring a saved Quick Share after clearing republishes it and the quick-share route works again.
- PASS — Android shortcut rank is deterministic in code: Slot 1 then Slot 2.
- PASS — assigning the same Quick Share profile to the opposite Direct Share slot did not publish an unintended duplicate or leave inconsistent shortcut state.
- PASS — Full Application selected into a Direct Share slot routed with the expected Full Application request/profile on the final combined candidate.

### Quick Share architecture decisions

The first v0.3 RC used per-profile `showInDirectShare` pin state. Physical testing showed that model hid the first active shortcut state and encouraged shortcut clutter.

The current v0.3 model separates **saved Quick Share profiles** from **active Android Direct Share slots**:

```text
Saved Quick Share profiles
├── Quick Analyze
├── Build & Store
└── Full Application

Active Android Direct Share
├── Slot 1: None / saved profile
├── Slot 2: None / saved profile
└── Clear Direct Share
```

- PASS — either slot can be reset to `None` in the implementation.
- PASS — `Clear Direct Share` resets both active slots without deleting the saved profile configurations.
- PASS — first-RC active shortcut state has a migration path into the explicit slot model.
- DEFERRED — arbitrary user-created Quick Share profiles, generated IDs, automatic/custom naming, create/rename/delete/order, and DataStore-style persistence are tracked in issue #14.

### Theme / app-shell validation

- PASS — Light appearance works on the physical device.
- PASS — Dark appearance works on the physical device.
- PASS — Follow Android System appearance works on the physical device.
- PASS — changing appearance does not crash or accidentally submit the original shared job.
- PASS — Share / Quick Shares / Settings separation is materially less crowded than the first v0.3 RC.
- PASS — the original status-bar/content-overlap problem is substantially improved by shell-level inset handling.
- ACCEPTED WITH FOLLOW-UP — initial immersive mode is usable for v0.3, but currently behaves primarily as hide/reveal system bars rather than a full edge-to-edge layout transformation.
- DEFERRED — true edge-to-edge immersive expansion remains issue #6.
- DEFERRED — broad Material styling, modern controls, and animated slide-out navigation drawer remain issue #13.
- DEFERRED — optional scheduled theme switching remains issue #5.

### Failure fallback

- PASS — an explicit destination failure was exercised on the physical device.
- PASS — immediate routing failure preserved the original incoming share and opened the interactive editor rather than losing the request.
- PASS — another destination could then be selected and the request sent successfully.

### Security assertions

v0.3 must retain:

- no `INTERNET` permission,
- no API/provider credentials,
- no GitHub credential,
- no background network submission,
- no signing key material in source control.

### Open follow-up issues

- issue #5 — optional scheduled theme behavior,
- issue #6 — true edge-to-edge/system-bar behavior,
- issue #8 — original scrolling/status-bar regression (covered by the shell/insets work; retain through final regression),
- issue #9 — app-shell information architecture (physically accepted; retain through final release validation),
- issue #10 — two-slot Direct Share curation (physically validated for slot use, clear, and restore; retain through final release validation),
- issue #13 — Material visual modernization and animated navigation drawer,
- issue #14 — user-created Quick Share profile library,
- CareerOps-Engine issue #20 — canonical intake request schema/version governance.

### Release gate

Release gate: PASS FOR MAIN MERGE — all required v0.3.0 pre-merge physical-device and signed-candidate gates are complete. Post-merge `main` CI and the guarded final Android Release workflow remain release-publication gates.

The v0.3 routing core, in-place signing upgrade, ordinary share path, independent regular-share defaults, live prompt regeneration, two explicit Direct Share slots, Clear Direct Share, restore behavior, Full Application Direct Share routing, duplicate-slot handling, Android chooser behavior, failure fallback, theme modes, and revised information architecture have been physically validated.

The signed/device-tested application candidate is exact commit `5b48c102b4ed6f98d37aefbcfcef2f1838c47ccf`. The subsequent closeout commit is documentation-only and does not change application, build, manifest, routing, signing, or workflow behavior. After this documentation head passes normal PR CI, PR #7 may be merged to `main`; the newest Android CI on `main` must then pass before the guarded `Android Release` workflow publishes `v0.3.0`.

---

## v0.2.1 — stable-signing release candidate

Branch: `release/v0.2.1-stable-signing`

### Automated validation

- PASS — pull-request Android CI completed successfully on the current candidate branch.
- PASS — Gradle unit tests completed successfully.
- PASS — debug APK build completed successfully in CI as a regression check.
- PASS — signed release-candidate workflow produced an installable release APK before device validation.
- PASS — signed RC workflow verifies the APK signature with Android `apksigner` before uploading the artifact.

### Stable-signing transition

- PASS — permanent PKCS12 release keystore created locally and GitHub Actions signing secrets configured.
- PASS — v0.2.1 release candidate uses `versionCode = 4` and `versionName = 0.2.1`.
- PASS — old debug-signing lineage was removed before installing the permanent-signing candidate.
- PASS — signed RC contains exactly one signer and verifies with APK Signature Scheme v2.
- PASS — signed RC signer identity: `CN=CareerOps Share, OU=Mobile, O=CareerOps, C=US`.
- PASS — permanent signer certificate SHA-256 fingerprint recorded and confirmed against the local permanent keystore:

```text
e77ad35df3c7444a8573693e4d83892a871c06cedebb7c21214f3fa55a9158d9
```

- PASS — permanent `careerops-share-release.p12` keystore has a second secure backup copy.

### Physical-device validation

- PASS — signed v0.2.1 release-candidate APK installed successfully on the physical Android device.
- PASS — CareerOps Share appears and sharing still functions correctly with the permanent-signing candidate.
- PASS — user-reported functional smoke test: sharing works.

### Release gate

Release gate: PASS

The signed build, APK signature verification, permanent-key fingerprint match, keystore backup, physical installation, functional sharing test, and PR Android CI are complete. The candidate is cleared to merge into `main`. After merge, `main` Android CI must pass before the guarded `v0.2.1` Android Release workflow is started.

---

## v0.2.0 — release candidate

Branch: `feature/v0.2-smart-intake`

### Automated core validation

- PASS — Android-independent parser/request smoke suite.
- PASS — LinkedIn source/job-ID extraction.
- PASS — LinkedIn canonical URL generation.
- PASS — Indeed `jk` extraction.
- PASS — Indeed canonical URL generation.
- PASS — generic tracking cleanup.
- PASS — lookalike-domain rejection.
- PASS — structured CareerOps action rendering.
- PASS — JSON schema rendering.

### CI validation

- PASS — Gradle unit tests.
- PASS — debug APK compiled with Android SDK 36 / JDK 17.
- PASS — pull-request GitHub Actions CI.
- PASS — CI uploaded the generated debug APK artifact.

### Physical-device validation

- PASS — v0.2.0 was installed on the physical Android device over the existing app.
- PASS — user-reported physical-device smoke/acceptance test: "Appears to be good."

The release candidate retains the v0.2 acceptance checklist below for regression testing:

- CareerOps Share appears in Android Sharesheet.
- LinkedIn share source/job ID/canonical URL are correct.
- Indeed share source/`jk`/canonical URL are correct.
- all three CareerOps actions regenerate correctly.
- action preference persists.
- destination preference persists.
- ChatGPT destination launches ChatGPT.
- system chooser works.
- missing explicit app target falls back safely.
- editable request can still be copied and sent.
- JSON request can be copied.
- app installs/upgrades over v0.1.1.

### Security assertions

v0.2 retains:

- no `INTERNET` permission,
- no API/provider credentials,
- no GitHub credential,
- no background network submission.

### Release gate

v0.2.0 is cleared for merge to `main`. After merge, the `main` CI run must pass before the `v0.2.0` release workflow is started.

---

## v0.1.1 — validated baseline

- PASS — Android Studio produced `app-debug.apk`.
- PASS — application ID `com.careerops.share`.
- PASS — `versionCode = 2`, `versionName = 0.1.1`.
- PASS — physical Android device install/launch.
- PASS — CareerOps Share appeared in Sharesheet.
- PASS — shared job content was received and forwarded successfully to ChatGPT.
- PASS — GitHub Actions build/release flow completed successfully.
- PASS — v0.1.1 GitHub Release produced through automation.

Validated local v0.1.1 APK SHA-256:

```text
ff110f7f64a0abb0d650dc26e3dd6ba3502d382591c588c79e2add7379fe2eda
```
