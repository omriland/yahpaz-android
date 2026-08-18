package com.yahpz.domain

/** Who may open a report. Mirrors the web registry `audience` field. */
enum class ReportAudience { MANAGES_UNIT, ADMIN }

enum class ReportKindId(val raw: String) {
    OPEN_DOCUMENTATION("open_documentation"),
    EVENTS_BY_RESPONDER("events_by_responder"),
    KM_EXCEPTIONS("km_exceptions"),
    KM_DISCREPANCY("km_discrepancy"),
    DUPLICATE_EVENTS("duplicate_events"),
    FUEL_REFUND("fuel_refund");

    companion object {
        fun fromRaw(raw: String?): ReportKindId? = entries.find { it.raw == raw }
    }
}

/**
 * A row rendered by the shared report screen. Reports differ only in how their
 * source rows collapse into this shape, so one screen serves all of them.
 */
data class ReportRow(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val detail: String? = null,
    val trailing: String? = null,
    val eventId: String? = null,
    val stampLabel: String? = null,
    val stampTone: StampTone = StampTone.PENDING,
    /** Set together when the row offers a write, e.g. replacing lead km with the odometer. */
    val actionId: String? = null,
    val actionTitle: String? = null,
    val actionConfirm: String? = null,
    val searchText: String = "",
)

data class ReportSpec(
    val id: ReportKindId,
    val title: String,
    val includes: String,
    val audience: ReportAudience,
    val searchPlaceholder: String,
    val emptyTitle: String,
    /** Fuel refund counts a calendar month; the event reports use a rolling window. */
    val rangeFromMonthStart: Boolean = false,
    val defaultRangeDays: Int = OPEN_DOC_DEFAULT_RANGE_DAYS,
    /** אירועים כפולים scans the whole history, so it hides the date fields. */
    val hasDateRange: Boolean = true,
)

const val REPORT_RANGE_ERROR = "יש להזין תאריך התחלה וסיום תקינים"
const val REPORT_FAILED_TITLE = "טעינת הדוח נכשלה. בדקו את החיבור ונסו שוב."
const val REPORT_LOADING_TITLE = "טוען את הדוח…"
const val REPORT_LOAD_ACTION = "טעינת הדוח"

val REPORT_SPECS: List<ReportSpec> = listOf(
    ReportSpec(
        id = ReportKindId.OPEN_DOCUMENTATION,
        title = OPEN_DOC_TITLE,
        includes = "אירועים שהוזנו על ידי אחמ״ש ומתנדב טרם השלים את התיעוד שלהם",
        audience = ReportAudience.MANAGES_UNIT,
        searchPlaceholder = "חיפוש לפי מתנדב, מספר אירוע או מיקום",
        emptyTitle = OPEN_DOC_EMPTY_TITLE,
    ),
    ReportSpec(
        id = ReportKindId.EVENTS_BY_RESPONDER,
        title = "אירועים לפי מתנדב",
        includes = "כל האירועים של כל מתנדב בטווח התאריכים שנבחר",
        audience = ReportAudience.MANAGES_UNIT,
        searchPlaceholder = "חיפוש לפי מתנדב, מספר אירוע או מיקום",
        emptyTitle = "אין אירועים בתקופה זו",
    ),
    ReportSpec(
        id = ReportKindId.KM_EXCEPTIONS,
        title = "חריגי ק״מ",
        includes = "אירועים עם $KM_EXCEPTION_THRESHOLD ק״מ ומעלה",
        audience = ReportAudience.MANAGES_UNIT,
        searchPlaceholder = "חיפוש לפי כונן, מספר אירוע או מיקום",
        emptyTitle = "אין חריגי ק״מ בתקופה זו",
    ),
    ReportSpec(
        id = ReportKindId.KM_DISCREPANCY,
        title = "אירועים עם פערי דיווח ק״מ",
        includes = "אירועים בהם יש פער בין דיווח האחמ״ש לבין הק״מ שהזין המתנדב",
        audience = ReportAudience.ADMIN,
        searchPlaceholder = "חיפוש לפי מתנדב, מספר אירוע או מיקום",
        emptyTitle = "אין פערי דיווח בתקופה זו",
    ),
    ReportSpec(
        id = ReportKindId.DUPLICATE_EVENTS,
        title = "אירועים כפולים",
        includes = "אירועים עם אותו הכונן, באותו מקום בחלון זמן של חצי שעה",
        audience = ReportAudience.MANAGES_UNIT,
        searchPlaceholder = "חיפוש לפי כונן, מספר אירוע או מיקום",
        emptyTitle = "לא נמצאו אירועים כפולים",
        hasDateRange = false,
    ),
    ReportSpec(
        id = ReportKindId.FUEL_REFUND,
        title = "החזר דלק",
        includes = "סיכום הק״מ שדווחו לכל מתנדב בטווח התאריכים שנבחר",
        audience = ReportAudience.ADMIN,
        searchPlaceholder = "חיפוש לפי שם או או״ק",
        emptyTitle = "אין ק״מ לדיווח בתקופה זו",
        rangeFromMonthStart = true,
    ),
)

fun reportSpec(id: ReportKindId): ReportSpec = REPORT_SPECS.first { it.id == id }

fun visibleReportSpecs(roles: List<String>): List<ReportSpec> {
    val admin = isAdmin(roles)
    val unit = managesUnit(roles)
    return REPORT_SPECS.filter { spec ->
        when (spec.audience) {
            ReportAudience.ADMIN -> admin
            ReportAudience.MANAGES_UNIT -> unit
        }
    }
}

/** From/to for a report's default window, both inclusive ISO days. */
fun defaultReportRange(spec: ReportSpec, today: String): Pair<String, String> {
    if (spec.rangeFromMonthStart) return "${today.take(7)}-01" to today
    return addCalendarDays(today, -spec.defaultRangeDays) to today
}

fun isValidReportRange(from: String, to: String): Boolean = from.isNotEmpty() && to.isNotEmpty() && from <= to

fun filterReportRows(rows: List<ReportRow>, query: String): List<ReportRow> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return rows
    return rows.filter { row ->
        fieldsMatchQuery(listOf(row.searchText, row.title, row.subtitle, row.detail), trimmed)
    }
}

fun reportRowSummary(count: Int): String = when (count) {
    0 -> "אין שורות בדוח"
    1 -> "שורה אחת בדוח"
    else -> "$count שורות בדוח"
}

/** `name · callsign`, dropping blanks, with a Hebrew fallback. */
fun personDisplay(name: String?, callsign: String?, fallback: String = "כונן"): String =
    listOfNotNull(
        name?.trim()?.takeIf { it.isNotEmpty() },
        callsign?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" · ").ifEmpty { fallback }

/** `road · location`, dropping blanks. */
fun placeDisplay(roadName: String?, location: String?): String =
    listOfNotNull(
        roadName?.trim()?.takeIf { it.isNotEmpty() },
        location?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")

/** Cancelled events keep their police number but are labelled first, like the web. */
fun policeEventLabel(policeEventId: String?, isCancelled: Boolean): String {
    val number = policeEventId?.trim()?.takeIf { it.isNotEmpty() }
    return when {
        isCancelled && number != null -> "בוטל · $number"
        isCancelled -> "בוטל"
        number != null -> number
        else -> "—"
    }
}
