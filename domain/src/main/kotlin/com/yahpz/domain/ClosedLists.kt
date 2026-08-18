package com.yahpz.domain

/** Admin closed-list tables — mirrors web `closedLists.ts` / `settingsPanes.ts` (lists only). */
enum class ClosedListKey(val raw: String) {
    DISTRICTS("districts"),
    EVENT_TYPES("event_types"),
    ROADS("roads"),
    VEHICLE_KINDS("vehicle_kinds");

    companion object {
        fun fromRaw(raw: String): ClosedListKey? = entries.firstOrNull { it.raw == raw }
    }
}

data class ClosedListItem(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val code: String? = null,
)

data class ClosedListUsage(
    val table: String,
    val column: String,
)

data class ClosedListMeta(
    val key: ClosedListKey,
    val label: String,
    val description: String? = null,
    val usage: ClosedListUsage,
)

val CLOSED_LISTS: List<ClosedListMeta> = listOf(
    ClosedListMeta(
        key = ClosedListKey.DISTRICTS,
        label = "שלוחות",
        usage = ClosedListUsage(table = "events", column = "district_id"),
    ),
    ClosedListMeta(
        key = ClosedListKey.EVENT_TYPES,
        label = "סוגי אירוע",
        usage = ClosedListUsage(table = "events", column = "event_type_id"),
    ),
    ClosedListMeta(
        key = ClosedListKey.ROADS,
        label = "כבישים",
        description = "מיובא אוטומטית מGov.il",
        usage = ClosedListUsage(table = "events", column = "road_id"),
    ),
    ClosedListMeta(
        key = ClosedListKey.VEHICLE_KINDS,
        label = "סוגי רכב לטיפול",
        usage = ClosedListUsage(table = "event_treated_vehicles", column = "vehicle_kind_id"),
    ),
)

const val SETTINGS_LIST_GROUP_LABEL = "רשימות"
const val CLOSED_LISTS_TITLE = "רשימות"
const val CLOSED_LISTS_CAPTION = "הגדרות רשימות"
const val CLOSED_LISTS_SEARCH_PLACEHOLDER = "חיפוש לפי שם"
const val CLOSED_LIST_LOAD_FAILED = "טעינת הרשימה נכשלה"
const val CLOSED_LIST_LOAD_FAILED_CAPTION = "בדקו את החיבור ונסו שוב."
const val CLOSED_LIST_EMPTY = "אין פריטים ברשימה זו. הפריט הראשון ישמש בטפסים מיד לאחר הוספתו."
const val CLOSED_LIST_NO_RESULTS = "לא נמצאו פריטים תואמים"
const val CLOSED_LIST_ADD = "הוספת פריט"
const val CLOSED_LIST_NAME_LABEL = "שם הפריט"
const val CLOSED_LIST_NAME_REQUIRED = "יש להזין שם לפריט."
const val CLOSED_LIST_DUPLICATE = "פריט בשם זה כבר קיים ברשימה."
const val CLOSED_LIST_CREATE_FAILED = "הוספת הפריט נכשלה. בדקו את החיבור ונסו שוב."
const val CLOSED_LIST_UPDATE_FAILED = "שמירת הפריט נכשלה. בדקו את החיבור ונסו שוב."
const val CLOSED_LIST_DELETE_FAILED = "הסרת הפריט נכשלה. בדקו את החיבור ונסו שוב."
const val CLOSED_LIST_IN_USE = "לא ניתן להסיר פריט שמשויך לאירועים קיימים."
const val CLOSED_LIST_IN_USE_CHECK_FAILED = "בדיקת השימוש בפריט נכשלה. נסו שוב."
const val CLOSED_LIST_CREATED = "הפריט נוסף"
const val CLOSED_LIST_UPDATED = "הפריט נשמר"
const val CLOSED_LIST_DELETED = "הפריט הוסר"
const val CLOSED_LIST_SYSTEM_BADGE = "מערכת"
const val CLOSED_LIST_EDIT = "עריכה"
const val CLOSED_LIST_REMOVE = "הסרה"
const val SYSTEM_DISTRICT_LOCKED_ERROR = "פריט מערכת — לא ניתן לערוך או למחוק."

fun closedListMeta(key: ClosedListKey): ClosedListMeta =
    CLOSED_LISTS.first { it.key == key }

/** System שלוחות cannot be renamed or deleted from the admin panel. */
fun canMutateClosedListItem(key: ClosedListKey, item: ClosedListItem): Boolean =
    !(key == ClosedListKey.DISTRICTS && isSystemClosedListItem(item))

fun isSystemClosedListItem(item: ClosedListItem?): Boolean =
    item?.code == SYSTEM_DISTRICT_CODE

fun filterClosedListItems(items: List<ClosedListItem>, query: String): List<ClosedListItem> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return items
    return items.filter { fieldsMatchQuery(listOf(it.name), trimmed) }
}

sealed class ClosedListMutationResult {
    data class Ok(val item: ClosedListItem? = null) : ClosedListMutationResult()
    data class Err(val error: String, val inUse: Boolean = false) : ClosedListMutationResult()
}

fun closedListNameError(name: String): String? =
    if (name.trim().isEmpty()) CLOSED_LIST_NAME_REQUIRED else null

fun mapClosedListWriteError(message: String?, create: Boolean): String {
    val raw = message.orEmpty()
    if (Regex("duplicate|unique", RegexOption.IGNORE_CASE).containsMatchIn(raw)) {
        return CLOSED_LIST_DUPLICATE
    }
    return if (create) CLOSED_LIST_CREATE_FAILED else CLOSED_LIST_UPDATE_FAILED
}

fun mapClosedListDeleteError(message: String?): ClosedListMutationResult.Err {
    val raw = message.orEmpty()
    if (Regex("foreign key|violates", RegexOption.IGNORE_CASE).containsMatchIn(raw)) {
        return ClosedListMutationResult.Err(CLOSED_LIST_IN_USE, inUse = true)
    }
    return ClosedListMutationResult.Err(CLOSED_LIST_DELETE_FAILED)
}
