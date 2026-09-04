package com.yahpz.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val FILL_DRAFT_STASH_SCOPE = "responder"

/** Anything older than this is stale enough that restoring it would confuse. */
const val FILL_DRAFT_MAX_AGE_MS = 1000L * 60 * 60 * 24 * 14

enum class FillBackAction { DROP_UNFINISHED_PHOTO, SHOW_DOCS, LEAVE }

fun fillDraftKey(scope: String, id: String): String = "yahpaz.fillDraft.$scope.$id"

fun shouldKeepLiveFormBoot(loadState: String, hasTypedDraft: Boolean): Boolean {
    if (loadState != "ready") return false
    return hasTypedDraft
}

fun isFillDraftStashFresh(savedAt: Long, now: Long): Boolean =
    now - savedAt <= FILL_DRAFT_MAX_AGE_MS

fun shouldPreferStashedFillDraft(
    stashed: ResponderFillDraft?,
    savedAt: Long?,
    server: ResponderFillDraft,
    now: Long,
): Boolean {
    if (stashed == null || savedAt == null) return false
    if (!isFillDraftStashFresh(savedAt, now)) return false
    return stashed != server
}

fun decideFillBack(onMediaPane: Boolean, unfinishedMediaDraftCount: Int): FillBackAction = when {
    unfinishedMediaDraftCount > 0 -> FillBackAction.DROP_UNFINISHED_PHOTO
    onMediaPane -> FillBackAction.SHOW_DOCS
    else -> FillBackAction.LEAVE
}

/** `HH:mm` in the viewer's own clock, for the "saved at" caption. */
fun fillDraftSavedLabel(savedAtMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.Builder().setLanguage("he").setRegion("IL").build())
        .withZone(ZoneId.of("Asia/Jerusalem"))
    return formatter.format(Instant.ofEpochMilli(savedAtMillis))
}
