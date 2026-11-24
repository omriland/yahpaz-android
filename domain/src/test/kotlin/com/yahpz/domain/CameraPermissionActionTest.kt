package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPermissionActionTest {
    @Test
    fun grantedPermissionNeedsNothing() {
        assertEquals(
            CameraPermissionAction.NONE,
            cameraPermissionAction(granted = true, canStillAsk = true, alreadyAsked = true),
        )
    }

    @Test
    fun deniedButAskableRequestsAgain() {
        assertEquals(
            CameraPermissionAction.REQUEST,
            cameraPermissionAction(granted = false, canStillAsk = true, alreadyAsked = true),
        )
    }

    @Test
    fun permanentlyDeniedOpensSettings() {
        assertEquals(
            CameraPermissionAction.OPEN_SETTINGS,
            cameraPermissionAction(granted = false, canStillAsk = false, alreadyAsked = true),
        )
    }

    @Test
    fun neverAskedYetRequestsInsteadOfOpeningSettings() {
        assertEquals(
            CameraPermissionAction.REQUEST,
            cameraPermissionAction(granted = false, canStillAsk = false, alreadyAsked = false),
        )
    }

    @Test
    fun settingsButtonLabel() {
        assertEquals("פתיחת הגדרות", OPEN_SETTINGS_LABEL)
    }
}
