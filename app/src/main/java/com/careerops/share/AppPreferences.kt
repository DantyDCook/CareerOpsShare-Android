package com.careerops.share

import android.content.Context

object AppPreferences {
    private const val FILE_NAME = "careerops_share_preferences"

    // v0.2 compatibility keys. They are retained so an in-place upgrade can seed
    // v0.3 state without losing the user's last action/destination.
    private const val KEY_ACTION = "default_action"
    private const val KEY_DESTINATION = "default_destination"

    private const val KEY_V03_MIGRATED = "v03_preset_migration_complete"
    private const val KEY_DIRECT_SHARE_CURATION_MIGRATED = "v03_direct_share_curation_complete"
    private const val KEY_DIRECT_SHARE_SLOTS_MIGRATED = "v03_direct_share_slots_migration_complete"
    private const val KEY_REGULAR_DEFAULTS_MIGRATED = "v03_regular_defaults_migration_complete"
    private const val KEY_DEFAULT_PRESET = "default_preset_id"
    private const val KEY_DIRECT_SHARE_ENABLED = "direct_share_enabled"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_SYSTEM_BAR_MODE = "system_bar_mode"

    private const val KEY_DIRECT_SHARE_SLOT_1 = "direct_share.slot_1"
    private const val KEY_DIRECT_SHARE_SLOT_2 = "direct_share.slot_2"

    private const val KEY_REGULAR_ACTION = "regular_share.action"
    private const val KEY_REGULAR_DESTINATION = "regular_share.destination"
    private const val KEY_REGULAR_MODEL = "regular_share.model"
    private const val KEY_REGULAR_REQUEST_PROFILE = "regular_share.request_profile"

    private const val PRESET_PREFIX = "preset."

    fun ensureV03Migration(context: Context) {
        val prefs = preferences(context)
        if (!prefs.getBoolean(KEY_V03_MIGRATED, false)) {
            val legacyAction = CareerOpsAction.fromId(
                prefs.getString(KEY_ACTION, CareerOpsAction.ANALYZE.id)
            )
            val legacyDestination = DestinationCatalog.fromId(
                prefs.getString(KEY_DESTINATION, DestinationCatalog.CHATGPT.id)
            )

            savePreset(
                context,
                PresetCatalog.QUICK_ANALYZE.copy(
                    action = legacyAction,
                    destinationId = legacyDestination.id
                )
            )

            prefs.edit()
                .putString(KEY_DEFAULT_PRESET, PresetCatalog.QUICK_ANALYZE.id)
                .putBoolean(KEY_DIRECT_SHARE_ENABLED, false)
                .putBoolean(KEY_V03_MIGRATED, true)
                .apply()
        }

        ensureDirectShareCurationMigration(context)
        ensureDirectShareSlotsMigration(context)
        ensureRegularShareDefaultsMigration(context)
    }

    private fun ensureDirectShareCurationMigration(context: Context) {
        val prefs = preferences(context)
        if (prefs.getBoolean(KEY_DIRECT_SHARE_CURATION_MIGRATED, false)) return

        // The first routing RC published all built-ins by default. Curate the
        // fast-action surface on upgrade: Quick Analyze stays pinned; the more
        // consequential profiles remain saved but become explicit opt-ins.
        PresetCatalog.builtIns.forEach { definition ->
            val current = loadPreset(context, definition.id)
            savePreset(
                context,
                current.copy(
                    showInDirectShare = definition.id == PresetCatalog.QUICK_ANALYZE.id
                )
            )
        }

        prefs.edit()
            .putBoolean(KEY_DIRECT_SHARE_CURATION_MIGRATED, true)
            .apply()
    }

    private fun ensureDirectShareSlotsMigration(context: Context) {
        val prefs = preferences(context)
        if (prefs.getBoolean(KEY_DIRECT_SHARE_SLOTS_MIGRATED, false)) return

        // Preserve the current active Quick Share set once, then make activation
        // an explicit two-slot configuration independent from profile storage.
        val previouslyPinned = loadPresets(context)
            .filter { it.showInDirectShare }
            .map { it.id }
            .take(PresetCatalog.MAX_DIRECT_SHARE_PRESETS)

        saveDirectShareSlots(
            context,
            DirectShareSlots(
                firstProfileId = previouslyPinned.getOrNull(0),
                secondProfileId = previouslyPinned.getOrNull(1)
            )
        )

        prefs.edit()
            .putBoolean(KEY_DIRECT_SHARE_SLOTS_MIGRATED, true)
            .apply()
    }

    private fun ensureRegularShareDefaultsMigration(context: Context) {
        val prefs = preferences(context)
        if (prefs.getBoolean(KEY_REGULAR_DEFAULTS_MIGRATED, false)) return

        // Preserve the current v0.3 regular-share behavior exactly once, then
        // decouple it from the Quick Share profile library going forward.
        val previousDefault = loadDefaultPreset(context)
        saveRegularShareDefaults(
            context,
            RegularShareDefaults(
                action = previousDefault.action,
                destinationId = previousDefault.destinationId,
                modelPreference = previousDefault.modelPreference,
                requestProfile = previousDefault.requestProfile
            )
        )

        prefs.edit()
            .putBoolean(KEY_REGULAR_DEFAULTS_MIGRATED, true)
            .apply()
    }

    fun loadPresets(context: Context): List<CareerOpsPreset> =
        PresetCatalog.builtIns.map { loadPreset(context, it.id) }

    fun loadPreset(context: Context, presetId: String): CareerOpsPreset {
        val base = PresetCatalog.fromId(presetId)
        val prefs = preferences(context)
        val prefix = presetKeyPrefix(base.id)

        return base.copy(
            action = CareerOpsAction.fromId(
                prefs.getString("${prefix}action", base.action.id)
            ),
            destinationId = prefs.getString(
                "${prefix}destination",
                base.destinationId
            ) ?: base.destinationId,
            modelPreference = ModelPreference.fromId(
                prefs.getString("${prefix}model", base.modelPreference.id)
            ),
            requestProfile = RequestProfile.fromId(
                prefs.getString("${prefix}request_profile", base.requestProfile.id)
            ),
            autoForward = prefs.getBoolean("${prefix}auto_forward", base.autoForward),
            // Legacy compatibility field only. Active Android Direct Share state
            // is now owned by DirectShareSlots rather than by each saved profile.
            showInDirectShare = prefs.getBoolean(
                "${prefix}show_in_direct_share",
                base.showInDirectShare
            )
        )
    }

    fun savePreset(context: Context, preset: CareerOpsPreset) {
        val prefix = presetKeyPrefix(preset.id)
        preferences(context).edit()
            .putString("${prefix}action", preset.action.id)
            .putString("${prefix}destination", preset.destinationId)
            .putString("${prefix}model", preset.modelPreference.id)
            .putString("${prefix}request_profile", preset.requestProfile.id)
            .putBoolean("${prefix}auto_forward", preset.autoForward)
            .putBoolean("${prefix}show_in_direct_share", preset.showInDirectShare)
            .apply()
    }

    fun loadDirectShareSlots(context: Context): DirectShareSlots {
        val prefs = preferences(context)
        return DirectShareSlots(
            firstProfileId = prefs.getString(KEY_DIRECT_SHARE_SLOT_1, null),
            secondProfileId = prefs.getString(KEY_DIRECT_SHARE_SLOT_2, null)
        ).normalized()
    }

    fun saveDirectShareSlots(context: Context, slots: DirectShareSlots) {
        val normalized = slots.normalized()
        preferences(context).edit()
            .putString(KEY_DIRECT_SHARE_SLOT_1, normalized.firstProfileId)
            .putString(KEY_DIRECT_SHARE_SLOT_2, normalized.secondProfileId)
            .apply()
    }

    fun saveDirectShareSlot(context: Context, slot: Int, profileId: String?) {
        val validId = PresetCatalog.fromIdOrNull(profileId)?.id
        val current = loadDirectShareSlots(context)
        val updated = when (slot) {
            1 -> DirectShareSlots(
                firstProfileId = validId,
                secondProfileId = current.secondProfileId?.takeUnless { it == validId }
            )
            2 -> DirectShareSlots(
                firstProfileId = current.firstProfileId?.takeUnless { it == validId },
                secondProfileId = validId
            )
            else -> current
        }
        saveDirectShareSlots(context, updated)
    }

    fun clearDirectShareSelections(context: Context) {
        saveDirectShareSlots(context, DirectShareSlots())
    }

    fun loadRegularShareDefaults(context: Context): RegularShareDefaults {
        val prefs = preferences(context)
        return RegularShareDefaults(
            action = CareerOpsAction.fromId(
                prefs.getString(KEY_REGULAR_ACTION, CareerOpsAction.ANALYZE.id)
            ),
            destinationId = prefs.getString(
                KEY_REGULAR_DESTINATION,
                DestinationCatalog.CHATGPT.id
            ) ?: DestinationCatalog.CHATGPT.id,
            modelPreference = ModelPreference.fromId(
                prefs.getString(KEY_REGULAR_MODEL, ModelPreference.AUTO.id)
            ),
            requestProfile = RequestProfile.fromId(
                prefs.getString(
                    KEY_REGULAR_REQUEST_PROFILE,
                    RequestProfile.CAREEROPS_STANDARD.id
                )
            )
        )
    }

    fun saveRegularShareDefaults(context: Context, defaults: RegularShareDefaults) {
        preferences(context).edit()
            .putString(KEY_REGULAR_ACTION, defaults.action.id)
            .putString(KEY_REGULAR_DESTINATION, defaults.destinationId)
            .putString(KEY_REGULAR_MODEL, defaults.modelPreference.id)
            .putString(KEY_REGULAR_REQUEST_PROFILE, defaults.requestProfile.id)
            .apply()
    }

    // Retained for compatibility with the first v0.3 RC. Quick Share profiles no
    // longer define the normal application's regular-share defaults.
    fun loadDefaultPresetId(context: Context): String {
        val stored = preferences(context).getString(
            KEY_DEFAULT_PRESET,
            PresetCatalog.QUICK_ANALYZE.id
        )
        return PresetCatalog.fromId(stored).id
    }

    fun loadDefaultPreset(context: Context): CareerOpsPreset =
        loadPreset(context, loadDefaultPresetId(context))

    fun saveDefaultPreset(context: Context, presetId: String) {
        preferences(context).edit()
            .putString(KEY_DEFAULT_PRESET, PresetCatalog.fromId(presetId).id)
            .apply()
    }

    fun loadDirectShareEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_DIRECT_SHARE_ENABLED, false)

    fun saveDirectShareEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_DIRECT_SHARE_ENABLED, enabled)
            .apply()
    }

    fun loadThemeMode(context: Context): AppThemeMode =
        AppThemeMode.fromId(
            preferences(context).getString(KEY_THEME_MODE, AppThemeMode.FOLLOW_SYSTEM.id)
        )

    fun saveThemeMode(context: Context, mode: AppThemeMode) {
        preferences(context).edit()
            .putString(KEY_THEME_MODE, mode.id)
            .apply()
    }

    fun loadSystemBarMode(context: Context): SystemBarMode =
        SystemBarMode.fromId(
            preferences(context).getString(KEY_SYSTEM_BAR_MODE, SystemBarMode.SAFE_INSETS.id)
        )

    fun saveSystemBarMode(context: Context, mode: SystemBarMode) {
        preferences(context).edit()
            .putString(KEY_SYSTEM_BAR_MODE, mode.id)
            .apply()
    }

    // Compatibility helpers for code paths still using the v0.2 vocabulary now
    // point at the independent regular-share defaults.
    fun loadAction(context: Context): CareerOpsAction =
        loadRegularShareDefaults(context).action

    fun saveAction(context: Context, action: CareerOpsAction) {
        val defaults = loadRegularShareDefaults(context)
        saveRegularShareDefaults(context, defaults.copy(action = action))
        preferences(context).edit().putString(KEY_ACTION, action.id).apply()
    }

    fun loadDestination(context: Context): DestinationProfile =
        DestinationCatalog.fromId(loadRegularShareDefaults(context).destinationId)

    fun saveDestination(context: Context, destination: DestinationProfile) {
        val defaults = loadRegularShareDefaults(context)
        saveRegularShareDefaults(context, defaults.copy(destinationId = destination.id))
        preferences(context).edit().putString(KEY_DESTINATION, destination.id).apply()
    }

    private fun presetKeyPrefix(presetId: String): String =
        "$PRESET_PREFIX$presetId."

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
