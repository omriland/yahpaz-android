package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors web responderFill.test.ts "validateResponderFillDraft (responder without a car)". */
class FillWithoutVehicleTest {
    private val detail = "טיפול ברכב תקוע בנתיב הימני, הוצב משולש וכוון התנועה עד לגרירה."

    @Test
    fun `completes without a plate or odometer when no car is linked`() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(route = "כביש 1", treatmentDetail = detail),
            FillMode.COMPLETE,
            emptyList(),
            null,
        )
        assertTrue(errors.isEmpty)
    }

    @Test
    fun `still asks for the route and the treatment detail`() {
        val errors = validateResponderFillDraft(ResponderFillDraft(), FillMode.COMPLETE, emptyList(), null)
        assertNull(errors.vehiclePlate)
        assertNull(errors.odometerStart)
        assertNull(errors.odometerEnd)
        assertEquals("יש למלא נתיב נסיעה.", errors.route)
        assertNotNull(errors.treatmentDetail)
    }

    @Test
    fun `still rejects a reversed odometer pair that was typed anyway`() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(odometerStart = "120", odometerEnd = "100", route = "כביש 1", treatmentDetail = detail),
            FillMode.COMPLETE,
            emptyList(),
            null,
        )
        assertEquals(ODOMETER_ORDER_ERROR, errors.odometerEnd)
    }

    @Test
    fun `keeps plate and odometer required for a responder who has a car`() {
        val errors = validateResponderFillDraft(
            ResponderFillDraft(route = "כביש 1", treatmentDetail = detail),
            FillMode.COMPLETE,
            listOf("1234567"),
            null,
        )
        assertEquals("יש לבחור רכב.", errors.vehiclePlate)
        assertEquals("יש למלא מד אוץ התחלה.", errors.odometerStart)
        assertEquals("יש למלא מד אוץ סיום.", errors.odometerEnd)
    }

    @Test
    fun `hint copy matches the web`() {
        assertEquals(
            "לא מקושר אליך רכב, לכן אין צורך בלוחית ובמד אוץ. הגעת ברכב שלך? פנו למנהל המערכת.",
            NO_LINKED_VEHICLE_FILL_HINT,
        )
    }
}
