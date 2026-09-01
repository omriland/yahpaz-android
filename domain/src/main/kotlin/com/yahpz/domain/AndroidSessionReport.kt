package com.yahpz.domain

const ANDROID_SESSION_REPORT_THROTTLE_MS = 15L * 60 * 1000

data class AndroidSessionRpcParams(
    val versionCode: Int,
    val versionName: String,
)

fun androidSessionRpcParams(versionCode: Int, versionName: String): AndroidSessionRpcParams =
    AndroidSessionRpcParams(versionCode = versionCode, versionName = versionName.trim())

fun shouldReportAndroidSession(
    lastSuccessAtMs: Long?,
    nowMs: Long,
    throttleMs: Long = ANDROID_SESSION_REPORT_THROTTLE_MS,
): Boolean {
    if (lastSuccessAtMs == null) return true
    return nowMs - lastSuccessAtMs >= throttleMs
}
