package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun `a phone is compact with the bottom bar, one pane and no width cap`() {
        val layout = adaptiveLayoutFor(widthDp = 411)
        assertEquals(WidthClass.COMPACT, layout.widthClass)
        assertEquals(NavigationStyle.BOTTOM_BAR, layout.navigation)
        assertFalse(layout.listDetail)
        assertNull(layout.contentMaxWidthDp)
    }

    @Test
    fun `the smallest phones and the widest phones stay compact`() {
        listOf(320, 599).forEach { widthDp ->
            val layout = adaptiveLayoutFor(widthDp)
            assertEquals(WidthClass.COMPACT, layout.widthClass)
            assertEquals(NavigationStyle.BOTTOM_BAR, layout.navigation)
            assertFalse(layout.listDetail)
            assertNull(layout.contentMaxWidthDp)
        }
    }

    @Test
    fun `600dp is the first medium width and uses the rail`() {
        val layout = adaptiveLayoutFor(widthDp = 600)
        assertEquals(WidthClass.MEDIUM, layout.widthClass)
        assertEquals(NavigationStyle.RAIL, layout.navigation)
    }

    @Test
    fun `medium keeps a single pane and caps content near 600dp`() {
        val layout = adaptiveLayoutFor(widthDp = 700)
        assertFalse(layout.listDetail)
        assertEquals(CONTENT_MAX_WIDTH_DP, layout.contentMaxWidthDp)
        assertTrue(CONTENT_MAX_WIDTH_DP in 560..640)
    }

    @Test
    fun `839dp is still medium and 840dp is expanded`() {
        val topOfMedium = adaptiveLayoutFor(widthDp = 839)
        assertEquals(WidthClass.MEDIUM, topOfMedium.widthClass)
        assertEquals(NavigationStyle.RAIL, topOfMedium.navigation)
        assertEquals(CONTENT_MAX_WIDTH_DP, topOfMedium.contentMaxWidthDp)
        assertEquals(WidthClass.EXPANDED, adaptiveLayoutFor(widthDp = 840).widthClass)
    }

    @Test
    fun `expanded uses the rail, list and detail side by side, and still caps forms`() {
        val layout = adaptiveLayoutFor(widthDp = 841)
        assertEquals(NavigationStyle.RAIL, layout.navigation)
        assertTrue(layout.listDetail)
        assertEquals(CONTENT_MAX_WIDTH_DP, layout.contentMaxWidthDp)
    }

    @Test
    fun `sheets are capped near 560dp and the list pane is about 360dp`() {
        assertTrue(SHEET_MAX_WIDTH_DP in 520..600)
        assertTrue(LIST_PANE_WIDTH_DP in 340..380)
    }

    @Test
    fun `only expanded width opens details beside the list`() {
        val splits = listOf(320, 411, 599, 600, 839, 840, 1280).filter { adaptiveLayoutFor(it).listDetail }
        assertEquals(listOf(840, 1280), splits)
    }
}
