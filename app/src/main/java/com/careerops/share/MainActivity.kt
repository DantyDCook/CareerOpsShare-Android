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
    private lateinit var actionSpinner: Spinner
    private lateinit var destinationSpinner: Spinner

    private val actions = CareerOpsAction.entries.toList()
    private val destinations = DestinationCatalog.localDestinations()

    private var currentIntake: JobShareIntake? = null
    private var suppressSelectionCallbacks = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        restoreSelections()
        suppressSelectionCallbacks = false
        consumeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
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
            text = "Smart CareerOps intake for Android. Normalize a shared job, choose the CareerOps action and destination, review the request, then send it."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, dp(20))
        })

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

        root.addView(sectionLabel("CareerOps action"))
        actionSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                actions.map { it.displayName }
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (suppressSelectionCallbacks) return
                    AppPreferences.saveAction(this@MainActivity, actions[position])
                    refreshRenderedPayload()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(actionSpinner)

        root.addView(sectionLabel("Send using"))
        destinationSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                destinations.map { it.displayName }
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (suppressSelectionCallbacks) return
                    AppPreferences.saveDestination(this@MainActivity, destinations[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        root.addView(destinationSpinner)

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
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })

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
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })

        root.addView(TextView(this).apply {
            text = "v0.2.0 • Local-only. Destination/transport abstraction added; network transport is intentionally disabled."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(22), 0, 0)
        })

        return scroll
    }

    private fun restoreSelections() {
        val savedAction = AppPreferences.loadAction(this)
        actionSpinner.setSelection(actions.indexOf(savedAction).coerceAtLeast(0))

        val savedDestination = AppPreferences.loadDestination(this)
        destinationSpinner.setSelection(
            destinations.indexOfFirst { it.id == savedDestination.id }.coerceAtLeast(0)
        )
    }

    private fun consumeIntent(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_SEND || incoming.type?.startsWith("text/") != true) {
            currentIntake = null
            sourceView.text = "Source\nOpen directly or share text/URL into this app"
            jobIdView.text = "Job ID\n—"
            urlView.text = "Canonical URL\n—"
            truncationView.visibility = View.GONE
            payloadEditor.setText(DEFAULT_PROMPT)
            return
        }

        val sharedText = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: incoming.clipData?.let { clip ->
                if (clip.itemCount > 0) {
                    clip.getItemAt(0).coerceToText(this)?.toString()
                } else {
                    null
                }
            }

        val parsed = ShareParser.parse(
            incoming.getStringExtra(Intent.EXTRA_SUBJECT),
            sharedText
        )

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
        val request = CareerOpsRequest(
            action = selectedAction(),
            job = intake
        )
        payloadEditor.setText(CareerOpsRequestRenderer.toText(request))
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
            toast("${destination.displayName} is not enabled in v0.2.0")
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

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val DEFAULT_PROMPT = "Analyze this job using CareerOps:"
    }
}
