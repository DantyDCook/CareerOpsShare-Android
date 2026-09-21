package com.careerops.share

data class DirectShareSlots(
    val firstProfileId: String? = null,
    val secondProfileId: String? = null
) {
    fun activeProfileIds(): List<String> =
        listOfNotNull(firstProfileId, secondProfileId).distinct()

    fun normalized(): DirectShareSlots {
        val first = PresetCatalog.fromIdOrNull(firstProfileId)?.id
        val second = PresetCatalog.fromIdOrNull(secondProfileId)?.id
            ?.takeUnless { it == first }
        return DirectShareSlots(first, second)
    }
}
