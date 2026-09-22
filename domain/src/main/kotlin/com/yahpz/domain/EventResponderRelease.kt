package com.yahpz.domain

/** Manual events stay off the responder until the lead enters מספר אירוע. */
fun eventReleasedToResponders(origin: String?, policeEventId: String?): Boolean {
    if (origin == "shift") return true
    return !policeEventId.isNullOrBlank()
}
