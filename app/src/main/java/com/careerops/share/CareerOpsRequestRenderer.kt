package com.careerops.share

object CareerOpsRequestRenderer {
    private const val COMPATIBILITY_PROMPT = "Analyze this job using CareerOps:"

    fun toText(request: CareerOpsRequest): String = buildString {
        appendLine(COMPATIBILITY_PROMPT)
        appendLine()
        appendLine("CAREEROPS_REQUEST")
        appendLine("schema_version: ${request.schemaVersion}")
        appendLine("action: ${request.action.id}")
        appendLine("source: ${request.job.source}")
        appendLine("source_id: ${request.job.sourceId}")
        appendLine("job_id: ${request.job.jobId ?: "unknown"}")
        appendLine("canonical_url: ${request.job.canonicalUrl ?: "none"}")
        appendLine()
        appendLine("Shared content:")
        if (request.job.rawSharedContent.isNotBlank()) {
            append(request.job.rawSharedContent)
        } else if (!request.job.canonicalUrl.isNullOrBlank()) {
            append(request.job.canonicalUrl)
        }
    }.trimEnd()

    fun toJson(request: CareerOpsRequest): String = buildString {
        append("{\n")
        append("  \"schema_version\": \"${escape(request.schemaVersion)}\",\n")
        append("  \"action\": \"${escape(request.action.id)}\",\n")
        append("  \"job\": {\n")
        append("    \"source\": \"${escape(request.job.source)}\",\n")
        append("    \"source_id\": \"${escape(request.job.sourceId)}\",\n")
        appendNullable("job_id", request.job.jobId, true, "    ")
        appendNullable("original_url", request.job.originalUrl, true, "    ")
        appendNullable("canonical_url", request.job.canonicalUrl, true, "    ")
        appendNullable("subject", request.job.subject, true, "    ")
        append("    \"was_truncated\": ${request.job.wasTruncated},\n")
        append("    \"raw_shared_content\": \"${escape(request.job.rawSharedContent)}\"\n")
        append("  }\n")
        append("}")
    }

    private fun StringBuilder.appendNullable(
        key: String,
        value: String?,
        trailingComma: Boolean,
        indent: String
    ) {
        append(indent)
        append("\"")
        append(key)
        append("\": ")
        if (value == null) append("null") else append("\"${escape(value)}\"")
        if (trailingComma) append(",")
        append("\n")
    }

    private fun escape(value: String): String = buildString(value.length + 16) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
    }
}
