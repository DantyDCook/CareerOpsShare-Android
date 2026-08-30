# Regular Share Defaults vs Quick Share Profiles

v0.3 separates the configuration used by the normal CareerOps Share target from the configurations published as Android Direct Share shortcuts.

## Regular Share Defaults

Regular Share Defaults initialize the interactive Share / Review flow. They are not themselves Android Direct Share shortcuts.

```text
RegularShareDefaults
├── action
├── destinationId
├── modelPreference
└── requestProfile
```

When the user changes a route field during review, the change is session-local unless explicitly saved as an app default. The prepared request and destination controls must update immediately to match the current review state.

## Quick Share Profiles

Quick Share Profiles are independent saved routing configurations used for fast routing from Android Direct Share.

```text
QuickShareProfile / CareerOpsPreset
├── id
├── name
├── action
├── destinationId
├── modelPreference
├── requestProfile
├── autoForward
└── activeInDirectShare
```

The initial v0.3 profile library uses the existing stable built-in identities:

- Quick Analyze
- Build & Store
- Full Application

A later persistence migration can support arbitrary user-created profile names without changing the regular-share-default contract.

## Active Direct Share subset

Saved Quick Share profiles and currently published Android shortcuts are separate concepts.

- Users may retain several saved Quick Share profiles.
- Only an explicitly selected subset is published to Android Direct Share.
- v0.3 keeps the current maximum of two active CareerOps Direct Share shortcuts.
- `Clear active shortcuts` removes all CareerOps Quick Share profiles from Android Direct Share while leaving the saved profile configurations intact.
- Re-activating a saved profile republishes it using its own action/destination/model/request settings.

This allows, for example, normal CareerOps Share to default to ChatGPT while an independent Quick Share profile targets another destination such as Claude once that destination is supported and verified.
