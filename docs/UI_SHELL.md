# v0.3 UI Shell

This document records the focused mobile shell introduced after physical-device testing of the first v0.3 routing RC.

## Why the shell changed

The first v0.3 RC proved Direct Share and preset routing, but putting preset configuration, routing configuration, shared-job metadata, request editing, send controls, and future settings on one long scrolling screen became too dense.

The shell separates the primary workflow from configuration while preserving the fast path.

## Top-level areas

### Share / Review

The primary job-sharing surface contains:

- current routing preset selector,
- compact route summary,
- shared-job/source summary,
- prepared request editor,
- primary send action,
- copy / Android chooser fallbacks.

Preset configuration is intentionally not expanded here.

### Presets

Preset management owns:

- CareerOps action,
- destination,
- model-preference metadata,
- request profile,
- default preset selection,
- Android Direct Share pinning.

Direct Share is curated rather than mirroring the entire preset library. `Quick Analyze` is pinned by default; `Build & Store` and `Full Application` are opt-in. At most two CareerOps preset shortcuts are published at one time in v0.3.

### Settings

Settings owns cross-cutting behavior:

- normal-share auto-forward / editor bypass,
- system-bar mode,
- future theme configuration,
- future destination configuration,
- future CareerOps Gateway / authentication / account settings.

## Navigation

The phone layout uses a compact menu opened from the shell header rather than a permanently visible sidebar. This preserves horizontal space while keeping Share, Presets, and Settings immediately reachable.

## System bars and window insets

`SystemUiController` owns status-bar, navigation-bar, display-cutout, and IME inset handling for the shell.

The default `SAFE_INSETS` mode keeps visible app content clear of Android system UI throughout scrolling. This is the regression fix for the v0.3 RC status-bar overlap captured in issues #6 and #8.

An optional `IMMERSIVE` mode hides system bars and uses Android swipe-to-reveal behavior. Insets are applied at the shell root rather than through hard-coded top/bottom padding on individual screens.

## Direct Share behavior

Direct Share remains a pre-UI route:

```text
Android Direct Share preset
        ↓
MainActivity receives ACTION_SEND + shortcut ID
        ↓
resolve stored preset
        ↓
ShareRouter
        ↓ success
launch destination + finish
```

The Share / Presets / Settings shell is only constructed when review/configuration is needed or when immediate routing falls back.

## Future homes

The shell reserves clear ownership for later work:

- theme and appearance → Settings,
- Gateway endpoint / login / auth / account state → Settings,
- arbitrary custom preset creation and ordering → Presets,
- request history / acknowledgements / failures → future Activity / Status area.
