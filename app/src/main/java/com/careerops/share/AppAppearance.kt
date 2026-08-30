package com.careerops.share

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

object AppAppearance {
    fun resolveDark(context: Context, mode: AppThemeMode): Boolean =
        when (mode) {
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
            AppThemeMode.FOLLOW_SYSTEM -> {
                val nightMode = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

    fun palette(context: Context): AppPalette {
        val dark = resolveDark(context, AppPreferences.loadThemeMode(context))
        return if (dark) AppPalette.dark() else AppPalette.light()
    }
}

data class AppPalette(
    val isDark: Boolean,
    val background: Int,
    val surface: Int,
    val primaryText: Int,
    val secondaryText: Int,
    val border: Int
) {
    companion object {
        fun light() = AppPalette(
            isDark = false,
            background = Color.rgb(247, 248, 250),
            surface = Color.WHITE,
            primaryText = Color.rgb(28, 34, 43),
            secondaryText = Color.rgb(96, 105, 118),
            border = Color.rgb(216, 222, 230)
        )

        fun dark() = AppPalette(
            isDark = true,
            background = Color.rgb(12, 14, 18),
            surface = Color.rgb(22, 25, 31),
            primaryText = Color.rgb(242, 244, 247),
            secondaryText = Color.rgb(177, 184, 194),
            border = Color.rgb(62, 68, 78)
        )
    }
}
