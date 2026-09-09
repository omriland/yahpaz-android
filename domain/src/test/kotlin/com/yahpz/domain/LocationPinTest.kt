package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPinTest {
    @Test
    fun `locks human-corrected pins so Google must not move them`() {
        assertTrue(locationPinIsLocked("shift_lead"))
        assertTrue(locationPinIsLocked("responder"))
        assertTrue(locationPinIsLocked("junction"))
        assertFalse(locationPinIsLocked("places"))
        assertFalse(locationPinIsLocked("geocode"))
        assertFalse(locationPinIsLocked(null))
    }

    @Test
    fun `formats a copyable lat lng pair`() {
        assertEquals("32.07412, 34.79202", formatLocationCoords(32.0741234, 34.7920199))
    }

    @Test
    fun `lead map pin writes coords and locks without touching location text`() {
        assertEquals(
            LocationPinFields(
                location = "מחלף השלום",
                locationPlaceId = null,
                locationLat = 32.07,
                locationLng = 34.79,
                locationPinSource = "shift_lead",
                locationPinnedAt = "2026-08-24T07:00:00.000Z",
                locationPinnedBy = "lead-1",
            ),
            applyLeadMapPin(
                LocationPinFields(
                    location = "מחלף השלום",
                    locationPlaceId = "ChIJx",
                    locationLat = 32.1,
                    locationLng = 34.8,
                    locationPinSource = "places",
                ),
                lat = 32.07,
                lng = 34.79,
                userId = "lead-1",
                at = "2026-08-24T07:00:00.000Z",
            ),
        )
    }

    @Test
    fun `keeps a locked pin when the location text is edited`() {
        val locked = LocationPinFields(
            location = "מחלף",
            locationLat = 32.07,
            locationLng = 34.79,
            locationPinSource = "shift_lead",
            locationPinnedAt = "2026-08-24T07:00:00.000Z",
            locationPinnedBy = "lead-1",
        )
        assertEquals(
            locked.copy(location = "מחלף השלום צפון"),
            applyLocationFieldChange(
                locked,
                nextLocation = "מחלף השלום צפון",
                nextPlaceId = null,
                nextLat = null,
                nextLng = null,
            ),
        )
    }

    @Test
    fun `replaces a locked pin when the user picks a Google place`() {
        assertEquals(
            LocationPinFields(
                location = "צומת גלילות",
                locationPlaceId = "ChIJx",
                locationLat = 32.14,
                locationLng = 34.81,
                locationPinSource = "places",
            ),
            applyLocationFieldChange(
                LocationPinFields(
                    location = "מחלף",
                    locationLat = 32.07,
                    locationLng = 34.79,
                    locationPinSource = "shift_lead",
                    locationPinnedAt = "2026-08-24T07:00:00.000Z",
                    locationPinnedBy = "lead-1",
                ),
                nextLocation = "צומת גלילות",
                nextPlaceId = "ChIJx",
                nextLat = 32.14,
                nextLng = 34.81,
            ),
        )
    }

    @Test
    fun `locks and tags a junction pick distinctly from a Google place`() {
        assertEquals(
            LocationPinFields(
                location = "צומת מסובים",
                locationPlaceId = "junction:11111111-1111-1111-1111-111111111111",
                locationLat = 31.6,
                locationLng = 34.7,
                locationPinSource = "junction",
            ),
            applyLocationFieldChange(
                LocationPinFields(),
                nextLocation = "צומת מסובים",
                nextPlaceId = "junction:11111111-1111-1111-1111-111111111111",
                nextLat = 31.6,
                nextLng = 34.7,
            ),
        )
    }

    @Test
    fun `clearing a locked pin drops coords`() {
        assertEquals(
            LocationPinFields(location = "מחלף השלום"),
            clearLockedLocationPin(
                LocationPinFields(
                    location = "מחלף השלום",
                    locationLat = 32.07,
                    locationLng = 34.79,
                    locationPinSource = "shift_lead",
                    locationPinnedAt = "2026-08-24T07:00:00.000Z",
                    locationPinnedBy = "lead-1",
                ),
            ),
        )
    }

    @Test
    fun `payload stores a junction pin and drops the transient marker`() {
        assertEquals(
            LocationPinFields(
                location = "צומת מסובים",
                locationPlaceId = null,
                locationLat = 31.6,
                locationLng = 34.7,
                locationPinSource = "junction",
            ),
            buildLocationPayload(
                LocationPinFields(
                    location = "צומת מסובים",
                    locationPlaceId = "junction:11111111-1111-1111-1111-111111111111",
                    locationLat = 31.6,
                    locationLng = 34.7,
                    locationPinSource = "junction",
                ),
            ),
        )
    }

    @Test
    fun `payload stores place fields for a Google pick`() {
        assertEquals(
            LocationPinFields(
                location = "צומת גלילות",
                locationPlaceId = "ChIJx",
                locationLat = 32.1,
                locationLng = 34.8,
                locationPinSource = "places",
            ),
            buildLocationPayload(
                LocationPinFields(
                    location = "צומת גלילות",
                    locationPlaceId = "ChIJx",
                    locationLat = 32.1,
                    locationLng = 34.8,
                ),
            ),
        )
    }

    @Test
    fun `payload keeps a locked pin even if location text is empty`() {
        assertEquals(
            LocationPinFields(
                location = "",
                locationPlaceId = null,
                locationLat = 32.07,
                locationLng = 34.79,
                locationPinSource = "shift_lead",
                locationPinnedAt = "2026-08-24T07:00:00.000Z",
                locationPinnedBy = "lead-1",
            ),
            buildLocationPayload(
                LocationPinFields(
                    location = "  ",
                    locationLat = 32.07,
                    locationLng = 34.79,
                    locationPinSource = "shift_lead",
                    locationPinnedAt = "2026-08-24T07:00:00.000Z",
                    locationPinnedBy = "lead-1",
                ),
            ),
        )
    }

    @Test
    fun `payload clears place fields for free-text`() {
        assertEquals(
            LocationPinFields(location = "משהו לא רשמי"),
            buildLocationPayload(LocationPinFields(location = "משהו לא רשמי")),
        )
    }
}
