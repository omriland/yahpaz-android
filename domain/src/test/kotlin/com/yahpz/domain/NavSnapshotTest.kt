package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavSnapshotTest {
    private val tabs = setOf("INBOX", "SHIFTS", "UNIT_EVENTS", "TOOLS")
    private val destinations = setOf("HUB", "REPORT_CATALOG")

    private fun snapshot(
        tab: String = "UNIT_EVENTS",
        fillEventId: String? = "ev-1",
        fillShiftBorn: Boolean = false,
        liveShiftScreenId: String? = "shift-1",
        toolsDestination: String = "HUB",
        unitEventsQuery: String = "אבן יהודה",
    ) = NavSnapshot(tab, fillEventId, fillShiftBorn, liveShiftScreenId, toolsDestination, unitEventsQuery)

    private fun restore(
        map: Map<String, String>,
        currentShiftId: String? = "shift-1",
    ) = restoreNavSnapshot(map, tabs, destinations, currentShiftId)

    @Test
    fun roundTripsThroughABundleSafeMap() {
        val original = snapshot(fillShiftBorn = true)
        assertEquals(original, restore(original.toBundleMap()))
    }

    @Test
    fun roundTripsAnOrdinaryOpenFill() {
        val original = snapshot(fillEventId = "ev-1", fillShiftBorn = false)
        assertEquals(original, restore(original.toBundleMap()))
    }

    @Test
    fun roundTripsWithoutFillOrLiveShift() {
        val original = snapshot(fillEventId = null, liveShiftScreenId = null, unitEventsQuery = "")
        assertEquals(original, restore(original.toBundleMap()))
    }

    @Test
    fun dropsAFillIdThatHasNoKind() {
        val full = snapshot(fillShiftBorn = true).toBundleMap()
        assertTrue(full.containsKey(NAV_FILL_SHIFT_BORN_KEY))
        val map = full.filterKeys { it != NAV_FILL_SHIFT_BORN_KEY }
        val restored = restore(map)!!
        assertNull(restored.fillEventId)
        assertFalse(restored.fillShiftBorn)
    }

    @Test
    fun dropsAFillKindThatHasNoId() {
        val map = snapshot(fillShiftBorn = true).toBundleMap().filterKeys { it != NAV_FILL_EVENT_ID_KEY }
        val restored = restore(map)!!
        assertNull(restored.fillEventId)
        assertFalse(restored.fillShiftBorn)
    }

    @Test
    fun dropsAStaleLiveShiftWhenTheUserHasNoCurrentShift() {
        assertNull(restore(snapshot().toBundleMap(), currentShiftId = null)!!.liveShiftScreenId)
    }

    @Test
    fun dropsALiveShiftThatIsNotTheCurrentShift() {
        assertNull(restore(snapshot().toBundleMap(), currentShiftId = "shift-2")!!.liveShiftScreenId)
    }

    @Test
    fun keepsTheLiveShiftWhenItIsTheCurrentShift() {
        assertEquals("shift-1", restore(snapshot().toBundleMap())!!.liveShiftScreenId)
    }

    @Test
    fun fallsBackToInboxForAnUnknownTab() {
        assertEquals("INBOX", restore(snapshot(tab = "REMOVED_TAB").toBundleMap())!!.tab)
    }

    @Test
    fun fallsBackToHubForAnUnknownToolsDestination() {
        assertEquals("HUB", restore(snapshot(toolsDestination = "GONE").toBundleMap())!!.toolsDestination)
    }

    @Test
    fun anEmptyMapRestoresNothing() {
        assertNull(restore(emptyMap()))
    }

    @Test
    fun aMapWithoutATabRestoresNothing() {
        assertNull(restore(snapshot().toBundleMap().filterKeys { it != NAV_TAB_KEY }))
    }
}
