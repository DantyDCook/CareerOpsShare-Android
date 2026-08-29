package com.careerops.share

enum class CareerOpsAction(val id: String, val displayName: String) {
    ANALYZE("ANALYZE", "Analyze"),
    ANALYZE_BUILD_STORE("ANALYZE_BUILD_STORE", "Analyze + Build & Store"),
    ANALYZE_BUILD_STORE_COVER_LETTER(
        "ANALYZE_BUILD_STORE_COVER_LETTER",
        "Analyze + Build & Store + Cover Letter"
    );

    companion object {
        fun fromId(id: String?): CareerOpsAction =
            entries.firstOrNull { it.id == id } ?: ANALYZE
    }
}

enum class TransportType {
    ANDROID_APP,
    ANDROID_CHOOSER,
    HTTP_POST
}

data class DestinationProfile(
    val id: String,
    val displayName: String,
    val transportType: TransportType,
    val packageName: String? = null,
    val endpointUrl: String? = null,
    val enabled: Boolean = true
)

object DestinationCatalog {
    val CHATGPT = DestinationProfile(
        id = "chatgpt",
        displayName = "ChatGPT",
        transportType = TransportType.ANDROID_APP,
        packageName = "com.openai.chatgpt"
    )

    val SYSTEM_CHOOSER = DestinationProfile(
        id = "system_chooser",
        displayName = "Choose Android app…",
        transportType = TransportType.ANDROID_CHOOSER
    )

    // Contract placeholder only. Networking remains deliberately disabled in v0.2.0.
    val CAREEROPS_GATEWAY_FUTURE = DestinationProfile(
        id = "careerops_gateway",
        displayName = "CareerOps Gateway (future)",
        transportType = TransportType.HTTP_POST,
        enabled = false
    )

    fun localDestinations(): List<DestinationProfile> =
        listOf(CHATGPT, SYSTEM_CHOOSER)

    fun fromId(id: String?): DestinationProfile =
        localDestinations().firstOrNull { it.id == id } ?: CHATGPT
}

data class JobShareIntake(
    val source: String,
    val sourceId: String,
    val jobId: String?,
    val originalUrl: String?,
    val canonicalUrl: String?,
    val subject: String?,
    val rawSharedContent: String,
    val wasTruncated: Boolean
)

data class CareerOpsRequest(
    val schemaVersion: String = "1.0",
    val action: CareerOpsAction,
    val job: JobShareIntake
)
