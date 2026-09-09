package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HighwayJunctionsTest {
    private val catalog = listOf(
        HighwayJunctionCatalogRow(
            id = "1",
            nameHe = "צומת אהרונסון",
            nameEn = "Aharonson",
            roads = "4/721 (דרומי)",
            lat = 32.71,
            lng = 34.97,
        ),
        HighwayJunctionCatalogRow(
            id = "2",
            nameHe = "צומת מסובים",
            nameEn = "Mesubim",
            aliasesHe = listOf("מסובים"),
            roads = "4/461",
            lat = 32.03,
            lng = 34.84,
        ),
        HighwayJunctionCatalogRow(
            id = "3",
            nameHe = "מחלף גלילות",
            nameEn = "Glilot",
            roads = "2/5",
            lat = 32.15,
            lng = 34.81,
        ),
    )

    @Test
    fun `place id round-trips a junction id`() {
        val id = "11111111-1111-1111-1111-111111111111"
        assertEquals(id, junctionIdFromPlaceId(junctionPlaceId(id)))
        assertNull(junctionIdFromPlaceId("ChIJx"))
        assertNull(junctionIdFromPlaceId(null))
    }

    @Test
    fun `empty query short-circuits without ranking`() {
        assertEquals(emptyList<HighwayJunction>(), searchHighwayJunctionsCached(catalog, ""))
        assertEquals(emptyList<HighwayJunction>(), searchHighwayJunctionsCached(catalog, "   "))
    }

    @Test
    fun `cached catalog matches an alias`() {
        assertEquals(
            listOf("צומת מסובים"),
            searchHighwayJunctionsCached(catalog, "  מסובים  ").map { it.nameHe },
        )
    }

    @Test
    fun `tolerates a missing letter and an adjacent transposition`() {
        assertTrue(searchHighwayJunctionsCached(catalog, "אהרונסן").any { it.nameHe == "צומת אהרונסון" })
        assertTrue(searchHighwayJunctionsCached(catalog, "מסבוים").any { it.nameHe == "צומת מסובים" })
    }

    @Test
    fun `ignores a trailing direction while searching`() {
        assertTrue(searchHighwayJunctionsCached(catalog, "אהרונסון למערב").any { it.nameHe == "צומת אהרונסון" })
        assertTrue(
            searchHighwayJunctionsCached(catalog, "מחלף גלילות לכיוון צפון")
                .any { it.nameHe == "מחלף גלילות" },
        )
    }

    @Test
    fun `ranks exact and prefix matches ahead of fuzzy matches`() {
        assertEquals("צומת מסובים", rankHighwayJunctions(catalog, "מסובים").first().nameHe)
    }

    @Test
    fun `supports one adjacent transposition at distance one`() {
        assertEquals(1, junctionEditDistance("מסובים", "מסבוים"))
    }

    @Test
    fun `splits common trailing direction variants`() {
        assertEquals(
            JunctionQueryParts(baseQuery = "צומת אהרונסון", directionSuffix = "למערב"),
            splitJunctionDirection("צומת אהרונסון למערב"),
        )
        assertEquals(
            JunctionQueryParts(baseQuery = "גלילות", directionSuffix = "לכיוון צפון"),
            splitJunctionDirection("גלילות לכיוון צפון"),
        )
    }

    @Test
    fun `keeps the typed direction after selecting the canonical junction`() {
        assertEquals("צומת אהרונסון למערב", junctionLocationLabel("צומת אהרונסון", "אהרונסון למערב"))
        assertEquals("צומת רמלה צפון", junctionLocationLabel("צומת רמלה צפון", "צומת רמלה צפון"))
    }

    private val roads = listOf(
        LookupOption("r4", "כביש 4"),
        LookupOption("r40", "40"),
        LookupOption("r44", "כביש 44"),
        LookupOption("r5", "כביש5"),
        LookupOption("r6", "6"),
        LookupOption("urban", "עירוני (101)"),
    )

    @Test
    fun `finds the first usable numeric junction-road token`() {
        assertEquals("4", firstJunctionRoadNumber("4/721 (דרומי)"))
        assertEquals("5", firstJunctionRoadNumber(" 5 /אל כרים קאסם (כפר קאסם)"))
        assertEquals("4", firstJunctionRoadNumber("כביש 4/721"))
        assertEquals("4", firstJunctionRoadNumber("כביש4/721"))
        assertEquals("5", firstJunctionRoadNumber("אל כרים קאסם/5"))
        assertNull(firstJunctionRoadNumber(null))
    }

    @Test
    fun `extracts exact road numbers from supported lookup labels`() {
        assertEquals("6", roadNumberFromLookupName("6"))
        assertEquals("6", roadNumberFromLookupName(" כביש 6 "))
        assertEquals("6", roadNumberFromLookupName("כביש6"))
        assertEquals("6", roadNumberFromLookupName("כביש 6 (צפון)"))
        assertNull(roadNumberFromLookupName("עירוני (101)"))
    }

    @Test
    fun `matches the first road exactly without confusing 4 with 40 or 44`() {
        assertEquals("r4", matchingRoadIdForJunction("4/721 (דרומי)", roads))
        assertEquals("r40", matchingRoadIdForJunction("40/406", roads))
    }

    @Test
    fun `tries later numeric tokens and returns no match when none resolve`() {
        assertEquals("r5", matchingRoadIdForJunction("אל כרים קאסם/5", roads))
        assertNull(matchingRoadIdForJunction("90/57", roads))
        assertEquals(
            "r40",
            matchingRoadIdForJunction("6/40", roads + LookupOption("r6-duplicate", "כביש 6")),
        )
    }

    @Test
    fun `overwrites an existing road when the selected junction resolves`() {
        assertEquals("r4", roadIdAfterJunctionSelection("", "4/721", roads))
        assertEquals("", roadIdAfterJunctionSelection("", "90/57", roads))
        assertEquals("r4", roadIdAfterJunctionSelection("r44", "4/721", roads))
        assertEquals("r44", roadIdAfterJunctionSelection("r44", "90/57", roads))
    }
}
