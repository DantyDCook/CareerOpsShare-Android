package com.careerops.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectShareSlotsTest {

    @Test
    fun duplicateProfileIsKeptOnlyInFirstSlot() {
        val slots = DirectShareSlots(
            firstProfileId = PresetCatalog.QUICK_ANALYZE.id,
            secondProfileId = PresetCatalog.QUICK_ANALYZE.id
        ).normalized()

        assertEquals(PresetCatalog.QUICK_ANALYZE.id, slots.firstProfileId)
        assertNull(slots.secondProfileId)
    }

    @Test
    fun unknownProfilesNormalizeToNone() {
        val slots = DirectShareSlots(
            firstProfileId = "missing-profile",
            secondProfileId = PresetCatalog.BUILD_STORE.id
        ).normalized()

        assertNull(slots.firstProfileId)
        assertEquals(PresetCatalog.BUILD_STORE.id, slots.secondProfileId)
    }

    @Test
    fun activeProfilesFollowSlotOrder() {
        val slots = DirectShareSlots(
            firstProfileId = PresetCatalog.FULL_APPLICATION.id,
            secondProfileId = PresetCatalog.QUICK_ANALYZE.id
        )

        assertEquals(
            listOf(PresetCatalog.FULL_APPLICATION.id, PresetCatalog.QUICK_ANALYZE.id),
            slots.activeProfileIds()
        )
    }
}
