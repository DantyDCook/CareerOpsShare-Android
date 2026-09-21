package com.careerops.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RegularShareDefaultsTest {

    @Test
    fun regularDefaultsCreateIndependentReviewRoute() {
        val defaults = RegularShareDefaults(
            action = CareerOpsAction.ANALYZE_BUILD_STORE,
            destinationId = DestinationCatalog.SYSTEM_CHOOSER.id,
            modelPreference = ModelPreference.FAST,
            requestProfile = RequestProfile.CAREEROPS_JSON
        )

        val route = defaults.asSessionPreset()

        assertEquals(CareerOpsAction.ANALYZE_BUILD_STORE, route.action)
        assertEquals(DestinationCatalog.SYSTEM_CHOOSER.id, route.destinationId)
        assertEquals(ModelPreference.FAST, route.modelPreference)
        assertEquals(RequestProfile.CAREEROPS_JSON, route.requestProfile)
        assertFalse(route.autoForward)
        assertFalse(route.showInDirectShare)
    }
}
