package com.yahpz.responder

import android.content.Context
import android.content.SharedPreferences
import com.yahpz.domain.EventDraft
import com.yahpz.domain.EventResponderDraft
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.SecondaryLead
import com.yahpz.domain.TreatedVehicleDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StashedEventDraft(
    val savedAt: Long,
    val draft: EventDraft,
)

/**
 * Local device store for in-progress new event drafts.
 * Prevents loss of typed notes, road, location, and assigned responders
 * if the app is closed, rotated, interrupted, or network is unavailable.
 */
object EventDraftStore {
    private const val PREFS = "yahpaz_event_draft"
    private const val KEY_NEW_EVENT = "new_event_draft"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var liveDraft: EventDraft? = null
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun rememberLive(draft: EventDraft) {
        liveDraft = draft
    }

    fun live(): EventDraft? = liveDraft

    fun stash(draft: EventDraft, now: Long = System.currentTimeMillis()) {
        rememberLive(draft)
        val store = prefs ?: return
        val payload = StashedEventPayload(
            savedAt = now,
            draft = draft.toPayload(),
        )
        try {
            store.edit().putString(KEY_NEW_EVENT, json.encodeToString(payload)).apply()
        } catch (_: Exception) {
            // Safe ignore
        }
    }

    fun read(maxAgeMs: Long = 24 * 60 * 60 * 1000L): StashedEventDraft? {
        val store = prefs ?: return liveDraft?.let { StashedEventDraft(System.currentTimeMillis(), it) }
        val raw = try {
            store.getString(KEY_NEW_EVENT, null)
        } catch (_: Exception) {
            null
        } ?: return liveDraft?.let { StashedEventDraft(System.currentTimeMillis(), it) }

        val parsed = try {
            json.decodeFromString<StashedEventPayload>(raw)
        } catch (_: Exception) {
            clear()
            return null
        }

        val age = System.currentTimeMillis() - parsed.savedAt
        if (age > maxAgeMs) {
            clear()
            return null
        }

        val domain = parsed.draft.toDomain()
        liveDraft = domain
        return StashedEventDraft(savedAt = parsed.savedAt, draft = domain)
    }

    fun clear() {
        liveDraft = null
        try {
            prefs?.edit()?.remove(KEY_NEW_EVENT)?.apply()
        } catch (_: Exception) {
        }
    }
}

@Serializable
private data class StashedEventPayload(
    val savedAt: Long,
    val draft: StashedEventDraftPayload,
)

@Serializable
private data class StashedEventDraftPayload(
    val eventDate: String = "",
    val policeEventId: String = "",
    val patrolCallsign: String = "",
    val patrolCallsignPrefix: String = "",
    val patrolCallsignNumber: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val eventTypeId: String = "",
    val roadId: String = "",
    val districtId: String = "",
    val location: String = "",
    val locationPlaceId: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationPinSource: String? = null,
    val locationPinnedAt: String? = null,
    val locationPinnedBy: String? = null,
    val station: String = "",
    val notes: String = "",
    val isCancelled: Boolean = false,
    val busLane: Boolean = false,
    val shiftLeadId: String = "",
    val responders: List<StashedEventResponderPayload> = emptyList(),
    val secondaryLeads: List<StashedSecondaryLeadPayload> = emptyList(),
)

@Serializable
private data class StashedEventResponderPayload(
    val responderId: String,
    val assignmentId: String = "",
    val totalKm: String = "",
    val emergencyMeans: Boolean = false,
    val hasVehicle: Boolean = true,
    val status: String = "pending",
    val treated: List<StashedTreatedVehiclePayload> = emptyList(),
)

@Serializable
private data class StashedTreatedVehiclePayload(
    val vehicleKindId: String,
    val quantity: Int,
)

@Serializable
private data class StashedSecondaryLeadPayload(
    val userId: String,
    val locked: Boolean = false,
    val fullName: String = "",
    val callsign: String = "",
    val addedAt: String? = null,
)

private fun EventDraft.toPayload() = StashedEventDraftPayload(
    eventDate = eventDate,
    policeEventId = policeEventId,
    patrolCallsign = patrolCallsign,
    patrolCallsignPrefix = patrolCallsignPrefix,
    patrolCallsignNumber = patrolCallsignNumber,
    startTime = startTime,
    endTime = endTime,
    eventTypeId = eventTypeId,
    roadId = roadId,
    districtId = districtId,
    location = location,
    locationPlaceId = locationPlaceId,
    locationLat = locationLat,
    locationLng = locationLng,
    locationPinSource = locationPinSource,
    locationPinnedAt = locationPinnedAt,
    locationPinnedBy = locationPinnedBy,
    station = station,
    notes = notes,
    isCancelled = isCancelled,
    busLane = busLane,
    shiftLeadId = shiftLeadId,
    responders = responders.map {
        StashedEventResponderPayload(
            responderId = it.responderId,
            assignmentId = it.assignmentId,
            totalKm = it.totalKm,
            emergencyMeans = it.emergencyMeans,
            hasVehicle = it.hasVehicle,
            status = it.status.raw,
            treated = it.treated.map { tv -> StashedTreatedVehiclePayload(tv.vehicleKindId, tv.quantity) },
        )
    },
    secondaryLeads = secondaryLeads.map {
        StashedSecondaryLeadPayload(
            userId = it.userId,
            locked = it.locked,
            fullName = it.fullName,
            callsign = it.callsign,
            addedAt = it.addedAt,
        )
    },
)

private fun StashedEventDraftPayload.toDomain() = EventDraft(
    eventDate = eventDate,
    policeEventId = policeEventId,
    patrolCallsign = patrolCallsign,
    patrolCallsignPrefix = patrolCallsignPrefix,
    patrolCallsignNumber = patrolCallsignNumber,
    startTime = startTime,
    endTime = endTime,
    eventTypeId = eventTypeId,
    roadId = roadId,
    districtId = districtId,
    location = location,
    locationPlaceId = locationPlaceId,
    locationLat = locationLat,
    locationLng = locationLng,
    locationPinSource = locationPinSource,
    locationPinnedAt = locationPinnedAt,
    locationPinnedBy = locationPinnedBy,
    station = station,
    notes = notes,
    isCancelled = isCancelled,
    busLane = busLane,
    shiftLeadId = shiftLeadId,
    responders = responders.map {
        EventResponderDraft(
            responderId = it.responderId,
            assignmentId = it.assignmentId,
            totalKm = it.totalKm,
            emergencyMeans = it.emergencyMeans,
            hasVehicle = it.hasVehicle,
            status = ParticipationStatus.fromRaw(it.status),
            treated = it.treated.map { tv -> TreatedVehicleDraft(tv.vehicleKindId, tv.quantity) },
        )
    },
    secondaryLeads = secondaryLeads.map {
        SecondaryLead(
            userId = it.userId,
            locked = it.locked,
            fullName = it.fullName,
            callsign = it.callsign,
            addedAt = it.addedAt,
        )
    },
)
