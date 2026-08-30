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

enum class ModelPreference(val id: String, val displayName: String) {
    AUTO("AUTO", "Auto / destination default"),
    FRONTIER("FRONTIER", "Frontier / highest capability"),
    FAST("FAST", "Fast / low latency"),
    ECONOMY("ECONOMY", "Economy / low cost");

    companion object {
        fun fromId(id: String?): ModelPreference =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

enum class RequestProfile(val id: String, val displayName: String) {
    CAREEROPS_STANDARD("careerops_standard", "CareerOps Standard"),
    CAREEROPS_JSON("careerops_json", "CareerOps JSON");

    companion object {
        fun fromId(id: String?): RequestProfile =
            entries.firstOrNull { it.id == id } ?: CAREEROPS_STANDARD
    }
}

enum class TransportType {
    ANDROID_APP,
    ANDROID_CHOOSER,
    HTTP_POST
}

enum class ModelRoutingCapability {
    DESTINATION_DEFAULT,
    ENFORCED
}

data class DestinationProfile(
    val id: String,
    val displayName: String,
    val transportType: TransportType,
    val packageName: String? = null,
    val endpointUrl: String? = null,
    val enabled: Boolean = true,
    val modelRoutingCapability: ModelRoutingCapability = ModelRoutingCapability.DESTINATION_DEFAULT
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

    // Contract placeholder only. Networking remains deliberately disabled in v0.3.0.
    val CAREEROPS_GATEWAY_FUTURE = DestinationProfile(
        id = "careerops_gateway",
        displayName = "CareerOps Gateway (future)",
        transportType = TransportType.HTTP_POST,
        enabled = false,
        modelRoutingCapability = ModelRoutingCapability.ENFORCED
    )

    fun localDestinations(): List<DestinationProfile> =
        listOf(CHATGPT, SYSTEM_CHOOSER)

    fun fromId(id: String?): DestinationProfile =
        localDestinations().firstOrNull { it.id == id } ?: CHATGPT
}

data class CareerOpsPreset(
    val id: String,
    val name: String,
    val action: CareerOpsAction,
    val destinationId: String,
    val modelPreference: ModelPreference = ModelPreference.AUTO,
    val requestProfile: RequestProfile = RequestProfile.CAREEROPS_STANDARD,
    val autoForward: Boolean = true,
    val showInDirectShare: Boolean = true
)

object PresetCatalog {
    val QUICK_ANALYZE = CareerOpsPreset(
        id = "quick-analyze",
        name = "Quick Analyze",
        action = CareerOpsAction.ANALYZE,
        destinationId = DestinationCatalog.CHATGPT.id
    )

    val BUILD_STORE = CareerOpsPreset(
        id = "build-store",
        name = "Build & Store",
        action = CareerOpsAction.ANALYZE_BUILD_STORE,
        destinationId = DestinationCatalog.CHATGPT.id
    )

    val FULL_APPLICATION = CareerOpsPreset(
        id = "full-application",
        name = "Full Application",
        action = CareerOpsAction.ANALYZE_BUILD_STORE_COVER_LETTER,
        destinationId = DestinationCatalog.CHATGPT.id
    )

    val builtIns: List<CareerOpsPreset> =
        listOf(QUICK_ANALYZE, BUILD_STORE, FULL_APPLICATION)

    fun fromIdOrNull(id: String?): CareerOpsPreset? =
        builtIns.firstOrNull { it.id == id }

    fun fromId(id: String?): CareerOpsPreset =
        fromIdOrNull(id) ?: QUICK_ANALYZE
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
