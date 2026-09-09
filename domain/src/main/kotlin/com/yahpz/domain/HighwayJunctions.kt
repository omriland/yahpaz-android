package com.yahpz.domain

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

const val JUNCTION_PLACE_ID_PREFIX = "junction:"
const val JUNCTION_RESULT_LIMIT = 15

const val LOCATION_JUNCTION_GROUP = "צמתים ומחלפים"
const val LOCATION_GOOGLE_GROUP = "תוצאות ממפות Google"
const val LOCATION_SEARCHING = "מחפשים צמתים ומקומות…"
const val LOCATION_JUNCTIONS_UNAVAILABLE = "חיפוש הצמתים אינו זמין כרגע."
const val LOCATION_GOOGLE_UNAVAILABLE = "השלמת מיקום מגוגל אינה זמינה כרגע. אפשר להזין מיקום ידנית."
const val LOCATION_PLACEHOLDER = "למשל: מחלף שורק"
const val LOCATION_FREE_TEXT_EMPTY = "שימוש בטקסט שהוזן"

data class HighwayJunction(
    val id: String,
    val nameHe: String,
    val nameEn: String? = null,
    val roads: String? = null,
    val lat: Double,
    val lng: Double,
)

data class HighwayJunctionCatalogRow(
    val id: String,
    val nameHe: String,
    val nameEn: String? = null,
    val aliasesHe: List<String> = emptyList(),
    val aliasesEn: List<String> = emptyList(),
    val roads: String? = null,
    val lat: Double,
    val lng: Double,
)

data class JunctionQueryParts(
    val baseQuery: String,
    val directionSuffix: String? = null,
)

private val hebrewLocale = Locale.forLanguageTag("he")
private val hebrewCollator: Collator = Collator.getInstance(hebrewLocale)
private val hebrewNikud = Regex("""[\u0591-\u05c7]""")
private val junctionKindPrefix = Regex("""^(?:צומת|מחלף)\s+""")
private val trailingDirection = Regex(
    """\s+((?:(?:ל|ב)כיוון\s+(?:ה?(?:מערב|מזרח|צפון|דרום)|ל(?:מערב|מזרח|צפון|דרום)))|(?:ל?(?:מערב|מזרח|צפון|דרום)|מערבה|מזרחה|צפונה|דרומה))$""",
)
private val punctuation = Regex("""["'׳״.,()\[\]{}\-–—]""")
private val parenNote = Regex("""\([^)]*\)""")
private val roadNumberPattern = Regex("""^(?:כביש\s*)?(\d+)$""")
private val whitespace = Regex("""\s+""")

fun junctionPlaceId(id: String): String = "$JUNCTION_PLACE_ID_PREFIX$id"

fun junctionIdFromPlaceId(placeId: String?): String? {
    if (placeId == null || !placeId.startsWith(JUNCTION_PLACE_ID_PREFIX)) return null
    return placeId.substring(JUNCTION_PLACE_ID_PREFIX.length)
}

fun locationFreeTextLabel(query: String): String {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) LOCATION_FREE_TEXT_EMPTY else "שימוש ב־\"$trimmed\" כפי שהוזן"
}

fun junctionRoadsCaption(roads: String?): String? =
    roads?.takeIf { it.isNotBlank() }?.let { "כביש $it" }

fun normalizedRoadNumber(value: String): String? {
    val token = parenNote.replace(value, "").trim()
    return roadNumberPattern.find(token)?.groupValues?.get(1)
}

/** First numeric junction-road token, accepting "4", "כביש 4", and "כביש4". */
fun firstJunctionRoadNumber(roads: String?): String? {
    for (token in roads?.split("/") ?: emptyList()) {
        val roadNumber = normalizedRoadNumber(token)
        if (roadNumber != null) return roadNumber
    }
    return null
}

/** Accept closed-list labels such as "6" or "כביש 6", but no partial number matches. */
fun roadNumberFromLookupName(name: String): String? = normalizedRoadNumber(name)

/**
 * Try numeric slash-delimited tokens in order and resolve the first unique
 * exact closed-list match. No partial numeric matching is allowed.
 */
fun matchingRoadIdForJunction(roads: String?, lookups: List<LookupOption>): String? {
    for (token in roads?.split("/") ?: emptyList()) {
        val junctionRoadNumber = normalizedRoadNumber(token) ?: continue
        val matches = lookups.filter { roadNumberFromLookupName(it.name) == junctionRoadNumber }
        if (matches.size == 1) return matches[0].id
    }
    return null
}

/** A junction is the source of truth: overwrite with a match, otherwise preserve the road. */
fun roadIdAfterJunctionSelection(
    currentRoadId: String,
    junctionRoads: String?,
    lookups: List<LookupOption>,
): String = matchingRoadIdForJunction(junctionRoads, lookups) ?: currentRoadId

fun splitJunctionDirection(query: String): JunctionQueryParts {
    val trimmed = query.trim()
    val match = trailingDirection.find(trimmed) ?: return JunctionQueryParts(trimmed)
    return JunctionQueryParts(
        baseQuery = trimmed.substring(0, match.range.first).trim(),
        directionSuffix = match.groupValues.getOrNull(1),
    )
}

fun normalizeJunctionText(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
    return hebrewNikud.replace(decomposed, "")
        .lowercase(hebrewLocale)
        .replace(punctuation, " ")
        .replace(whitespace, " ")
        .trim()
}

private fun searchVariants(value: String): List<String> {
    val normalized = normalizeJunctionText(value)
    val withoutKind = junctionKindPrefix.replace(normalized, "")
    return if (withoutKind.isNotEmpty() && withoutKind != normalized) {
        listOf(normalized, withoutKind)
    } else {
        listOf(normalized)
    }
}

/** Damerau-Levenshtein distance, including one adjacent transposition. */
fun junctionEditDistance(left: String, right: String): Int {
    val rows = left.length + 1
    val columns = right.length + 1
    val matrix = Array(rows) { IntArray(columns) }
    for (row in 0 until rows) matrix[row][0] = row
    for (column in 0 until columns) matrix[0][column] = column

    for (row in 1 until rows) {
        for (column in 1 until columns) {
            val substitutionCost = if (left[row - 1] == right[column - 1]) 0 else 1
            matrix[row][column] = minOf(
                matrix[row - 1][column] + 1,
                matrix[row][column - 1] + 1,
                matrix[row - 1][column - 1] + substitutionCost,
            )
            if (
                row > 1 &&
                column > 1 &&
                left[row - 1] == right[column - 2] &&
                left[row - 2] == right[column - 1]
            ) {
                matrix[row][column] = minOf(matrix[row][column], matrix[row - 2][column - 2] + 1)
            }
        }
    }
    return matrix[left.length][right.length]
}

private fun matchScore(term: String, query: String): Int? {
    if (term.isEmpty() || query.isEmpty()) return null
    if (term == query) return 0
    if (term.startsWith(query)) return 10 + minOf(term.length - query.length, 9)
    if (term.contains(query)) return 20 + minOf(term.indexOf(query), 9)
    if (query.length < 4) return null
    val distance = junctionEditDistance(term, query)
    val maxDistance = maxOf(1, query.length / 4)
    return if (distance <= maxDistance) 40 + distance else null
}

fun rankHighwayJunctions(
    rows: List<HighwayJunctionCatalogRow>,
    query: String,
): List<HighwayJunction> {
    val parts = splitJunctionDirection(query)
    val queryVariants = (searchVariants(query) + searchVariants(parts.baseQuery))
        .distinct()
        .filter { it.isNotEmpty() }

    return rows.mapNotNull { row ->
        val terms = listOfNotNull(row.nameHe, row.nameEn) + row.aliasesHe + row.aliasesEn
        val scores = terms.flatMap { term ->
            searchVariants(term).mapNotNull { variant ->
                queryVariants.mapNotNull { queryVariant -> matchScore(variant, queryVariant) }
            }.flatten()
        }
        val score = scores.minOrNull() ?: return@mapNotNull null
        score to row
    }
        .sortedWith(
            compareBy<Pair<Int, HighwayJunctionCatalogRow>> { it.first }
                .thenBy { hebrewCollator.getCollationKey(it.second.nameHe) },
        )
        .take(JUNCTION_RESULT_LIMIT)
        .map { (_, row) ->
            HighwayJunction(
                id = row.id,
                nameHe = row.nameHe,
                nameEn = row.nameEn,
                roads = row.roads,
                lat = row.lat,
                lng = row.lng,
            )
        }
}

fun searchHighwayJunctionsCached(
    rows: List<HighwayJunctionCatalogRow>,
    query: String,
): List<HighwayJunction> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    return rankHighwayJunctions(rows, trimmed)
}

fun junctionLocationLabel(junctionName: String, typedQuery: String): String {
    val suffix = splitJunctionDirection(typedQuery).directionSuffix ?: return junctionName
    if (normalizeJunctionText(junctionName) == normalizeJunctionText(typedQuery)) return junctionName
    return "$junctionName $suffix"
}
