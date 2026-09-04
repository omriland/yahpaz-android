package com.yahpz.responder

import android.content.Context

object OptionalUpdatePrefs {
    private const val PREF = "yahpaz_app_update"
    private const val SKIPPED = "skipped_latest_version_code"

    fun skippedLatestVersionCode(context: Context): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(SKIPPED, 0)

    fun skip(context: Context, latestVersionCode: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt(SKIPPED, latestVersionCode)
            .apply()
    }
}
