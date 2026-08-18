package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class PlateLookupTest {
    @Test
    fun misparStripsDashesAndLeadingZeros() {
        assertEquals(71386301, plateLookupMispar("713-86-301"))
        assertEquals(1234567, plateLookupMispar("01234567"))
    }

    @Test
    fun parseReadsModelColorAndManufacturer() {
        val body =
            """{"success":true,"result":{"records":[{"tzeva_rechev":"שחור","kinuy_mishari":"REXTON","tozeret_nm":"סאנגיונג ד.קור"}]}}"""
        val hit = parsePlateLookupBody(body)!!
        assertEquals("REXTON", hit.model)
        assertEquals("שחור", hit.color)
        assertEquals("סאנגיונג ד.קור", hit.manufacturer)
    }

    @Test
    fun parseReadsModelAndColorFromHit() {
        val body =
            """{"success":true,"result":{"records":[{"tzeva_rechev":"שחור","kinuy_mishari":"REXTON"}]}}"""
        val hit = parsePlateLookupBody(body)!!
        assertEquals("REXTON", hit.model)
        assertEquals("שחור", hit.color)
        assertNull(hit.manufacturer)
    }

    @Test
    fun parseReturnsNullOnEmptyRecords() {
        val body = """{"success":true,"result":{"records":[]}}"""
        assertNull(parsePlateLookupBody(body))
    }

    @Test
    fun parseReturnsNullOnWafHtml() {
        assertNull(parsePlateLookupBody("<html>blocked</html>"))
    }

    @Test
    fun urlEncodesResourceAndNumericFilter() {
        val url = plateLookupUrl("713-86-301")
        assertTrue(url.contains("resource_id=053cea08-09bc-40ec-8f7a-156f0677aff3"))
        val encoded = URLEncoder.encode("""{"mispar_rechev":71386301}""", StandardCharsets.UTF_8)
        assertTrue(url.contains(encoded))
    }
}
