package com.yahpz.domain

const val PATROL_CALLSIGN_PREFIX_MAX_LENGTH = 16
const val PATROL_CALLSIGN_NUMBER_MAX_LENGTH = 5
const val PATROL_CALLSIGN_PREFIX_LABEL = "אוק - כינוי"
const val PATROL_CALLSIGN_NUMBER_LABEL = "אוק - מס"
const val PATROL_CALLSIGN_PREFIX_PLACEHOLDER = "אביב"
const val PATROL_CALLSIGN_NUMBER_PLACEHOLDER = "411"
const val PATROL_CALLSIGN_NUMBER_ERROR = "יש למלא אוק - מס."

data class SplitPatrolCallsign(
    val prefix: String,
    val number: String,
)

/**
 * Last contiguous digit run → number (first 5 if longer). Leftover text → prefix.
 * `411` → number; `אביב` → prefix; `אביב 411` / `אביב411` → both.
 */
fun splitPatrolCallsign(raw: String?): SplitPatrolCallsign {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return SplitPatrolCallsign("", "")
    val match = Regex("""^(.*?)(\d+)(?!.*\d)(.*)$""").find(trimmed)
        ?: return SplitPatrolCallsign(trimmed, "")
    val number = match.groupValues[2].take(PATROL_CALLSIGN_NUMBER_MAX_LENGTH)
    val prefix = (match.groupValues[1] + match.groupValues[3]).replace(Regex("""\s+"""), " ").trim()
    return SplitPatrolCallsign(prefix, number)
}

fun formatPatrolCallsign(prefix: String?, number: String?): String =
    listOf(prefix?.trim().orEmpty(), number?.trim().orEmpty()).filter { it.isNotEmpty() }.joinToString(" ")

fun patrolCallsignPrefixForInput(raw: String): String = raw.take(PATROL_CALLSIGN_PREFIX_MAX_LENGTH)

fun patrolCallsignNumberForInput(raw: String): String =
    digitsOnly(raw).take(PATROL_CALLSIGN_NUMBER_MAX_LENGTH)

fun resolvePatrolCallsign(prefix: String?, number: String?, legacy: String?): SplitPatrolCallsign {
    val resolvedPrefix = prefix.orEmpty()
    val resolvedNumber = number.orEmpty()
    if (resolvedPrefix.isNotBlank() || resolvedNumber.isNotBlank()) {
        return SplitPatrolCallsign(resolvedPrefix.trim(), digitsOnly(resolvedNumber))
    }
    return splitPatrolCallsign(legacy)
}
