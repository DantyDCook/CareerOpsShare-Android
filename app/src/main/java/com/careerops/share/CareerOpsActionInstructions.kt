package com.careerops.share

/**
 * Conversation-facing fulfillment guidance for build actions.
 *
 * The Engine owns the authoritative action/workflow contract. These instructions
 * make the requested deliverables explicit when the Share App is used as a
 * transport-only client with a conversational destination such as ChatGPT.
 * They do not imply that the Android client generates or stores documents.
 */
object CareerOpsActionInstructions {
    fun forAction(action: CareerOpsAction): String? = when (action) {
        CareerOpsAction.ANALYZE -> null
        CareerOpsAction.ANALYZE_BUILD_STORE,
        CareerOpsAction.ANALYZE_BUILD_STORE_COVER_LETTER -> buildString {
            appendLine("Execute the requested CareerOps application-artifact workflow, not just a cover-letter draft.")
            appendLine("- Retrieve the job posting, perform the fit analysis, and select only validated CareerOps evidence.")
            appendLine("- Build a job-tailored, ATS-readable resume and render BOTH DOCX and PDF.")
            if (action == CareerOpsAction.ANALYZE_BUILD_STORE_COVER_LETTER) {
                appendLine("- Build a matching, evidence-grounded cover letter and render BOTH DOCX and PDF.")
            }
            appendLine("- Review the requested documents for factual accuracy, job relevance, and rendering defects.")
            appendLine("- Store and verify the outputs in the configured CareerOps Workspace and artifact store (e.g., Google Drive), where accessible and authorized.")
            appendLine("- Return a fulfillment report with the fit result, document status, and real artifact locations or links.")
            appendLine("Mark the package PARTIAL if any required document, format, or persistence verification is missing; identify each blocker. Never claim a file was created or stored without verification.")
            append("Do not submit the application or change an external application account without separate explicit authorization.")
        }.trim()
    }
}
