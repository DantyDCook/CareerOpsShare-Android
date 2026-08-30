package com.careerops.share

import android.content.Context

object AppPreferences {
    private const val FILE_NAME = "careerops_share_preferences"

    // v0.2 compatibility keys. They are retained so an in-place upgrade can seed
    // the first v0.3 preset without losing the user's last action/destination.
    private const val KEY_ACTION = "default_action"
    private const val KEY_DESTINATION = "default_destination"

    private const val KEY_V03_MIGRATED = "v03_preset_migration_complete"
    private const val KEY_DIRECT_SHARE_CURATION_MIGRATED = "v03_direct_share_curation_complete"
    private const val KEY_DEFAULT_PRESET = "default_preset_id"
    private const val KEY_DIRECT_SHARE_ENABLED = "direct_share_enabled"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_SYSTEM_BAR_MODE = "system_bar_mode"
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
    }

    private fun ensureDirectShareCurationMigration(context: Context) {
        val prefs = preferences(context)
        if (prefs.getBoolean(KEY_DIRECT_SHARE_CURATION_MIGRATED, false)) return

        // The first routing RC published all built-ins by default. Curate the
        // fast-action surface on upgrade: Quick Analyze stays pinned; the more
        // consequential presets remain available in-app but become explicit opt-ins.
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

    // Compatibility helpers for any code paths still using the v0.2 vocabulary.
    fun loadAction(context: Context): CareerOpsAction =
        loadDefaultPreset(context).action

    fun saveAction(context: Context, action: CareerOpsAction) {
        val preset = loadDefaultPreset(context)
        savePreset(context, preset.copy(action = action))
        preferences(context).edit().putString(KEY_ACTION, action.id).apply()
    }

    fun loadDestination(context: Context): DestinationProfile =
        DestinationCatalog.fromId(loadDefaultPreset(context).destinationId)

    fun saveDestination(context: Context, destination: DestinationProfile) {
        val preset = loadDefaultPreset(context)
        savePreset(context, preset.copy(destinationId = destination.id))
        preferences(context).edit().putString(KEY_DESTINATION, destination.id).apply()
    }

    private fun presetKeyPrefix(presetId: String): String =
        "$PRESET_PREFIX$presetId."

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
