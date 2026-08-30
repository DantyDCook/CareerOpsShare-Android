package com.careerops.share

data class RegularShareDefaults(
    val action: CareerOpsAction = CareerOpsAction.ANALYZE,
    val destinationId: String = DestinationCatalog.CHATGPT.id,
    val modelPreference: ModelPreference = ModelPreference.AUTO,
    val requestProfile: RequestProfile = RequestProfile.CAREEROPS_STANDARD
) {
    fun asSessionPreset(): CareerOpsPreset = CareerOpsPreset(
        id = SESSION_PRESET_ID,
        name = "Regular Share",
        action = action,
        destinationId = destinationId,
        modelPreference = modelPreference,
        requestProfile = requestProfile,
        autoForward = false,
        showInDirectShare = false
    )

    companion object {
        const val SESSION_PRESET_ID = "regular-share-session"
    }
}
