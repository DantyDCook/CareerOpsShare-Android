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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var sourceView: TextView
    private lateinit var jobIdView: TextView
    private lateinit var urlView: TextView
    private lateinit var payloadEditor: EditText
    private lateinit var truncationView: TextView
    private lateinit var presetSpinner: Spinner
    private lateinit var actionSpinner: Spinner
    private lateinit var destinationSpinner: Spinner
    private lateinit var modelSpinner: Spinner
    private lateinit var requestProfileSpinner: Spinner
    private lateinit var directShareCheckBox: CheckBox
    private lateinit var showInDirectShareCheckBox: CheckBox
    private lateinit var defaultPresetView: TextView

    private val presetDefinitions = PresetCatalog.builtIns
    private val actions = CareerOpsAction.entries.toList()
    private val destinations = DestinationCatalog.localDestinations()
    private val modelPreferences = ModelPreference.entries.toList()
    private val requestProfiles = RequestProfile.entries.toList()

    private var currentIntake: JobShareIntake? = null
    private var currentPresetId: String = PresetCatalog.QUICK_ANALYZE.id
    private var suppressSelectionCallbacks = true
    private var uiBuilt = false
    private var pendingRoutingError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppPreferences.ensureV03Migration(this)
        DirectShareShortcutPublisher.publish(this)

        if (tryImmediateRoute(intent)) return
        buildInteractiveUi(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (tryImmediateRoute(intent)) return

        if (!uiBuilt) {
            buildInteractiveUi(intent)
        } else {
            applyPresetSelectionForIntent(intent)
            consumeIntent(intent)
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

    private fun buildInteractiveUi(incoming: Intent?) {
        suppressSelectionCallbacks = true
        setContentView(buildUi())
        uiBuilt = true
        restoreSettings(incoming)
        suppressSelectionCallbacks = false
        consumeIntent(incoming)
        showPendingRoutingError()
    }

    private fun buildUi(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "CareerOps Share"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(24, 30, 40))
        })

        root.addView(TextView(this).apply {
            text = "Choose CareerOps Share for review/default behavior, or tap a CareerOps preset in Android Direct Share to route immediately."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, dp(12))
        })

        root.addView(sectionLabel("Routing preset"))
        presetSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                presetDefinitions.map { it.name }
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (suppressSelectionCallbacks) return
                    loadPresetIntoControls(presetDefinitions[position].id)
                    refreshRenderedPayload()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(presetSpinner)

        defaultPresetView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(6), 0, 0)
        }
        root.addView(defaultPresetView)

        root.addView(Button(this).apply {
            text = "Set selected preset as default"
            setOnClickListener {
                AppPreferences.saveDefaultPreset(this@MainActivity, currentPresetId)
                updateDefaultPresetSummary()
                toast("Default preset: ${selectedStoredPreset().name}")
            }
        }, fullWidthButtonParams(dp(8)))

        directShareCheckBox = CheckBox(this).apply {
            text = "Skip editor for normal shares (use default preset)"
            setOnCheckedChangeListener { _, isChecked ->
                if (suppressSelectionCallbacks) return@setOnCheckedChangeListener
                AppPreferences.saveDirectShareEnabled(this@MainActivity, isChecked)
                updateDefaultPresetSummary()
            }
        }
        root.addView(directShareCheckBox)

        showInDirectShareCheckBox = CheckBox(this).apply {
            text = "Show selected preset in Android Direct Share"
            setOnCheckedChangeListener { _, isChecked ->
                if (suppressSelectionCallbacks) return@setOnCheckedChangeListener
                val stored = selectedStoredPreset().copy(showInDirectShare = isChecked)
                AppPreferences.savePreset(this@MainActivity, stored)
                DirectShareShortcutPublisher.publish(this@MainActivity)
            }
        }
        root.addView(showInDirectShareCheckBox)

        root.addView(sectionLabel("CareerOps action"))
        actionSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                actions.map { it.displayName }
            )
            onItemSelectedListener = refreshListener()
        }
        root.addView(actionSpinner)

        root.addView(sectionLabel("Send using"))
        destinationSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                destinations.map { it.displayName }
            )
        }
        root.addView(destinationSpinner)

        root.addView(sectionLabel("Model preference"))
        modelSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                modelPreferences.map { it.displayName }
            )
        }
        root.addView(modelSpinner)
        root.addView(TextView(this).apply {
            text = "Model preference is stored with the preset for future routing. Android can choose the destination app, but cannot force that app's internal model; enforced model routing belongs to the future CareerOps Gateway."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(4), 0, 0)
        })

        root.addView(sectionLabel("Request profile"))
        requestProfileSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                requestProfiles.map { it.displayName }
            )
            onItemSelectedListener = refreshListener()
        }
        root.addView(requestProfileSpinner)

        root.addView(Button(this).apply {
            text = "Save preset"
            setOnClickListener { saveSelectedPreset() }
        }, fullWidthButtonParams(dp(12)))

        root.addView(sectionLabel("Shared job"))
        sourceView = infoRow("Source", "Waiting for shared content")
        root.addView(sourceView)

        jobIdView = infoRow("Job ID", "—")
        root.addView(jobIdView)

        urlView = infoRow("Canonical URL", "—")
        root.addView(urlView)

        truncationView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(150, 80, 20))
            visibility = View.GONE
            setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(truncationView)

        root.addView(sectionLabel("Prepared request"))

        payloadEditor = EditText(this).apply {
            minLines = 10
            gravity = Gravity.TOP or Gravity.START
            textSize = 15f
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBox(Color.rgb(245, 247, 250), Color.rgb(205, 211, 220))
            setText(DEFAULT_PROMPT)
        }
        root.addView(
            payloadEditor,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(Button(this).apply {
            text = "Send"
            setOnClickListener { sendToSelectedDestination() }
        }, fullWidthButtonParams(dp(16)))

        val rowOne = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(8), 0, 0)
        }

        rowOne.addView(Button(this).apply {
            text = "Copy"
            setOnClickListener { copyPayload() }
        }, buttonParams())

        rowOne.addView(Button(this).apply {
            text = "Other app…"
            setOnClickListener { sendWithSystemChooser() }
        }, buttonParams(leftMargin = dp(10)))

        root.addView(rowOne)

        root.addView(Button(this).apply {
            text = "Copy request JSON"
            setOnClickListener { copyRequestJson() }
        }, fullWidthButtonParams(dp(8)))

        root.addView(TextView(this).apply {
            text = "v0.3.0-dev • Preset routing + Android Direct Share. Local app transports only; HTTP/Gateway transport remains disabled."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(22), 0, 0)
        })

        return scroll
    }

    private fun refreshListener(): AdapterView.OnItemSelectedListener =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (suppressSelectionCallbacks) return
                refreshRenderedPayload()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

    private fun restoreSettings(incoming: Intent?) {
        directShareCheckBox.isChecked = AppPreferences.loadDirectShareEnabled(this)
        applyPresetSelectionForIntent(incoming)
        updateDefaultPresetSummary()
    }

    private fun applyPresetSelectionForIntent(incoming: Intent?) {
        val requestedPresetId = when {
            incoming?.action == DirectShareContract.ACTION_OPEN_PRESET ->
                incoming.getStringExtra(DirectShareContract.EXTRA_PRESET_ID)
            incoming?.action == Intent.ACTION_SEND ->
                incoming.getStringExtra(DirectShareContract.EXTRA_SHORTCUT_ID)
            else -> null
        }

        val presetId = PresetCatalog.fromIdOrNull(requestedPresetId)?.id
            ?: AppPreferences.loadDefaultPresetId(this)
        val index = presetDefinitions.indexOfFirst { it.id == presetId }.coerceAtLeast(0)

        val previousSuppress = suppressSelectionCallbacks
        suppressSelectionCallbacks = true
        presetSpinner.setSelection(index)
        loadPresetIntoControls(presetDefinitions[index].id)
        suppressSelectionCallbacks = previousSuppress
    }

    private fun loadPresetIntoControls(presetId: String) {
        currentPresetId = PresetCatalog.fromId(presetId).id
        val preset = AppPreferences.loadPreset(this, currentPresetId)

        val previousSuppress = suppressSelectionCallbacks
        suppressSelectionCallbacks = true

        actionSpinner.setSelection(actions.indexOf(preset.action).coerceAtLeast(0))
        destinationSpinner.setSelection(
            destinations.indexOfFirst { it.id == preset.destinationId }.coerceAtLeast(0)
        )
        modelSpinner.setSelection(
            modelPreferences.indexOf(preset.modelPreference).coerceAtLeast(0)
        )
        requestProfileSpinner.setSelection(
            requestProfiles.indexOf(preset.requestProfile).coerceAtLeast(0)
        )
        showInDirectShareCheckBox.isChecked = preset.showInDirectShare

        suppressSelectionCallbacks = previousSuppress
    }

    private fun saveSelectedPreset() {
        val preset = workingPresetFromControls()
        AppPreferences.savePreset(this, preset)
        val publishResult = DirectShareShortcutPublisher.publish(this)

        val shortcutMessage = when {
            publishResult.error != null -> " • shortcut update: ${publishResult.error}"
            publishResult.rateLimited -> " • shortcut update deferred by Android rate limit"
            else -> " • ${publishResult.publishedCount} Direct Share preset(s)"
        }
        toast("${preset.name} saved$shortcutMessage")
        updateDefaultPresetSummary()
    }

    private fun selectedStoredPreset(): CareerOpsPreset =
        AppPreferences.loadPreset(this, currentPresetId)

    private fun workingPresetFromControls(): CareerOpsPreset {
        val base = selectedStoredPreset()
        return base.copy(
            action = selectedAction(),
            destinationId = selectedDestination().id,
            modelPreference = selectedModelPreference(),
            requestProfile = selectedRequestProfile(),
            showInDirectShare = showInDirectShareCheckBox.isChecked
        )
    }

    private fun updateDefaultPresetSummary() {
        val defaultPreset = AppPreferences.loadDefaultPreset(this)
        val behavior = if (AppPreferences.loadDirectShareEnabled(this)) {
            "normal shares send immediately"
        } else {
            "normal shares open the editor"
        }
        defaultPresetView.text = "Default: ${defaultPreset.name} • $behavior"
    }

    private fun consumeIntent(incoming: Intent?) {
        val parsed = IncomingShareReader.read(this, incoming)
        if (parsed == null) {
            currentIntake = null
            sourceView.text = "Source\nOpen directly or share text/URL into this app"
            jobIdView.text = "Job ID\n—"
            urlView.text = "Canonical URL\n—"
            truncationView.visibility = View.GONE
            payloadEditor.setText(DEFAULT_PROMPT)
            return
        }

        currentIntake = parsed
        sourceView.text = "Source\n${parsed.source}"
        jobIdView.text = "Job ID\n${parsed.jobId ?: "Not detected"}"
        urlView.text = "Canonical URL\n${parsed.canonicalUrl ?: "No URL found — shared text will still be sent"}"
        truncationView.visibility = if (parsed.wasTruncated) View.VISIBLE else View.GONE
        truncationView.text = if (parsed.wasTruncated) {
            "Very large shared text was capped at 100,000 characters."
        } else {
            ""
        }

        refreshRenderedPayload()
    }

    private fun refreshRenderedPayload() {
        val intake = currentIntake ?: return
        val plan = CareerOpsRoutePlanner.plan(intake, workingPresetFromControls())
        payloadEditor.setText(plan.payload)
        payloadEditor.setSelection(payloadEditor.text.length)
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

    private fun sendToSelectedDestination() {
        val payload = currentPayload() ?: return
        val destination = selectedDestination()
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

    private fun currentRequest(): CareerOpsRequest? =
        currentIntake?.let {
            CareerOpsRequest(
                action = selectedAction(),
                job = it
            )
        }

    private fun currentPayload(): String? {
        val payload = payloadEditor.text.toString().trim()
        if (payload.isBlank()) {
            toast("Nothing to send")
            return null
        }
        return payload
    }

    private fun selectedAction(): CareerOpsAction =
        actions.getOrElse(actionSpinner.selectedItemPosition) { CareerOpsAction.ANALYZE }

    private fun selectedDestination(): DestinationProfile =
        destinations.getOrElse(destinationSpinner.selectedItemPosition) {
            DestinationCatalog.CHATGPT
        }

    private fun selectedModelPreference(): ModelPreference =
        modelPreferences.getOrElse(modelSpinner.selectedItemPosition) {
            ModelPreference.AUTO
        }

    private fun selectedRequestProfile(): RequestProfile =
        requestProfiles.getOrElse(requestProfileSpinner.selectedItemPosition) {
            RequestProfile.CAREEROPS_STANDARD
        }

    private fun showPendingRoutingError() {
        pendingRoutingError?.let {
            pendingRoutingError = null
            toast(it)
        }
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    }

    private fun sectionLabel(textValue: String): TextView =
        TextView(this).apply {
            text = textValue
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(6))
        }

    private fun infoRow(label: String, value: String): TextView =
        TextView(this).apply {
            text = "$label\n$value"
            textSize = 14f
            setTextColor(Color.rgb(40, 46, 56))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = roundedBox(Color.rgb(248, 249, 251), Color.rgb(224, 228, 234))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

    private fun roundedBox(fill: Int, stroke: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(fill)
            setStroke(dp(1), stroke)
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
    }
}
