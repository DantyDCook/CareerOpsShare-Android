package com.careerops.share

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

object SystemUiController {
    fun apply(
        activity: Activity,
        root: View,
        mode: SystemBarMode,
        darkBackground: Boolean
    ) {
        val window = activity.window
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)

            val controller = window.insetsController
            val appearanceMask =
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            val appearance = if (darkBackground) 0 else appearanceMask
            controller?.setSystemBarsAppearance(appearance, appearanceMask)

            if (mode == SystemBarMode.IMMERSIVE) {
                controller?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsets.Type.systemBars())
            } else {
                controller?.show(WindowInsets.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            var flags =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

            if (!darkBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            if (!darkBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            if (mode == SystemBarMode.IMMERSIVE) {
                @Suppress("DEPRECATION")
                flags = flags or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }

            @Suppress("DEPRECATION")
            root.systemUiVisibility = flags
        }

        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val system = insets.getInsets(
                    WindowInsets.Type.systemBars() or
                        WindowInsets.Type.displayCutout() or
                        WindowInsets.Type.ime()
                )
                view.setPadding(system.left, system.top, system.right, system.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
            }
            insets
        }
        root.requestApplyInsets()
    }
}
