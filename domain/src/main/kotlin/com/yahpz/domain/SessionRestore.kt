package com.yahpz.domain

const val SIGN_IN_FAILED_TEXT = "הכניסה נכשלה. בדקו את החיבור ונסו שוב."
const val OFFLINE_RETRY_BANNER = "אין חיבור. נסו שוב"
const val OFFLINE_RETRY_ACTION = "נסו שוב"

enum class SessionLoadFailure { NETWORK, AUTH }

enum class SessionRestoreOutcome { SIGNED_IN, SIGNED_OUT, KEEP_SIGNED_IN_OFFLINE }

/** Only a real auth failure or a missing stored session signs the user out; a network failure keeps them in. */
fun sessionRestoreOutcome(hasSession: Boolean, failure: SessionLoadFailure?): SessionRestoreOutcome = when {
    !hasSession -> SessionRestoreOutcome.SIGNED_OUT
    failure == SessionLoadFailure.NETWORK -> SessionRestoreOutcome.KEEP_SIGNED_IN_OFFLINE
    failure == SessionLoadFailure.AUTH -> SessionRestoreOutcome.SIGNED_OUT
    else -> SessionRestoreOutcome.SIGNED_IN
}
