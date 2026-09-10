package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventFormFieldNotesTest {
    @Test
    fun storesApprovedEventTimesCallsignAndLocationCopy() {
        assertEquals(
            FieldNoteCopy(note = EVENT_TIMES_FIELD_NOTE, tooltip = EVENT_TIMES_FIELD_TOOLTIP),
            eventFormFieldNote(EventFormFieldNoteId.EVENT_TIMES),
        )
        assertEquals(
            "שימו לב! מעתה הזנת זמנים תהיה עבור האירוע כולו ולא לכל מתנדב בנפרד",
            EVENT_TIMES_FIELD_NOTE,
        )
        assertEquals(
            "זמן ההתחלה יהיה זמן היציאה של המתנדב הראשון וזמן הסיום יהיה זמן העזיבה של המתנדב האחרון",
            EVENT_TIMES_FIELD_TOOLTIP,
        )

        assertEquals(
            FieldNoteCopy(note = PATROL_CALLSIGN_FIELD_NOTE),
            eventFormFieldNote(EventFormFieldNoteId.PATROL_CALLSIGN),
        )
        assertEquals(
            "אתם מתבקשים להזין או\"ק מלא של הניידת כולל קידומת (אביב, חוף וכו')",
            PATROL_CALLSIGN_FIELD_NOTE,
        )
        assertNull(eventFormFieldNote(EventFormFieldNoteId.PATROL_CALLSIGN)?.tooltip)

        assertEquals(
            FieldNoteCopy(note = LOCATION_FIELD_NOTE, tooltip = LOCATION_FIELD_TOOLTIP),
            eventFormFieldNote(EventFormFieldNoteId.LOCATION),
        )
        assertEquals("חדש! הזנת כביש באופן אוטומטי מבוסס על המיקום הנבחר", LOCATION_FIELD_NOTE)
        assertEquals(
            "מיקמנו את שדה 'מיקום' ראשון כדי להקל עליכם והטמענו הזנה אוטומטית של מספר הכביש. במקרה של כביש וק\"מ או מיקום שאינו נמצא, תוכלו עדין להזין מספר כביש באופן ידני",
            LOCATION_FIELD_TOOLTIP,
        )
    }

    @Test
    fun leavesPerHalfAndUnusedIdsEmpty() {
        assertNull(eventFormFieldNote(EventFormFieldNoteId.POLICE_EVENT_ID))
        assertNull(eventFormFieldNote(EventFormFieldNoteId.PATROL_CALLSIGN_PREFIX))
        assertNull(eventFormFieldNote(EventFormFieldNoteId.PATROL_CALLSIGN_NUMBER))
        assertNull(eventFormFieldNote(EventFormFieldNoteId.STARTED_AT))
        assertNull(eventFormFieldNote(EventFormFieldNoteId.ENDED_AT))
        assertEquals(
            setOf(
                EventFormFieldNoteId.EVENT_TIMES,
                EventFormFieldNoteId.PATROL_CALLSIGN,
                EventFormFieldNoteId.LOCATION,
            ),
            EVENT_FORM_FIELD_NOTES.keys,
        )
    }
}
