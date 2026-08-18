package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarLogoMapTest {
    @Test
    fun resolvesVolkswagenWithCountrySuffix() {
        assertEquals("volkswagen", resolveCarLogoSlug("פולקסווגן גרמנ"))
    }

    @Test
    fun resolvesSsangYongWithCountrySuffix() {
        assertEquals("ssangyong", resolveCarLogoSlug("סאנגיונג ד.קור"))
    }

    @Test
    fun resolvesCommonHebrewBrands() {
        assertEquals("toyota", resolveCarLogoSlug("טויוטה יפן"))
        assertEquals("hyundai", resolveCarLogoSlug("יונדאי קוריאה"))
        assertEquals("kia", resolveCarLogoSlug("קיה"))
        assertEquals("skoda", resolveCarLogoSlug("סקודה"))
        assertEquals("bmw", resolveCarLogoSlug("ב מ וו"))
    }

    @Test
    fun resolvesLatinFallback() {
        assertEquals("byd", resolveCarLogoSlug("BYD China"))
        assertEquals("tesla", resolveCarLogoSlug("TESLA"))
    }

    @Test
    fun unknownReturnsNull() {
        assertNull(resolveCarLogoSlug(""))
        assertNull(resolveCarLogoSlug("   "))
        assertNull(resolveCarLogoSlug("יצרן לא קיים בעולם"))
    }
}
