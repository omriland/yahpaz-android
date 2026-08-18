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
}
