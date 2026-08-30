package com.careerops.share

data class CareerOpsRoutePlan(
    val preset: CareerOpsPreset,
    val request: CareerOpsRequest,
    val destination: DestinationProfile,
    val payload: String
)

object CareerOpsRoutePlanner {
    fun plan(
        intake: JobShareIntake,
        preset: CareerOpsPreset
    ): CareerOpsRoutePlan {
        val request = CareerOpsRequest(
            action = preset.action,
            job = intake
        )
        val destination = DestinationCatalog.fromId(preset.destinationId)
        val payload = when (preset.requestProfile) {
            RequestProfile.CAREEROPS_STANDARD -> CareerOpsRequestRenderer.toText(request)
            RequestProfile.CAREEROPS_JSON -> CareerOpsRequestRenderer.toJson(request)
        }

        return CareerOpsRoutePlan(
            preset = preset,
            request = request,
            destination = destination,
            payload = payload
        )
    }
}
