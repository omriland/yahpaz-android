package com.yahpz.domain

enum class CameraPermissionAction { NONE, REQUEST, OPEN_SETTINGS }

const val OPEN_SETTINGS_LABEL = "פתיחת הגדרות"

/** Android stops showing the prompt after a permanent denial; only then does the button open settings. */
fun cameraPermissionAction(granted: Boolean, canStillAsk: Boolean, alreadyAsked: Boolean): CameraPermissionAction = when {
    granted -> CameraPermissionAction.NONE
    !canStillAsk && alreadyAsked -> CameraPermissionAction.OPEN_SETTINGS
    else -> CameraPermissionAction.REQUEST
}
