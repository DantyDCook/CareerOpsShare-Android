package com.careerops.share

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var sourceView: TextView
    private lateinit var urlView: TextView
    private lateinit var payloadEditor: EditText
    private lateinit var truncationView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
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
            text = "Share a job posting or URL from LinkedIn, Indeed, Chrome, or another app. Review the CareerOps payload, then send it to ChatGPT."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, dp(20))
        })

        sourceView = infoRow("Source", "Waiting for shared content")
        root.addView(sourceView)

        urlView = infoRow("Detected URL", "—")
        root.addView(urlView)

        truncationView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(150, 80, 20))
            visibility = View.GONE
            setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(truncationView)

        root.addView(TextView(this).apply {
            text = "Prepared payload"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(6))
        })

        payloadEditor = EditText(this).apply {
            minLines = 8
            gravity = Gravity.TOP or Gravity.START
            textSize = 15f
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBox(Color.rgb(245, 247, 250), Color.rgb(205, 211, 220))
            setText(DEFAULT_PROMPT)
        }
        root.addView(payloadEditor, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        root.addView(Button(this).apply {
            text = "Send to ChatGPT"
            setOnClickListener { sendToChatGPT() }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) })

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(8), 0, 0)
        }

        buttons.addView(Button(this).apply {
            text = "Copy"
            setOnClickListener { copyPayload() }
        }, buttonParams())

        buttons.addView(Button(this).apply {
            text = "Other app…"
            setOnClickListener { forwardPayload() }
        }, buttonParams(leftMargin = dp(10)))

        root.addView(buttons)

        root.addView(TextView(this).apply {
            text = "v0.1.1 • Local-only. No network or account permissions."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(22), 0, 0)
        })

        return scroll
    }

    private fun consumeIntent(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_SEND || incoming.type?.startsWith("text/") != true) {
            sourceView.text = "Source\nOpen directly or share text/URL into this app"
            urlView.text = "Detected URL\n—"
            truncationView.visibility = View.GONE
            if (payloadEditor.text.isNullOrBlank()) payloadEditor.setText(DEFAULT_PROMPT)
            return
        }

        val sharedText = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            ?: incoming.clipData?.let { clip ->
                if (clip.itemCount > 0) clip.getItemAt(0).coerceToText(this)?.toString() else null
            }

        val parsed = ShareParser.parse(
            incoming.getStringExtra(Intent.EXTRA_SUBJECT),
            sharedText
        )

        sourceView.text = "Source\n${parsed.source}"
        urlView.text = "Detected URL\n${parsed.url ?: "No URL found — shared text will still be sent"}"
        truncationView.visibility = if (parsed.wasTruncated) View.VISIBLE else View.GONE
        truncationView.text = if (parsed.wasTruncated) "Very large shared text was capped at 100,000 characters." else ""
        payloadEditor.setText(parsed.payload)
        payloadEditor.setSelection(payloadEditor.text.length)
    }

    private fun copyPayload() {
        val payload = currentPayload() ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("CareerOps payload", payload))
        toast("CareerOps payload copied")
    }

    private fun sendToChatGPT() {
        val payload = currentPayload() ?: return
        val intent = makeShareIntent(payload).apply { setPackage(CHATGPT_PACKAGE) }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast("ChatGPT app not found — choose a destination")
            forwardPayload(payload)
        }
    }

    private fun forwardPayload(payloadOverride: String? = null) {
        val payload = payloadOverride ?: currentPayload() ?: return
        startActivity(Intent.createChooser(makeShareIntent(payload), "Send CareerOps payload"))
    }

    private fun makeShareIntent(payload: String) = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, payload)
        putExtra(Intent.EXTRA_SUBJECT, "CareerOps job analysis")
        putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(ComponentName(this@MainActivity, MainActivity::class.java))
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

    private fun infoRow(label: String, value: String): TextView = TextView(this).apply {
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

    private fun roundedBox(fill: Int, stroke: Int): GradientDrawable = GradientDrawable().apply {
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
        ).apply { this.leftMargin = leftMargin }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val DEFAULT_PROMPT = "Analyze this job using CareerOps:"
        private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    }
}
