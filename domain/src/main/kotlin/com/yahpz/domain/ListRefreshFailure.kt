package com.yahpz.domain

const val UNIT_EVENTS_REFRESH_FAILED_TOAST = "רענון האירועים נכשל. בדקו את החיבור ונסו שוב."

/** What a failed list load does: a refresh over rows keeps them and toasts, a first load shows the empty failure. */
data class ListRefreshFailure(val keepRows: Boolean, val showEmptyFailure: Boolean, val toast: String?)

fun listRefreshFailure(hadRows: Boolean, toastText: String): ListRefreshFailure =
    if (hadRows) {
        ListRefreshFailure(keepRows = true, showEmptyFailure = false, toast = toastText)
    } else {
        ListRefreshFailure(keepRows = false, showEmptyFailure = true, toast = null)
    }
