# Custom Quick Share Profile Library — Planned Extension

The v0.3 slot model deliberately separates **saved Quick Share profiles** from the two **active Android Direct Share slots**. The first implementation keeps the three built-in profile identities while establishing the contract needed for arbitrary locally saved profiles.

## Planned user flow

```text
Quick Shares
├── Saved profiles
│   ├── Quick Analyze
│   ├── Build & Store
│   ├── Full Application
│   └── + New Quick Share
│
├── Direct Share Slot 1
│   └── None / any saved profile
│
├── Direct Share Slot 2
│   └── None / any saved profile
│
└── Clear Direct Share
```

## Custom profile fields

A user-created Quick Share should store:

- stable generated ID,
- display name,
- CareerOps action,
- destination,
- model preference,
- request profile,
- optional future transport/destination metadata.

## Naming

Support both:

1. **automatic names** generated from the route, for example `Build & Store → ChatGPT`, and
2. **custom user names**, for example `Claude quick analysis`.

The user-entered name, when present, wins for display. Automatic naming provides a useful default and avoids requiring text entry for every profile.

## Persistence

Custom profiles should be stored locally behind a profile repository rather than expanding fixed `SharedPreferences` keys indefinitely. The existing built-in profiles should migrate into or coexist with that repository without changing their stable IDs.

A future DataStore-backed repository is preferred once arbitrary profile creation/rename/delete/order is implemented.

## Slot behavior

The active Direct Share slots should reference profile IDs only. Therefore adding, editing, or renaming a saved profile does not change the slot model. Deleting an active profile must clear any slot that references it and republish Android shortcuts.
