package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsTest {
    private val dana = ContactSearchFields("דנה כהן", "12", "dana@yahpz.com", "050-123-4567")
    private val ofer = ContactSearchFields("אופר לוי", "7", "ofer@yahpz.com", null)

    @Test
    fun `phone formatting and hrefs match the web`() {
        assertEquals("050-1234567", formatPhone("050-123-4567"))
        assertEquals("tel:+972501234567", telHref("0501234567"))
        assertEquals("https://wa.me/972501234567", whatsAppHref("050-1234567"))
        assertNull(telHref("05012345"))
        assertNull(whatsAppHref("021234567"))
        assertTrue(isValidIlMobile("0521234567"))
        assertFalse(isValidIlMobile("0212345678"))
    }

    @Test
    fun `filter matches name callsign email and digits`() {
        val all = listOf(dana, ofer)
        assertEquals(all, filterContacts(all, "  ") { it })
        assertEquals(listOf(dana), filterContacts(all, "דנה") { it })
        assertEquals(listOf(ofer), filterContacts(all, "ofer@") { it })
        assertTrue(filterContacts(all, "אין כזה") { it }.isEmpty())
    }

    @Test
    fun `digit search ignores phone formatting`() {
        assertTrue(contactMatchesQuery(dana, "0501234567"))
        assertTrue(contactMatchesQuery(dana, "12-34"))
        assertFalse(contactMatchesQuery(dana, "9-8"))
    }
}
