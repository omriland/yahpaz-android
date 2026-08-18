package com.yahpz.domain

const val CONTACTS_TITLE = "אנשי קשר"
const val CONTACTS_SEARCH_PLACEHOLDER = "חיפוש לפי שם, או״ק, טלפון או דוא״ל"
const val CONTACTS_EMPTY_TITLE = "אין אנשי קשר להצגה"
const val CONTACTS_NO_RESULTS_TITLE = "לא נמצאו אנשי קשר תואמים"
const val CONTACTS_FAILED_TITLE = "טעינת אנשי הקשר נכשלה. בדקו את החיבור ונסו שוב."

fun phoneDigits(raw: String?): String = digitsOnly(raw.orEmpty()).take(10)

/** 0501234567 → 050-1234567. */
fun formatPhone(raw: String?): String {
    val digits = phoneDigits(raw)
    if (digits.length <= 3) return digits
    return "${digits.take(3)}-${digits.drop(3)}"
}

/** True when raw is an Israeli mobile: 10 digits starting with 05. */
fun isValidIlMobile(raw: String?): Boolean {
    val digits = phoneDigits(raw)
    return digits.length == 10 && digits.startsWith("05")
}

/** 0501234567 → tel:+972501234567. Null unless 10 digits. */
fun telHref(raw: String?): String? {
    val digits = phoneDigits(raw)
    if (digits.length != 10) return null
    return "tel:+972${digits.drop(1)}"
}

/** Israeli mobile only → https://wa.me/972… */
fun whatsAppHref(raw: String?): String? {
    if (!isValidIlMobile(raw)) return null
    return "https://wa.me/972${phoneDigits(raw).drop(1)}"
}

data class ContactSearchFields(
    val fullName: String,
    val callsign: String,
    val email: String,
    val phone: String?,
)

fun contactMatchesQuery(fields: ContactSearchFields, query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    val matched = fieldsMatchQuery(
        listOf(
            fields.fullName,
            fields.callsign,
            fields.email,
            fields.phone,
            fields.phone?.let { formatPhone(it) },
        ),
        trimmed,
    )
    if (matched) return true
    val queryDigits = phoneDigits(trimmed)
    return queryDigits.length >= 3 &&
        !fields.phone.isNullOrEmpty() &&
        phoneDigits(fields.phone).contains(queryDigits)
}

fun <T> filterContacts(
    contacts: List<T>,
    query: String,
    fields: (T) -> ContactSearchFields,
): List<T> {
    if (query.isBlank()) return contacts
    return contacts.filter { contactMatchesQuery(fields(it), query) }
}
