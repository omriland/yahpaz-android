package com.yahpz.responder

import android.content.Context
import android.content.SharedPreferences
import com.yahpz.domain.FILL_DRAFT_STASH_SCOPE
import com.yahpz.domain.ResponderFillDraft
import com.yahpz.domain.TreatedPlate
import com.yahpz.domain.fillDraftKey
import com.yahpz.domain.isFillDraftStashFresh
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StashedResponderFill(
    val savedAt: Long,
    val draft: ResponderFillDraft,
)

/**
 * Device-local mirror of an in-progress fill draft.
 *
 * Same floor as web `fillDraftStash`: typed מלל lives only in RAM until an
 * explicit save, so a photo picker, process death, or Back must not wipe it.
 */
object FillDraftStore {
    private const val PREFS = "yahpaz_fill_draft"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val live = mutableMapOf<String, ResponderFillDraft>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun rememberLive(assignmentId: String, draft: ResponderFillDraft) {
        live[assignmentId] = draft
    }

    fun live(assignmentId: String): ResponderFillDraft? = live[assignmentId]

    fun stash(assignmentId: String, draft: ResponderFillDraft, now: Long) {
        rememberLive(assignmentId, draft)
        val store = prefs ?: return
        val payload = StashedFillDraftPayload(
            savedAt = now,
            draft = draft.toPayload(),
        )
        try {
            store.edit().putString(key(assignmentId), json.encodeToString(payload)).apply()
        } catch (_: Exception) {
            // A full quota must never break the form the user is typing into.
        }
    }

    fun read(assignmentId: String, now: Long): StashedResponderFill? {
        val store = prefs ?: return null
        val raw = try {
            store.getString(key(assignmentId), null)
        } catch (_: Exception) {
            return null
        } ?: return null
        val parsed = try {
            json.decodeFromString<StashedFillDraftPayload>(raw)
        } catch (_: Exception) {
            clear(assignmentId)
            return null
        }
        if (!isFillDraftStashFresh(parsed.savedAt, now)) {
            clear(assignmentId)
            return null
        }
        val draft = parsed.draft.toDomain()
        if (live[assignmentId] == null) rememberLive(assignmentId, draft)
        return StashedResponderFill(savedAt = parsed.savedAt, draft = draft)
    }

    fun clear(assignmentId: String) {
        live.remove(assignmentId)
        try {
            prefs?.edit()?.remove(key(assignmentId))?.apply()
        } catch (_: Exception) {
            // Nothing to do; a stale key expires on its own.
        }
    }

    private fun key(assignmentId: String): String =
        fillDraftKey(FILL_DRAFT_STASH_SCOPE, assignmentId)
}

@Serializable
private data class StashedFillDraftPayload(
    val savedAt: Long,
    val draft: StashedResponderFillDraft,
)

@Serializable
private data class StashedResponderFillDraft(
    val vehiclePlate: String = "",
    val odometerStart: String = "",
    val odometerEnd: String = "",
    val route: String = "",
    val treatmentDetail: String = "",
    val treatmentNotes: String = "",
    val treatedPlates: List<StashedTreatedPlate> = emptyList(),
    val treatedPlatePending: String = "",
)

@Serializable
private data class StashedTreatedPlate(
    val plateNumber: String,
    val model: String? = null,
    val color: String? = null,
    val leftWhere: String? = null,
    val manufacturer: String? = null,
    val logoSlug: String? = null,
)

private fun ResponderFillDraft.toPayload() = StashedResponderFillDraft(
    vehiclePlate = vehiclePlate,
    odometerStart = odometerStart,
    odometerEnd = odometerEnd,
    route = route,
    treatmentDetail = treatmentDetail,
    treatmentNotes = treatmentNotes,
    treatedPlates = treatedPlates.map {
        StashedTreatedPlate(
            plateNumber = it.plateNumber,
            model = it.model,
            color = it.color,
            leftWhere = it.leftWhere,
            manufacturer = it.manufacturer,
            logoSlug = it.logoSlug,
        )
    },
    treatedPlatePending = treatedPlatePending,
)

private fun StashedResponderFillDraft.toDomain() = ResponderFillDraft(
    vehiclePlate = vehiclePlate,
    odometerStart = odometerStart,
    odometerEnd = odometerEnd,
    route = route,
    treatmentDetail = treatmentDetail,
    treatmentNotes = treatmentNotes,
    treatedPlates = treatedPlates.map {
        TreatedPlate(
            plateNumber = it.plateNumber,
            model = it.model,
            color = it.color,
            leftWhere = it.leftWhere,
            manufacturer = it.manufacturer,
            logoSlug = it.logoSlug,
        )
    },
    treatedPlatePending = treatedPlatePending,
)
