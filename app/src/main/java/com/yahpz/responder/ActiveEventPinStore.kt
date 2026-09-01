package com.yahpz.responder

import android.content.Context

/** Local pins onto "האירועים הפעילים שלי" — device-only, no backend. */
object ActiveEventPinStore {
    private const val PREFS = "yahpaz_active_event_pin"
    private fun key(userId: String) = "pinned_$userId"

    fun pinnedIds(context: Context, userId: String): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(key(userId), emptySet())
            ?.toSet()
            .orEmpty()

    fun pin(context: Context, userId: String, eventId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = pinnedIds(context, userId) + eventId
        prefs.edit().putStringSet(key(userId), next).apply()
    }

    fun unpin(context: Context, userId: String, eventId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = pinnedIds(context, userId) - eventId
        prefs.edit().putStringSet(key(userId), next).apply()
    }

    fun prune(context: Context, userId: String, knownEventIds: Set<String>) {
        val current = pinnedIds(context, userId)
        val next = current.intersect(knownEventIds)
        if (next.size == current.size) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key(userId), next)
            .apply()
    }
}
