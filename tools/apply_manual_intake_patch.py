from pathlib import Path

MAIN = Path("app/src/main/java/com/careerops/share/MainActivity.kt")
TEST = Path("app/src/test/java/com/careerops/share/ShareParserTest.kt")

text = MAIN.read_text(encoding="utf-8")

replacements = [
    (
        "import android.os.Bundle\nimport android.view.Gravity",
        "import android.os.Bundle\nimport android.text.Editable\nimport android.text.TextWatcher\nimport android.view.Gravity",
    ),
    (
        "        buildInteractiveShell(\n            incoming = intent,\n            initialSection = restoredSection,\n            restoredSession = restoredSessionPreset(savedInstanceState)\n        )",
        "        buildInteractiveShell(\n            incoming = intent,\n            initialSection = restoredSection,\n            restoredSession = restoredSessionPreset(savedInstanceState),\n            restoredIntakeText = savedInstanceState?.getString(STATE_INTAKE_TEXT)\n        )",
    ),
    (
        "        currentSessionPreset?.let { preset ->\n            outState.putString(STATE_SESSION_ACTION, preset.action.id)\n            outState.putString(STATE_SESSION_DESTINATION, preset.destinationId)\n            outState.putString(STATE_SESSION_MODEL, preset.modelPreference.id)\n            outState.putString(STATE_SESSION_REQUEST_PROFILE, preset.requestProfile.id)\n        }\n        super.onSaveInstanceState(outState)",
        "        currentSessionPreset?.let { preset ->\n            outState.putString(STATE_SESSION_ACTION, preset.action.id)\n            outState.putString(STATE_SESSION_DESTINATION, preset.destinationId)\n            outState.putString(STATE_SESSION_MODEL, preset.modelPreference.id)\n            outState.putString(STATE_SESSION_REQUEST_PROFILE, preset.requestProfile.id)\n        }\n        outState.putString(STATE_INTAKE_TEXT, currentIntake?.rawSharedContent)\n        super.onSaveInstanceState(outState)",
    ),
    (
        "            buildInteractiveShell(intent, AppSection.SHARE, null)",
        "            buildInteractiveShell(intent, AppSection.SHARE, null, null)",
    ),
    (
        "    private fun buildInteractiveShell(\n        incoming: Intent?,\n        initialSection: AppSection,\n        restoredSession: CareerOpsPreset?\n    ) {",
        "    private fun buildInteractiveShell(\n        incoming: Intent?,\n        initialSection: AppSection,\n        restoredSession: CareerOpsPreset?,\n        restoredIntakeText: String?\n    ) {",
    ),
    (
        "        uiBuilt = true\n        consumeIncomingIntent(incoming)\n        if (restoredSession != null) currentSessionPreset = restoredSession\n        showSection(initialSection)",
        "        uiBuilt = true\n        consumeIncomingIntent(incoming)\n        if (currentIntake == null && !restoredIntakeText.isNullOrBlank()) {\n            currentIntake = ShareParser.parse(null, restoredIntakeText)\n        }\n        if (restoredSession != null) currentSessionPreset = restoredSession\n        showSection(initialSection)",
    ),
    (
        "        root.addView(sectionLabel(\"Shared job\"))\n        val intake = currentIntake\n        if (intake == null) {\n            root.addView(\n                infoCard(\n                    \"No shared job loaded.\\n\\nShare a LinkedIn, Indeed, browser, or text job posting into CareerOps Share.\"\n                )\n            )\n        } else {\n            val jobId = intake.jobId ?: \"Not detected\"\n            val url = intake.canonicalUrl ?: \"No URL detected\"\n            root.addView(infoCard(\"Source: ${intake.source}\\nJob ID: $jobId\\n$url\"))\n            if (intake.wasTruncated) {\n                root.addView(TextView(this).apply {\n                    text = \"Very large shared text was capped at 100,000 characters.\"\n                    textSize = 12f\n                    setTextColor(Color.rgb(210, 132, 53))\n                    setPadding(0, dp(6), 0, 0)\n                })\n            }\n        }\n\n        root.addView(sectionLabel(\"Prepared request\"))",
        "        root.addView(sectionLabel(\"Job link or shared text\"))\n        val intakeEditor = EditText(this).apply {\n            minLines = 2\n            maxLines = 6\n            gravity = Gravity.TOP or Gravity.START\n            textSize = 14f\n            setTextColor(COLOR_PRIMARY_TEXT)\n            setHintTextColor(COLOR_SECONDARY_TEXT)\n            hint = \"Paste a LinkedIn, Indeed, browser URL, or shared job text\"\n            setPadding(dp(14), dp(12), dp(14), dp(12))\n            background = roundedBox(COLOR_SURFACE, COLOR_BORDER)\n            setText(currentIntake?.rawSharedContent.orEmpty())\n        }\n        root.addView(\n            intakeEditor,\n            LinearLayout.LayoutParams(\n                ViewGroup.LayoutParams.MATCH_PARENT,\n                ViewGroup.LayoutParams.WRAP_CONTENT\n            )\n        )\n        root.addView(\n            helperText(\n                if (currentIntake == null) {\n                    \"Paste a job here to build the CareerOps request. Android shares populate this automatically.\"\n                } else {\n                    \"Loaded automatically from the share. Edit this field to replace or correct the intake.\"\n                }\n            )\n        )\n\n        val intakeInfoView = infoCard(intakeSummary(currentIntake))\n        root.addView(intakeInfoView, marginTop = dp(8))\n\n        root.addView(sectionLabel(\"Prepared request\"))",
    ),
    (
        "        root.addView(\n            payloadEditor,\n            LinearLayout.LayoutParams(\n                ViewGroup.LayoutParams.MATCH_PARENT,\n                ViewGroup.LayoutParams.WRAP_CONTENT\n            )\n        )\n\n        val destination = DestinationCatalog.fromId(route.destinationId)",
        "        root.addView(\n            payloadEditor,\n            LinearLayout.LayoutParams(\n                ViewGroup.LayoutParams.MATCH_PARENT,\n                ViewGroup.LayoutParams.WRAP_CONTENT\n            )\n        )\n\n        intakeEditor.addTextChangedListener(object : TextWatcher {\n            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit\n            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit\n\n            override fun afterTextChanged(s: Editable?) {\n                updateManualIntake(s?.toString().orEmpty(), intakeInfoView)\n            }\n        })\n\n        val destination = DestinationCatalog.fromId(route.destinationId)",
    ),
    (
        "    private fun renderCurrentPayload(): String {\n        val intake = currentIntake ?: return DEFAULT_PROMPT",
        "    private fun updateManualIntake(rawValue: String, intakeInfoView: TextView) {\n        val value = rawValue.trim()\n        currentIntake = if (value.isBlank()) null else ShareParser.parse(null, value)\n        intakeInfoView.text = intakeSummary(currentIntake)\n        payloadEditor?.setText(renderCurrentPayload())\n    }\n\n    private fun intakeSummary(intake: JobShareIntake?): String {\n        if (intake == null) return \"No job loaded yet.\"\n        val jobId = intake.jobId ?: \"Not detected\"\n        val url = intake.canonicalUrl ?: \"No URL detected\"\n        val truncation = if (intake.wasTruncated) \"\\nShared text was capped at 100,000 characters.\" else \"\"\n        return \"Source: ${intake.source}\\nJob ID: $jobId\\n$url$truncation\"\n    }\n\n    private fun renderCurrentPayload(): String {\n        val intake = currentIntake ?: return DEFAULT_PROMPT",
    ),
    (
        "            toast(\"Share a job first\")",
        "            toast(\"Add or share a job first\")",
    ),
    (
        "    private fun currentPayload(): String? {\n        val payload = payloadEditor?.text?.toString()?.trim().orEmpty()",
        "    private fun currentPayload(): String? {\n        if (currentIntake == null) {\n            toast(\"Add or share a job first\")\n            return null\n        }\n        val payload = payloadEditor?.text?.toString()?.trim().orEmpty()",
    ),
    (
        "        private const val DEFAULT_PROMPT = \"Analyze this job using CareerOps:\"",
        "        private const val DEFAULT_PROMPT = \"Paste or share a job above to build the CareerOps request.\"",
    ),
    (
        "        private const val STATE_SESSION_REQUEST_PROFILE = \"careerops.session.request_profile\"",
        "        private const val STATE_SESSION_REQUEST_PROFILE = \"careerops.session.request_profile\"\n        private const val STATE_INTAKE_TEXT = \"careerops.session.intake_text\"",
    ),
]

for old, new in replacements:
    if old not in text:
        raise SystemExit(f"Expected MainActivity block not found:\n{old[:180]}")
    text = text.replace(old, new, 1)

MAIN.write_text(text, encoding="utf-8")

test = TEST.read_text(encoding="utf-8")
anchor = '''    @Test\n    fun requestRendererPreservesCompatibilityPromptAndAction() {'''
new_test = '''    @Test\n    fun manualUrlInputBuildsStructuredCareerOpsRequest() {\n        val intake = ShareParser.parse(\n            null,\n            "https://www.linkedin.com/jobs/view/4457534369/?trackingId=manual"\n        )\n        val request = CareerOpsRequest(\n            action = CareerOpsAction.ANALYZE_BUILD_STORE,\n            job = intake\n        )\n\n        val rendered = CareerOpsRequestRenderer.toText(request)\n\n        assertEquals("LinkedIn", intake.source)\n        assertEquals("4457534369", intake.jobId)\n        assertEquals("https://www.linkedin.com/jobs/view/4457534369/", intake.canonicalUrl)\n        assertTrue(rendered.contains("schema_version: 1.0"))\n        assertTrue(rendered.contains("action: ANALYZE_BUILD_STORE"))\n    }\n\n    @Test\n    fun requestRendererPreservesCompatibilityPromptAndAction() {'''
if anchor not in test:
    raise SystemExit("ShareParserTest anchor not found")
test = test.replace(anchor, new_test, 1)
TEST.write_text(test, encoding="utf-8")
