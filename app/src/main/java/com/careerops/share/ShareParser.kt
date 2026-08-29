package com.careerops.share

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ShareParser {
    private const val MAX_SHARED_TEXT_CHARS = 100_000
    private val urlRegex = Regex("""https?://[^\s<>\"']+""")
    private val linkedInIdRegex = Regex("""(\d{6,})/?$""")

    fun parse(subject: String?, sharedText: String?): JobShareIntake {
        val cleanSubject = subject?.trim()?.takeIf { it.isNotBlank() }
        val parts = listOf(cleanSubject.orEmpty(), sharedText.orEmpty().trim())
            .filter { it.isNotBlank() }
            .distinct()

        val rawCombined = parts.joinToString("\n")
        val wasTruncated = rawCombined.length > MAX_SHARED_TEXT_CHARS
        val combined = if (wasTruncated) rawCombined.take(MAX_SHARED_TEXT_CHARS) else rawCombined

        val originalUrl = findFirstUrl(combined)
        val source = classifySource(originalUrl)
        val sourceId = sourceId(source)
        val jobId = extractJobId(sourceId, originalUrl)
        val canonicalUrl = canonicalizeUrl(sourceId, originalUrl, jobId)

        return JobShareIntake(
            source = source,
            sourceId = sourceId,
            jobId = jobId,
            originalUrl = originalUrl,
            canonicalUrl = canonicalUrl,
            subject = cleanSubject,
            rawSharedContent = combined,
            wasTruncated = wasTruncated
        )
    }

    fun findFirstUrl(text: String): String? =
        urlRegex.find(text)?.value?.trimEnd('.', ',', ';', ':', ')', ']', '}')

    fun classifySource(url: String?): String {
        if (url.isNullOrBlank()) return "Shared text"
        val host = hostFor(url)

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

    fun canonicalizeUrl(sourceId: String, url: String?, jobId: String?): String? {
        if (url.isNullOrBlank()) return null

        if (sourceId == "linkedin" && !jobId.isNullOrBlank()) {
            return "https://www.linkedin.com/jobs/view/$jobId/"
        }

        if (sourceId == "indeed" && !jobId.isNullOrBlank()) {
            return "https://www.indeed.com/viewjob?jk=$jobId"
        }

        return stripTrackingParameters(url, sourceId)
    }

    fun extractJobId(sourceId: String, url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null

        return when (sourceId) {
            "linkedin" -> {
                val path = uri.path.orEmpty().trimEnd('/')
                if (!path.contains("/jobs/view/")) null
                else linkedInIdRegex.find(path)?.groupValues?.getOrNull(1)
            }

            "indeed" -> queryParameters(uri.rawQuery)["jk"]?.firstOrNull()

            else -> null
        }
    }

    private fun sourceId(source: String): String =
        when (source) {
            "LinkedIn" -> "linkedin"
            "Indeed" -> "indeed"
            "Glassdoor" -> "glassdoor"
            "ZipRecruiter" -> "ziprecruiter"
            "Monster" -> "monster"
            "Shared text" -> "shared_text"
            "Shared URL" -> "shared_url"
            else -> source.lowercase()
        }

    private fun stripTrackingParameters(url: String, sourceId: String): String {
        val uri = runCatching { URI(url) }.getOrNull() ?: return url
        val rawQuery = uri.rawQuery ?: return stripFragment(url)

        val kept = rawQuery.split("&")
            .filter { it.isNotBlank() }
            .filterNot { parameter ->
                val key = parameter.substringBefore("=")
                isTrackingParameter(sourceId, decode(key))
            }

        return buildString {
            if (!uri.scheme.isNullOrBlank()) {
                append(uri.scheme)
                append("://")
            }
            append(uri.rawAuthority.orEmpty())
            append(uri.rawPath.orEmpty())
            if (kept.isNotEmpty()) {
                append("?")
                append(kept.joinToString("&"))
            }
        }
    }

    private fun stripFragment(url: String): String =
        url.substringBefore("#")

    private fun isTrackingParameter(sourceId: String, key: String): Boolean {
        val normalized = key.lowercase()
        if (normalized.startsWith("utm_")) return true

        return when (sourceId) {
            "linkedin" -> normalized in setOf(
                "trackingid", "refid", "trk", "lipi", "midtoken",
                "mid", "epp", "recommendedflavor"
            )
            "indeed" -> normalized in setOf(
                "from", "fromage", "vjs", "advn", "aceid"
            )
            else -> normalized in setOf("gclid", "fbclid")
        }
    }

    private fun queryParameters(rawQuery: String?): Map<String, List<String>> {
        if (rawQuery.isNullOrBlank()) return emptyMap()

        return rawQuery.split("&")
            .filter { it.isNotBlank() }
            .map { part ->
                val key = decode(part.substringBefore("="))
                val value = decode(part.substringAfter("=", ""))
                key to value
            }
            .groupBy({ it.first }, { it.second })
    }

    private fun decode(value: String): String =
        runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }.getOrDefault(value)

    private fun hostFor(url: String): String =
        runCatching { URI(url).host.orEmpty().lowercase() }.getOrDefault("")
}
