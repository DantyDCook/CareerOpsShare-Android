# Validation Status

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
