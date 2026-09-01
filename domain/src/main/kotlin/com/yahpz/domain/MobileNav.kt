package com.yahpz.domain

/**
 * Port of web `mobileNav.ts`. Same ranks, same overflow rule, same Hebrew labels.
 * Native skips `map` (Google Maps JS) and desktop-only `cockpit`.
 */

const val MOBILE_MORE_LABEL = "עוד"

/** Daily destinations — keep these in the tab bar when they exist. */
private val MOBILE_TAB_PRIMARY = listOf("events", "mine", "users", "my_shifts")

/** Reachable on mobile, but not every-session. Overflow into עוד. */
private val MOBILE_TAB_SECONDARY = listOf("shifts", "contacts", "reports")

/** Five is the hard limit; the last slot is reserved for עוד when anything overflows. */
private const val MOBILE_TAB_MAX = 4

data class MobileNavEntry(
    val view: String,
    val label: String,
)

data class SplitMobileNav(
    val tabs: List<MobileNavEntry>,
    val more: List<MobileNavEntry>,
)

private fun rank(view: String): Int {
    val primary = MOBILE_TAB_PRIMARY.indexOf(view)
    if (primary >= 0) return primary
    val secondary = MOBILE_TAB_SECONDARY.indexOf(view)
    if (secondary >= 0) return MOBILE_TAB_PRIMARY.size + secondary
    return MOBILE_TAB_PRIMARY.size + MOBILE_TAB_SECONDARY.size
}

fun splitMobileNav(entries: List<MobileNavEntry>): SplitMobileNav {
    val ordered = entries.sortedBy { rank(it.view) }
    if (ordered.size <= MOBILE_TAB_MAX) {
        return SplitMobileNav(tabs = ordered, more = emptyList())
    }
    return SplitMobileNav(
        tabs = ordered.take(MOBILE_TAB_MAX - 1),
        more = ordered.drop(MOBILE_TAB_MAX - 1),
    )
}

/**
 * Destinations a signed-in user can open on the phone.
 * Matches web `App.tsx` entries for mobile, minus מפה and הקוקפיט.
 */
fun mobileNavEntries(roles: List<String>): List<MobileNavEntry> {
    val list = mutableListOf<MobileNavEntry>()
    val hasMineList = isResponder(roles) || roleSet(roles).contains(AppRole.SHIFT_LEAD)
    val manages = managesUnit(roles)
    val admin = isAdmin(roles)

    if (hasMineList) {
        list += MobileNavEntry("mine", "האירועים שלי")
        list += MobileNavEntry("my_shifts", "המשמרות שלי")
    }
    list += MobileNavEntry("contacts", "אנשי קשר")
    if (manages) {
        list += MobileNavEntry("events", "אירועים")
        list += MobileNavEntry("shifts", "משמרות")
        if (!admin) list += MobileNavEntry("reports", "דוחות")
    }
    if (admin) {
        list += MobileNavEntry("users", "ניהול")
    }
    list += MobileNavEntry("profile", "פרופיל")
    return list
}

/**
 * First screen after login. Leads and admins land on unit אירועים so
 * creating and assigning is the first job; responders land on האירועים שלי.
 */
fun defaultMobileView(roles: List<String>): String =
    if (managesUnit(roles)) "events" else "mine"
