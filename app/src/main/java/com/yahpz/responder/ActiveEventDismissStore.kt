package com.yahpz.responder

import android.content.Context

/** Local dismissals for "האירועים הפעילים שלי" — device-only, no backend. */
object ActiveEventDismissStore {
    private const val PREFS = "yahpaz_active_event_dismiss"
    private fun key(userId: String) = "dismissed_$userId"

    fun dismissedIds(context: Context, userId: String): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(key(userId), emptySet())
            ?.toSet()
            .orEmpty()

    fun dismiss(context: Context, userId: String, eventId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = dismissedIds(context, userId) + eventId
        prefs.edit().putStringSet(key(userId), next).apply()
    }

    /** Drop ids that are no longer on the device lists so prefs stay small. */
    fun prune(context: Context, userId: String, knownEventIds: Set<String>) {
        val current = dismissedIds(context, userId)
        val next = current.intersect(knownEventIds)
        if (next.size == current.size) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key(userId), next)
            .apply()
    }
}
