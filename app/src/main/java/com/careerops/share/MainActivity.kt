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
import android.text.Editable
import android.text.TextWatcher
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
        QUICK_SHARES("Quick Shares"),
        SETTINGS("Settings")
    }

    private lateinit var shellRoot: LinearLayout
    private lateinit var screenTitleView: TextView
    private lateinit var contentHost: FrameLayout

    private var payloadEditor: EditText? = null
    private var currentIntake: JobShareIntake? = null
    private var currentSessionPreset: CareerOpsPreset? = null
    private var currentQuickShareId: String = PresetCatalog.QUICK_ANALYZE.id
    private var activeSection: AppSection = AppSection.SHARE
    private var uiBuilt = false
    private var pendingRoutingError: String? = null

    private val quickShareDefinitions = PresetCatalog.builtIns
    private val actions = CareerOpsAction.entries.toList()
    private val destinations = DestinationCatalog.localDestinations()
    private val modelPreferences = ModelPreference.entries.toList()
    private val requestProfiles = RequestProfile.entries.toList()
    private val systemBarModes = SystemBarMode.entries.toList()
    private val themeModes = AppThemeMode.entries.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        val requestedTheme = AppPreferences.loadThemeMode(this)
        val darkTheme = AppAppearance.resolveDark(this, requestedTheme)
        setTheme(
            if (darkTheme) android.R.style.Theme_Material_NoActionBar
            else android.R.style.Theme_Material_Light_NoActionBar
        )
        super.onCreate(savedInstanceState)

        AppPreferences.ensureV03Migration(this)
        applyPalette()
        DirectShareShortcutPublisher.publish(this)

        // Recreate/configuration changes rebuild the editor but must never
        // auto-forward the original ACTION_SEND a second time.
        if (savedInstanceState == null && tryImmediateRoute(intent)) return

        val restoredSection = savedInstanceState
            ?.getString(STATE_SECTION)
            ?.let { name -> runCatching { AppSection.valueOf(name) }.getOrNull() }
            ?: AppSection.SHARE

        buildInteractiveShell(
            incoming = intent,
            initialSection = restoredSection,
            restoredSession = restoredSessionPreset(savedInstanceState),
            restoredIntakeText = savedInstanceState?.getString(STATE_INTAKE_TEXT)
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SECTION, activeSection.name)
        currentSessionPreset?.let { preset ->
            outState.putString(STATE_SESSION_ACTION, preset.action.id)
            outState.putString(STATE_SESSION_DESTINATION, preset.destinationId)
            outState.putString(STATE_SESSION_MODEL, preset.modelPreference.id)
            outState.putString(STATE_SESSION_REQUEST_PROFILE, preset.requestProfile.id)
        }
        outState.putString(STATE_INTAKE_TEXT, currentIntake?.rawSharedContent)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (tryImmediateRoute(intent)) return

        if (!uiBuilt) {
            buildInteractiveShell(intent, AppSection.SHARE, null, null)
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
        val route = if (!shortcutId.isNullOrBlank()) {
            val knownProfile = PresetCatalog.fromIdOrNull(shortcutId)
            if (knownProfile == null) {
                pendingRoutingError = "Quick Share '$shortcutId' is no longer available"
                return false
            }
            AppPreferences.loadPreset(this, knownProfile.id)
        } else if (AppPreferences.loadDirectShareEnabled(this)) {
            AppPreferences.loadRegularShareDefaults(this).asSessionPreset(autoForward = true)
        } else {
            return false
        }

        if (!route.autoForward) return false

        return when (val result = ShareRouter.route(this, incoming, route)) {
            is ShareRouteResult.Sent -> {
                if (!shortcutId.isNullOrBlank()) {
                    DirectShareShortcutPublisher.reportUsed(this, route.id)
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

    private fun buildInteractiveShell(
        incoming: Intent?,
        initialSection: AppSection,
        restoredSession: CareerOpsPreset?,
        restoredIntakeText: String?
    ) {
        shellRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_APP_BACKGROUND)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(14), dp(4))
            setBackgroundColor(COLOR_SURFACE)
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
            text = initialSection.title
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
            darkBackground = IS_DARK_THEME
        )

        uiBuilt = true
        consumeIncomingIntent(incoming)
        if (currentIntake == null && !restoredIntakeText.isNullOrBlank()) {
            currentIntake = ShareParser.parse(null, restoredIntakeText)
        }
        if (restoredSession != null) currentSessionPreset = restoredSession
        showSection(initialSection)
        showPendingRoutingError()
    }

    private fun showNavigationMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_SHARE, 0, "Share / Review")
            menu.add(0, MENU_QUICK_SHARES, 1, "Quick Shares")
            menu.add(0, MENU_SETTINGS, 2, "Settings")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_SHARE -> showSection(AppSection.SHARE)
                    MENU_QUICK_SHARES -> showSection(AppSection.QUICK_SHARES)
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
            AppSection.QUICK_SHARES -> buildQuickSharesScreen()
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

        val route = currentSessionPreset
            ?: AppPreferences.loadRegularShareDefaults(this).asSessionPreset()
                .also { currentSessionPreset = it }

        root.addView(
            sectionHeaderWithAction(
                title = "Route",
                actionText = "↗",
                contentDescription = "Edit regular share defaults"
            ) { showSection(AppSection.SETTINGS) }
        )

        val actionSpinner = spinner(actions.map { it.displayName })
        actionSpinner.setSelection(actions.indexOf(route.action).coerceAtLeast(0), false)
        actionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = actions.getOrNull(position) ?: return
                val current = currentSessionPreset ?: return
                if (selected == current.action) return
                currentSessionPreset = current.copy(action = selected)
                showSection(AppSection.SHARE)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        root.addView(actionSpinner)

        val destinationSpinner = spinner(destinations.map { it.displayName })
        destinationSpinner.setSelection(
            destinations.indexOfFirst { it.id == route.destinationId }.coerceAtLeast(0),
            false
        )
        destinationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = destinations.getOrNull(position) ?: return
                val current = currentSessionPreset ?: return
                if (selected.id == current.destinationId) return
                currentSessionPreset = current.copy(destinationId = selected.id)
                showSection(AppSection.SHARE)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        root.addView(destinationSpinner, marginTop = dp(6))
        root.addView(
            helperText(
                "${route.requestProfile.displayName} • ${route.modelPreference.displayName}. Review changes are temporary; ↗ edits saved regular-share defaults."
            )
        )

        root.addView(sectionLabel("Job link or shared text"))
        val intakeEditor = EditText(this).apply {
            minLines = 2
            maxLines = 6
            gravity = Gravity.TOP or Gravity.START
            textSize = 14f
            setTextColor(COLOR_PRIMARY_TEXT)
            setHintTextColor(COLOR_SECONDARY_TEXT)
            hint = "Paste a LinkedIn, Indeed, browser URL, or shared job text"
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBox(COLOR_SURFACE, COLOR_BORDER)
            setText(currentIntake?.rawSharedContent.orEmpty())
        }
        root.addView(
            intakeEditor,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            helperText(
                if (currentIntake == null) {
                    "Paste a job here to build the CareerOps request. Android shares populate this automatically."
                } else {
                    "Loaded automatically from the share. Edit this field to replace or correct the intake."
                }
            )
        )

        val intakeInfoView = infoCard(intakeSummary(currentIntake))
        root.addView(intakeInfoView, marginTop = dp(8))

        root.addView(sectionLabel("Prepared request"))
        payloadEditor = EditText(this).apply {
            minLines = 9
            gravity = Gravity.TOP or Gravity.START
            textSize = 15f
            setTextColor(COLOR_PRIMARY_TEXT)
            setHintTextColor(COLOR_SECONDARY_TEXT)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBox(COLOR_SURFACE, COLOR_BORDER)
            setText(renderCurrentPayload())
        }
        root.addView(
            payloadEditor,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        intakeEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                updateManualIntake(s?.toString().orEmpty(), intakeInfoView)
            }
        })

        val destination = DestinationCatalog.fromId(route.destinationId)
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

        return scroll
    }

    private fun buildQuickSharesScreen(): View {
        val scroll = ScrollView(this)
        val root = contentColumn()
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "Quick Share profiles are saved independently from regular app defaults. Edit a saved profile here, then choose which profiles occupy Android Direct Share Slot 1 and Slot 2."
            textSize = 14f
            setTextColor(COLOR_SECONDARY_TEXT)
        })

        root.addView(sectionLabel("Saved Quick Share profile"))
        val profileSpinner = spinner(quickShareDefinitions.map { it.name })
        val profileIndex = quickShareDefinitions.indexOfFirst { it.id == currentQuickShareId }
            .coerceAtLeast(0)
        profileSpinner.setSelection(profileIndex, false)
        profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = quickShareDefinitions.getOrNull(position) ?: return
                if (selected.id == currentQuickShareId) return
                currentQuickShareId = selected.id
                showSection(AppSection.QUICK_SHARES)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        root.addView(profileSpinner)

        val stored = AppPreferences.loadPreset(this, currentQuickShareId)

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
        root.addView(helperText("Stored independently for this Quick Share profile. App destinations currently use their own internal model selection."))

        root.addView(sectionLabel("Request profile"))
        val requestProfileSpinner = spinner(requestProfiles.map { it.displayName })
        requestProfileSpinner.setSelection(
            requestProfiles.indexOf(stored.requestProfile).coerceAtLeast(0),
            false
        )
        root.addView(requestProfileSpinner)

        root.addView(Button(this).apply {
            text = "Save Quick Share profile"
            setOnClickListener {
                saveQuickShareProfile(
                    actionSpinner,
                    destinationSpinner,
                    modelSpinner,
                    requestProfileSpinner
                )
            }
        }, fullWidthButtonParams(dp(16)))

        root.addView(sectionLabel("Active Android Direct Share"))
        root.addView(helperText("Each slot may be None. Slot order controls Android shortcut rank. Choosing a profile already used by the other slot moves it to the selected slot."))

        val slotOptions = listOf<String?>(null) + quickShareDefinitions.map { it.id }
        val optionLabels = listOf("None") + quickShareDefinitions.map { it.name }
        val slots = AppPreferences.loadDirectShareSlots(this)

        root.addView(helperText("Slot 1"))
        val slot1Spinner = spinner(optionLabels)
        slot1Spinner.setSelection(slotOptions.indexOf(slots.firstProfileId).coerceAtLeast(0), false)
        slot1Spinner.onItemSelectedListener = directShareSlotListener(
            slot = 1,
            options = slotOptions,
            currentId = slots.firstProfileId
        )
        root.addView(slot1Spinner)

        root.addView(helperText("Slot 2"))
        val slot2Spinner = spinner(optionLabels)
        slot2Spinner.setSelection(slotOptions.indexOf(slots.secondProfileId).coerceAtLeast(0), false)
        slot2Spinner.onItemSelectedListener = directShareSlotListener(
            slot = 2,
            options = slotOptions,
            currentId = slots.secondProfileId
        )
        root.addView(slot2Spinner)

        root.addView(Button(this).apply {
            text = "Clear Direct Share"
            setOnClickListener {
                AppPreferences.clearDirectShareSelections(this@MainActivity)
                val result = DirectShareShortcutPublisher.publish(this@MainActivity)
                toast(shortcutResultMessage("Direct Share cleared", result))
                showSection(AppSection.QUICK_SHARES)
            }
        }, fullWidthButtonParams(dp(14)))

        root.addView(helperText("Custom named Quick Share profiles are the next extension; the two-slot activation model is already independent from the saved profile library."))
        return scroll
    }

    private fun buildSettingsScreen(): View {
        val scroll = ScrollView(this)
        val root = contentColumn()
        scroll.addView(root)

        root.addView(sectionLabel("Regular Share defaults"))
        val defaults = AppPreferences.loadRegularShareDefaults(this)

        root.addView(helperText("CareerOps action"))
        val defaultActionSpinner = spinner(actions.map { it.displayName })
        defaultActionSpinner.setSelection(actions.indexOf(defaults.action).coerceAtLeast(0), false)
        root.addView(defaultActionSpinner)

        root.addView(helperText("Destination"))
        val defaultDestinationSpinner = spinner(destinations.map { it.displayName })
        defaultDestinationSpinner.setSelection(
            destinations.indexOfFirst { it.id == defaults.destinationId }.coerceAtLeast(0),
            false
        )
        root.addView(defaultDestinationSpinner)

        root.addView(helperText("Model preference"))
        val defaultModelSpinner = spinner(modelPreferences.map { it.displayName })
        defaultModelSpinner.setSelection(
            modelPreferences.indexOf(defaults.modelPreference).coerceAtLeast(0),
            false
        )
        root.addView(defaultModelSpinner)

        root.addView(helperText("Request profile"))
        val defaultRequestProfileSpinner = spinner(requestProfiles.map { it.displayName })
        defaultRequestProfileSpinner.setSelection(
            requestProfiles.indexOf(defaults.requestProfile).coerceAtLeast(0),
            false
        )
        root.addView(defaultRequestProfileSpinner)

        root.addView(Button(this).apply {
            text = "Save regular-share defaults"
            setOnClickListener {
                val updated = RegularShareDefaults(
                    action = actions.getOrElse(defaultActionSpinner.selectedItemPosition) {
                        defaults.action
                    },
                    destinationId = destinations.getOrElse(
                        defaultDestinationSpinner.selectedItemPosition
                    ) { DestinationCatalog.CHATGPT }.id,
                    modelPreference = modelPreferences.getOrElse(
                        defaultModelSpinner.selectedItemPosition
                    ) { ModelPreference.AUTO },
                    requestProfile = requestProfiles.getOrElse(
                        defaultRequestProfileSpinner.selectedItemPosition
                    ) { RequestProfile.CAREEROPS_STANDARD }
                )
                AppPreferences.saveRegularShareDefaults(this@MainActivity, updated)
                toast("Regular Share defaults saved")
            }
        }, fullWidthButtonParams(dp(10)))

        val autoForwardCheckBox = CheckBox(this).apply {
            text = "Skip review for normal shares and use regular-share defaults"
            setTextColor(COLOR_PRIMARY_TEXT)
            isChecked = AppPreferences.loadDirectShareEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, isChecked ->
                AppPreferences.saveDirectShareEnabled(this@MainActivity, isChecked)
            }
        }
        root.addView(autoForwardCheckBox, fullWidthButtonParams(dp(8)))
        root.addView(helperText("Quick Share profiles and Direct Share slots are configured separately under Quick Shares."))

        root.addView(sectionLabel("Appearance"))
        val themeSpinner = spinner(themeModes.map { it.displayName })
        val currentTheme = AppPreferences.loadThemeMode(this)
        themeSpinner.setSelection(themeModes.indexOf(currentTheme).coerceAtLeast(0), false)
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = themeModes.getOrElse(position) { AppThemeMode.FOLLOW_SYSTEM }
                if (selected == AppPreferences.loadThemeMode(this@MainActivity)) return
                AppPreferences.saveThemeMode(this@MainActivity, selected)
                toast("Appearance: ${selected.displayName}")
                recreate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        root.addView(themeSpinner)
        root.addView(helperText("Follow system, Light, and Dark are available now. Optional scheduled switching remains a later #5 enhancement."))

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
                    darkBackground = IS_DARK_THEME
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        root.addView(systemBarSpinner)
        root.addView(helperText("Standard mode keeps content clear of system bars. Immersive currently hides them; true edge-to-edge expansion remains tracked in #6."))

        root.addView(sectionLabel("Destinations & accounts"))
        root.addView(
            infoCard(
                "Enabled now: ChatGPT and Android chooser.\n\nFuture destination integrations, CareerOps Gateway, authentication/login hooks, and account state live here instead of on the Share screen."
            )
        )
        return scroll
    }

    private fun saveQuickShareProfile(
        actionSpinner: Spinner,
        destinationSpinner: Spinner,
        modelSpinner: Spinner,
        requestProfileSpinner: Spinner
    ) {
        val stored = AppPreferences.loadPreset(this, currentQuickShareId)
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
            }
        )
        AppPreferences.savePreset(this, updated)
        val result = DirectShareShortcutPublisher.publish(this)
        toast(shortcutResultMessage("Quick Share saved", result))
        showSection(AppSection.QUICK_SHARES)
    }

    private fun directShareSlotListener(
        slot: Int,
        options: List<String?>,
        currentId: String?
    ): AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedId = options.getOrNull(position)
                if (selectedId == currentId) return
                AppPreferences.saveDirectShareSlot(this@MainActivity, slot, selectedId)
                val result = DirectShareShortcutPublisher.publish(this@MainActivity)
                toast(shortcutResultMessage("Direct Share updated", result))
                showSection(AppSection.QUICK_SHARES)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

    private fun shortcutResultMessage(prefix: String, result: ShortcutPublishResult): String =
        when {
            result.error != null -> "$prefix • ${result.error}"
            result.rateLimited -> "$prefix • Android deferred shortcut refresh"
            else -> "$prefix • ${result.publishedCount} active"
        }

    private fun consumeIncomingIntent(incoming: Intent?) {
        val requestedProfileId = when {
            incoming?.action == DirectShareContract.ACTION_OPEN_PRESET ->
                incoming.getStringExtra(DirectShareContract.EXTRA_PRESET_ID)
            incoming?.action == Intent.ACTION_SEND ->
                incoming.getStringExtra(DirectShareContract.EXTRA_SHORTCUT_ID)
            else -> null
        }

        val quickProfile = PresetCatalog.fromIdOrNull(requestedProfileId)
        if (quickProfile != null) {
            currentQuickShareId = quickProfile.id
            currentSessionPreset = AppPreferences.loadPreset(this, quickProfile.id)
        } else {
            currentSessionPreset = AppPreferences.loadRegularShareDefaults(this).asSessionPreset()
        }
        currentIntake = IncomingShareReader.read(this, incoming)
    }

    private fun updateManualIntake(rawValue: String, intakeInfoView: TextView) {
        val value = rawValue.trim()
        currentIntake = if (value.isBlank()) null else ShareParser.parse(null, value)
        intakeInfoView.text = intakeSummary(currentIntake)
        payloadEditor?.setText(renderCurrentPayload())
    }

    private fun intakeSummary(intake: JobShareIntake?): String {
        if (intake == null) return "No job loaded yet."
        val jobId = intake.jobId ?: "Not detected"
        val url = intake.canonicalUrl ?: "No URL detected"
        val truncation = if (intake.wasTruncated) "\nShared text was capped at 100,000 characters." else ""
        return "Source: ${intake.source}\nJob ID: $jobId\n$url$truncation"
    }

    private fun renderCurrentPayload(): String {
        val intake = currentIntake ?: return DEFAULT_PROMPT
        val route = currentSessionPreset
            ?: AppPreferences.loadRegularShareDefaults(this).asSessionPreset()
        return CareerOpsRoutePlanner.plan(intake, route).payload
    }

    private fun sendToCurrentDestination() {
        val payload = currentPayload() ?: return
        val route = currentSessionPreset
            ?: AppPreferences.loadRegularShareDefaults(this).asSessionPreset()
        val destination = DestinationCatalog.fromId(route.destinationId)
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
            toast("Add or share a job first")
            return
        }
        copyToClipboard("CareerOps request JSON", CareerOpsRequestRenderer.toJson(request))
        toast("CareerOps request JSON copied")
    }

    private fun currentRequest(): CareerOpsRequest? {
        val intake = currentIntake ?: return null
        val route = currentSessionPreset
            ?: AppPreferences.loadRegularShareDefaults(this).asSessionPreset()
        return CareerOpsRequest(action = route.action, job = intake)
    }

    private fun currentPayload(): String? {
        if (currentIntake == null) {
            toast("Add or share a job first")
            return null
        }
        val payload = payloadEditor?.text?.toString()?.trim().orEmpty()
        if (payload.isBlank()) {
            toast("Nothing to send")
            return null
        }
        return payload
    }

    private fun restoredSessionPreset(state: Bundle?): CareerOpsPreset? {
        state ?: return null
        val action = state.getString(STATE_SESSION_ACTION) ?: return null
        return RegularShareDefaults(
            action = CareerOpsAction.fromId(action),
            destinationId = state.getString(STATE_SESSION_DESTINATION)
                ?: DestinationCatalog.CHATGPT.id,
            modelPreference = ModelPreference.fromId(state.getString(STATE_SESSION_MODEL)),
            requestProfile = RequestProfile.fromId(state.getString(STATE_SESSION_REQUEST_PROFILE))
        ).asSessionPreset()
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

    private fun applyPalette() {
        val palette = AppAppearance.palette(this)
        IS_DARK_THEME = palette.isDark
        COLOR_APP_BACKGROUND = palette.background
        COLOR_SURFACE = palette.surface
        COLOR_PRIMARY_TEXT = palette.primaryText
        COLOR_SECONDARY_TEXT = palette.secondaryText
        COLOR_BORDER = palette.border
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

    private fun sectionHeaderWithAction(
        title: String,
        actionText: String,
        contentDescription: String,
        onAction: () -> Unit
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(12), 0, dp(4))
        addView(TextView(this@MainActivity).apply {
            text = title
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(COLOR_PRIMARY_TEXT)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(TextView(this@MainActivity).apply {
            text = actionText
            textSize = 18f
            setTextColor(COLOR_SECONDARY_TEXT)
            gravity = Gravity.CENTER
            this.contentDescription = contentDescription
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { onAction() }
        })
    }

    private fun helperText(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 12f
            setTextColor(COLOR_SECONDARY_TEXT)
            setPadding(0, dp(5), 0, 0)
        }

    private fun infoCard(value: String): TextView =
        TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(COLOR_PRIMARY_TEXT)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBox(COLOR_SURFACE, COLOR_BORDER)
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
        ).apply { this.leftMargin = leftMargin }

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
        private const val DEFAULT_PROMPT = "Paste or share a job above to build the CareerOps request."
        private const val STATE_SECTION = "careerops.active_section"
        private const val STATE_SESSION_ACTION = "careerops.session.action"
        private const val STATE_SESSION_DESTINATION = "careerops.session.destination"
        private const val STATE_SESSION_MODEL = "careerops.session.model"
        private const val STATE_SESSION_REQUEST_PROFILE = "careerops.session.request_profile"
        private const val STATE_INTAKE_TEXT = "careerops.session.intake_text"

        private const val MENU_SHARE = 100
        private const val MENU_QUICK_SHARES = 101
        private const val MENU_SETTINGS = 102

        private var IS_DARK_THEME = false
        private var COLOR_APP_BACKGROUND = Color.rgb(247, 248, 250)
        private var COLOR_SURFACE = Color.WHITE
        private var COLOR_PRIMARY_TEXT = Color.rgb(28, 34, 43)
        private var COLOR_SECONDARY_TEXT = Color.rgb(96, 105, 118)
        private var COLOR_BORDER = Color.rgb(216, 222, 230)
    }
}
