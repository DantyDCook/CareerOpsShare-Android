package com.careerops.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareerOpsRoutePlannerTest {

    private val intake = JobShareIntake(
        source = "LinkedIn",
        sourceId = "linkedin",
        jobId = "4459362657",
        originalUrl = "https://www.linkedin.com/jobs/view/4459362657/?trackingId=test",
        canonicalUrl = "https://www.linkedin.com/jobs/view/4459362657/",
        subject = "Data Engineer",
        rawSharedContent = "https://www.linkedin.com/jobs/view/4459362657/",
        wasTruncated = false
    )

    @Test
    fun planUsesPresetActionAndDestination() {
        val preset = PresetCatalog.BUILD_STORE.copy(
            destinationId = DestinationCatalog.SYSTEM_CHOOSER.id,
            modelPreference = ModelPreference.FRONTIER
        )

        val plan = CareerOpsRoutePlanner.plan(intake, preset)

        assertEquals(CareerOpsAction.ANALYZE_BUILD_STORE, plan.request.action)
        assertEquals(DestinationCatalog.SYSTEM_CHOOSER.id, plan.destination.id)
        assertEquals(ModelPreference.FRONTIER, plan.preset.modelPreference)
        assertTrue(plan.payload.contains("action: ANALYZE_BUILD_STORE"))
    }

    @Test
    fun jsonRequestProfileUsesStructuredRenderer() {
        val preset = PresetCatalog.QUICK_ANALYZE.copy(
            requestProfile = RequestProfile.CAREEROPS_JSON
        )

        val plan = CareerOpsRoutePlanner.plan(intake, preset)

        assertTrue(plan.payload.startsWith("{"))
        assertTrue(plan.payload.contains("\"action\": \"ANALYZE\""))
        assertTrue(plan.payload.contains("\"canonical_url\""))
    }
}
