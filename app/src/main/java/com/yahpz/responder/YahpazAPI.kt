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
import com.yahpz.domain.EVENT_DRAFT_DATE_ERROR
import com.yahpz.domain.EVENT_DRAFT_FORM_ERROR
import com.yahpz.domain.EVENT_DRAFT_SAVE_FAILED
import com.yahpz.domain.EventDraft
import com.yahpz.domain.EventStatus
import com.yahpz.domain.FillMode
import com.yahpz.domain.FuelRefundCreditInput
import com.yahpz.domain.FuelRefundParticipationInput
import com.yahpz.domain.FuelRefundProfileInput
import com.yahpz.domain.FuelRefundRow
import com.yahpz.domain.INVITE_IDENTITY_ERROR
import com.yahpz.domain.INVITE_SAVE_FAILED
import com.yahpz.domain.InviteDraft
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
import com.yahpz.domain.SHIFT_DRAFT_SAVE_FAILED
import com.yahpz.domain.SYSTEM_DISTRICT_LOCKED_ERROR
import com.yahpz.domain.ShiftDraft
import com.yahpz.domain.buildAvailabilityWrite
import com.yahpz.domain.buildDuplicateClusters
import com.yahpz.domain.buildEventsByResponderRows
import com.yahpz.domain.buildFuelQuarterRows
import com.yahpz.domain.buildFuelRefundRows
import com.yahpz.domain.buildKmDiscrepancyRows
import com.yahpz.domain.buildKmExceptionRows
import com.yahpz.domain.buildOpenDocRows
import com.yahpz.domain.canToggleEventCancelled
import com.yahpz.domain.closedListMeta
import com.yahpz.domain.closedListNameError
import com.yahpz.domain.deriveEventStatusAfterParticipation
import com.yahpz.domain.duplicateEventsReportRows
import com.yahpz.domain.eventDraftStatus
import com.yahpz.domain.eventsByResponderReportRows
import com.yahpz.domain.FuelQuarterParticipationInput
import com.yahpz.domain.FuelQuarterProfileInput
import com.yahpz.domain.FuelQuarterSavedDistribution
import com.yahpz.domain.fuelRefundReportRows
import com.yahpz.domain.isAdmin
import com.yahpz.domain.isSystemClosedListItem
import com.yahpz.domain.israelToday
import com.yahpz.domain.kmDiscrepancyReportRows
import com.yahpz.domain.kmExceptionReportRows
import com.yahpz.domain.mapClosedListDeleteError
import com.yahpz.domain.mapClosedListWriteError
import com.yahpz.domain.mapTreatedPlateRows
import com.yahpz.domain.needsBroadcastSubject
import com.yahpz.domain.normalizeReturnDate
import com.yahpz.domain.openDocReportRows
import com.yahpz.domain.parsedOdometer
import com.yahpz.domain.passwordStrengthError
import com.yahpz.domain.phoneDigits
import com.yahpz.domain.ProfileVehicle
import com.yahpz.domain.quarterLocalDateRange
import com.yahpz.domain.quarterMonthLabels
import com.yahpz.domain.resolveLeadKmReplacement
import com.yahpz.domain.VehicleRowInput
import com.yahpz.domain.plateDigits
import com.yahpz.domain.plateNumberForSave
import com.yahpz.domain.sortByRoadName
import com.yahpz.domain.validateBroadcastDraft
import com.yahpz.domain.validateEventDraft
import com.yahpz.domain.validateInviteDraft
import com.yahpz.domain.validateResponderFillDraft
import com.yahpz.domain.validateShiftDraft
import com.yahpz.domain.visibleProfileVehicles
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
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class APIException(override val message: String) : Exception(message)

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
    private val eventListSelect = """
        id, event_date, police_event_id, location, status, is_cancelled, origin, shift_id,
        event_type:event_types(name),
        road:roads(name),
        shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
        shift:shifts!events_shift_id_fkey(
          shift_date, shift_kind, vehicle_type,
          personal_vehicle:vehicles!shifts_personal_vehicle_id_fkey(plate_number)
        ),
        responders:event_responders(id, responder_id, status, profile:profiles(full_name, callsign))
    """.trimIndent()

    private val fillSelect = """
        id, status, event_date, police_event_id, location, is_cancelled,
        event_type:event_types(name),
        road:roads(name),
        shift_lead:profiles!events_shift_lead_id_fkey(full_name, callsign),
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
    }

    suspend fun sessionUserId(): String? {
        client.auth.awaitInitialization()
        return client.auth.currentSessionOrNull()?.user?.id
    }

    suspend fun signIn(email: String, password: String): String? {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
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
        runCatching { client.auth.signOut() }
    }

    suspend fun requestPasswordReset(email: String): String? {
        return try {
            client.auth.resetPasswordForEmail(email, AppConfig.passwordResetRedirect)
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

    suspend fun fetchUnitEvents(limit: Int = 80): List<EventListItem> =
        client.from("events").select(Columns.raw(eventListSelect)) {
            order("event_date", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<EventListItem>()

    suspend fun fetchUnitShifts(limit: Int = 80): List<ShiftListItem> =
        client.from("shifts").select(Columns.raw(shiftListSelect)) {
            order("shift_date", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<ShiftListItem>()

    suspend fun fetchAdminUsers(): List<AdminUserListItem> {
        val profiles = client.from("profiles").select(
            Columns.raw(
                "id, full_name, email, callsign, phone, active, availability, available_from, volunteer_status",
            ),
        ) {
            order("full_name", Order.ASCENDING)
        }.decodeList<AdminProfileRow>()
        val roles = client.from("user_roles").select(Columns.raw("user_id, role"))
            .decodeList<AdminRoleRow>()
            .groupBy({ it.userId }, { it.role })
        val vehicleCounts = client.from("vehicles").select(Columns.raw("user_id, archived"))
            .decodeList<AdminVehicleRow>()
            .filter { it.archived != true }
            .groupingBy { it.userId }
            .eachCount()
        return profiles.map { row ->
            AdminUserListItem(
                id = row.id,
                fullName = row.fullName,
                email = row.email,
                callsign = row.callsign,
                phone = row.phone,
                active = row.active,
                availability = row.availability,
                availableFrom = row.availableFrom,
                volunteerStatus = row.volunteerStatus,
                roles = roles[row.id].orEmpty(),
                vehicleCount = vehicleCounts[row.id] ?: 0,
            )
        }
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

    suspend fun inviteAdminUser(draft: InviteDraft): String? {
        val errors = validateInviteDraft(draft)
        if (!errors.isEmpty) return errors.formMessage ?: INVITE_IDENTITY_ERROR
        val call = AdminInviteCall(
            fullName = draft.fullName.trim(),
            email = draft.email.trim().lowercase(),
            callsign = draft.callsign.trim(),
            phone = phoneDigits(draft.phone).ifEmpty { null },
            volunteerStatus = draft.volunteerStatus.raw,
            roles = draft.roles.distinct(),
        )
        return invokeAdminUsers(call, INVITE_SAVE_FAILED)
    }

    suspend fun setAdminUserActive(userId: String, active: Boolean): String? = invokeAdminUsers(
        AdminSetActiveCall(action = if (active) "reactivate" else "deactivate", userId = userId),
        SET_ACTIVE_FAILED,
    )

    private suspend inline fun <reified T : Any> invokeAdminUsers(body: T, fallback: String): String? {
        val response = invokeEdge<T, AdminUsersResponse>("admin-users", body, fallback)
        return response.fold(
            onSuccess = { it.error },
            onFailure = { it.message ?: fallback },
        )
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
        Result.success(json.decodeFromString<R>(client.functions.invoke(function, body).bodyAsText()))
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
        val eventIds = client.from("events").select(Columns.raw("id")) {
            filter {
                eq("origin", "manual")
                gte("created_at", bounds.first)
                lte("created_at", bounds.second)
            }
        }.decodeList<IdRow>().map { it.id }
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
            districts = lookup("districts", "id, name, code"),
            eventTypes = lookup("event_types", "id, name"),
            roads = sortByRoadName(roads) { it.name },
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
        return if (key == ClosedListKey.ROADS) sortByRoadName(items) { it.name } else items
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

    /** Insert the event then its pending crew. Mirrors the web `saveEventForm` create path. */
    suspend fun createUnitEvent(draft: EventDraft, districts: List<LookupOption>): String? {
        val errors = validateEventDraft(draft, districts)
        if (!errors.isEmpty) return errors.formMessage ?: EVENT_DRAFT_FORM_ERROR
        val userId = sessionUserId() ?: return "יש להתחבר מחדש."
        val eventDate = normalizeReturnDate(draft.eventDate) ?: return EVENT_DRAFT_DATE_ERROR
        return try {
            val inserted = client.from("events").insert(
                EventInsert(
                    eventDate = eventDate,
                    policeEventId = draft.policeEventId.nilIfEmpty(),
                    districtId = draft.districtId.nilIfEmpty(),
                    eventTypeId = draft.eventTypeId,
                    roadId = draft.roadId,
                    location = draft.location.nilIfEmpty(),
                    notes = draft.notes.nilIfEmpty(),
                    status = eventDraftStatus(draft.responderIds.size).raw,
                    shiftLeadId = userId,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                select(Columns.raw("id"))
            }.decodeSingle<IdRow>()
            if (draft.responderIds.isNotEmpty()) {
                client.from("event_responders").insert(
                    draft.responderIds.distinct().map { responderId ->
                        EventResponderInsert(eventId = inserted.id, responderId = responderId)
                    },
                )
            }
            null
        } catch (_: Exception) {
            EVENT_DRAFT_SAVE_FAILED
        }
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
                id, event_date, police_event_id, district_id, event_type_id, road_id,
                location, notes, is_cancelled, status,
                responders:event_responders(id, responder_id, status)
                """.trimIndent(),
            ),
        ) {
            filter { eq("id", eventId) }
        }.decodeSingle()

    suspend fun updateUnitEvent(
        eventId: String,
        draft: EventDraft,
        districts: List<LookupOption>,
        viewerIsAdmin: Boolean,
        previousIsCancelled: Boolean,
    ): String? {
        val errors = validateEventDraft(draft, districts)
        if (!errors.isEmpty) return errors.formMessage ?: EVENT_DRAFT_FORM_ERROR
        if (previousIsCancelled && !draft.isCancelled) {
            canToggleEventCancelled(false, viewerIsAdmin)?.let { return it }
        }
        val eventDate = normalizeReturnDate(draft.eventDate) ?: return EVENT_DRAFT_DATE_ERROR
        return try {
            val existing = client.from("event_responders")
                .select(Columns.raw("id, responder_id, status")) {
                    filter { eq("event_id", eventId) }
                }.decodeList<EventFormResponderRow>()
            val keepIds = draft.responderIds.distinct().toSet()
            val toRemove = existing.filter { it.responderId !in keepIds }
            if (toRemove.isNotEmpty()) {
                client.from("event_responders").delete {
                    filter { isIn("id", toRemove.map { it.id }) }
                }
            }
            val existingResponderIds = existing.map { it.responderId }.toSet()
            val toAdd = keepIds.filter { it !in existingResponderIds }
            if (toAdd.isNotEmpty()) {
                client.from("event_responders").insert(
                    toAdd.map { responderId ->
                        EventResponderInsert(eventId = eventId, responderId = responderId)
                    },
                )
            }
            val remainingStatuses = existing
                .filter { it.responderId in keepIds }
                .map { it.status } + List(toAdd.size) { ParticipationStatus.PENDING }
            val nextStatus = deriveEventStatusAfterParticipation(remainingStatuses)
            val updated = client.from("events").update(
                EventUpdateWrite(
                    eventDate = eventDate,
                    policeEventId = draft.policeEventId.nilIfEmpty(),
                    districtId = draft.districtId.nilIfEmpty(),
                    eventTypeId = draft.eventTypeId,
                    roadId = draft.roadId,
                    location = draft.location.nilIfEmpty(),
                    notes = draft.notes.nilIfEmpty(),
                    isCancelled = draft.isCancelled,
                    status = nextStatus.raw,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                filter { eq("id", eventId) }
                select(Columns.raw("id"))
            }.decodeList<IdRow>()
            if (updated.isEmpty()) "אין הרשאה לעדכן את האירוע." else null
        } catch (_: Exception) {
            EVENT_DRAFT_SAVE_FAILED
        }
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
                detail.personalVehicleId
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
        val rows = client.from("vehicles").select(Columns.raw("plate_number, model, archived")) {
            filter { eq("user_id", userId) }
        }.decodeList<VehicleOption>()
        return visibleProfileVehicles(
            rows.map { VehicleRowInput(it.plateNumber, it.model, it.archived) },
        )
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
            shiftLeadName = event.shiftLead?.display,
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

    suspend fun saveFill(context: FillContext, draft: ResponderFillDraft, complete: Boolean): String? {
        val errors = validateResponderFillDraft(
            draft,
            if (complete) FillMode.COMPLETE else FillMode.DRAFT,
            context.vehicles.map { it.plate },
            context.totalKm,
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
        return try {
            val current = client.from("event_responders").select(
                Columns.raw("status, event:events!inner(status)"),
            ) {
                filter { eq("id", context.assignmentId) }
            }.decodeSingle<FillLockRow>()
            if (current.status == ParticipationStatus.DONE || current.eventStatus == EventStatus.DONE) {
                return "לא ניתן לערוך דיווח שהושלם. רק אחמ״ש יכול לערוך."
            }
            val updated = client.from("event_responders").update(
                FillWrite(
                    vehiclePlate = plateNumberForSave(draft.vehiclePlate),
                    odometerStart = start,
                    odometerEnd = end,
                    route = draft.route.nilIfEmpty(),
                    treatmentDetail = draft.treatmentDetail.nilIfEmpty(),
                    treatmentNotes = draft.treatmentNotes.nilIfEmpty(),
                    status = if (complete) ParticipationStatus.DONE.raw else ParticipationStatus.IN_PROGRESS.raw,
                    updatedAt = Instant.now().toString(),
                ),
            ) {
                filter { eq("id", context.assignmentId) }
                select(Columns.raw("id"))
            }.decodeList<IdRow>()
            if (updated.isEmpty()) {
                return "לא ניתן לערוך דיווח שהושלם. רק אחמ״ש יכול לערוך."
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
            runCatching {
                client.postgrest.rpc(
                    "apply_event_status_from_participations",
                    mapOf("p_event_id" to context.eventId),
                )
            }
            null
        } catch (_: Exception) {
            "שמירת הדיווח נכשלה. בדקו את החיבור ונסו שוב."
        }
    }

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

    suspend fun loadTrack(token: String): TrackLoadResponse =
        invokeTrack(TrackCall("load", token, null, null, null, null))

    suspend fun pingTrack(token: String, lat: Double, lng: Double, accuracy: Double?): TrackLoadResponse =
        invokeTrack(TrackCall("ping", token, lat, lng, accuracy, Instant.now().toString()))

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun invokeTrack(body: TrackCall): TrackLoadResponse {
        return try {
            val text = client.functions.invoke("responder-track", body).bodyAsText()
            json.decodeFromString<TrackLoadResponse>(text)
        } catch (_: Exception) {
            TrackLoadResponse(
                ok = false,
                error = "שיתוף המיקום נכשל. בדקו את החיבור ונסו שוב.",
            )
        }
    }
}

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
