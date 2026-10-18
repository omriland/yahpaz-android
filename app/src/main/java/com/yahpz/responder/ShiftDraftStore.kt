package com.yahpz.responder

import android.content.Context
import android.content.SharedPreferences
import com.yahpz.domain.ShiftDraft
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class StashedShiftDraft(
    val savedAt: Long,
    val draft: ShiftDraft,
)

/**
 * Local device store for in-progress shift creation drafts.
 * Prevents loss of entered shift data on phone calls, screen rotation, or app backgrounding.
 */
object ShiftDraftStore {
    private const val PREFS = "yahpaz_shift_draft"
    private const val KEY_NEW_SHIFT = "new_shift_draft"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var liveDraft: ShiftDraft? = null
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun rememberLive(draft: ShiftDraft) {
        liveDraft = draft
    }

    fun live(): ShiftDraft? = liveDraft

    fun stash(draft: ShiftDraft, now: Long = System.currentTimeMillis()) {
        rememberLive(draft)
        val store = prefs ?: return
        val payload = StashedShiftPayload(
            savedAt = now,
            draft = draft.toPayload(),
        )
        try {
            store.edit().putString(KEY_NEW_SHIFT, json.encodeToString(payload)).apply()
        } catch (_: Exception) {
            // Quota or IO exception must never crash the UI
        }
    }

    fun read(maxAgeMs: Long = 24 * 60 * 60 * 1000L): StashedShiftDraft? {
        val store = prefs ?: return liveDraft?.let { StashedShiftDraft(System.currentTimeMillis(), it) }
        val raw = try {
            store.getString(KEY_NEW_SHIFT, null)
        } catch (_: Exception) {
            null
        } ?: return liveDraft?.let { StashedShiftDraft(System.currentTimeMillis(), it) }

        val parsed = try {
            json.decodeFromString<StashedShiftPayload>(raw)
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
        return StashedShiftDraft(savedAt = parsed.savedAt, draft = domain)
    }

    fun clear() {
        liveDraft = null
        try {
            prefs?.edit()?.remove(KEY_NEW_SHIFT)?.apply()
        } catch (_: Exception) {
        }
    }
}

@Serializable
private data class StashedShiftPayload(
    val savedAt: Long,
    val draft: StashedShiftDraftPayload,
)

@Serializable
private data class StashedShiftDraftPayload(
    val shiftDate: String = "",
    val shiftKind: String = "",
    val vehicleType: String = "",
    val notes: String = "",
    val responderIds: List<String> = emptyList(),
    val personalVehicleId: String? = null,
)

private fun ShiftDraft.toPayload() = StashedShiftDraftPayload(
    shiftDate = shiftDate,
    shiftKind = shiftKind,
    vehicleType = vehicleType,
    notes = notes,
    responderIds = responderIds,
    personalVehicleId = personalVehicleId,
)

private fun StashedShiftDraftPayload.toDomain() = ShiftDraft(
    shiftDate = shiftDate,
    shiftKind = shiftKind,
    vehicleType = vehicleType,
    notes = notes,
    responderIds = responderIds,
    personalVehicleId = personalVehicleId,
)
