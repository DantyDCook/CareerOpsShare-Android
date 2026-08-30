package com.careerops.share

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private enum class AppSection(val title: String) {
        SHARE("Share / Review"),
        PRESETS("Presets"),
        SETTINGS("Settings")
    }

    private lateinit var shellRoot: LinearLayout
    private lateinit var screenTitleView: TextView
    private lateinit var contentHost: FrameLayout

    private var payloadEditor: EditText? = null
    private var currentIntake: JobShareIntake? = null
    private var currentPresetId: String = PresetCatalog.QUICK_ANALYZE.id
    private var activeSection: AppSection = AppSection.SHARE
    private var uiBuilt = false
    private var pendingRoutingError: String? = null

    private val presetDefinitions = PresetCatalog.builtIns
    private val actions = CareerOpsAction.entries.toList()
    private val destinations = DestinationCatalog.localDestinations()
    private val modelPreferences = ModelPreference.entries.toList()
    private val requestProfiles = RequestProfile.entries.toList()
    private val systemBarModes = SystemBarMode.entries.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppPreferences.ensureV03Migration(this)
        DirectShareShortcutPublisher.publish(this)

        if (tryImmediateRoute(intent)) return
        buildInteractiveShell(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (tryImmediateRoute(intent)) return

        if (!uiBuilt) {
            buildInteractiveShell(intent)
        } else {
            consumeIncomingIntent(intent)
            showSection(AppSection.SHARE)
            showPendingRoutingError()
        }
    }

    private fun tryImmediateRoute(incoming: Intent?): Boolean {
        if (incoming?.action != Intent.ACTION_SEND || incoming.type?.startsWith("text/") != true) {
            return false
        }

        val shortcutId = incoming.getStringExtra(DirectShareContract.EXTRA_SHORTCUT_ID)
        val preset = if (!shortcutId.isNullOrBlank()) {
            val knownPreset = PresetCatalog.fromIdOrNull(shortcutId)
            if (knownPreset == null) {
                pendingRoutingError = "Direct Share preset '$shortcutId' is no longer available"
                return false
            }
            AppPreferences.loadPreset(this, knownPreset.id)
        } else if (AppPreferences.loadDirectShareEnabled(this)) {
            AppPreferences.loadDefaultPreset(this)
        } else {
            return false
        }

        if (!preset.autoForward) return false

        return when (val result = ShareRouter.route(this, incoming, preset)) {
            is ShareRouteResult.Sent -> {
                if (!shortcutId.isNullOrBlank()) {
                    DirectShareShortcutPublisher.reportUsed(this, preset.id)
                }
                finish()
                true
            }
            is ShareRouteResult.Failed -> {
                pendingRoutingError = "${result.reason}. Opened the editor instead."
                false
            }
            ShareRouteResult.NotShare -> false
        }
    }

    private fun buildInteractiveShell(incoming: Intent?) {
        shellRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_APP_BACKGROUND)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(14), dp(4))
            setBackgroundColor(Color.WHITE)
            elevation = dp(3).toFloat()
        }

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(COLOR_PRIMARY_TEXT)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            contentDescription = "Open navigation menu"
            setOnClickListener { showNavigationMenu(this) }
        }
        topBar.addView(
            menuButton,
            LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        screenTitleView = TextView(this).apply {
            text = AppSection.SHARE.title
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(COLOR_PRIMARY_TEXT)
            gravity = Gravity.CENTER_VERTICAL
        }
        topBar.addView(
            screenTitleView,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        topBar.addView(TextView(this).apply {
            text = "v0.3"
            textSize = 12f
            setTextColor(COLOR_SECONDARY_TEXT)
        })

        shellRoot.addView(
            topBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        contentHost = FrameLayout(this)
        shellRoot.addView(
            contentHost,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(shellRoot)
        SystemUiController.apply(
            activity = this,
            root = shellRoot,
            mode = AppPreferences.loadSystemBarMode(this),
            darkBackground = false
        )

        uiBuilt = true
        consumeIncomingIntent(incoming)
        showSection(AppSection.SHARE)
        showPendingRoutingError()
    }

    private fun showNavigationMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_SHARE, 0, "Share / Review")
            menu.add(0, MENU_PRESETS, 1, "Presets")
            menu.add(0, MENU_SETTINGS, 2, "Settings")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_SHARE -> showSection(AppSection.SHARE)
                    MENU_PRESETS -> showSection(AppSection.PRESETS)
                    MENU_SETTINGS -> showSection(AppSection.SETTINGS)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    private fun showSection(section: AppSection) {
        activeSection = section
        screenTitleView.text = section.title
        payloadEditor = null
        contentHost.removeAllViews()

        val view = when (section) {
            AppSection.SHARE -> buildShareScreen()
            AppSection.PRESETS -> buildPresetsScreen()
            AppSection.SETTINGS -> buildSettingsScreen()
        }

        contentHost.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun buildShareScreen(): View {
        val scroll = ScrollView(this)
        val root = contentColumn()
        scroll.addView(root)

        val presets = AppPreferences.loadPresets(this)
        val currentPreset = AppPreferences.loadPreset(this, currentPresetId)

        root.addView(sectionLabel("Route"))
        val presetSpinner = spinner(presets.map { it.name })
        val presetIndex = presets.indexOfFirst { it.id == currentPresetId }.coerceAtLeast(0)
        presetSpinner.setSelection(presetIndex, false)
        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = presets.getOrNull(position) ?: return
                if (selected.id == currentPresetId) return
                currentPresetId = selected.id
                showSection(AppSection.SHARE)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        root.addView(presetSpinner)

        root.addView(infoCard(routeSummary(currentPreset)), marginTop = dp(8))

        root.addView(Button(this).apply {
            text = "Manage presets"
            setOnClickListener { showSection(AppSection.PRESETS) }
        }, fullWidthButtonParams(dp(8)))

        root.addView(sectionLabel("Shared job"))
        val intake = currentIntake
        if (intake == null) {
            root.addView(
                infoCard(
                    "No shared job loaded.\n\nShare a LinkedIn, Indeed, browser, or text job posting into CareerOps Share."
                )
            )
        } else {
            val jobId = intake.jobId ?: "Not detected"
            val url = intake.canonicalUrl ?: "No URL detected"
            root.addView(
                infoCard(
                    "Source: ${intake.source}\nJob ID: $jobId\n$url"
                )
            )
            if (intake.wasTruncated) {
                root.addView(TextView(this).apply {
                    text = "Very large shared text was capped at 100,000 characters."
                    textSize = 12f
                    setTextColor(Color.rgb(150, 80, 20))
                    setPadding(0, dp(6), 0, 0)
                })
            }
        }

        root.addView(sectionLabel("Prepared request"))
        payloadEditor = EditText(this).apply {
            minLines = 9
            gravity = Gravity.TOP or Gravity.START
            textSize = 15f
            setTextColor(COLOR_PRIMARY_TEXT)
            setHintTextColor(COLOR_SECONDARY_TEXT)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBox(Color.WHITE, COLOR_BORDER)
            setText(renderCurrentPayload())
        }
        root.addView(
            payloadEditor,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val destination = DestinationCatalog.fromId(currentPreset.destinationId)
        root.addView(Button(this).apply {
            text = "Send to ${destination.displayName}"
            setOnClickListener { sendToCurrentDestination() }
        }, fullWidthButtonParams(dp(14)))

        val secondaryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(8), 0, 0)
        }
        secondaryRow.addView(Button(this).apply {
            text = "Copy"
            setOnClickListener { copyPayload() }
        }, buttonParams())
        secondaryRow.addView(Button(this).apply {
            text = "Other app…"
            setOnClickListener { sendWithSystemChooser() }
        }, buttonParams(leftMargin = dp(8)))
        root.addView(secondaryRow)

        root.addView(Button(this).apply {
            text = "Copy request JSON"
            setOnClickListener { copyRequestJson() }
        }, fullWidthButtonParams(dp(8)))

        root.addView(footerText("Direct Share fast actions bypass this screen when routing succeeds."))
        return scroll
    }

    private fun buildPresetsScreen(): View {
        val scroll = ScrollView(this)
        val root = contentColumn()
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "Edit routing here instead of crowding the Share screen. Direct Share is intentionally limited to ${PresetCatalog.MAX_DIRECT_SHARE_PRESETS} pinned presets."
            textSize = 14f
            setTextColor(COLOR_SECONDARY_TEXT)
        })

        root.addView(sectionLabel("Preset"))
        val presetSpinner = spinner(presetDefinitions.map { it.name })
        val presetIndex = presetDefinitions.indexOfFirst { it.id == currentPresetId }.coerceAtLeast(0)
        presetSpinner.setSelection(presetIndex, false)
        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = presetDefinitions.getOrNull(position) ?: return
                if (selected.id == currentPresetId) return
                currentPresetId = selected.id
                showSection(AppSection.PRESETS)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        root.addView(presetSpinner)

        val stored = AppPreferences.loadPreset(this, currentPresetId)
        val isDefault = AppPreferences.loadDefaultPresetId(this) == currentPresetId
        root.addView(
            infoCard(
                if (isDefault) "Default preset" else "Not the default preset"
            ),
            marginTop = dp(8)
        )

        root.addView(sectionLabel("CareerOps action"))
        val actionSpinner = spinner(actions.map { it.displayName })
        actionSpinner.setSelection(actions.indexOf(stored.action).coerceAtLeast(0), false)
        root.addView(actionSpinner)

        root.addView(sectionLabel("Destination"))
        val destinationSpinner = spinner(destinations.map { it.displayName })
        destinationSpinner.setSelection(
            destinations.indexOfFirst { it.id == stored.destinationId }.coerceAtLeast(0),
            false
        )
        root.addView(destinationSpinner)

        root.addView(sectionLabel("Model preference"))
        val modelSpinner = spinner(modelPreferences.map { it.displayName })
        modelSpinner.setSelection(modelPreferences.indexOf(stored.modelPreference).coerceAtLeast(0), false)
        root.addView(modelSpinner)
        root.addView(helperText("Stored as routing metadata. Android cannot force the destination app's internal model."))

        root.addView(sectionLabel("Request profile"))
        val requestProfileSpinner = spinner(requestProfiles.map { it.displayName })
        requestProfileSpinner.setSelection(
            requestProfiles.indexOf(stored.requestProfile).coerceAtLeast(0),
            false
        )
        root.addView(requestProfileSpinner)

        val pinnedCount = AppPreferences.loadPresets(this).count { it.showInDirectShare }
        val pinCheckBox = CheckBox(this).apply {
            text = "Pin to Android Direct Share ($pinnedCount/${PresetCatalog.MAX_DIRECT_SHARE_PRESETS} currently pinned)"
            setTextColor(COLOR_PRIMARY_TEXT)
            isChecked = stored.showInDirectShare
        }
        root.addView(pinCheckBox, fullWidthButtonParams(dp(14)))
        root.addView(helperText("Quick Analyze is pinned by default. Build & Store and Full Application are opt-in fast actions."))

        root.addView(Button(this).apply {
            text = "Save preset"
            setOnClickListener {
                savePresetFromScreen(
                    actionSpinner = actionSpinner,
                    destinationSpinner = destinationSpinner,
                    modelSpinner = modelSpinner,
                    requestProfileSpinner = requestProfileSpinner,
                    pinCheckBox = pinCheckBox
                )
            }
        }, fullWidthButtonParams(dp(16)))

        root.addView(Button(this).apply {
            text = if (isDefault) "Default preset" else "Set as default preset"
            isEnabled = !isDefault
            setOnClickListener {
                AppPreferences.saveDefaultPreset(this@MainActivity, currentPresetId)
                toast("Default preset: ${stored.name}")
                showSection(AppSection.PRESETS)
            }
        }, fullWidthButtonParams(dp(8)))

        root.addView(Button(this).apply {
            text = "Back to Share / Review"
            setOnClickListener { showSection(AppSection.SHARE) }
        }, fullWidthButtonParams(dp(8)))

        return scroll
    }

    private fun buildSettingsScreen(): View {
        val scroll = ScrollView(this)
        val root = contentColumn()
        scroll.addView(root)

        root.addView(sectionLabel("Share behavior"))
        val directShareCheckBox = CheckBox(this).apply {
            text = "Skip editor for normal shares and use the default preset"
            setTextColor(COLOR_PRIMARY_TEXT)
            isChecked = AppPreferences.loadDirectShareEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, isChecked ->
                AppPreferences.saveDirectShareEnabled(this@MainActivity, isChecked)
            }
        }
        root.addView(directShareCheckBox)
        root.addView(
            helperText(
                "Default preset: ${AppPreferences.loadDefaultPreset(this).name}. Android Direct Share preset buttons remain independent of this setting."
            )
        )

        root.addView(sectionLabel("System bars"))
        val systemBarSpinner = spinner(systemBarModes.map { it.displayName })
        val currentSystemBarMode = AppPreferences.loadSystemBarMode(this)
        systemBarSpinner.setSelection(systemBarModes.indexOf(currentSystemBarMode).coerceAtLeast(0), false)
        systemBarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = systemBarModes.getOrElse(position) { SystemBarMode.SAFE_INSETS }
                if (selected == AppPreferences.loadSystemBarMode(this@MainActivity)) return

                AppPreferences.saveSystemBarMode(this@MainActivity, selected)
                SystemUiController.apply(
                    activity = this@MainActivity,
                    root = shellRoot,
                    mode = selected,
                    darkBackground = false
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        root.addView(systemBarSpinner)
        root.addView(helperText("Standard mode keeps content clear of the status/navigation bars. Immersive mode hides them and allows swipe-to-reveal."))

        root.addView(sectionLabel("Appearance"))
        root.addView(
            infoCard(
                "Theme controls are assigned to this Settings area. Follow-system / Light / Dark is the next visual slice; scheduled theme remains tracked under issue #5."
            )
        )

        root.addView(sectionLabel("Destinations & accounts"))
        root.addView(
            infoCard(
                "Enabled now: ChatGPT and Android chooser.\n\nFuture CareerOps Gateway, authentication/login hooks, account state, and destination configuration will live here instead of on the Share screen."
            )
        )

        root.addView(footerText("Settings are intentionally separated from job review so future configuration does not crowd the primary workflow."))
        return scroll
    }

    private fun savePresetFromScreen(
        actionSpinner: Spinner,
        destinationSpinner: Spinner,
        modelSpinner: Spinner,
        requestProfileSpinner: Spinner,
        pinCheckBox: CheckBox
    ) {
        val stored = AppPreferences.loadPreset(this, currentPresetId)
        val currentlyPinned = AppPreferences.loadPresets(this).count { it.showInDirectShare }
        val wantsNewPin = pinCheckBox.isChecked && !stored.showInDirectShare

        if (wantsNewPin && currentlyPinned >= PresetCatalog.MAX_DIRECT_SHARE_PRESETS) {
            pinCheckBox.isChecked = false
            toast("Direct Share is limited to ${PresetCatalog.MAX_DIRECT_SHARE_PRESETS} pinned presets. Unpin one first.")
            return
        }

        val updated = stored.copy(
            action = actions.getOrElse(actionSpinner.selectedItemPosition) { stored.action },
            destinationId = destinations.getOrElse(destinationSpinner.selectedItemPosition) {
                DestinationCatalog.CHATGPT
            }.id,
            modelPreference = modelPreferences.getOrElse(modelSpinner.selectedItemPosition) {
                ModelPreference.AUTO
            },
            requestProfile = requestProfiles.getOrElse(requestProfileSpinner.selectedItemPosition) {
                RequestProfile.CAREEROPS_STANDARD
            },
            showInDirectShare = pinCheckBox.isChecked
        )

        AppPreferences.savePreset(this, updated)
        val publishResult = DirectShareShortcutPublisher.publish(this)
        val message = when {
            publishResult.error != null -> "Saved • shortcut update: ${publishResult.error}"
            publishResult.rateLimited -> "Saved • Android deferred shortcut update"
            else -> "Saved • ${publishResult.publishedCount} Direct Share shortcut(s)"
        }
        toast(message)
        showSection(AppSection.PRESETS)
    }

    private fun consumeIncomingIntent(incoming: Intent?) {
        val requestedPresetId = when {
            incoming?.action == DirectShareContract.ACTION_OPEN_PRESET ->
                incoming.getStringExtra(DirectShareContract.EXTRA_PRESET_ID)
            incoming?.action == Intent.ACTION_SEND ->
                incoming.getStringExtra(DirectShareContract.EXTRA_SHORTCUT_ID)
            else -> null
        }

        currentPresetId = PresetCatalog.fromIdOrNull(requestedPresetId)?.id
            ?: AppPreferences.loadDefaultPresetId(this)
        currentIntake = IncomingShareReader.read(this, incoming)
    }

    private fun routeSummary(preset: CareerOpsPreset): String {
        val destination = DestinationCatalog.fromId(preset.destinationId)
        return buildString {
            append(preset.name)
            append("\n")
            append(preset.action.displayName)
            append(" → ")
            append(destination.displayName)
            append("\n")
            append(preset.requestProfile.displayName)
            append(" • ")
            append(preset.modelPreference.displayName)
            if (preset.showInDirectShare) {
                append(" • Pinned to Direct Share")
            }
        }
    }

    private fun renderCurrentPayload(): String {
        val intake = currentIntake ?: return DEFAULT_PROMPT
        val preset = AppPreferences.loadPreset(this, currentPresetId)
        return CareerOpsRoutePlanner.plan(intake, preset).payload
    }

    private fun sendToCurrentDestination() {
        val payload = currentPayload() ?: return
        val preset = AppPreferences.loadPreset(this, currentPresetId)
        val destination = DestinationCatalog.fromId(preset.destinationId)
        val transport = TransportRegistry.transportFor(destination)

        if (transport == null) {
            toast("${destination.displayName} is not enabled in v0.3.0")
            return
        }

        when (val result = transport.send(this, payload, destination)) {
            TransportResult.Sent -> Unit
            is TransportResult.Failed -> {
                toast("${result.reason} — choose another destination")
                AndroidChooserTransport.send(
                    this,
                    payload,
                    DestinationCatalog.SYSTEM_CHOOSER
                )
            }
        }
    }

    private fun sendWithSystemChooser() {
        val payload = currentPayload() ?: return
        AndroidChooserTransport.send(
            this,
            payload,
            DestinationCatalog.SYSTEM_CHOOSER
        )
    }

    private fun copyPayload() {
        val payload = currentPayload() ?: return
        copyToClipboard("CareerOps request", payload)
        toast("CareerOps request copied")
    }

    private fun copyRequestJson() {
        val request = currentRequest() ?: run {
            toast("Share a job first")
            return
        }
        copyToClipboard(
            "CareerOps request JSON",
            CareerOpsRequestRenderer.toJson(request)
        )
        toast("CareerOps request JSON copied")
    }

    private fun currentRequest(): CareerOpsRequest? {
        val intake = currentIntake ?: return null
        val preset = AppPreferences.loadPreset(this, currentPresetId)
        return CareerOpsRequest(action = preset.action, job = intake)
    }

    private fun currentPayload(): String? {
        val payload = payloadEditor?.text?.toString()?.trim().orEmpty()
        if (payload.isBlank()) {
            toast("Nothing to send")
            return null
        }
        return payload
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    }

    private fun showPendingRoutingError() {
        pendingRoutingError?.let {
            pendingRoutingError = null
            toast(it)
        }
    }

    private fun contentColumn(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(28))
        }

    private fun sectionLabel(textValue: String): TextView =
        TextView(this).apply {
            text = textValue
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(COLOR_PRIMARY_TEXT)
            setPadding(0, dp(18), 0, dp(6))
        }

    private fun helperText(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(COLOR_SECONDARY_TEXT)
            setPadding(0, dp(5), 0, 0)
        }

    private fun footerText(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(COLOR_SECONDARY_TEXT)
            setPadding(0, dp(22), 0, dp(8))
        }

    private fun infoCard(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(COLOR_PRIMARY_TEXT)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBox(Color.WHITE, COLOR_BORDER)
        }

    private fun spinner(items: List<String>): Spinner =
        Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                items
            )
        }

    private fun roundedBox(fill: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(fill)
            setStroke(dp(1), stroke)
        }

    private fun LinearLayout.addView(view: View, marginTop: Int) {
        addView(
            view,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = marginTop }
        )
    }

    private fun buttonParams(leftMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            this.leftMargin = leftMargin
        }

    private fun fullWidthButtonParams(topMargin: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { this.topMargin = topMargin }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val DEFAULT_PROMPT = "Analyze this job using CareerOps:"

        private const val MENU_SHARE = 100
        private const val MENU_PRESETS = 101
        private const val MENU_SETTINGS = 102

        private val COLOR_APP_BACKGROUND = Color.rgb(247, 248, 250)
        private val COLOR_PRIMARY_TEXT = Color.rgb(28, 34, 43)
        private val COLOR_SECONDARY_TEXT = Color.rgb(96, 105, 118)
        private val COLOR_BORDER = Color.rgb(216, 222, 230)
    }
}
