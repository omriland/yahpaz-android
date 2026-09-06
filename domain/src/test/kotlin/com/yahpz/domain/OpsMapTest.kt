package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpsMapTest {
    private fun pin(
        userId: String = "u1",
        callsign: String = "336",
        kind: AddressKind = AddressKind.HOME,
        lat: Double = 32.0,
        lng: Double = 34.8,
        status: VolunteerStatus = VolunteerStatus.ACTIVE_VOLUNTEER,
        availability: AvailabilityStatus = AvailabilityStatus.AVAILABLE,
        availableFrom: String? = null,
    ) = MapPin(
        userId = userId,
        fullName = "Test",
        callsign = callsign,
        kind = kind,
        name = addressKindLabel(kind),
        label = mapPinLabel(callsign, kind),
        formattedAddress = "Somewhere",
        lat = lat,
        lng = lng,
        volunteerStatus = status,
        availability = availability,
        availableFrom = availableFrom,
    )

    @Test
    fun mapPinsFromUnitRowsHidesAdminStatuses() {
        val rows = listOf(
            UnitMapPinRow(
                userId = "a",
                fullName = "A",
                callsign = "1",
                kind = AddressKind.HOME,
                label = null,
                formattedAddress = "x",
                lat = 32.0,
                lng = 34.8,
                volunteerStatus = VolunteerStatus.ADMINISTRATION,
                availability = AvailabilityStatus.AVAILABLE,
                availableFrom = null,
            ),
            UnitMapPinRow(
                userId = "b",
                fullName = "B",
                callsign = "2",
                kind = AddressKind.WORK,
                label = null,
                formattedAddress = "y",
                lat = 32.1,
                lng = 34.9,
                volunteerStatus = VolunteerStatus.PHONE_TRAINING,
                availability = AvailabilityStatus.AVAILABLE,
                availableFrom = null,
            ),
        )
        val pins = mapPinsFromUnitRows(rows)
        assertEquals(1, pins.size)
        assertEquals("b", pins[0].userId)
        assertEquals(MapUserPinTone.PHONE, mapUserPinChrome(pins[0]).tone)
    }

    @Test
    fun nearbyPicksClosestAddressPerUserWithin30km() {
        val pins = listOf(
            pin(userId = "u1", callsign = "A", lat = 32.0, lng = 34.8),
            pin(userId = "u1", callsign = "A", kind = AddressKind.WORK, lat = 32.05, lng = 34.85),
            pin(userId = "u2", callsign = "B", lat = 33.5, lng = 35.5),
        )
        val nearby = nearbyResponders(pins, 32.0, 34.8, maxKm = 30.0)
        assertEquals(1, nearby.size)
        assertEquals("u1", nearby[0].userId)
        assertEquals(AddressKind.HOME, nearby[0].kind)
    }

    @Test
    fun catalogClustersAtLowZoom() {
        val pins = listOf(
            pin(userId = "1", lat = 32.00, lng = 34.80),
            pin(userId = "2", lat = 32.001, lng = 34.801),
            pin(userId = "3", lat = 33.0, lng = 35.0),
        )
        val (clusters, points) = catalogViewForViewport(pins, ISRAEL_VIEW_BBOX, zoom = 8f)
        assertTrue(clusters.any { it.count >= 2 })
        assertTrue(points.size + clusters.sumOf { it.count } == pins.size)
    }

    @Test
    fun milePostVisibilityGates() {
        assertFalse(shouldShowMilePosts(true, 13f, 10))
        assertTrue(shouldShowMilePosts(true, 14f, 10))
        assertFalse(shouldShowMilePosts(true, 14f, 401))
        assertTrue(shouldShowMilePosts(true, 15f, 401))
        assertFalse(shouldShowMilePosts(false, 15f, 10))
    }

    @Test
    fun livePinFreshness() {
        val recorded = "2026-09-06T05:00:00Z"
        val at = java.time.Instant.parse(recorded).toEpochMilli()
        assertTrue(isLivePinFresh(recorded, at + LIVE_PIN_STALE_AFTER_MS))
        assertFalse(isLivePinFresh(recorded, at + LIVE_PIN_STALE_AFTER_MS + 1))
    }

    @Test
    fun unavailableChrome() {
        val chrome = mapUserPinChrome(
            pin(availability = AvailabilityStatus.UNAVAILABLE, availableFrom = "2026-09-10"),
            today = "2026-09-06",
        )
        assertTrue(chrome.unavailable)
        assertTrue(chrome.tooltip.contains("לא זמין"))
        assertNull(mapAvailabilityHoverLabel(AvailabilityStatus.AVAILABLE, null, "2026-09-06"))
    }

    @Test
    fun responderPinLabelAndZoomGate() {
        assertEquals("336 · עמרי", mapResponderPinLabel("336", "עמרי"))
        assertEquals("336", mapResponderPinLabel("336", "  "))
        assertFalse(shouldShowMapPinLabels(11.9f))
        assertTrue(shouldShowMapPinLabels(12f))
    }
}
