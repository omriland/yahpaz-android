package com.yahpz.domain

/**
 * אירועים כפולים. Mirrors the web `duplicateEventsReport.ts`: the same responder logged
 * on two different events, same day, same location, started within half an hour.
 * Matches are transitive, so a triple collapses into one cluster.
 */
const val DUPLICATE_TIME_WINDOW_MINUTES = 30

private const val WINDOW_MS = DUPLICATE_TIME_WINDOW_MINUTES * 60 * 1000L

data class DuplicateParticipation(
    val eventId: String,
    val responderId: String,
    val eventDate: String,
    val location: String? = null,
    val startedAt: String? = null,
    val isCancelled: Boolean = false,
    val policeEventId: String? = null,
    val eventTypeName: String? = null,
    val roadName: String? = null,
    val name: String? = null,
    val callsign: String? = null,
)

data class DuplicateCluster(
    val id: String,
    val sizeLabel: String,
    val eventDate: String,
    val members: List<DuplicateParticipation>,
)

private fun normalizedLocation(location: String?): String? =
    location?.trim()?.takeIf { it.isNotEmpty() }

/** Epoch millis off a wall `timestamp`, read as-is because both sides share the zone. */
private fun startedMillis(value: String?): Long? {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val date = raw.take(10).split("-").mapNotNull { it.toIntOrNull() }
    if (date.size != 3) return null
    val time = formatTime(raw)?.split(":")?.mapNotNull { it.toIntOrNull() } ?: return null
    if (time.size != 2) return null
    val days = java.time.LocalDate.of(date[0], date[1], date[2]).toEpochDay()
    return ((days * 24L + time[0]) * 60L + time[1]) * 60_000L
}

private fun matches(left: DuplicateParticipation, right: DuplicateParticipation): Boolean {
    if (left.eventId == right.eventId) return false
    if (left.responderId != right.responderId) return false
    if (left.eventDate != right.eventDate) return false
    val leftPlace = normalizedLocation(left.location) ?: return false
    val rightPlace = normalizedLocation(right.location) ?: return false
    if (leftPlace != rightPlace) return false
    val leftStart = startedMillis(left.startedAt) ?: return false
    val rightStart = startedMillis(right.startedAt) ?: return false
    return kotlin.math.abs(leftStart - rightStart) <= WINDOW_MS
}

fun buildDuplicateClusters(sources: List<DuplicateParticipation>): List<DuplicateCluster> {
    val parent = IntArray(sources.size) { it }

    fun find(index: Int): Int {
        var root = index
        while (parent[root] != root) root = parent[root]
        var walk = index
        while (parent[walk] != root) {
            val next = parent[walk]
            parent[walk] = root
            walk = next
        }
        return root
    }

    for (i in sources.indices) {
        for (j in i + 1 until sources.size) {
            if (!matches(sources[i], sources[j])) continue
            val left = find(i)
            val right = find(j)
            if (left != right) parent[left] = right
        }
    }

    val groups = sources.indices.groupBy { find(it) }
    return groups.values
        .filter { it.size >= 2 }
        .map { indices ->
            val members = indices.map { sources[it] }.sortedBy { it.startedAt.orEmpty() }
            DuplicateCluster(
                id = members.map { it.eventId }.sorted().joinToString(":"),
                sizeLabel = if (members.size >= 3) "משולש" else "כפול",
                eventDate = members.first().eventDate,
                members = members,
            )
        }
        .sortedWith(
            compareByDescending<DuplicateCluster> { it.eventDate }
                .thenByDescending { it.members.size },
        )
}

fun duplicateEventsReportRows(clusters: List<DuplicateCluster>): List<ReportRow> =
    clusters.flatMap { cluster ->
        cluster.members.map { member ->
            val place = placeDisplay(member.roadName, member.location)
            val responder = personDisplay(member.name, member.callsign)
            ReportRow(
                id = "${cluster.id}:${member.eventId}:${member.responderId}",
                eventId = member.eventId,
                title = responder,
                subtitle = listOfNotNull(
                    formatDate(member.eventDate),
                    formatTime(member.startedAt),
                    member.eventTypeName?.trim()?.takeIf { it.isNotEmpty() },
                ).joinToString(" · "),
                detail = listOfNotNull(
                    place.takeIf { it.isNotEmpty() },
                    policeEventLabel(member.policeEventId, member.isCancelled).takeIf { it != "—" },
                ).joinToString(" · ").takeIf { it.isNotEmpty() },
                stampLabel = cluster.sizeLabel,
                stampTone = StampTone.PENDING,
                searchText = listOf(responder, member.policeEventId.orEmpty(), place).joinToString(" "),
            )
        }
    }
