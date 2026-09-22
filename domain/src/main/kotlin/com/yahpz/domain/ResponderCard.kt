package com.yahpz.domain

/** View-only lead KM on event detail — hidden from responder-only viewers. */
const val LEAD_KM_VIEW_LABEL = "ק״מ (אחמ״ש)"

/**
 * Lead KM is refund data. A כונן-only viewer never sees it. Any other role
 * (אחמ״ש / מנהל / מנהל־על) always does, including when they are also assigned.
 */
fun responderCardShowsLeadKm(hasLeadRole: Boolean): Boolean = hasLeadRole
