package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileVehiclesTest {
    @Test
    fun emptyInputYieldsEmptyList() {
        assertEquals(emptyList<ProfileVehicle>(), visibleProfileVehicles(emptyList()))
    }

    @Test
    fun dropsArchived() {
        val rows = listOf(
            VehicleRowInput("1234567", "טויוטה", false),
            VehicleRowInput("7654321", "קיה", true),
            VehicleRowInput("1111111", "הונדה", null),
        )
        assertEquals(
            listOf(
                ProfileVehicle("1234567", "טויוטה"),
                ProfileVehicle("1111111", "הונדה"),
            ),
            visibleProfileVehicles(rows),
        )
    }

    @Test
    fun dropsEmptyAndNonDigitPlates() {
        val rows = listOf(
            VehicleRowInput("", "טויוטה", false),
            VehicleRowInput("abc", "קיה", false),
            VehicleRowInput("12-345-67", "הונדה", false),
        )
        assertEquals(
            listOf(ProfileVehicle("1234567", "הונדה")),
            visibleProfileVehicles(rows),
        )
    }

    @Test
    fun trimsModelAndKeepsBlank() {
        val rows = listOf(
            VehicleRowInput("1234567", "  טויוטה  ", false),
            VehicleRowInput("7654321", null, false),
            VehicleRowInput("1111111", "   ", false),
        )
        assertEquals(
            listOf(
                ProfileVehicle("1234567", "טויוטה"),
                ProfileVehicle("7654321", ""),
                ProfileVehicle("1111111", ""),
            ),
            visibleProfileVehicles(rows),
        )
    }

    @Test
    fun preservesOrderOfRemainingRows() {
        val rows = listOf(
            VehicleRowInput("1111111", "א", false),
            VehicleRowInput("2222222", "ב", true),
            VehicleRowInput("3333333", "ג", false),
        )
        assertEquals(
            listOf(
                ProfileVehicle("1111111", "א"),
                ProfileVehicle("3333333", "ג"),
            ),
            visibleProfileVehicles(rows),
        )
    }

    @Test
    fun vehicleRemoveModeArchivesWhenAttached() {
        assertEquals("archive", vehicleRemoveMode(true))
        assertEquals("delete", vehicleRemoveMode(false))
    }

    @Test
    fun setDefaultVehicleLabelMatchesWebTooltip() {
        assertEquals("הגדר כרכב ברירת מחדל", SET_DEFAULT_VEHICLE_LABEL)
        assertEquals("רכב ראשי", DEFAULT_VEHICLE_LABEL)
    }

    @Test
    fun canChooseDefaultVehicleNeedsTwoActiveCars() {
        assertEquals(false, canChooseDefaultVehicle(emptyList()))
        assertEquals(false, canChooseDefaultVehicle(listOf(ProfileVehicle("1", "א"))))
        assertEquals(
            true,
            canChooseDefaultVehicle(
                listOf(
                    ProfileVehicle("1", "א"),
                    ProfileVehicle("2", "ב", archived = true),
                    ProfileVehicle("3", "ג"),
                ),
            ),
        )
    }

    @Test
    fun isProfileVehicleEditingKeepsUnsavedOpenAndSavedClosedUntilPencil() {
        assertEquals(true, isProfileVehicleEditing(null, "new-1", null))
        assertEquals(false, isProfileVehicleEditing("v1", "v1", null))
        assertEquals(true, isProfileVehicleEditing("v1", "v1", "v1"))
        assertEquals(false, isProfileVehicleEditing("v1", "v1", "v2"))
    }

    @Test
    fun managedProfileVehiclesKeepsArchivedAndIds() {
        val rows = listOf(
            VehicleRowInput("12-345-67", "טויוטה", false, id = "v1", isDefault = true),
            VehicleRowInput("7654321", "קיה", true, id = "v2", isDefault = false),
        )
        assertEquals(
            listOf(
                ProfileVehicle("1234567", "טויוטה", id = "v1", archived = false, isDefault = true),
                ProfileVehicle("7654321", "קיה", id = "v2", archived = true, isDefault = false),
            ),
            managedProfileVehicles(rows),
        )
    }

    @Test
    fun vehicleFieldsForSaveRequiresPlateAndModel() {
        assertEquals(
            VehicleFieldsError("יש להזין לוחית רישוי ודגם."),
            vehicleFieldsForSave("", "קורולה"),
        )
        assertEquals(
            VehicleFieldsOk("12-345-67", "קורולה"),
            vehicleFieldsForSave("1234567", "  קורולה  "),
        )
    }
}
