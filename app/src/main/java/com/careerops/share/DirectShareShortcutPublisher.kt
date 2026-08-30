package com.careerops.share

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build

object DirectShareContract {
    const val CATEGORY_PRESET = "com.careerops.share.category.CAREEROPS_PRESET"
    const val EXTRA_SHORTCUT_ID = "android.intent.extra.shortcut.ID"
    const val EXTRA_PRESET_ID = "com.careerops.share.extra.PRESET_ID"
    const val ACTION_OPEN_PRESET = "com.careerops.share.action.OPEN_PRESET"
}

data class ShortcutPublishResult(
    val publishedCount: Int,
    val rateLimited: Boolean = false,
    val error: String? = null
)

object DirectShareShortcutPublisher {
    fun publish(context: Context): ShortcutPublishResult {
        val manager = context.getSystemService(ShortcutManager::class.java)
            ?: return ShortcutPublishResult(0, error = "ShortcutManager unavailable")

        val maxCount = manager.maxShortcutCountPerActivity
        if (maxCount <= 0) {
            return ShortcutPublishResult(0)
        }

        val presets = AppPreferences.loadPresets(context)
            .filter { it.showInDirectShare }
            .take(minOf(maxCount, PresetCatalog.builtIns.size))

        val expectedShortcutState = presets.mapIndexed { rank, preset ->
            ShortcutState(preset.id, preset.name, rank)
        }
        val currentShortcutState = manager.dynamicShortcuts
            .sortedBy { it.rank }
            .map { ShortcutState(it.id, it.shortLabel.toString(), it.rank) }

        // Avoid consuming the platform shortcut update quota when nothing changed.
        if (currentShortcutState == expectedShortcutState) {
            return ShortcutPublishResult(currentShortcutState.size)
        }

        if (manager.isRateLimitingActive) {
            return ShortcutPublishResult(
                publishedCount = manager.dynamicShortcuts.size,
                rateLimited = true
            )
        }

        if (presets.isEmpty()) {
            manager.removeAllDynamicShortcuts()
            return ShortcutPublishResult(0)
        }

        val shortcuts = presets.mapIndexed { rank, preset ->
            val builder = ShortcutInfo.Builder(context, preset.id)
                .setShortLabel(preset.name)
                .setLongLabel("CareerOps: ${preset.name}")
                .setIcon(Icon.createWithResource(context, R.drawable.ic_careerops))
                .setCategories(setOf(DirectShareContract.CATEGORY_PRESET))
                .setRank(rank)
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = DirectShareContract.ACTION_OPEN_PRESET
                        putExtra(DirectShareContract.EXTRA_PRESET_ID, preset.id)
                    }
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setLongLived(true)
            }

            builder.build()
        }

        return try {
            val accepted = manager.setDynamicShortcuts(shortcuts)
            if (accepted) {
                ShortcutPublishResult(shortcuts.size)
            } else {
                ShortcutPublishResult(
                    manager.dynamicShortcuts.size,
                    error = "Android rejected the shortcut update"
                )
            }
        } catch (error: RuntimeException) {
            ShortcutPublishResult(
                manager.dynamicShortcuts.size,
                error = error.message ?: error.javaClass.simpleName
            )
        }
    }

    fun reportUsed(context: Context, presetId: String) {
        if (PresetCatalog.fromIdOrNull(presetId) == null) return

        runCatching {
            context.getSystemService(ShortcutManager::class.java)
                ?.reportShortcutUsed(presetId)
        }
    }

    private data class ShortcutState(
        val id: String,
        val label: String,
        val rank: Int
    )
}
