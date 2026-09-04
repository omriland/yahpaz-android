package com.yahpz.responder

import com.yahpz.domain.AssignableProfile
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.AvailabilityWrite
import com.yahpz.domain.BROADCAST_SEND_FAILED
import com.yahpz.domain.BroadcastCandidate
import com.yahpz.domain.BroadcastDraft
import com.yahpz.domain.BroadcastLogEntry
import com.yahpz.domain.BroadcastSendResult
import com.yahpz.domain.CLOSED_LIST_IN_USE
import com.yahpz.domain.CLOSED_LIST_IN_USE_CHECK_FAILED
import com.yahpz.domain.ClosedListItem
import com.yahpz.domain.ClosedListKey
import com.yahpz.domain.ClosedListMutationResult
import com.yahpz.domain.COCKPIT_WINDOW_MS
import com.yahpz.domain.EVENT_DELETE_FAILED
import com.yahpz.domain.EVENT_DELETE_OTHER_LEAD
import com.yahpz.domain.AUTO_MY_ACTIVE_STATUSES
import com.yahpz.domain.MY_ACTIVE_PREF_FAILED
import com.yahpz.domain.EVENT_DRAFT_DATE_ERROR
import com.yahpz.domain.EVENT_DRAFT_FORM_ERROR
import com.yahpz.domain.EVENT_DRAFT_SAVE_FAILED
import com.yahpz.domain.EVENT_SELF_ASSIGN_ON_CREATE_ERROR
import com.yahpz.domain.EventDraft
import com.yahpz.domain.EventResponderDraft
import com.yahpz.domain.EventStatus
import com.yahpz.domain.FillMode
import com.yahpz.domain.FillWriteGate
import com.yahpz.domain.FuelRefundCreditInput
import com.yahpz.domain.FuelRefundParticipationInput
import com.yahpz.domain.FuelRefundProfileInput
import com.yahpz.domain.FuelRefundRow
import com.yahpz.domain.COPY_INVITE_FAILED
import com.yahpz.domain.DELETE_USER_FAILED
import com.yahpz.domain.DUPLICATE_PLATE_ERROR
import com.yahpz.domain.INVITE_IDENTITY_ERROR
import com.yahpz.domain.INVITE_SAVE_FAILED
import com.yahpz.domain.IMPERSONATION_ALREADY
import com.yahpz.domain.IMPERSONATION_OPEN_FAILED
import com.yahpz.domain.IMPERSONATION_NONE
import com.yahpz.domain.IMPERSONATION_RESTORE_FAILED
import com.yahpz.domain.ImpersonationTarget
import com.yahpz.domain.InviteDraft
import com.yahpz.domain.OTP_SET_FAILED
import com.yahpz.domain.RESEND_INVITE_FAILED
import com.yahpz.domain.SAVE_ROLES_FAILED
import com.yahpz.domain.SAVE_USER_FAILED
import com.yahpz.domain.SAVE_VEHICLES_FAILED
import com.yahpz.domain.SET_ACTIVE_FAILED
import com.yahpz.domain.VEHICLE_ARCHIVE_FAILED
import com.yahpz.domain.VEHICLE_DELETE_FAILED
import com.yahpz.domain.VEHICLE_UNARCHIVE_FAILED
import com.yahpz.domain.KM_DISCREPANCY_ALIGNED
import com.yahpz.domain.KM_DISCREPANCY_APPLY_FAILED
import com.yahpz.domain.LeadKmReplacement
import com.yahpz.domain.LookupOption
import com.yahpz.domain.OpenDocRow
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.ReportKindId
import com.yahpz.domain.ReportRow
import com.yahpz.domain.ResponderFillDraft
import com.yahpz.domain.SET_ACTIVE_FAILED
import com.yahpz.domain.SHIFT_DRAFT_DATE_ERROR
import com.yahpz.domain.SHIFT_DRAFT_FORM_ERROR
import com.yahpz.domain.filterCockpitEvents
import com.yahpz.domain.SHIFT_DRAFT_SAVE_FAILED
import com.yahpz.domain.SYSTEM_DISTRICT_LOCKED_ERROR
import com.yahpz.domain.ShiftDraft
import com.yahpz.domain.buildAvailabilityWrite
import com.yahpz.domain.canImpersonateTarget
import com.yahpz.domain.buildDuplicateClusters
import com.yahpz.domain.buildEventsByResponderRows
import com.yahpz.domain.buildFuelQuarterRows
import com.yahpz.domain.buildFuelRefundRows
import com.yahpz.domain.buildKmDiscrepancyRows
import com.yahpz.domain.buildKmExceptionRows
import com.yahpz.domain.buildOpenDocRows
import com.yahpz.domain.canToggleEventCancelled
import com.yahpz.domain.createTimeCreatorSecondary
import com.yahpz.domain.forPersistCompare
import com.yahpz.domain.SecondaryLead
import com.yahpz.domain.closedListMeta
import com.yahpz.domain.closedListNameError
import com.yahpz.domain.deriveEventStatusAfterParticipation
import com.yahpz.domain.duplicateEventsReportRows
import com.yahpz.domain.createIncludesSelfAssign
import com.yahpz.domain.digitsOnly
import com.yahpz.domain.ownResumableEventId
import com.yahpz.domain.SameDayPoliceEventRow
import com.yahpz.domain.deriveEventStatusFromDraft
import com.yahpz.domain.eventDraftStatus
import com.yahpz.domain.eventsByResponderReportRows
import com.yahpz.domain.isOvernightEnd
import com.yahpz.domain.leadKmForSave
import com.yahpz.domain.stationForSave
import com.yahpz.domain.FillReadyNextRow
import com.yahpz.domain.FillReadyPreviousRow
import com.yahpz.domain.fillReadyNotifyIds
import com.yahpz.domain.wallTimestamp
import com.yahpz.domain.FuelQuarterParticipationInput
import com.yahpz.domain.FuelQuarterProfileInput
import com.yahpz.domain.FuelQuarterSavedDistribution
import com.yahpz.domain.fuelRefundReportRows
import com.yahpz.domain.gateResponderFillWrite
import com.yahpz.domain.isAdmin
import com.yahpz.domain.isSystemClosedListItem
import com.yahpz.domain.israelToday
import com.yahpz.domain.kmDiscrepancyReportRows
import com.yahpz.domain.kmExceptionReportRows
import com.yahpz.domain.mapClosedListDeleteError
import com.yahpz.domain.mapClosedListWriteError
import com.yahpz.domain.EventMedia
import com.yahpz.domain.EventMediaPlateOption
import com.yahpz.domain.captionError
import com.yahpz.domain.eventMediaStoragePath
import com.yahpz.domain.mapEventMediaError
import com.yahpz.domain.mapTreatedPlateRows
import com.yahpz.domain.mergeMediaPlates
import com.yahpz.domain.parseEventMediaTakenWhen
import com.yahpz.domain.uniquePlateIds
import com.yahpz.domain.EVENT_MEDIA_NETWORK
import com.yahpz.domain.FEEDBACK_ATTACH_TYPE_ERROR
import com.yahpz.domain.FEEDBACK_ATTACH_UNAVAILABLE
import com.yahpz.domain.FEEDBACK_AUDIO_MAX_BYTES
import com.yahpz.domain.FEEDBACK_AUDIO_SIZE_ERROR
import com.yahpz.domain.FEEDBACK_NETWORK
import com.yahpz.domain.FeedbackPickedMeta
import com.yahpz.domain.addFeedbackAttachments
import com.yahpz.domain.feedbackAttachmentStoragePath
import com.yahpz.domain.feedbackStoragePath
import com.yahpz.domain.feedbackSubmitError
import com.yahpz.domain.isMissingFeedbackAttachmentsColumn
import com.yahpz.domain.normalizeFeedbackAttachmentMime
import com.yahpz.domain.normalizeFeedbackAudioMime
import com.yahpz.domain.sanitizeFeedbackAttachmentName
import com.yahpz.domain.normalizeLoginEmail
import com.yahpz.domain.normalizeLoginSecret
import com.yahpz.domain.EventMediaTakenWhen
import com.yahpz.domain.needsBroadcastSubject
import com.yahpz.domain.normalizeReturnDate
import com.yahpz.domain.openDocReportRows
import com.yahpz.domain.parsedOdometer
import com.yahpz.domain.passwordStrengthError
import com.yahpz.domain.compareAdminUsers
import com.yahpz.domain.findDuplicatePlate
import com.yahpz.domain.compareAdminUsers
import com.yahpz.domain.findDuplicatePlate
import com.yahpz.domain.syncUserRolesDiff
import com.yahpz.domain.phoneDigits
import com.yahpz.domain.ProfileVehicle
import com.yahpz.domain.plateDigits
import com.yahpz.domain.plateNumberForSave
import com.yahpz.domain.syncUserRolesDiff
import com.yahpz.domain.validateInviteDraft
import com.yahpz.domain.quarterLocalDateRange
import com.yahpz.domain.quarterMonthLabels
import com.yahpz.domain.resolveLeadKmReplacement
import com.yahpz.domain.VehicleRowInput
import com.yahpz.domain.VehicleFieldsError
import com.yahpz.domain.VehicleFieldsOk
import com.yahpz.domain.managedProfileVehicles
import com.yahpz.domain.vehicleFieldsForSave
import com.yahpz.domain.plateDigits
import com.yahpz.domain.plateNumberForSave
import com.yahpz.domain.sortByRoadName
import com.yahpz.domain.sortLookupsBySortOrder
import com.yahpz.domain.validateBroadcastDraft
import com.yahpz.domain.validateEventDraft
import com.yahpz.domain.validateEventDraftPartial
import com.yahpz.domain.validateInviteDraft
import com.yahpz.domain.validateResponderFillDraft
import com.yahpz.domain.validateShiftDraft
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class APIException(override val message: String) : Exception(message)

sealed class EventMediaWriteResult {
    data class Uploaded(val media: EventMedia) : EventMediaWriteResult()
    data object Done : EventMediaWriteResult()
    data class Error(val message: String) : EventMediaWriteResult()
}

private val edgeErrorPattern = Regex("\"error\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")

/** Recover the Hebrew `error` string an edge function returned, if the raw body carries one. */
internal fun edgeErrorMessage(raw: String?, fallback: String): String {
    val match = edgeErrorPattern.find(raw.orEmpty()) ?: return fallback
    val message = match.groupValues[1]
        .replace("\\\"", "\"")
        .replace("\\n", " ")
        .replace("\\\\", "\\")
        .trim()
    return message.ifEmpty { fallback }
}

object YahpazAPI {
    private const val EVENT_SECONDARY_LEADS_EMBED =
        "secondary_leads:event_secondary_leads(user_id, locked, added_at, profile:profiles!event_secondary_leads_user_id_fkey(full_name, callsign))"

    private val eventListSelect = """
        id, event_date, police_event_id, patrol_callsign, location, status, is_cancelled, bus_lane, origin, shift_lead_id, shift_id,
        frozen_over_60km, frozen_suspicious_duplicate,
        district:districts(name),
        event_type:event_types(name),
        road:roads(name),
        shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
        $EVENT_SECONDARY_LEADS_EMBED,
        shift:shifts!events_shift_id_fkey(
          shift_date, shift_kind, vehicle_type,
          personal_vehicle:vehicles!shifts_personal_vehicle_id_fkey(plate_number)
        ),
        responders:event_responders(id, responder_id, status, fill_completable_at, total_km, started_at, ended_at, profile:profiles(full_name, callsign))
    """.trimIndent()

    private val cockpitSelect = """
        id,
        created_at,
        police_event_id,
        status,
        is_cancelled,
        location,
        location_lat,
        location_lng,
        frozen_over_60km,
        frozen_suspicious_duplicate,
        event_type:event_types(name),
        road:roads(name),
        shift_lead_id,
        shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
        $EVENT_SECONDARY_LEADS_EMBED,
        responders:event_responders(id, responder_id, status, ended_at)
    """.trimIndent()

    private val fillSelect = """
        id, status, event_date, police_event_id, location, is_cancelled,
        event_type:event_types(name),
        road:roads(name),
        shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
        $EVENT_SECONDARY_LEADS_EMBED,
        responders:event_responders(
          id, responder_id, vehicle_plate, odometer_start, odometer_end, total_km,
          route, treatment_detail, treatment_notes, status, updated_at, ended_at,
          treated_plates:event_treated_plates(plate_number, model, color, left_where, manufacturer, logo_slug, sort_order)
        )
    """.trimIndent()

    private val shiftListSelect = """
        id, shift_date, shift_kind, vehicle_type, status, odometer_start, odometer_end,
        personal_vehicle:vehicles!shifts_personal_vehicle_id_fkey(plate_number),
        shift_lead:profiles!shifts_shift_lead_id_fkey(full_name, callsign),
        responders:shift_responders(id, responder_id),
        born_events:events!events_shift_id_fkey(
          id, event_date, police_event_id, status,
          event_type:event_types(name)
        )
    """.trimIndent()

    private val reportEventSelect = """
        id, event_date, is_cancelled, police_event_id, location,
        event_type:event_types(name),
        district:districts(name),
        road:roads(name),
        shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
        responders:event_responders(
          responder_id, total_km,
          profile:profiles(full_name, callsign)
        )
    """.trimIndent()

    private val kmDiscrepancySelect = """
        id, event_date, is_cancelled, police_event_id, location,
        road:roads(name),
        shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
        responders:event_responders(
          id, status, total_km, odometer_start, odometer_end,
          profile:profiles(full_name, callsign)
        )
    """.trimIndent()

    private val duplicateEventSelect = """
        id, event_date, is_cancelled, police_event_id, location,
        event_type:event_types(name),
        road:roads(name),
        responders:event_responders(
          responder_id, started_at,
          profile:profiles(full_name, callsign)
        )
    """.trimIndent()

    private val openDocSelect = """
        id, event_date, status, is_cancelled, police_event_id, location, shift_lead_id,
        road:roads(name),
        shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
        responders:event_responders(
          responder_id, status,
          profile:profiles(full_name, callsign)
        )
    """.trimIndent()

    val client = createSupabaseClient(
        supabaseUrl = AppConfig.supabaseUrl,
        supabaseKey = AppConfig.supabaseAnonKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Functions)
        install(Storage)
    }

    suspend fun sessionUserId(): String? {
        client.auth.awaitInitialization()
        return client.auth.currentSessionOrNull()?.user?.id
    }

    suspend fun signIn(email: String, password: String): String? {
        val loginEmail = normalizeLoginEmail(email)
        val loginPassword = normalizeLoginSecret(password)
        return try {
            client.auth.signInWith(Email) {
                this.email = loginEmail
                this.password = loginPassword
            }
            null
        } catch (error: Exception) {
            val message = error.message.orEmpty().lowercase()
            if (message.contains("invalid login") || message.contains("invalid_credentials")) {
                "הדוא״ל או הסיסמה שגויים. נסו שוב."
            } else {
                "הכניסה נכשלה. בדקו את החיבור ונסו שוב."
            }
        }
    }

    suspend fun signOut() {
        ViewAsStore.clearAll()
        runCatching { client.auth.signOut() }
    }

    suspend fun startImpersonation(targetUserId: String): String? {
        if (ViewAsStore.readImpersonation() != null) return IMPERSONATION_ALREADY
        ViewAsStore.clearRolePreview()
        val session = client.auth.currentSessionOrNull() ?: return "יש להתחבר מחדש."
        val actorId = session.user?.id ?: return "יש להתחבר מחדש."
        val response = invokeEdge<ImpersonateCall, AdminUsersResponse>(
            "admin-users",
            ImpersonateCall(action = "impersonate", targetUserId = targetUserId),
            IMPERSONATION_OPEN_FAILED,
        )
        val payload = response.getOrElse { return it.message ?: IMPERSONATION_OPEN_FAILED }
        if (payload.error != null) return payload.error
        val access = payload.accessToken
        val refresh = payload.refreshToken
        val target = payload.target
        if (access.isNullOrEmpty() || refresh.isNullOrEmpty() || target == null) {
            return IMPERSONATION_OPEN_FAILED
        }
        ViewAsStore.writeImpersonation(
            ImpersonationStash(
                actorAccessToken = session.accessToken,
                actorRefreshToken = session.refreshToken,
                actorUserId = actorId,
                targetUserId = target.id,
                targetFullName = target.fullName,
                targetCallsign = target.callsign,
            ),
        )
        return try {
            client.auth.importAuthToken(access, refresh, retrieveUser = true)
            null
        } catch (_: Exception) {
            ViewAsStore.clearImpersonation()
            IMPERSONATION_OPEN_FAILED
        }
    }

    suspend fun stopImpersonation(): String? {
        val stash = ViewAsStore.readImpersonation() ?: return IMPERSONATION_NONE
        return try {
            client.auth.importAuthToken(stash.actorAccessToken, stash.actorRefreshToken, retrieveUser = true)
            ViewAsStore.clearImpersonation()
            invokeEdge<ImpersonateCall, AdminUsersResponse>(
                "admin-users",
                ImpersonateCall(action = "stop_impersonation", targetUserId = stash.targetUserId),
                IMPERSONATION_RESTORE_FAILED,
            )
            null
        } catch (_: Exception) {
            ViewAsStore.clearImpersonation()
            IMPERSONATION_RESTORE_FAILED
        }
    }

    suspend fun fetchImpersonationCandidates(actorUserId: String): List<AdminUserListItem> =
        fetchAdminUsers().filter { row ->
            canImpersonateTarget(
                actorUserId,
                ImpersonationTarget(id = row.id, active = row.active, roles = row.roles),
            )
        }

    suspend fun requestPasswordReset(email: String): String? {
        return try {
            client.auth.resetPasswordForEmail(normalizeLoginEmail(email), AppConfig.passwordResetRedirect)
            null
        } catch (_: Exception) {
            "שליחת הקישור נכשלה. בדקו את החיבור ונסו שוב."
        }
    }

    suspend fun updatePassword(password: String): String? {
        passwordStrengthError(password)?.let { return it }
        return try {
            client.auth.updateUser { this.password = password }
            runCatching { client.postgrest.rpc("clear_must_change_password") }
            sessionUserId()?.let { userId ->
                runCatching {
                    client.from("profiles").update(
                        InviteClearRow(updatedAt = Instant.now().toString()),
                    ) {
                        filter { eq("id", userId) }
                    }
                }
            }
            null
        } catch (_: Exception) {
            "שמירת הסיסמה נכשלה. נסו שוב."
        }
    }

    suspend fun loadProfile(): Pair<ProfileRecord, List<String>> {
        val userId = sessionUserId() ?: throw APIException("יש להתחבר מחדש.")
        val profile = client.from("profiles").select(
            Columns.raw(
                "id, full_name, email, callsign, phone, active, must_change_password, availability, available_from, lifetime_event_count, lifetime_km, lifetime_stats_updated_at",
            ),
        ) {
            filter { eq("id", userId) }
        }.decodeSingle<ProfileRecord>()
        val roles = client.from("user_roles").select(Columns.raw("role")) {
            filter { eq("user_id", userId) }
        }.decodeList<RoleRow>().map { it.role }
        if (!profile.active) {
            signOut()
            throw APIException("החשבון אינו פעיל. פנו למנהל המערכת.")
        }
        return profile to roles
    }

    suspend fun reportAndroidSession(versionCode: Int, versionName: String) {
        client.postgrest.rpc(
            "report_android_session",
            ReportAndroidSessionCall(versionCode = versionCode, versionName = versionName),
        )
    }

    suspend fun fetchMyEvents(): List<EventListItem> {
        val userId = sessionUserId() ?: return emptyList()
        val ids = client.from("event_responders").select(Columns.raw("event_id")) {
            filter { eq("responder_id", userId) }
        }.decodeList<AssignmentIdRow>().map { it.eventId }
        if (ids.isEmpty()) return emptyList()
        return fetchByIds(ids, "events", eventListSelect)
    }

    suspend fun fetchMyShifts(): List<ShiftListItem> {
        val userId = sessionUserId() ?: return emptyList()
        val ids = client.from("shift_responders").select(Columns.raw("shift_id")) {
            filter { eq("responder_id", userId) }
        }.decodeList<ShiftAssignmentIdRow>().map { it.shiftId }
        if (ids.isEmpty()) return emptyList()
        return fetchByIds(ids, "shifts", shiftListSelect)
    }

    suspend fun fetchUnitContacts(): List<UnitContact> =
        client.postgrest.rpc("list_unit_contacts").decodeList<UnitContact>()

    suspend fun fetchUnitEvents(limit: Int = 80, shiftLeadId: String? = null): List<EventListItem> =
        client.from("events").select(Columns.raw(eventListSelect)) {
            if (shiftLeadId != null) {
                filter { eq("shift_lead_id", shiftLeadId) }
            }
            order("event_date", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<EventListItem>()

    suspend fun fetchMyActiveUnitEvents(): List<EventListItem> {
        val userId = sessionUserId() ?: return emptyList()
        return client.from("events").select(Columns.raw(eventListSelect)) {
            filter {
                eq("shift_lead_id", userId)
                eq("is_cancelled", false)
                isIn("status", AUTO_MY_ACTIVE_STATUSES.map { it.raw })
            }
            order("event_date", Order.DESCENDING)
        }.decodeList<EventListItem>()
    }

    suspend fun fetchMyActiveEventPrefs(): List<MyActiveEventPrefRow> {
        val userId = sessionUserId() ?: return emptyList()
        return client.from("my_active_event_prefs").select(
            Columns.raw("user_id, event_id, kind"),
        ) {
            filter { eq("user_id", userId) }
        }.decodeList<MyActiveEventPrefRow>()
    }

    suspend fun fetchUnitEventsByIds(ids: List<String>): List<EventListItem> {
        if (ids.isEmpty()) return emptyList()
        return fetchByIds(ids, "events", eventListSelect)
    }

    suspend fun addEventToMyActive(eventId: String, alreadyAuto: Boolean): String? {
        val userId = sessionUserId() ?: return MY_ACTIVE_PREF_FAILED
        return try {
            deleteMyActivePref(userId, eventId)
            if (!alreadyAuto) {
                client.from("my_active_event_prefs").insert(
                    MyActiveEventPrefWrite(userId = userId, eventId = eventId, kind = "pin"),
                )
            }
            null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            MY_ACTIVE_PREF_FAILED
        }
    }

    suspend fun removeEventFromMyActive(eventId: String, isAuto: Boolean): String? {
        val userId = sessionUserId() ?: return MY_ACTIVE_PREF_FAILED
        return try {
            deleteMyActivePref(userId, eventId)
            if (isAuto) {
                client.from("my_active_event_prefs").insert(
                    MyActiveEventPrefWrite(userId = userId, eventId = eventId, kind = "hide"),
                )
            }
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val raw = error.message.orEmpty()
            if (raw.contains("בהזנה")) {
                "לא ניתן להסיר אירוע בהזנה שאתם אחמ״ש שלו."
            } else {
                MY_ACTIVE_PREF_FAILED
            }
        }
    }

    private suspend fun deleteMyActivePref(userId: String, eventId: String) {
        client.from("my_active_event_prefs").delete {
            filter {
                eq("user_id", userId)
                eq("event_id", eventId)
            }
        }
    }

    suspend fun deleteUnitEvent(eventId: String): String? = try {
        client.from("events").delete { filter { eq("id", eventId) } }
        val stillThere = client.from("events").select(Columns.raw("id, shift_lead_id")) {
            filter { eq("id", eventId) }
            limit(1)
        }.decodeList<EventOwnerRow>()
        if (stillThere.isEmpty()) null
        else {
            val viewerId = sessionUserId()
            val ownerId = stillThere.first().shiftLeadId
            if (viewerId != null && ownerId != null && ownerId != viewerId) EVENT_DELETE_OTHER_LEAD
            else EVENT_DELETE_FAILED
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        EVENT_DELETE_FAILED
    }

    suspend fun fetchUnitEventDetailResponders(eventId: String): List<UnitEventDetailResponderRow> =
        client.from("events").select(
            Columns.raw(
                """
                responders:event_responders(
                  id, responder_id, started_at, ended_at, vehicle_plate, total_km,
                  odometer_start, odometer_end, route, treatment_detail, treatment_notes,
                  emergency_means, status,
                  profile:profiles(full_name, callsign),
                  treated:event_treated_vehicles(quantity, kind:vehicle_kinds(name)),
                  treated_plates:event_treated_plates(plate_number, model, color, left_where, manufacturer, logo_slug, sort_order)
                )
                """.trimIndent(),
            ),
        ) {
            filter { eq("id", eventId) }
        }.decodeSingle<UnitEventDetailRespondersWrap>().responders

    /** Ops cockpit reel — recent window, open statuses (in_progress/partial), not cancelled. */
    suspend fun fetchCockpitEvents(now: Instant = Instant.now()): List<CockpitEventListItem> {
        val since = now.minusMillis(COCKPIT_WINDOW_MS).toString()
        val rows = client.from("events").select(Columns.raw(cockpitSelect)) {
            filter {
                gte("created_at", since)
                eq("is_cancelled", false)
                isIn("status", listOf(EventStatus.IN_PROGRESS.raw, EventStatus.PARTIAL.raw))
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<CockpitEventListItem>()
        val kept = filterCockpitEvents(rows.map { it.asInput }, now).map { it.id }.toSet()
        return rows.filter { it.id in kept }.sortedByDescending { it.createdAt }
    }

    suspend fun fetchUnitShifts(limit: Int = 80): List<ShiftListItem> =
        client.from("shifts").select(Columns.raw(shiftListSelect)) {
            order("shift_date", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<ShiftListItem>()

    suspend fun fetchAdminUsers(): List<AdminUserListItem> {
        val profiles = client.from("profiles").select(
            Columns.raw(
                "id, full_name, email, callsign, phone, active, invite_pending, " +
                    "otp_login_enabled, otp_users_page_enabled, availability, available_from, volunteer_status",
            ),
        ) {
            order("full_name", Order.ASCENDING)
        }.decodeList<AdminProfileRow>()
        val roles = client.from("user_roles").select(Columns.raw("user_id, role"))
            .decodeList<AdminRoleRow>()
            .groupBy({ it.userId }, { it.role })
        val vehiclesByUser = client.from("vehicles").select(Columns.raw("id, user_id, plate_number, model, archived"))
            .decodeList<AdminVehicleRow>()
            .groupBy { it.userId }
        val addressesByUser = client.from("user_addresses").select(
            Columns.raw("user_id, kind, label, formatted_address"),
        ).decodeList<AdminAddressRow>().groupBy { it.userId }
        return profiles.map { row ->
            AdminUserListItem(
                id = row.id,
                fullName = row.fullName,
                email = row.email,
                callsign = row.callsign,
                phone = row.phone,
                active = row.active,
                invitePending = row.invitePending,
                otpLoginEnabled = row.otpLoginEnabled,
                otpUsersPageEnabled = row.otpUsersPageEnabled,
                availability = row.availability,
                availableFrom = row.availableFrom,
                volunteerStatus = row.volunteerStatus,
                roles = roles[row.id].orEmpty(),
                vehicles = vehiclesByUser[row.id].orEmpty().map {
                    AdminVehicleItem(
                        id = it.id,
                        plateNumber = it.plateNumber,
                        model = it.model,
                        archived = it.archived == true,
                    )
                },
                addresses = addressesByUser[row.id].orEmpty().map {
                    AdminAddressItem(
                        kind = it.kind,
                        label = it.label,
                        formattedAddress = it.formattedAddress,
                    )
                },
            )
        }.sortedWith { left, right -> compareAdminUsers(left.sortKey, right.sortKey) }
    }

    suspend fun fetchOpenDocumentation(from: String, to: String): List<OpenDocRow> {
        val userId = sessionUserId() ?: return emptyList()
        val viewerIsAdmin = isAdmin(client.from("user_roles").select(Columns.raw("role")) {
            filter { eq("user_id", userId) }
        }.decodeList<RoleRow>().map { it.role })
        val rows = client.from("events").select(Columns.raw(openDocSelect)) {
            filter {
                isIn("status", listOf(EventStatus.IN_PROGRESS.raw, EventStatus.PARTIAL.raw))
                eq("is_cancelled", false)
                gte("event_date", from)
                lte("event_date", to)
                if (!viewerIsAdmin) eq("shift_lead_id", userId)
            }
            order("event_date", Order.DESCENDING)
        }.decodeList<OpenDocEventRow>()
        return buildOpenDocRows(
            events = rows.map { it.asInput },
            from = from,
            to = to,
            viewerId = userId,
            viewerIsAdmin = viewerIsAdmin,
        )
    }

    suspend fun fetchReport(kind: ReportKindId, from: String, to: String): List<ReportRow> = when (kind) {
        ReportKindId.OPEN_DOCUMENTATION -> openDocReportRows(fetchOpenDocumentation(from, to))
        ReportKindId.EVENTS_BY_RESPONDER -> eventsByResponderReportRows(
            buildEventsByResponderRows(fetchReportEvents(from, to).map { it.asEventsByResponderInput }, from, to),
        )
        ReportKindId.KM_EXCEPTIONS -> kmExceptionReportRows(
            buildKmExceptionRows(fetchReportEvents(from, to).map { it.asKmExceptionInput }, from, to),
        )
        ReportKindId.KM_DISCREPANCY -> kmDiscrepancyReportRows(
            buildKmDiscrepancyRows(fetchKmDiscrepancyEvents(from, to).map { it.asInput }, from, to),
        )
        ReportKindId.DUPLICATE_EVENTS -> duplicateEventsReportRows(
            buildDuplicateClusters(fetchDuplicateEvents().flatMap { it.asParticipations }),
        )
        ReportKindId.FUEL_REFUND -> fuelRefundReportRows(fetchFuelRefundRows(from, to))
    }

    private suspend fun fetchKmDiscrepancyEvents(from: String, to: String): List<KmDiscrepancyEventRow> =
        client.from("events").select(Columns.raw(kmDiscrepancySelect)) {
            filter {
                gte("event_date", from)
                lte("event_date", to)
            }
            order("event_date", Order.DESCENDING)
        }.decodeList<KmDiscrepancyEventRow>()

    /** Duplicates are looked for across the whole history, like the web report. */
    private suspend fun fetchDuplicateEvents(): List<DuplicateEventRow> =
        client.from("events").select(Columns.raw(duplicateEventSelect)) {
            order("event_date", Order.DESCENDING)
        }.decodeList<DuplicateEventRow>()

    /** Overwrite the lead's `total_km` with the responder's odometer difference. */
    suspend fun applyLeadKmFromOdometer(assignmentId: String): String? {
        return try {
            val row = client.from("event_responders").select(
                Columns.raw("id, total_km, odometer_start, odometer_end"),
            ) {
                filter { eq("id", assignmentId) }
            }.decodeSingle<LeadKmRow>()
            when (val resolved = resolveLeadKmReplacement(row.totalKm, row.odometerStart, row.odometerEnd)) {
                is LeadKmReplacement.Invalid -> KM_DISCREPANCY_APPLY_FAILED
                is LeadKmReplacement.AlreadyAligned -> KM_DISCREPANCY_ALIGNED
                is LeadKmReplacement.Replace -> {
                    val updated = client.from("event_responders").update(
                        LeadKmWrite(totalKm = resolved.totalKm, updatedAt = Instant.now().toString()),
                    ) {
                        filter { eq("id", assignmentId) }
                        select(Columns.raw("id"))
                    }.decodeList<IdRow>()
                    if (updated.isEmpty()) KM_DISCREPANCY_APPLY_FAILED else null
                }
            }
        } catch (_: Exception) {
            KM_DISCREPANCY_APPLY_FAILED
        }
    }

    /**
     * Everyone the unit could reach, plus who has a registered device. The device-token
     * RPC is admin-only and optional: without it the preview simply shows no push count.
     */
    suspend fun fetchBroadcastCandidates(): List<BroadcastCandidate> {
        val profiles = client.from("profiles").select(
            Columns.raw("id, email, phone, active, invite_pending"),
        ).decodeList<BroadcastProfileRow>()
        if (profiles.isEmpty()) return emptyList()
        val roles = client.from("user_roles").select(Columns.raw("user_id, role"))
            .decodeList<AdminRoleRow>()
            .groupBy({ it.userId }, { it.role })
        val withApp = runCatching {
            client.postgrest.rpc("user_ids_with_device_tokens").decodeList<String>().toSet()
        }.getOrDefault(emptySet())
        return profiles.map { row ->
            BroadcastCandidate(
                id = row.id,
                email = row.email,
                phone = row.phone,
                roles = roles[row.id].orEmpty(),
                active = row.active,
                invitePending = row.invitePending,
                hasApp = withApp.contains(row.id),
            )
        }
    }

    suspend fun fetchBroadcastLog(limit: Int = 50): List<BroadcastLogEntry> =
        client.from("unit_broadcasts").select(
            Columns.raw(
                "id, created_at, channel, audience, subject, body, recipient_count, " +
                    "push_count, push_failed_count, " +
                    "sender:profiles!sent_by(full_name, callsign)",
            ),
        ) {
            order("created_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<UnitBroadcastRow>().map { it.asEntry }

    suspend fun sendUnitBroadcast(draft: BroadcastDraft): Result<BroadcastSendResult> {
        val errors = validateBroadcastDraft(draft)
        if (!errors.isEmpty) {
            return Result.failure(APIException(errors.firstMessage ?: BROADCAST_SEND_FAILED))
        }
        val call = BroadcastSendCall(
            channel = draft.channel.raw,
            audience = draft.audience.raw,
            subject = if (needsBroadcastSubject(draft.channel)) draft.subject.trim() else "",
            body = draft.body.trim(),
        )
        val response = invokeEdge<BroadcastSendCall, BroadcastSendResponse>(
            function = "unit-broadcast",
            body = call,
            fallback = BROADCAST_SEND_FAILED,
        )
        return response.mapCatching { payload ->
            payload.error?.let { throw APIException(it) }
            payload.asResult
        }
    }

    suspend fun inviteAdminUser(draft: InviteDraft): AdminUsersActionResult {
        val errors = validateInviteDraft(draft)
        if (!errors.isEmpty) {
            return AdminUsersActionResult(error = errors.formMessage ?: INVITE_IDENTITY_ERROR)
        }
        val call = AdminInviteCall(
            fullName = draft.fullName.trim(),
            email = draft.email.trim().lowercase(),
            callsign = draft.callsign.trim(),
            phone = phoneDigits(draft.phone).ifEmpty { null },
            volunteerStatus = draft.volunteerStatus.raw,
            roles = draft.roles.distinct(),
            vehicles = draft.vehicles
                .filter { !it.archived && it.plateNumber.isNotBlank() && it.model.isNotBlank() }
                .map {
                    AdminInviteVehicle(
                        plateNumber = plateNumberForSave(it.plateNumber) ?: it.plateNumber.trim(),
                        model = it.model.trim(),
                    )
                },
        )
        val result = invokeAdminUsersAction(call, INVITE_SAVE_FAILED)
        if (result.ok && result.userId != null) {
            runCatching {
                client.from("profiles").update(
                    VolunteerStatusWrite(
                        volunteerStatus = draft.volunteerStatus.raw,
                        updatedAt = Instant.now().toString(),
                    ),
                ) {
                    filter { eq("id", result.userId) }
                }
            }
        }
        return result
    }

    suspend fun setAdminUserActive(userId: String, active: Boolean): String? =
        invokeAdminUsersAction(
            AdminSetActiveCall(action = if (active) "reactivate" else "deactivate", userId = userId),
            SET_ACTIVE_FAILED,
        ).error

    suspend fun deleteAdminUser(userId: String): String? =
        invokeAdminUsersAction(
            AdminSetActiveCall(action = "delete", userId = userId),
            DELETE_USER_FAILED,
        ).error

    suspend fun resendAdminInvite(userId: String): AdminUsersActionResult =
        invokeAdminUsersAction(
            AdminInviteLinkCall(action = "resend_invite", userId = userId, sendEmail = true),
            RESEND_INVITE_FAILED,
        )

    suspend fun copyAdminInviteLink(userId: String): AdminUsersActionResult =
        invokeAdminUsersAction(
            AdminInviteLinkCall(action = "copy_invite_link", userId = userId, sendEmail = false),
            COPY_INVITE_FAILED,
        )

    suspend fun setAdminUserOtp(userId: String, kind: String, enabled: Boolean): String? {
        val response = if (kind == "users_page") {
            invokeEdge<SetOtpUsersPageCall, PhoneOtpResponse>(
                "phone-otp",
                SetOtpUsersPageCall(userId = userId, otpUsersPageEnabled = enabled),
                OTP_SET_FAILED,
            )
        } else {
            invokeEdge<SetOtpLoginCall, PhoneOtpResponse>(
                "phone-otp",
                SetOtpLoginCall(userId = userId, otpLoginEnabled = enabled),
                OTP_SET_FAILED,
            )
        }
        return response.fold(
            onSuccess = { it.error },
            onFailure = { it.message ?: OTP_SET_FAILED },
        )
    }

    /** Profiles update + role/vehicle sync. Addresses stay untouched (no Places picker). */
    suspend fun saveAdminUser(draft: InviteDraft): String? {
        val userId = draft.id ?: return SAVE_USER_FAILED
        return try {
            client.from("profiles").update(
                AdminProfileSaveRow(
                    fullName = draft.fullName.trim(),
                    callsign = draft.callsign.trim(),
                    phone = phoneDigits(draft.phone).ifEmpty { null },
                    volunteerStatus = draft.volunteerStatus.raw,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                filter { eq("id", userId) }
            }
            syncUserRoles(userId, draft.roles)?.let { return it }
            syncUserVehicles(userId, draft.vehicles)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            SAVE_USER_FAILED
        }
    }

    suspend fun deleteAdminVehicle(vehicleId: String): String? = try {
        client.from("vehicles").delete { filter { eq("id", vehicleId) } }
        null
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        VEHICLE_DELETE_FAILED
    }

    suspend fun archiveAdminVehicle(vehicleId: String): String? = try {
        client.from("vehicles").update(AdminVehicleArchivedWrite(archived = true)) {
            filter { eq("id", vehicleId) }
        }
        null
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        VEHICLE_ARCHIVE_FAILED
    }

    suspend fun unarchiveAdminVehicle(vehicleId: String): String? = try {
        client.from("vehicles").update(AdminVehicleArchivedWrite(archived = false)) {
            filter { eq("id", vehicleId) }
        }
        null
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        VEHICLE_UNARCHIVE_FAILED
    }

    suspend fun isVehicleAttachedToEvents(userId: String, vehicleId: String, plateNumber: String): Boolean {
        val digits = plateDigits(plateNumber)
        val participations = client.from("event_responders").select(Columns.raw("vehicle_plate")) {
            filter { eq("responder_id", userId) }
        }.decodeList<VehiclePlateRef>()
        if (digits.isNotEmpty() && participations.any { plateDigits(it.vehiclePlate.orEmpty()) == digits }) {
            return true
        }
        return try {
            client.from("shifts").select(Columns.raw("id")) {
                filter { eq("personal_vehicle_id", vehicleId) }
                limit(1)
            }.decodeList<IdRow>().isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun syncUserRoles(userId: String, nextRoles: List<String>): String? {
        val existing = try {
            client.from("user_roles").select(Columns.raw("role")) {
                filter { eq("user_id", userId) }
            }.decodeList<RoleRow>().map { it.role }
        } catch (_: Exception) {
            return SAVE_ROLES_FAILED
        }
        val diff = syncUserRolesDiff(existing, nextRoles)
        try {
            if (diff.toRemove.isNotEmpty()) {
                client.from("user_roles").delete {
                    filter {
                        eq("user_id", userId)
                        isIn("role", diff.toRemove)
                    }
                }
            }
            if (diff.toAdd.isNotEmpty()) {
                client.from("user_roles").insert(
                    diff.toAdd.map { UserRoleWrite(userId = userId, role = it) },
                )
            }
        } catch (_: Exception) {
            return SAVE_ROLES_FAILED
        }
        return null
    }

    private suspend fun syncUserVehicles(
        userId: String,
        nextVehicles: List<com.yahpz.domain.AdminVehicleDraft>,
    ): String? {
        if (findDuplicatePlate(nextVehicles.map { it.plateNumber }) != null) {
            return DUPLICATE_PLATE_ERROR
        }
        val existing = try {
            client.from("vehicles").select(Columns.raw("id, user_id, plate_number, model, archived")) {
                filter { eq("user_id", userId) }
            }.decodeList<AdminVehicleRow>()
        } catch (_: Exception) {
            return SAVE_VEHICLES_FAILED
        }
        val nextWithIds = nextVehicles.filter { !it.id.isNullOrEmpty() }
        val nextIds = nextWithIds.mapNotNull { it.id }.toSet()
        try {
            for (row in existing) {
                if (row.id !in nextIds) {
                    client.from("vehicles").delete { filter { eq("id", row.id) } }
                }
            }
            for (vehicle in nextWithIds) {
                val id = vehicle.id ?: continue
                if (vehicle.archived) {
                    client.from("vehicles").update(AdminVehicleArchivedWrite(archived = true)) {
                        filter {
                            eq("id", id)
                            eq("user_id", userId)
                        }
                    }
                    continue
                }
                val plate = plateNumberForSave(vehicle.plateNumber) ?: vehicle.plateNumber.trim()
                val model = vehicle.model.trim()
                if (plate.isEmpty() || model.isEmpty()) continue
                client.from("vehicles").update(
                    AdminVehiclePlateWrite(plateNumber = plate, model = model, archived = false),
                ) {
                    filter {
                        eq("id", id)
                        eq("user_id", userId)
                    }
                }
            }
            val toInsert = nextVehicles.filter {
                it.id.isNullOrEmpty() && it.plateNumber.isNotBlank() && it.model.isNotBlank()
            }
            if (toInsert.isNotEmpty()) {
                client.from("vehicles").insert(
                    toInsert.map {
                        AdminVehicleInsert(
                            userId = userId,
                            plateNumber = plateNumberForSave(it.plateNumber) ?: it.plateNumber.trim(),
                            model = it.model.trim(),
                        )
                    },
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return if (isUniqueViolation(error.message)) DUPLICATE_PLATE_ERROR else SAVE_VEHICLES_FAILED
        }
        return null
    }

    private fun isUniqueViolation(message: String?): Boolean {
        val raw = message.orEmpty()
        return raw.contains("23505") ||
            raw.contains("duplicate", ignoreCase = true) ||
            raw.contains("unique", ignoreCase = true)
    }

    private suspend inline fun <reified T : Any> invokeAdminUsersAction(
        body: T,
        fallback: String,
    ): AdminUsersActionResult {
        val response = invokeEdge<T, AdminUsersResponse>("admin-users", body, fallback)
        return response.fold(
            onSuccess = { payload ->
                if (payload.error != null) AdminUsersActionResult(error = payload.error)
                else AdminUsersActionResult(
                    message = payload.message,
                    userId = payload.userId,
                    actionLink = payload.actionLink,
                )
            },
            onFailure = { AdminUsersActionResult(error = it.message ?: fallback) },
        )
    }

    private fun extraFunctionHeaders(): Headers = Headers.build {
        if (ViewAsStore.isImpersonating()) {
            append("x-yahpaz-impersonating", "1")
        }
    }

    private suspend fun notifyFillReady(eventResponderIds: List<String>) {
        runCatching {
            invokeEdge<NotifyFillReadyCall, NotifyFillReadyResponse>(
                "responder-fill",
                NotifyFillReadyCall(eventResponderIds = eventResponderIds),
                "שליחת התראת הדיווח נכשלה.",
            )
        }
    }

    /**
     * Edge functions answer with `{ "error": "…" }` in Hebrew on failure. supabase-kt
     * raises those as exceptions, so pull the message back out of the body when it is there.
     */
    private suspend inline fun <reified B : Any, reified R : Any> invokeEdge(
        function: String,
        body: B,
        fallback: String,
    ): Result<R> = try {
        Result.success(json.decodeFromString<R>(
            client.functions.invoke(function, body, headers = extraFunctionHeaders()).bodyAsText(),
        ))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(APIException(edgeErrorMessage(error.message, fallback)))
    }

    private suspend fun fetchReportEvents(from: String, to: String): List<ReportEventRow> =
        client.from("events").select(Columns.raw(reportEventSelect)) {
            filter {
                gte("event_date", from)
                lte("event_date", to)
            }
            order("event_date", Order.DESCENDING)
        }.decodeList<ReportEventRow>()

    private suspend fun fetchFuelRefundRows(from: String, to: String): List<FuelRefundRow> {
        val profiles = client.from("profiles").select(Columns.raw("id, full_name, callsign")) {
            filter { eq("active", true) }
        }.decodeList<AssignableProfileRow>()
        // Refunds follow when the event was *reported*, so bound `created_at`, not `event_date`.
        val bounds = jerusalemDayBounds(from, to)
        val eventRows = client.from("events").select(Columns.raw("id, frozen_over_60km, frozen_suspicious_duplicate")) {
            filter {
                eq("origin", "manual")
                gte("created_at", bounds.first)
                lte("created_at", bounds.second)
            }
        }.decodeList<FuelEventFreezeRow>()
        val eventIds = eventRows.filter { !it.isFrozen }.map { it.id }
        val participations = if (eventIds.isEmpty()) {
            emptyList()
        } else {
            eventIds.chunked(100).flatMap { chunk ->
                client.from("event_responders")
                    .select(Columns.raw("responder_id, event_id, total_km")) {
                        filter { isIn("event_id", chunk) }
                    }.decodeList<FuelParticipationRow>()
            }
        }
        val credits = client.from("shifts").select(
            Columns.raw("total_km, vehicles!shifts_personal_vehicle_id_fkey(user_id)"),
        ) {
            filter {
                eq("vehicle_type", "personal")
                gte("shift_date", from)
                lte("shift_date", to)
            }
        }.decodeList<FuelShiftRow>()
        return buildFuelRefundRows(
            profiles = profiles.map { FuelRefundProfileInput(it.id, it.fullName, it.callsign) },
            participations = participations.map {
                FuelRefundParticipationInput(it.responderId, it.eventId, it.totalKm)
            },
            credits = credits.mapNotNull { row ->
                val owner = row.vehicles?.userId ?: return@mapNotNull null
                val km = row.totalKm ?: return@mapNotNull null
                FuelRefundCreditInput(owner, km)
            },
        )
    }

    /** Inclusive local-day bounds in Asia/Jerusalem, as UTC instants for `created_at` filters. */
    private fun jerusalemDayBounds(from: String, to: String): Pair<String, String> {
        val zone = ZoneId.of("Asia/Jerusalem")
        val start = LocalDate.parse(from).atStartOfDay(zone).toInstant().toString()
        val end = LocalDate.parse(to).plusDays(1).atStartOfDay(zone).minusNanos(1_000_000)
            .toInstant().toString()
        return start to end
    }

    suspend fun fetchEventLookups(): EventLookups {
        suspend fun lookup(table: String, columns: String): List<LookupOption> =
            client.from(table).select(Columns.raw(columns)) {
                filter { eq("active", true) }
                order("sort_order", Order.ASCENDING)
                order("name", Order.ASCENDING)
            }.decodeList<LookupRow>().map { it.asOption }
        val roads = lookup("roads", "id, name")
        return EventLookups(
            districts = sortLookupsBySortOrder(lookup("districts", "id, name, code, sort_order")),
            eventTypes = lookup("event_types", "id, name"),
            roads = sortByRoadName(roads) { it.name },
            vehicleKinds = lookup("vehicle_kinds", "id, name"),
        )
    }

    /** Admin closed lists — all rows (active + inactive), matching web `fetchClosedListItems`. */
    suspend fun fetchClosedListItems(key: ClosedListKey): List<ClosedListItem> {
        val columns = if (key == ClosedListKey.DISTRICTS) {
            "id, name, active, sort_order, code"
        } else {
            "id, name, active, sort_order"
        }
        val items = client.from(key.raw).select(Columns.raw(columns)) {
            order("sort_order", Order.ASCENDING)
            order("name", Order.ASCENDING)
        }.decodeList<ClosedListItemRow>().map { it.asItem }
        return when (key) {
            ClosedListKey.ROADS -> sortByRoadName(items) { it.name }
            ClosedListKey.DISTRICTS -> items.sortedWith(compareBy({ it.sortOrder }, { it.name }))
            else -> items
        }
    }

    suspend fun createClosedListItem(key: ClosedListKey, name: String): ClosedListMutationResult {
        closedListNameError(name)?.let { return ClosedListMutationResult.Err(it) }
        val trimmed = name.trim()
        return try {
            val existing = fetchClosedListItems(key)
            val sortOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val item = client.from(key.raw).insert(
                ClosedListInsert(name = trimmed, sortOrder = sortOrder, active = true),
            ) {
                select(Columns.raw("id, name, active, sort_order"))
            }.decodeSingle<ClosedListItemRow>().asItem
            ClosedListMutationResult.Ok(item)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ClosedListMutationResult.Err(mapClosedListWriteError(error.message, create = true))
        }
    }

    suspend fun updateClosedListItem(
        key: ClosedListKey,
        id: String,
        name: String,
    ): ClosedListMutationResult {
        closedListNameError(name)?.let { return ClosedListMutationResult.Err(it) }
        val trimmed = name.trim()
        if (key == ClosedListKey.DISTRICTS) {
            val item = runCatching { fetchClosedListItems(key).firstOrNull { it.id == id } }.getOrNull()
            if (isSystemClosedListItem(item)) {
                return ClosedListMutationResult.Err(SYSTEM_DISTRICT_LOCKED_ERROR)
            }
        }
        return try {
            client.from(key.raw).update(ClosedListNameUpdate(name = trimmed)) {
                filter { eq("id", id) }
            }
            ClosedListMutationResult.Ok()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ClosedListMutationResult.Err(mapClosedListWriteError(error.message, create = false))
        }
    }

    private suspend fun isClosedListItemInUse(key: ClosedListKey, id: String): Boolean {
        val usage = closedListMeta(key).usage
        val rows = client.from(usage.table).select(Columns.raw("id")) {
            filter { eq(usage.column, id) }
            limit(1)
        }.decodeList<IdRow>()
        return rows.isNotEmpty()
    }

    suspend fun deleteClosedListItem(key: ClosedListKey, id: String): ClosedListMutationResult {
        if (key == ClosedListKey.DISTRICTS) {
            val item = try {
                fetchClosedListItems(key).firstOrNull { it.id == id }
            } catch (_: Exception) {
                return ClosedListMutationResult.Err(CLOSED_LIST_IN_USE_CHECK_FAILED)
            }
            if (isSystemClosedListItem(item)) {
                return ClosedListMutationResult.Err(SYSTEM_DISTRICT_LOCKED_ERROR)
            }
        }
        try {
            if (isClosedListItemInUse(key, id)) {
                return ClosedListMutationResult.Err(CLOSED_LIST_IN_USE, inUse = true)
            }
        } catch (_: Exception) {
            return ClosedListMutationResult.Err(CLOSED_LIST_IN_USE_CHECK_FAILED)
        }
        return try {
            client.from(key.raw).delete {
                filter { eq("id", id) }
            }
            ClosedListMutationResult.Ok()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            mapClosedListDeleteError(error.message)
        }
    }

    suspend fun fetchAssignableProfiles(): List<AssignableProfile> =
        client.from("profiles").select(Columns.raw("id, full_name, callsign")) {
            filter { eq("active", true) }
            order("full_name", Order.ASCENDING)
        }.decodeList<AssignableProfileRow>().map { it.asProfile }

    suspend fun fetchShiftLeadProfiles(): List<AssignableProfile> =
        client.postgrest.rpc("list_shift_lead_profiles").decodeList<AssignableProfileRow>().map { it.asProfile }

    suspend fun fetchVehiclesForResponders(responderIds: List<String>): List<CrewVehicleRow> {
        if (responderIds.isEmpty()) return emptyList()
        return client.from("vehicles").select(Columns.raw("id, user_id, plate_number, model, archived")) {
            filter { isIn("user_id", responderIds.distinct()) }
            order("plate_number", Order.ASCENDING)
        }.decodeList<CrewVehicleRow>().filter { it.archived != true }
    }

    /** Insert the event then its pending crew. Mirrors the web `saveEventForm` create path. */
    suspend fun createUnitEvent(
        draft: EventDraft,
        districts: List<LookupOption>,
        vehicleKinds: List<LookupOption>,
        allowPartial: Boolean = false,
    ): String? {
        val errors = if (allowPartial) {
            validateEventDraftPartial(draft)
        } else {
            validateEventDraft(draft, districts)
        }
        if (!errors.isEmpty) {
            return errors.eventDate ?: errors.formMessage ?: EVENT_DRAFT_FORM_ERROR
        }
        val userId = sessionUserId() ?: return "יש להתחבר מחדש."
        if (createIncludesSelfAssign(userId, draft.responders)) {
            return EVENT_SELF_ASSIGN_ON_CREATE_ERROR
        }
        val eventDate = normalizeReturnDate(draft.eventDate) ?: return EVENT_DRAFT_DATE_ERROR
        val mainLeadId = draft.shiftLeadId.ifBlank { userId }
        return try {
            val nextStatus = deriveEventStatusFromDraft(draft.responders)
            val inserted = client.from("events").insert(
                EventInsert(
                    eventDate = eventDate,
                    policeEventId = draft.policeEventId.nilIfEmpty(),
                    districtId = draft.districtId.nilIfEmpty(),
                    patrolCallsign = draft.patrolCallsign.nilIfEmpty(),
                    eventTypeId = draft.eventTypeId.nilIfEmpty(),
                    roadId = draft.roadId.nilIfEmpty(),
                    location = draft.location.nilIfEmpty(),
                    station = stationForSave(districts, draft.districtId, draft.station),
                    notes = draft.notes.nilIfEmpty(),
                    busLane = draft.busLane,
                    status = nextStatus.raw,
                    shiftLeadId = mainLeadId,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                select(Columns.raw("id"))
            }.decodeSingle<IdRow>()
            syncEventResponders(
                eventId = inserted.id,
                eventDate = eventDate,
                responders = draft.responders,
                vehicleKinds = vehicleKinds,
                isCancelled = draft.isCancelled,
            )?.let { return it }
            syncEventSecondaryLeads(
                eventId = inserted.id,
                desired = draft.secondaryLeads,
                creatorSecondary = createTimeCreatorSecondary(userId, mainLeadId),
                mainLeadId = mainLeadId,
            )
        } catch (_: Exception) {
            recoverOwnCreatedEvent(
                draft = draft,
                eventDate = eventDate,
                mainLeadId = mainLeadId,
                districts = districts,
                vehicleKinds = vehicleKinds,
                allowPartial = allowPartial,
            ) ?: EVENT_DRAFT_SAVE_FAILED
        }
    }

    private suspend fun recoverOwnCreatedEvent(
        draft: EventDraft,
        eventDate: String,
        mainLeadId: String,
        districts: List<LookupOption>,
        vehicleKinds: List<LookupOption>,
        allowPartial: Boolean,
    ): String? {
        val policeId = digitsOnly(draft.policeEventId)
        if (policeId.isEmpty()) return null
        val existing = runCatching {
            client.from("events").select(Columns.raw("id, shift_lead_id, is_cancelled, police_event_id")) {
                filter {
                    eq("event_date", eventDate)
                    eq("shift_lead_id", mainLeadId)
                    eq("is_cancelled", false)
                }
            }.decodeList<SameDayPoliceEventApiRow>().filter {
                digitsOnly(it.policeEventId.orEmpty()) == policeId
            }
        }.getOrDefault(emptyList())
        val recovered = ownResumableEventId(
            currentEventId = null,
            viewerLeadId = mainLeadId,
            existing = existing.map {
                SameDayPoliceEventRow(
                    id = it.id,
                    shiftLeadId = it.shiftLeadId,
                    isCancelled = it.isCancelled,
                )
            },
        ) ?: return null
        return updateUnitEvent(
            eventId = recovered,
            draft = draft,
            districts = districts,
            vehicleKinds = vehicleKinds,
            viewerIsAdmin = false,
            previousIsCancelled = false,
            allowPartial = allowPartial,
        )
    }

    suspend fun createUnitShift(draft: ShiftDraft): String? {
        val errors = validateShiftDraft(draft)
        if (!errors.isEmpty) return errors.formMessage ?: SHIFT_DRAFT_FORM_ERROR
        val userId = sessionUserId() ?: return "יש להתחבר מחדש."
        val shiftDate = normalizeReturnDate(draft.shiftDate) ?: return SHIFT_DRAFT_DATE_ERROR
        return try {
            val inserted = client.from("shifts").insert(
                ShiftInsert(
                    shiftDate = shiftDate,
                    shiftKind = draft.shiftKind,
                    vehicleType = draft.vehicleType,
                    personalVehicleId = if (draft.vehicleType == "personal") draft.personalVehicleId else null,
                    notes = draft.notes.nilIfEmpty(),
                    shiftLeadId = userId,
                    lastSavedBy = userId,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                select(Columns.raw("id"))
            }.decodeSingle<IdRow>()
            client.from("shift_responders").insert(
                draft.responderIds.distinct().map { responderId ->
                    ShiftResponderInsert(shiftId = inserted.id, responderId = responderId)
                },
            )
            // A fresh shift carries no rollup counts, so this is a no-op the server may skip.
            runCatching {
                client.postgrest.rpc("sync_shift_born_events", mapOf("p_shift_id" to inserted.id))
            }
            null
        } catch (_: Exception) {
            SHIFT_DRAFT_SAVE_FAILED
        }
    }

    suspend fun setEventCancelled(eventId: String, isCancelled: Boolean): String? {
        return try {
            val updated = client.from("events").update(
                EventCancelWrite(isCancelled = isCancelled, updatedAt = Instant.now().toString()),
            ) {
                filter { eq("id", eventId) }
                select(Columns.raw("id"))
            }.decodeList<IdRow>()
            if (updated.isEmpty()) "אין הרשאה לעדכן את האירוע." else null
        } catch (_: Exception) {
            EVENT_DRAFT_SAVE_FAILED
        }
    }

    suspend fun fetchEventFormDetail(eventId: String): EventFormDetail =
        client.from("events").select(
            Columns.raw(
                """
                id, event_date, police_event_id, district_id, patrol_callsign, event_type_id, road_id,
                location, station, notes, is_cancelled, bus_lane, status, shift_lead_id,
                shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
                $EVENT_SECONDARY_LEADS_EMBED,
                responders:event_responders(
                  id, responder_id, started_at, ended_at, total_km, emergency_means, status,
                  treated:event_treated_vehicles(vehicle_kind_id, quantity)
                )
                """.trimIndent(),
            ),
        ) {
            filter { eq("id", eventId) }
        }.decodeSingle()

    suspend fun updateUnitEvent(
        eventId: String,
        draft: EventDraft,
        districts: List<LookupOption>,
        vehicleKinds: List<LookupOption>,
        viewerIsAdmin: Boolean,
        previousIsCancelled: Boolean,
        allowPartial: Boolean = false,
        previousDraft: EventDraft? = null,
    ): String? {
        val errors = if (allowPartial) {
            validateEventDraftPartial(draft)
        } else {
            validateEventDraft(draft, districts)
        }
        if (!errors.isEmpty) {
            return errors.eventDate ?: errors.formMessage ?: EVENT_DRAFT_FORM_ERROR
        }
        if (previousIsCancelled && !draft.isCancelled) {
            canToggleEventCancelled(false, viewerIsAdmin)?.let { return it }
        }
        if (previousDraft != null && previousDraft.forPersistCompare() == draft.forPersistCompare()) {
            return null
        }
        val eventDate = normalizeReturnDate(draft.eventDate) ?: return EVENT_DRAFT_DATE_ERROR
        val mainLeadId = draft.shiftLeadId.ifBlank { return "אין אחמ״ש ראשי." }
        return try {
            val nextStatus = deriveEventStatusFromDraft(draft.responders)
            val updated = client.from("events").update(
                EventUpdateWrite(
                    eventDate = eventDate,
                    policeEventId = draft.policeEventId.nilIfEmpty(),
                    districtId = draft.districtId.nilIfEmpty(),
                    patrolCallsign = draft.patrolCallsign.nilIfEmpty(),
                    eventTypeId = draft.eventTypeId.nilIfEmpty(),
                    roadId = draft.roadId.nilIfEmpty(),
                    location = draft.location.nilIfEmpty(),
                    station = stationForSave(districts, draft.districtId, draft.station),
                    notes = draft.notes.nilIfEmpty(),
                    isCancelled = draft.isCancelled,
                    busLane = draft.busLane,
                    status = nextStatus.raw,
                    shiftLeadId = mainLeadId,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                filter { eq("id", eventId) }
                select(Columns.raw("id"))
            }.decodeList<IdRow>()
            if (updated.isEmpty()) return "אין הרשאה לעדכן את האירוע."
            syncEventResponders(
                eventId = eventId,
                eventDate = eventDate,
                responders = draft.responders,
                vehicleKinds = vehicleKinds,
                isCancelled = draft.isCancelled,
            )?.let { return it }
            syncEventSecondaryLeads(
                eventId = eventId,
                desired = draft.secondaryLeads,
                creatorSecondary = null,
                mainLeadId = mainLeadId,
            )
        } catch (_: Exception) {
            EVENT_DRAFT_SAVE_FAILED
        }
    }

    private suspend fun syncEventSecondaryLeads(
        eventId: String,
        desired: List<SecondaryLead>,
        creatorSecondary: SecondaryLead?,
        mainLeadId: String,
    ): String? {
        val existing = client.from("event_secondary_leads").select(
            Columns.raw(
                "user_id, locked, added_at, profile:profiles!event_secondary_leads_user_id_fkey(full_name, callsign)",
            ),
        ) {
            filter { eq("event_id", eventId) }
            order("added_at", Order.ASCENDING)
        }.decodeList<EventSecondaryLeadRow>()
        val wanted = linkedMapOf<String, Boolean>()
        for (row in desired) {
            val id = row.userId.trim()
            if (id.isNotEmpty() && id != mainLeadId) wanted[id] = row.locked
        }
        creatorSecondary?.userId?.trim()?.takeIf { it.isNotEmpty() && it != mainLeadId }?.let { id ->
            wanted[id] = wanted[id] == true
        }
        for (row in existing) {
            if (row.userId !in wanted && !row.locked) {
                try {
                    client.from("event_secondary_leads").delete {
                        filter {
                            eq("event_id", eventId)
                            eq("user_id", row.userId)
                            eq("locked", false)
                        }
                    }
                } catch (_: Exception) {
                    return EVENT_DRAFT_SAVE_FAILED
                }
            }
        }
        for ((userId, locked) in wanted) {
            val found = existing.firstOrNull { it.userId == userId }
            try {
                if (found == null) {
                    client.from("event_secondary_leads").insert(
                        EventSecondaryLeadInsert(eventId = eventId, userId = userId, locked = locked),
                    )
                } else if (locked && !found.locked) {
                    client.from("event_secondary_leads").update(EventSecondaryLeadLockWrite(locked = true)) {
                        filter {
                            eq("event_id", eventId)
                            eq("user_id", userId)
                        }
                    }
                }
            } catch (_: Exception) {
                return EVENT_DRAFT_SAVE_FAILED
            }
        }
        return null
    }

    private suspend fun syncEventResponders(
        eventId: String,
        eventDate: String,
        responders: List<EventResponderDraft>,
        vehicleKinds: List<LookupOption>,
        isCancelled: Boolean,
    ): String? {
        val existing = client.from("event_responders")
            .select(Columns.raw("id, responder_id, status, total_km")) {
                filter { eq("event_id", eventId) }
            }.decodeList<EventFormResponderRow>()
        val keepIds = responders.map { it.responderId }.distinct().toSet()
        val toRemove = existing.filter { it.responderId !in keepIds }
        if (toRemove.isNotEmpty()) {
            client.from("event_responders").delete {
                filter { isIn("id", toRemove.map { it.id }) }
            }
        }
        val existingByResponder = existing.associate { it.responderId to it.id }
        val nextKmRows = mutableListOf<FillReadyNextRow>()
        for (responder in responders) {
            val km = leadKmForSave(responder.hasVehicle, responder.totalKm)
            if (responder.hasVehicle && responder.totalKm.isNotBlank() && km == null) {
                return "קילומטרים חייבים להיות מספר."
            }
            val overnight = isOvernightEnd(responder.startTime, responder.endTime)
            val startedAt = wallTimestamp(eventDate, responder.startTime, 0)
            val endedAt = wallTimestamp(eventDate, responder.endTime, if (overnight) 1 else 0)
            val assignmentId = responder.assignmentId.ifEmpty { null }
                ?: existingByResponder[responder.responderId]
            val now = Instant.now().toString()
            val resolvedId = if (assignmentId != null) {
                client.from("event_responders").update(
                    EventResponderLeadWrite(
                        startedAt = startedAt,
                        endedAt = endedAt,
                        totalKm = km,
                        emergencyMeans = responder.emergencyMeans,
                        updatedAt = now,
                    ),
                ) {
                    filter { eq("id", assignmentId) }
                    select(Columns.raw("id"))
                }.decodeList<IdRow>().firstOrNull()?.id ?: assignmentId
            } else {
                client.from("event_responders").insert(
                    EventResponderInsert(
                        eventId = eventId,
                        responderId = responder.responderId,
                        startedAt = startedAt,
                        endedAt = endedAt,
                        totalKm = km,
                        emergencyMeans = responder.emergencyMeans,
                        status = responder.status.raw,
                    ),
                ) {
                    select(Columns.raw("id"))
                }.decodeSingle<IdRow>().id
            }
            nextKmRows += FillReadyNextRow(assignmentId = resolvedId, totalKm = km)
            client.from("event_treated_vehicles").delete {
                filter { eq("event_responder_id", resolvedId) }
            }
            if (!isCancelled) {
                val treatedRows = vehicleKinds.mapNotNull { kind ->
                    val quantity = responder.treated.firstOrNull { it.vehicleKindId == kind.id }?.quantity ?: 0
                    if (quantity > 0) {
                        EventTreatedVehicleInsert(
                            eventResponderId = resolvedId,
                            vehicleKindId = kind.id,
                            quantity = quantity,
                        )
                    } else {
                        null
                    }
                }
                if (treatedRows.isNotEmpty()) {
                    client.from("event_treated_vehicles").insert(treatedRows)
                }
            }
        }
        if (!isCancelled) {
            val notifyIds = fillReadyNotifyIds(
                previous = existing.map { FillReadyPreviousRow(it.id, it.totalKm) },
                next = nextKmRows,
            )
            if (notifyIds.isNotEmpty()) notifyFillReady(notifyIds)
        }
        return null
    }

    suspend fun fetchShiftFormDetail(shiftId: String): ShiftFormDetail =
        client.from("shifts").select(
            Columns.raw(
                """
                id, shift_date, shift_kind, vehicle_type, notes, personal_vehicle_id,
                responders:shift_responders(id, responder_id)
                """.trimIndent(),
            ),
        ) {
            filter { eq("id", shiftId) }
        }.decodeSingle()

    suspend fun updateUnitShift(shiftId: String, draft: ShiftDraft): String? {
        val errors = validateShiftDraft(draft)
        if (!errors.isEmpty) return errors.formMessage ?: SHIFT_DRAFT_FORM_ERROR
        val userId = sessionUserId() ?: return "יש להתחבר מחדש."
        val shiftDate = normalizeReturnDate(draft.shiftDate) ?: return SHIFT_DRAFT_DATE_ERROR
        return try {
            val detail = fetchShiftFormDetail(shiftId)
            val personalVehicleId = if (draft.vehicleType == "personal") {
                draft.personalVehicleId
            } else {
                null
            }
            val existing = detail.responders
            val keepIds = draft.responderIds.distinct().toSet()
            val toRemove = existing.filter { it.responderId !in keepIds }
            if (toRemove.isNotEmpty()) {
                client.from("shift_responders").delete {
                    filter { isIn("id", toRemove.map { it.id }) }
                }
            }
            val existingResponderIds = existing.map { it.responderId }.toSet()
            val toAdd = keepIds.filter { it !in existingResponderIds }
            if (toAdd.isNotEmpty()) {
                client.from("shift_responders").insert(
                    toAdd.map { responderId ->
                        ShiftResponderInsert(shiftId = shiftId, responderId = responderId)
                    },
                )
            }
            val updated = client.from("shifts").update(
                ShiftUpdateWrite(
                    shiftDate = shiftDate,
                    shiftKind = draft.shiftKind,
                    vehicleType = draft.vehicleType,
                    personalVehicleId = personalVehicleId,
                    notes = draft.notes.nilIfEmpty(),
                    lastSavedBy = userId,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                filter { eq("id", shiftId) }
                select(Columns.raw("id"))
            }.decodeList<IdRow>()
            if (updated.isEmpty()) return "אין הרשאה לעדכן את המשמרת."
            runCatching {
                client.postgrest.rpc("sync_shift_born_events", mapOf("p_shift_id" to shiftId))
            }
            null
        } catch (_: Exception) {
            SHIFT_DRAFT_SAVE_FAILED
        }
    }

    suspend fun loadFuelQuarterWorkbook(year: Int, quarter: Int): FuelQuarterWorkbook {
        require(quarter in 1..4)
        val q = ensureFuelQuarter(year, quarter)
        val openingByUser = fetchFuelOpeningByUser(year, quarter)
        val participations = fetchFuelParticipationsInQuarter(year, quarter)
        val savedByUser = fetchFuelSavedDistributions(q.id)
        val idSet = linkedSetOf<String>()
        participations.forEach { idSet.add(it.responderId) }
        idSet.addAll(openingByUser.keys)
        idSet.addAll(savedByUser.keys)
        val profiles = if (idSet.isEmpty()) {
            emptyList()
        } else {
            idSet.toList().chunked(100).flatMap { chunk ->
                client.from("profiles").select(Columns.raw("id, full_name, callsign, active")) {
                    filter { isIn("id", chunk) }
                }.decodeList<FuelQuarterProfileRow>()
            }
        }
        val rows = buildFuelQuarterRows(
            year = year,
            quarter = quarter,
            profiles = profiles.map {
                FuelQuarterProfileInput(it.id, it.fullName, it.callsign, it.active)
            },
            participations = participations,
            openingByUser = openingByUser,
            savedByUser = savedByUser,
        )
        return FuelQuarterWorkbook(
            quarterId = q.id,
            year = year,
            quarter = quarter,
            status = q.status,
            monthLabels = quarterMonthLabels(quarter),
            rows = rows,
        )
    }

    private suspend fun ensureFuelQuarter(year: Int, quarter: Int): FuelQuarterRowDb {
        val existing = client.from("fuel_quarters").select(Columns.raw("id, year, quarter, status")) {
            filter {
                eq("year", year)
                eq("quarter", quarter)
            }
        }.decodeList<FuelQuarterRowDb>().firstOrNull()
        if (existing != null) return existing
        return client.from("fuel_quarters").insert(
            FuelQuarterInsert(year = year, quarter = quarter),
        ) {
            select(Columns.raw("id, year, quarter, status"))
        }.decodeSingle()
    }

    private suspend fun fetchFuelOpeningByUser(year: Int, quarter: Int): Map<String, Double> {
        val prevYear = if (quarter == 1) year - 1 else year
        val prevQuarter = if (quarter == 1) 4 else quarter - 1
        val prev = client.from("fuel_quarters").select(Columns.raw("id, year, quarter, status")) {
            filter {
                eq("year", prevYear)
                eq("quarter", prevQuarter)
            }
        }.decodeList<FuelQuarterRowDb>().firstOrNull()
        if (prev == null || prev.status != "locked") return emptyMap()
        return client.from("fuel_quarter_distributions")
            .select(Columns.raw("responder_id, remaining_km, cards, card_numbers")) {
                filter { eq("quarter_id", prev.id) }
            }.decodeList<FuelQuarterDistributionRow>()
            .associate { it.responderId to (it.remainingKm ?: 0.0) }
    }

    private suspend fun fetchFuelSavedDistributions(quarterId: String): Map<String, FuelQuarterSavedDistribution> =
        client.from("fuel_quarter_distributions")
            .select(Columns.raw("responder_id, cards, card_numbers, remaining_km")) {
                filter { eq("quarter_id", quarterId) }
            }.decodeList<FuelQuarterDistributionRow>()
            .associate { row ->
                row.responderId to FuelQuarterSavedDistribution(
                    cards = row.cards ?: 0,
                    cardNumbers = row.cardNumbers.orEmpty(),
                )
            }

    private suspend fun fetchFuelParticipationsInQuarter(
        year: Int,
        quarter: Int,
    ): List<FuelQuarterParticipationInput> {
        val (from, to) = quarterLocalDateRange(year, quarter)
        val bounds = jerusalemDayBounds(from, to)
        val rows = client.from("event_responders").select(
            Columns.raw(
                """
                responder_id, total_km,
                events!inner(created_at, status)
                """.trimIndent(),
            ),
        ) {
            filter {
                eq("events.status", EventStatus.DONE.raw)
                gte("events.created_at", bounds.first)
                lte("events.created_at", bounds.second)
            }
        }.decodeList<FuelQuarterParticipationRow>()
        return rows.mapNotNull { row ->
            val event = row.events ?: return@mapNotNull null
            if (row.totalKm == null) return@mapNotNull null
            FuelQuarterParticipationInput(
                responderId = row.responderId,
                createdAt = event.createdAt,
                totalKm = row.totalKm,
            )
        }
    }

    suspend fun fetchMyVehicles(): List<ProfileVehicle> {
        val userId = sessionUserId() ?: return emptyList()
        val rows = client.from("vehicles").select(
            Columns.raw("id, plate_number, model, archived, is_default"),
        ) {
            filter { eq("user_id", userId) }
        }.decodeList<VehicleOption>()
        return managedProfileVehicles(
            rows.map {
                VehicleRowInput(
                    plateRaw = it.plateNumber,
                    modelRaw = it.model,
                    archived = it.archived,
                    id = it.id,
                    isDefault = it.isDefault,
                )
            },
        )
    }

    suspend fun createOwnVehicle(plateNumber: String, model: String): String? {
        val userId = sessionUserId() ?: return SAVE_VEHICLES_FAILED
        val fields = vehicleFieldsForSave(plateNumber, model)
        if (fields is VehicleFieldsError) return fields.message
        val ok = fields as VehicleFieldsOk
        return try {
            client.from("vehicles").insert(
                AdminVehicleInsert(
                    userId = userId,
                    plateNumber = ok.plateNumber,
                    model = ok.model,
                    archived = false,
                ),
            )
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (isUniqueViolation(error.message)) DUPLICATE_PLATE_ERROR else SAVE_VEHICLES_FAILED
        }
    }

    suspend fun updateOwnVehicle(vehicleId: String, plateNumber: String, model: String): String? {
        val fields = vehicleFieldsForSave(plateNumber, model)
        if (fields is VehicleFieldsError) return fields.message
        val ok = fields as VehicleFieldsOk
        return try {
            client.from("vehicles").update(
                AdminVehiclePlateWrite(plateNumber = ok.plateNumber, model = ok.model, archived = false),
            ) {
                filter { eq("id", vehicleId) }
            }
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (isUniqueViolation(error.message)) DUPLICATE_PLATE_ERROR else SAVE_VEHICLES_FAILED
        }
    }

    suspend fun setDefaultVehicle(vehicleId: String): String? = try {
        client.postgrest.rpc("set_default_vehicle", SetDefaultVehicleCall(vehicleId))
        null
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        "עדכון הרכב הראשי נכשל."
    }

    private suspend inline fun <reified T : Any> fetchByIds(
        ids: List<String>,
        table: String,
        columns: String,
    ): List<T> {
        return ids.chunked(100).flatMap { chunk ->
            client.from(table).select(Columns.raw(columns)) {
                filter { isIn("id", chunk) }
            }.decodeList<T>()
        }
    }

    suspend fun fetchFillContext(eventId: String): FillContext? {
        val userId = sessionUserId() ?: return null
        val event = client.from("events").select(Columns.raw(fillSelect)) {
            filter { eq("id", eventId) }
        }.decodeSingle<FillEventRow>()
        val vehicles = client.from("vehicles").select(Columns.raw("plate_number, model, archived")) {
            filter { eq("user_id", userId) }
        }.decodeList<VehicleOption>()
        val mine = event.responders.firstOrNull { it.responderId.equals(userId, ignoreCase = true) } ?: return null
        val existingPlate = plateDigits(mine.vehiclePlate.orEmpty())
        val options = vehicles.map {
            ResponderVehicle(
                plate = plateDigits(it.plateNumber),
                model = it.model?.trim().orEmpty(),
            )
        }.filter { it.plate.isNotEmpty() }.filter { vehicle ->
            val archived = vehicles.firstOrNull { plateDigits(it.plateNumber) == vehicle.plate }?.archived ?: false
            !archived || vehicle.plate == existingPlate
        }
        val allowed = options.map { it.plate }.toSet()
        val selected = when {
            existingPlate.isNotEmpty() && existingPlate in allowed -> existingPlate
            options.size == 1 -> options[0].plate
            else -> ""
        }
        return FillContext(
            eventId = event.id,
            assignmentId = mine.id,
            eventStatus = event.status,
            eventDate = event.eventDate,
            policeEventId = event.policeEventId,
            eventTypeName = event.eventType?.name,
            isCancelled = event.isCancelled,
            roadName = event.road?.name,
            location = event.location,
            shiftLeadName = event.secondaryLeads.leadsCaptionWith(event.shiftLead)
                .ifEmpty { event.shiftLead?.display },
            totalKm = mine.totalKm,
            participationStatus = mine.status,
            updatedAt = mine.updatedAt,
            draft = ResponderFillDraft(
                vehiclePlate = selected,
                odometerStart = mine.odometerStart?.toInt()?.toString() ?: "",
                odometerEnd = mine.odometerEnd?.toInt()?.toString() ?: "",
                route = mine.route.orEmpty(),
                treatmentDetail = mine.treatmentDetail.orEmpty(),
                treatmentNotes = mine.treatmentNotes.orEmpty(),
                treatedPlates = mapTreatedPlateRows(mine.treatedPlates.map { it.asInput }),
                treatedPlatePending = "",
            ),
            vehicles = options,
            endedAt = mine.endedAt,
        )
    }

    suspend fun saveFill(
        context: FillContext,
        draft: ResponderFillDraft,
        complete: Boolean,
        unfinishedMediaDraftCount: Int = 0,
    ): String? {
        val errors = validateResponderFillDraft(
            draft,
            if (complete) FillMode.COMPLETE else FillMode.DRAFT,
            context.vehicles.map { it.plate },
            context.totalKm,
            unfinishedMediaDraftCount,
        )
        if (!errors.isEmpty) {
            return if (complete) {
                errors.firstMessage ?: "יש למלא את כל שדות החובה לפני סיום הדיווח."
            } else {
                errors.firstMessage ?: "בדקו את השדות המסומנים."
            }
        }
        val start = parsedOdometer(draft.odometerStart)
        val end = parsedOdometer(draft.odometerEnd)
        val lockError = "לא ניתן לערוך דיווח שהושלם. רק אחמ״ש יכול לערוך."
        val saveError = "שמירת הדיווח נכשלה. בדקו את החיבור ונסו שוב."
        return try {
            val current = client.from("event_responders").select(
                Columns.raw("status, event:events!inner(status)"),
            ) {
                filter { eq("id", context.assignmentId) }
            }.decodeSingle<FillLockRow>()
            when (
                gateResponderFillWrite(
                    complete = complete,
                    participationStatus = current.status,
                    eventStatus = current.eventStatus,
                )
            ) {
                FillWriteGate.ALREADY_COMPLETE -> return null
                FillWriteGate.LOCKED -> return lockError
                FillWriteGate.PROCEED -> Unit
            }
            // Keep status writable until plates are saved — RLS blocks plate writes after done.
            val fieldsUpdated = client.from("event_responders").update(
                FillWrite(
                    vehiclePlate = plateNumberForSave(draft.vehiclePlate),
                    odometerStart = start,
                    odometerEnd = end,
                    route = draft.route.nilIfEmpty(),
                    treatmentDetail = draft.treatmentDetail.nilIfEmpty(),
                    treatmentNotes = draft.treatmentNotes.nilIfEmpty(),
                    status = ParticipationStatus.IN_PROGRESS.raw,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                filter { eq("id", context.assignmentId) }
                select(Columns.raw("id"))
            }.decodeList<IdRow>()
            if (fieldsUpdated.isEmpty()) {
                return if (complete && participationIsDone(context.assignmentId)) null else lockError
            }
            client.from("event_treated_plates").delete {
                filter { eq("event_responder_id", context.assignmentId) }
            }
            if (draft.treatedPlates.isNotEmpty()) {
                client.from("event_treated_plates").insert(
                    draft.treatedPlates.mapIndexed { index, row ->
                        TreatedPlateWrite(
                            eventResponderId = context.assignmentId,
                            plateNumber = row.plateNumber,
                            model = row.model,
                            color = row.color,
                            leftWhere = row.leftWhere?.trim()?.takeIf { it.isNotEmpty() },
                            manufacturer = row.manufacturer?.trim()?.takeIf { it.isNotEmpty() },
                            logoSlug = row.logoSlug?.trim()?.takeIf { it.isNotEmpty() },
                            sortOrder = index,
                        )
                    },
                )
            }
            if (complete) {
                val completed = client.from("event_responders").update(
                    FillStatusWrite(
                        status = ParticipationStatus.DONE.raw,
                        updatedAt = Instant.now().toString(),
                    ),
                ) {
                    filter { eq("id", context.assignmentId) }
                    select(Columns.raw("id"))
                }.decodeList<IdRow>()
                if (completed.isEmpty() && !participationIsDone(context.assignmentId)) {
                    return lockError
                }
            }
            runCatching {
                client.postgrest.rpc(
                    "apply_event_status_from_participations",
                    mapOf("p_event_id" to context.eventId),
                )
            }
            null
        } catch (_: Exception) {
            if (complete && participationIsDone(context.assignmentId)) null else saveError
        }
    }

    private suspend fun participationIsDone(assignmentId: String): Boolean =
        runCatching {
            client.from("event_responders").select(Columns.raw("status")) {
                filter { eq("id", assignmentId) }
            }.decodeSingle<FillLockRow>().status == ParticipationStatus.DONE
        }.getOrDefault(false)

    suspend fun saveAvailability(userId: String, status: AvailabilityStatus, availableFrom: String?): String? {
        return when (val write = buildAvailabilityWrite(status, availableFrom, israelToday())) {
            is AvailabilityWrite.Error -> write.message
            is AvailabilityWrite.Ok -> try {
                client.from("profiles").update(
                    AvailabilityWriteRow(
                        availability = write.availability.raw,
                        availableFrom = write.availableFrom,
                    ),
                ) {
                    filter { eq("id", userId) }
                }
                null
            } catch (_: Exception) {
                "עדכון הזמינות נכשל."
            }
        }
    }

    private val eventMediaSelect =
        "id, event_id, uploaded_by, caption, taken_when, storage_path, mime_type, byte_size, width, height, created_at, uploader:profiles!event_media_uploaded_by_fkey(full_name), plates:event_media_plates(treated_plate_id)"

    suspend fun listEventMedia(eventId: String): List<EventMedia> {
        return try {
            val rows = client.from("event_media").select(Columns.raw(eventMediaSelect)) {
                filter { eq("event_id", eventId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<EventMediaRow>()
            rows.map { it.toDomain(signedUrl = signedUrlFor(it.storagePath)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun listEventMediaPlates(eventId: String): List<EventMediaPlateOption> {
        val plateSelect = "id, plate_number, model, color, logo_slug"
        val eventKeyed = runCatching {
            client.from("event_treated_plates").select(Columns.raw(plateSelect)) {
                filter { eq("event_id", eventId) }
            }.decodeList<EventMediaPlateOptionRow>()
        }.getOrDefault(emptyList())
        val responderKeyed = runCatching {
            client.from("event_treated_plates").select(
                Columns.raw("$plateSelect, event_responders!event_treated_plates_event_responder_id_fkey!inner(event_id)"),
            ) {
                filter { eq("event_responders.event_id", eventId) }
            }.decodeList<EventMediaPlateOptionRow>()
        }.getOrDefault(emptyList())
        return mergeMediaPlates(responderKeyed.mapNotNull { it.toOption() }, eventKeyed.mapNotNull { it.toOption() })
    }

    suspend fun uploadEventMedia(
        eventId: String,
        jpegBytes: ByteArray,
        width: Int,
        height: Int,
        takenWhen: EventMediaTakenWhen,
        treatedPlateIds: List<String>,
        caption: String?,
    ): EventMediaWriteResult {
        val userId = sessionUserId() ?: return EventMediaWriteResult.Error(EVENT_MEDIA_NETWORK)
        val trimmed = caption?.trim()?.ifEmpty { null }
        captionError(trimmed.orEmpty())?.let { return EventMediaWriteResult.Error(it) }
        val id = UUID.randomUUID().toString()
        val storagePath = eventMediaStoragePath(eventId, id)
        try {
            client.storage.from("event-media").upload(storagePath, jpegBytes) {
                upsert = false
                contentType = ContentType.Image.JPEG
            }
        } catch (error: Exception) {
            return EventMediaWriteResult.Error(mapEventMediaError(error.message))
        }
        val inserted = try {
            client.from("event_media").insert(
                EventMediaInsert(
                    id = id,
                    eventId = eventId,
                    uploadedBy = userId,
                    caption = trimmed,
                    takenWhen = takenWhen.raw,
                    storagePath = storagePath,
                    mimeType = "image/jpeg",
                    byteSize = jpegBytes.size,
                    width = width,
                    height = height,
                ),
            ) {
                select(Columns.raw(eventMediaSelect))
            }.decodeSingle<EventMediaRow>()
        } catch (error: Exception) {
            runCatching { client.storage.from("event-media").delete(storagePath) }
            return EventMediaWriteResult.Error(mapEventMediaError(error.message))
        }
        when (val plates = replaceMediaPlates(id, treatedPlateIds)) {
            is EventMediaWriteResult.Error -> {
                runCatching { client.from("event_media").delete { filter { eq("id", id) } } }
                runCatching { client.storage.from("event-media").delete(storagePath) }
                return plates
            }
            else -> Unit
        }
        return EventMediaWriteResult.Uploaded(
            inserted.toDomain(
                signedUrl = signedUrlFor(storagePath),
                treatedPlateIds = uniquePlateIds(treatedPlateIds),
            ),
        )
    }

    suspend fun updateEventMedia(
        id: String,
        takenWhen: EventMediaTakenWhen,
        treatedPlateIds: List<String>,
        caption: String?,
    ): EventMediaWriteResult {
        val trimmed = caption?.trim()?.ifEmpty { null }
        captionError(trimmed.orEmpty())?.let { return EventMediaWriteResult.Error(it) }
        try {
            client.from("event_media").update(
                EventMediaUpdate(takenWhen = takenWhen.raw, caption = trimmed),
            ) {
                filter { eq("id", id) }
            }
        } catch (error: Exception) {
            return EventMediaWriteResult.Error(mapEventMediaError(error.message))
        }
        return replaceMediaPlates(id, treatedPlateIds)
    }

    suspend fun deleteEventMedia(id: String, storagePath: String): EventMediaWriteResult {
        return try {
            client.from("event_media").delete { filter { eq("id", id) } }
            runCatching { client.storage.from("event-media").delete(storagePath) }
            EventMediaWriteResult.Done
        } catch (error: Exception) {
            EventMediaWriteResult.Error(mapEventMediaError(error.message))
        }
    }

    private suspend fun replaceMediaPlates(
        mediaId: String,
        plateIds: List<String>,
    ): EventMediaWriteResult {
        val unique = uniquePlateIds(plateIds)
        try {
            client.from("event_media_plates").delete { filter { eq("media_id", mediaId) } }
            if (unique.isNotEmpty()) {
                client.from("event_media_plates").insert(
                    unique.map { EventMediaPlateWrite(mediaId = mediaId, treatedPlateId = it) },
                )
            }
            return EventMediaWriteResult.Done
        } catch (error: Exception) {
            return EventMediaWriteResult.Error(mapEventMediaError(error.message))
        }
    }

    private suspend fun signedUrlFor(storagePath: String): String? {
        return runCatching {
            client.storage.from("event-media").createSignedUrl(storagePath, 3600.seconds)
        }.getOrNull()
    }

    suspend fun loadTrack(token: String): TrackLoadResponse =
        invokeTrack(TrackCall("load", token, null, null, null, null))

    suspend fun pingTrack(token: String, lat: Double, lng: Double, accuracy: Double?): TrackLoadResponse =
        invokeTrack(TrackCall("ping", token, lat, lng, accuracy, Instant.now().toString()))

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun invokeTrack(body: TrackCall): TrackLoadResponse {
        return try {
            val text = client.functions.invoke(
                "responder-track",
                body,
                headers = extraFunctionHeaders(),
            ).bodyAsText()
            json.decodeFromString<TrackLoadResponse>(text)
        } catch (_: Exception) {
            TrackLoadResponse(
                ok = false,
                error = "שיתוף המיקום נכשל. בדקו את החיבור ונסו שוב.",
            )
        }
    }

    suspend fun submitUserFeedback(
        kind: String,
        body: String,
        pagePath: String?,
        audioBytes: ByteArray?,
        audioMime: String?,
        attachments: List<FeedbackAttachmentUpload> = emptyList(),
    ): String? {
        val hasAudio = audioBytes != null && audioBytes.isNotEmpty()
        feedbackSubmitError(kind, body, hasAudio)?.let { return it }
        val userId = sessionUserId() ?: return FEEDBACK_NETWORK
        if (hasAudio && audioBytes!!.size > FEEDBACK_AUDIO_MAX_BYTES) return FEEDBACK_AUDIO_SIZE_ERROR
        val incomingMeta = attachments.map {
            FeedbackPickedMeta(name = it.name, mime = it.mime, size = it.bytes.size)
        }
        val added = addFeedbackAttachments(emptyList(), incomingMeta)
        if (added.error != null) return added.error
        val id = UUID.randomUUID().toString()
        var storagePath: String? = null
        var mime: String? = null
        var size: Int? = null
        val uploadedPaths = mutableListOf<String>()
        if (hasAudio) {
            mime = normalizeFeedbackAudioMime(audioMime ?: "audio/mp4")
            storagePath = feedbackStoragePath(userId, id, mime)
            size = audioBytes.size
            try {
                client.storage.from("user-feedback").upload(storagePath, audioBytes) {
                    upsert = false
                    contentType = ContentType.parse(mime)
                }
                uploadedPaths += storagePath
            } catch (_: Exception) {
                return FEEDBACK_NETWORK
            }
        }
        val attachmentRows = mutableListOf<UserFeedbackAttachmentJson>()
        for (file in attachments) {
            val fileMime = normalizeFeedbackAttachmentMime(file.mime, file.name)
            val attachmentId = UUID.randomUUID().toString()
            val filePath = fileMime?.let {
                feedbackAttachmentStoragePath(userId, id, attachmentId, it, file.name)
            }
            if (fileMime == null || filePath == null) {
                uploadedPaths.forEach { path ->
                    runCatching { client.storage.from("user-feedback").delete(path) }
                }
                return FEEDBACK_ATTACH_TYPE_ERROR
            }
            try {
                client.storage.from("user-feedback").upload(filePath, file.bytes) {
                    upsert = false
                    contentType = ContentType.parse(fileMime)
                }
                uploadedPaths += filePath
                attachmentRows += UserFeedbackAttachmentJson(
                    path = filePath,
                    mime = fileMime,
                    size = file.bytes.size,
                    name = sanitizeFeedbackAttachmentName(file.name),
                )
            } catch (_: Exception) {
                uploadedPaths.forEach { path ->
                    runCatching { client.storage.from("user-feedback").delete(path) }
                }
                return FEEDBACK_NETWORK
            }
        }
        val trimmed = body.trim().ifEmpty { null }
        val path = pagePath?.trim()?.take(200)?.ifEmpty { null }
        try {
            client.from("user_feedback").insert(
                UserFeedbackInsert(
                    id = id,
                    userId = userId,
                    kind = kind,
                    body = trimmed,
                    pagePath = path,
                    status = "open",
                    audioStoragePath = storagePath,
                    audioMimeType = mime,
                    audioByteSize = size,
                    attachments = attachmentRows.takeIf { it.isNotEmpty() },
                ),
            )
        } catch (error: Exception) {
            uploadedPaths.forEach { path ->
                runCatching { client.storage.from("user-feedback").delete(path) }
            }
            if (attachmentRows.isNotEmpty() && isMissingFeedbackAttachmentsColumn(error.message)) {
                return FEEDBACK_ATTACH_UNAVAILABLE
            }
            return FEEDBACK_NETWORK
        }
        return null
    }
}

private fun EventMediaRow.toDomain(
    signedUrl: String?,
    treatedPlateIds: List<String>? = null,
): EventMedia {
    val whenTaken = parseEventMediaTakenWhen(takenWhen) ?: EventMediaTakenWhen.BEFORE_TREATMENT
    return EventMedia(
        id = id,
        eventId = eventId,
        uploadedBy = uploadedBy,
        uploaderName = uploader?.fullName?.trim()?.ifEmpty { null },
        treatedPlateIds = treatedPlateIds ?: uniquePlateIds(plates.map { it.treatedPlateId }),
        caption = caption,
        takenWhen = whenTaken,
        storagePath = storagePath,
        mimeType = mimeType,
        byteSize = byteSize,
        width = width,
        height = height,
        createdAt = createdAt,
        signedUrl = signedUrl,
    )
}

private fun EventMediaPlateOptionRow.toOption(): EventMediaPlateOption? {
    val plate = plateNumber?.trim().orEmpty()
    if (id.isEmpty() || plate.isEmpty()) return null
    return EventMediaPlateOption(
        id = id,
        plateNumber = plate,
        model = model,
        color = color,
        logoSlug = logoSlug,
    )
}

@Serializable
private data class UnitEventDetailRespondersWrap(
    val responders: List<UnitEventDetailResponderRow> = emptyList(),
)

@Serializable
private data class MyActiveEventPrefWrite(
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
    val kind: String,
)

@Serializable
private data class InviteClearRow(
    @SerialName("invite_pending") val invitePending: Boolean = false,
    @SerialName("invite_token") val inviteToken: String? = null,
    @SerialName("invite_token_expires_at") val inviteTokenExpiresAt: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
private data class TrackCall(
    val action: String,
    @SerialName("track_token") val trackToken: String,
    val lat: Double?,
    val lng: Double?,
    @SerialName("accuracy_m") val accuracyM: Double?,
    @SerialName("recorded_at") val recordedAt: String?,
)

@Serializable
private data class FillWrite(
    @SerialName("vehicle_plate") val vehiclePlate: String?,
    @SerialName("odometer_start") val odometerStart: Double?,
    @SerialName("odometer_end") val odometerEnd: Double?,
    val route: String?,
    @SerialName("treatment_detail") val treatmentDetail: String?,
    @SerialName("treatment_notes") val treatmentNotes: String?,
    val status: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
private data class FillStatusWrite(
    val status: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
private data class AvailabilityWriteRow(
    val availability: String,
    @SerialName("available_from") val availableFrom: String?,
)

@Serializable
private data class FillLockRow(
    @Serializable(with = ParticipationStatusSerializer::class)
    val status: ParticipationStatus,
    @Serializable(with = OptionalEventHolderSerializer::class)
    val event: EventStatusHolder? = null,
) {
    val eventStatus: EventStatus? get() = event?.status
}

@Serializable
private data class EventStatusHolder(
    @Serializable(with = EventStatusSerializer::class)
    val status: EventStatus,
)

private object OptionalEventHolderSerializer : OneOrNullSerializer<EventStatusHolder>(EventStatusHolder.serializer())
