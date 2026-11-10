package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors web src/lib/eventForm.responderPicker.test.ts. */
class ResponderPickerTest {
    private fun person(
        id: String,
        hasVehicle: Boolean = true,
        volunteerStatus: String? = "active_volunteer",
    ) = AssignableProfile(id, id, id, hasVehicle = hasVehicle, volunteerStatus = volunteerStatus)

    private val driver = person("driver")
    private val phoneTrainee = person("phone", volunteerStatus = "phone_training")
    private val carTrainee = person("car", volunteerStatus = "personal_vehicle_training")
    private val noCar = person("nocar", hasVehicle = false)
    private val shiftsOnly = person("shifts", volunteerStatus = "shifts_only")
    private val basic = person("basic", volunteerStatus = "basic_training")
    private val admin = person("admin", volunteerStatus = "administration")
    private val roster = listOf(driver, phoneTrainee, carTrainee, noCar, shiftsOnly, basic, admin)

    private fun ids(people: List<AssignableProfile>) = people.map { it.id }
    private fun assigned(id: String, hasVehicle: Boolean = false) =
        EventResponderDraft(responderId = id, hasVehicle = hasVehicle)

    @Test
    fun `needs an active car and one of the three statuses that come out`() {
        assertTrue(canBeVehicleResponder(driver))
        assertTrue(canBeVehicleResponder(phoneTrainee))
        assertTrue(canBeVehicleResponder(carTrainee))
        assertFalse(canBeVehicleResponder(noCar))
        assertFalse(canBeVehicleResponder(shiftsOnly))
        assertFalse(canBeVehicleResponder(basic))
        assertFalse(canBeVehicleResponder(admin))
    }

    @Test
    fun `refuses a person whose status was not loaded`() {
        assertFalse(canBeVehicleResponder(person("x", volunteerStatus = null)))
    }

    @Test
    fun `offers only car-holders who come out for the first responder`() {
        assertTrue(pickerNeedsVehicleResponder(emptyList(), roster))
        assertEquals(listOf("driver", "phone", "car"), ids(pickerCandidates(emptyList(), roster)))
    }

    @Test
    fun `offers everyone not yet assigned once a car-holder is on the event`() {
        val responders = listOf(assigned("driver", true))
        assertFalse(pickerNeedsVehicleResponder(responders, roster))
        assertEquals(
            listOf("phone", "car", "nocar", "shifts", "basic", "admin"),
            ids(pickerCandidates(responders, roster)),
        )
    }

    @Test
    fun `goes back to car-holders when the car-holder is removed and only riders remain`() {
        val responders = listOf(assigned("nocar"), assigned("shifts", true))
        assertTrue(pickerNeedsVehicleResponder(responders, roster))
        assertEquals(listOf("driver", "phone", "car"), ids(pickerCandidates(responders, roster)))
    }

    @Test
    fun `stays open to everyone when a later responder is the car-holder`() {
        val responders = listOf(assigned("nocar"), assigned("phone", true))
        assertFalse(pickerNeedsVehicleResponder(responders, roster))
        assertTrue("basic" in ids(pickerCandidates(responders, roster)))
    }

    @Test
    fun `judges a responder no longer on the roster by the vehicle flag on the draft`() {
        assertFalse(pickerNeedsVehicleResponder(listOf(assigned("gone", true)), roster))
        assertTrue(pickerNeedsVehicleResponder(listOf(assigned("gone", false)), roster))
    }

    @Test
    fun `opens an old event without a car-holder on car-holders only`() {
        val responders = listOf(assigned("nocar"), assigned("basic", true))
        assertEquals(listOf("driver", "phone", "car"), ids(pickerCandidates(responders, roster)))
    }

    @Test
    fun `copy matches the web`() {
        assertEquals(
            "צריך קודם מתנדב עם רכב, ולכן מוצגים כרגע רק מתנדבים עם רכב. אחריו אפשר לשבץ כל מתנדב.",
            PICKER_NEEDS_VEHICLE_NOTE,
        )
        assertEquals("אין מתנדבים עם רכב זמינים להקצאה.", PICKER_NO_VEHICLE_CANDIDATES)
    }
}
