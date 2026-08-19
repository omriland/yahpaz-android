package com.yahpz.domain

import java.time.Instant

const val OVERDUE_48H_MS = 48L * 60 * 60 * 1000
const val OVERDUE_FILL_CARD_TIP = "אירוע ממתין לתיעוד מעל ל־48 שעות"

fun isMineFillOverdue(
    isCancelled: Boolean,
    participationStatus: ParticipationStatus?,
    fillCompletableAt: String?,
    nowMs: Long = System.currentTimeMillis(),
): Boolean {
    if (isCancelled) return false
    if (participationStatus == null || participationStatus == ParticipationStatus.DONE) return false
    val start = parseInstantMs(fillCompletableAt) ?: return false
    return nowMs - start >= OVERDUE_48H_MS
}

private fun parseInstantMs(value: String?): Long? {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
}
