package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClosedListsTest {
    private val systemItem = ClosedListItem(
        id = "1",
        name = "תחנה / אחר / משוכפל",
        sortOrder = 1,
        code = SYSTEM_DISTRICT_CODE,
    )
    private val normalItem = ClosedListItem(
        id = "2",
        name = "צפון",
        sortOrder = 2,
        code = null,
    )

    @Test
    fun canMutateBlocksSystemDistrictsOnly() {
        assertFalse(canMutateClosedListItem(ClosedListKey.DISTRICTS, systemItem))
        assertTrue(canMutateClosedListItem(ClosedListKey.DISTRICTS, normalItem))
        assertTrue(
            canMutateClosedListItem(
                ClosedListKey.ROADS,
                systemItem.copy(code = SYSTEM_DISTRICT_CODE),
            ),
        )
    }

    @Test
    fun closedListMetaMatchesWebLabels() {
        assertEquals("שלוחות", closedListMeta(ClosedListKey.DISTRICTS).label)
        assertEquals("סוגי אירוע", closedListMeta(ClosedListKey.EVENT_TYPES).label)
        assertEquals("כבישים", closedListMeta(ClosedListKey.ROADS).label)
        assertEquals("מיובא אוטומטית מGov.il", closedListMeta(ClosedListKey.ROADS).description)
        assertEquals("סוגי רכב לטיפול", closedListMeta(ClosedListKey.VEHICLE_KINDS).label)
        assertEquals(4, CLOSED_LISTS.size)
        assertEquals("רשימות", SETTINGS_LIST_GROUP_LABEL)
    }

    @Test
    fun filterClosedListItemsByName() {
        val items = listOf(systemItem, normalItem, ClosedListItem("3", "דרום"))
        assertEquals(items, filterClosedListItems(items, ""))
        assertEquals(listOf(normalItem), filterClosedListItems(items, "צפון"))
        assertTrue(filterClosedListItems(items, "אין").isEmpty())
    }

    @Test
    fun nameAndErrorHelpers() {
        assertEquals(CLOSED_LIST_NAME_REQUIRED, closedListNameError("  "))
        assertEquals(null, closedListNameError("צפון"))
        assertEquals(CLOSED_LIST_DUPLICATE, mapClosedListWriteError("duplicate key value", create = true))
        assertEquals(CLOSED_LIST_CREATE_FAILED, mapClosedListWriteError("timeout", create = true))
        assertEquals(CLOSED_LIST_UPDATE_FAILED, mapClosedListWriteError("timeout", create = false))
        assertTrue(mapClosedListDeleteError("violates foreign key constraint").inUse)
        assertEquals(CLOSED_LIST_IN_USE, mapClosedListDeleteError("foreign key").error)
        assertEquals(CLOSED_LIST_DELETE_FAILED, mapClosedListDeleteError("timeout").error)
        assertEquals(SYSTEM_DISTRICT_LOCKED_ERROR, "פריט מערכת — לא ניתן לערוך או למחוק.")
    }
}
