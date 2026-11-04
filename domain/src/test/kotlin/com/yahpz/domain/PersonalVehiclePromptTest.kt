package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalVehiclePromptTest {
    @Test
    fun `prompts active volunteers and trainees without an active vehicle`() {
        assertTrue(needsPersonalVehiclePrompt("active_volunteer", 0))
        assertTrue(needsPersonalVehiclePrompt("personal_vehicle_training", 0))
        assertFalse(needsPersonalVehiclePrompt("active_volunteer", 1))
        assertFalse(needsPersonalVehiclePrompt(VolunteerStatus.SHIFTS_ONLY.raw, 0))
        assertFalse(needsPersonalVehiclePrompt(VolunteerStatus.ADMINISTRATION.raw, 0))
        assertFalse(needsPersonalVehiclePrompt("training", 0))
        assertFalse(needsPersonalVehiclePrompt(null, 0))
    }

    @Test
    fun `whatsapp link carries the encoded message`() {
        assertNull(personalVehicleWhatsAppHref("   "))
        val href = personalVehicleWhatsAppHref(" 12-345-67 ")!!
        assertTrue(href.startsWith("$SUPPORT_WHATSAPP_HREF?text="))
        assertFalse(href.contains("+"))
        assertEquals(
            personalVehicleWhatsAppMessage("12-345-67"),
            java.net.URLDecoder.decode(href.substringAfter("?text="), "UTF-8"),
        )
    }
}
