# Validation Status

## v0.3.0 — Direct Share / Preset Routing candidate

Branch: `feature/v0.3.0-direct-share-presets`

Expected Android version:

- `versionName = 0.3.0`
- `versionCode = 5`

Expected permanent signer certificate SHA-256:

```text
e77ad35df3c7444a8573693e4d83892a871c06cedebb7c21214f3fa55a9158d9
```

### Automated validation

- PASS — first v0.3 implementation Android CI run #22 completed successfully.
- PASS — Gradle unit tests cover preset-driven route planning and JSON/text request-profile selection.
- PASS — debug APK compiled with Android SDK 36 / JDK 17 in CI.
- PASS — current-head Android CI #27 completed successfully on commit `61e8b08062eb0b42693da690f024ff7bb711d8f7` before signed-RC device testing.
- PENDING — signed `Android Release Candidate` build metadata/fingerprint independently verified in the repository record.
- PENDING — signed RC `apksigner` fingerprint match against the established permanent signer independently recorded for v0.3.0.

### Physical-device upgrade validation

- PASS — signed v0.3.0 RC installed over the stable-signed v0.2.1 app using the Android package installer update path.
- PASS — no uninstall/signature migration was required between v0.2.1 and v0.3.0.
- PASS — app launched normally after the in-place upgrade; v0.3 preset-routing UI displayed successfully.

### Interactive share regression

- PENDING — ordinary **CareerOps Share** target still appears in the Android Sharesheet.
- PENDING — with **Skip editor for normal shares** disabled, sharing opens the interactive editor.
- PENDING — LinkedIn source/job ID/canonical URL are still correct.
- PENDING — Indeed source/`jk`/canonical URL are still correct.
- PENDING — prepared CareerOps Standard request renders correctly.
- PENDING — CareerOps JSON request profile renders correctly.
- PENDING — manual Send to ChatGPT works.
- PENDING — Android system chooser works.

### Preset persistence

- PENDING — Quick Analyze preset loads `ANALYZE`.
- PENDING — Build & Store preset loads `ANALYZE_BUILD_STORE`.
- PENDING — Full Application preset loads `ANALYZE_BUILD_STORE_COVER_LETTER`.
- PENDING — action changes persist after **Save preset**.
- PENDING — destination changes persist after **Save preset**.
- PENDING — request-profile changes persist after **Save preset**.
- PENDING — model-preference changes persist after **Save preset** without claiming to switch the target app's internal model.
- PENDING — selected default preset persists across app restart.
- PENDING — Direct Share visibility setting persists across app restart.

### Normal-share fast path

- PENDING — enable **Skip editor for normal shares (use default preset)**.
- PENDING — share a job through the ordinary CareerOps Share app target.
- PENDING — editor is skipped and the saved default preset routes immediately.
- PENDING — routed request contains the default preset's expected CareerOps action.
- PENDING — disabling the setting restores the interactive editor path.

### Android Direct Share / Sharing Shortcuts

- PENDING — enabled Quick Analyze / Build & Store / Full Application shortcuts are published after app launch.
- PENDING — CareerOps preset targets appear in the Android Direct Share row where supported by the device/launcher.
- PENDING — Quick Analyze Direct Share routes without showing the editor in the normal success path.
- PENDING — Build & Store Direct Share routes `ANALYZE_BUILD_STORE`.
- PENDING — Full Application Direct Share routes `ANALYZE_BUILD_STORE_COVER_LETTER`.
- PENDING — disabling **Show selected preset in Android Direct Share** removes that dynamic shortcut after republish/system refresh.

### Failure fallback

- PENDING — configure/use a route whose explicit destination cannot be completed.
- PENDING — immediate routing failure preserves the original incoming share and opens the interactive editor rather than losing the request.
- PENDING — user can select another destination and send successfully.

### UI / system-insets observation

- FAIL/OPEN — v0.3.0 RC demonstrates status-bar/content overlap while scrolling: visible Android status-bar icons/text can sit over app title, labels, and controls.
- TRACKED — issue #8, **Bug: Status bar overlays scrolling content in v0.3.0**.
- RELATED — issue #6, **Feature: Edge-to-edge UI and configurable system-bar behavior**.
- PLAN — implement #6 and #8 together in a dedicated UI/system-insets branch if the fix remains contained enough for v0.3.0; otherwise move them into the immediate UI follow-up release without destabilizing Direct Share routing.

### Security assertions

v0.3 must retain:

- no `INTERNET` permission,
- no API/provider credentials,
- no GitHub credential,
- no background network submission,
- no signing key material in source control.

### UI backlog decision

The following UI work is tracked separately from the routing core:

- issue #5 — theme controls / optional scheduled theme,
- issue #6 — edge-to-edge and status/navigation-bar behavior,
- issue #8 — concrete v0.3.0 scrolling/status-bar overlap regression.

### Release gate

Release gate: PENDING

Do not merge/release v0.3.0 until signed-RC metadata/fingerprint verification, preset/Direct Share behavior, failure fallback, and this validation record are complete. The v0.3.0 in-place permanent-signing upgrade itself is confirmed PASS. Resolve or explicitly defer the #6/#8 UI regression before release approval.

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
