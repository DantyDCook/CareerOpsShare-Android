package com.careerops.share

import java.net.URI

data class SharedJobPayload(
    val source: String,
    val url: String?,
    val payload: String,
    val wasTruncated: Boolean
)

object ShareParser {
    private const val DEFAULT_PROMPT = "Analyze this job using CareerOps:"
    private const val MAX_SHARED_TEXT_CHARS = 100_000
    private val urlRegex = Regex("https?://[^\\s<>\\\"']+")

    fun parse(subject: String?, sharedText: String?): SharedJobPayload {
        val parts = listOf(subject.orEmpty().trim(), sharedText.orEmpty().trim())
            .filter { it.isNotBlank() }
            .distinct()
        val rawCombined = parts.joinToString("\n")
        val wasTruncated = rawCombined.length > MAX_SHARED_TEXT_CHARS
        val combined = if (wasTruncated) rawCombined.take(MAX_SHARED_TEXT_CHARS) else rawCombined
        val url = findFirstUrl(combined)
        val source = classifySource(url)
        val payload = if (combined.isBlank()) DEFAULT_PROMPT else "$DEFAULT_PROMPT\n\n$combined"
        return SharedJobPayload(source, url, payload, wasTruncated)
    }

    fun findFirstUrl(text: String): String? =
        urlRegex.find(text)?.value?.trimEnd('.', ',', ';', ':', ')', ']', '}')

    fun classifySource(url: String?): String {
        if (url.isNullOrBlank()) return "Shared text"
        val host = runCatching { URI(url).host.orEmpty().lowercase() }.getOrDefault("")
        fun isDomain(domain: String) = host == domain || host.endsWith(".$domain")
        return when {
            isDomain("linkedin.com") -> "LinkedIn"
            isDomain("indeed.com") -> "Indeed"
            isDomain("glassdoor.com") -> "Glassdoor"
            isDomain("ziprecruiter.com") -> "ZipRecruiter"
            isDomain("monster.com") -> "Monster"
            host.isNotBlank() -> host.removePrefix("www.")
            else -> "Shared URL"
        }
    }
}
