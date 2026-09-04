package com.yahpz.domain

const val MINE_PENDING_TAB_LABEL = "ממתינים לתיעוד"
const val MINE_LOGGED_TAB_LABEL = "תועדו"
const val MINE_PENDING_EMPTY_TITLE = "אין אירועים שממתינים לתיעוד."
const val MINE_PENDING_EMPTY_CAPTION = "אירוע חדש יופיע כאן כשישויך אליך."
const val MINE_PENDING_EMPTY_VIEW_LOGGED = "לצפייה באירועים שתועדו"
const val MINE_LOGGED_EMPTY_TITLE = "אין אירועים שתועדו בתקופה זו"
const val MINE_LOGGED_WINDOW_DAYS = 30
const val FUEL_NOTE = "שימו לב! אירועים שלא תועדו במלואם לא נכללים בהחזר הדלק הרבעוני"

fun openMineSummary(count: Int, ready: Boolean): String {
    if (!ready) return "טוען את הדיווחים שלך…"
    if (count == 0) return "אין אירועים שממתינים לתיעוד."
    if (count == 1) return "יש לך אירוע אחד לתעד."
    if (count == 2) return "יש לך שני אירועים לתעד."
    return "יש לך $count אירועים לתעד."
}

fun minePendingTabLabel(count: Int): String =
    if (count > 0) "$MINE_PENDING_TAB_LABEL $count" else MINE_PENDING_TAB_LABEL

fun mineLoggedNoResultsTitle(query: String): String = "אין אירועים שתועדו התואמים ל־“$query”"

data class MineSearchFields(
    val policeEventId: String? = null,
    val roadName: String? = null,
    val location: String? = null,
)

fun mineEventMatchesQuery(event: MineSearchFields, query: String): Boolean =
    fieldsMatchQuery(listOf(event.policeEventId, event.roadName, event.location), query)

data class MineListEvent(
    val id: String,
    val date: String,
    val participation: ParticipationStatus,
    val totalKm: Double? = null,
)

data class MineListSections<T>(
    val pending: List<T>,
    val logged: List<T>,
    val hasMoreLogged: Boolean,
)

fun partitionMineList(
    items: List<MineListEvent>,
    today: String,
    windowsLoaded: Int,
): MineListSections<MineListEvent> {
    val start = loggedWindowStart(today, windowsLoaded)
    val pending = mutableListOf<MineListEvent>()
    val logged = mutableListOf<MineListEvent>()
    var hasMoreLogged = false
    for (item in items) {
        if (mineInboxIsOpen(item.participation, item.totalKm)) {
            pending += item
            continue
        }
        when {
            item.date >= start && item.date <= today -> logged += item
            item.date < start -> hasMoreLogged = true
        }
    }
    return MineListSections(
        pending = pending.sortedByDescending { it.date },
        logged = logged.sortedByDescending { it.date },
        hasMoreLogged = hasMoreLogged,
    )
}

fun loggedWindowStart(today: String, windowsLoaded: Int): String {
    val windows = maxOf(1, windowsLoaded)
    return addCalendarDays(today, -(windows * MINE_LOGGED_WINDOW_DAYS))
}

fun shiftGroupPendingCaption(count: Int): String =
    if (count == 1) "אירוע אחד לתעד" else "$count לתעד"

fun shiftGroupShouldStartOpen(pendingCount: Int): Boolean = pendingCount > 0

fun fuelNoteNeeded(openCount: Int): Boolean = openCount >= 3

private val enToHe = mapOf(
    'q' to '/', 'w' to '\'', 'e' to 'ק', 'r' to 'ר', 't' to 'א', 'y' to 'ט', 'u' to 'ו',
    'i' to 'ן', 'o' to 'ם', 'p' to 'פ', 'a' to 'ש', 's' to 'ד', 'd' to 'ג', 'f' to 'כ',
    'g' to 'ע', 'h' to 'י', 'j' to 'ח', 'k' to 'ל', 'l' to 'ך', 'z' to 'ז', 'x' to 'ס',
    'c' to 'ב', 'v' to 'ה', 'b' to 'נ', 'n' to 'מ', 'm' to 'צ',
)

fun searchQueryVariants(query: String): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    val mapped = trimmed.map { ch -> enToHe[ch.lowercaseChar()] ?: ch }.joinToString("")
    return if (mapped == trimmed) listOf(trimmed) else listOf(trimmed, mapped)
}

fun fieldsMatchQuery(fields: List<String?>, query: String): Boolean =
    fields.any { field -> field != null && textIncludesQuery(field, query) }

fun textIncludesQuery(haystack: String, query: String): Boolean {
    val variants = searchQueryVariants(query)
    if (variants.isEmpty()) return true
    val hay = haystack.lowercase()
    return variants.any { hay.contains(it.lowercase()) }
}

data class TextHighlightRange(val start: Int, val endExclusive: Int)

fun searchHighlightRanges(text: String, query: String): List<TextHighlightRange> {
    val variants = searchQueryVariants(query).map { it.lowercase() }.filter { it.isNotEmpty() }
    if (variants.isEmpty() || text.isEmpty()) return emptyList()
    val hay = text.lowercase()
    val raw = mutableListOf<TextHighlightRange>()
    for (variant in variants) {
        var from = 0
        while (from <= hay.length - variant.length) {
            val at = hay.indexOf(variant, from)
            if (at < 0) break
            raw += TextHighlightRange(at, at + variant.length)
            from = at + 1
        }
    }
    if (raw.isEmpty()) return emptyList()
    val ordered = raw.sortedBy { it.start }
    val merged = mutableListOf(ordered.first())
    for (range in ordered.drop(1)) {
        val last = merged.last()
        if (range.start <= last.endExclusive) {
            merged[merged.lastIndex] = TextHighlightRange(last.start, maxOf(last.endExclusive, range.endExclusive))
        } else {
            merged += range
        }
    }
    return merged
}
