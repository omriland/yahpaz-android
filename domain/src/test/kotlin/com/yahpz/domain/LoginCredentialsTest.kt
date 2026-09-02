package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginCredentialsTest {
    @Test
    fun `trims and lowercases email`() {
        assertEquals("ron.gal72@gmail.com", normalizeLoginEmail("  Ron.Gal72@Gmail.com  "))
    }

    @Test
    fun `strips bidi marks that RTL fields inject around a dotted gmail address`() {
        val typed =
            "\u200Fron.gal72@gmail.com\u200E" +
                "\u202A" + "\u202C" +
                "\u2066" + "\u2069"
        assertEquals("ron.gal72@gmail.com", normalizeLoginEmail(typed))
    }

    @Test
    fun `keeps the gmail local-part dot — GoTrue does not ignore it`() {
        assertEquals("ron.gal72@gmail.com", normalizeLoginEmail("ron.gal72@gmail.com"))
        assertEquals("rongal72@gmail.com", normalizeLoginEmail("rongal72@gmail.com"))
    }

    @Test
    fun `strips bidi marks from the password without trimming secrets`() {
        assertEquals(" AbC!12 ", normalizeLoginSecret("\u200F AbC!12 \u200E"))
    }
}
