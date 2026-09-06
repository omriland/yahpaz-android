package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileNavTest {
    private fun views(list: List<MobileNavEntry>) = list.map { it.view }

    @Test
    fun `responder set of three stays in the tab bar with no overflow`() {
        val split = splitMobileNav(
            listOf(
                MobileNavEntry("mine", "האירועים שלי"),
                MobileNavEntry("my_shifts", "המשמרות שלי"),
                MobileNavEntry("contacts", "אנשי קשר"),
            ),
        )
        assertEquals(listOf("mine", "my_shifts", "contacts"), views(split.tabs))
        assertTrue(split.more.isEmpty())
    }

    @Test
    fun `shift-lead daily work stays in the bar and the rest goes behind עוד`() {
        val split = splitMobileNav(
            listOf(
                MobileNavEntry("contacts", "אנשי קשר"),
                MobileNavEntry("reports", "דוחות"),
                MobileNavEntry("shifts", "משמרות"),
                MobileNavEntry("events", "אירועים"),
                MobileNavEntry("my_shifts", "המשמרות שלי"),
                MobileNavEntry("mine", "האירועים שלי"),
            ),
        )
        assertEquals(listOf("events", "mine", "my_shifts"), views(split.tabs))
        assertEquals(listOf("shifts", "contacts", "reports"), views(split.more))
    }

    @Test
    fun `admin keeps ניהול in the bar and demotes personal shifts`() {
        val split = splitMobileNav(
            listOf(
                MobileNavEntry("mine", "האירועים שלי"),
                MobileNavEntry("my_shifts", "המשמרות שלי"),
                MobileNavEntry("contacts", "אנשי קשר"),
                MobileNavEntry("events", "אירועים"),
                MobileNavEntry("shifts", "משמרות"),
                MobileNavEntry("users", "ניהול"),
            ),
        )
        assertEquals(listOf("events", "mine", "users"), views(split.tabs))
        assertEquals(listOf("my_shifts", "shifts", "contacts"), views(split.more))
    }

    @Test
    fun `mobileNavEntries uses web labels including map and skips cockpit`() {
        val lead = mobileNavEntries(listOf("shift_lead", "responder"))
        assertEquals(
            listOf("mine", "my_shifts", "contacts", "map", "events", "shifts", "reports", "profile"),
            lead.map { it.view },
        )
        assertEquals("אירועים", lead.first { it.view == "events" }.label)
        assertEquals("מפה", lead.first { it.view == "map" }.label)
        assertTrue(lead.none { it.view == "cockpit" })

        val admin = mobileNavEntries(listOf("admin"))
        assertEquals("ניהול", admin.first { it.view == "users" }.label)
        assertTrue(admin.none { it.view == "reports" })
        assertTrue(admin.any { it.view == "map" })
    }

    @Test
    fun `map sits after contacts in secondary overflow for leads`() {
        val split = splitMobileNav(
            listOf(
                MobileNavEntry("contacts", "אנשי קשר"),
                MobileNavEntry("map", "מפה"),
                MobileNavEntry("reports", "דוחות"),
                MobileNavEntry("shifts", "משמרות"),
                MobileNavEntry("events", "אירועים"),
                MobileNavEntry("my_shifts", "המשמרות שלי"),
                MobileNavEntry("mine", "האירועים שלי"),
            ),
        )
        assertEquals(listOf("events", "mine", "my_shifts"), views(split.tabs))
        assertEquals(listOf("shifts", "contacts", "map", "reports"), views(split.more))
    }

    @Test
    fun `leads land on unit events, responders on mine`() {
        assertEquals("events", defaultMobileView(listOf("shift_lead")))
        assertEquals("events", defaultMobileView(listOf("admin")))
        assertEquals("mine", defaultMobileView(listOf("responder")))
    }

    @Test
    fun `overflow label is the web עוד`() {
        assertEquals("עוד", MOBILE_MORE_LABEL)
    }
}
