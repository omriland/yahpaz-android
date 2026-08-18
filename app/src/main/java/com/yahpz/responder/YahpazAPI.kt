package com.yahpz.responder

import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.AvailabilityWrite
import com.yahpz.domain.EventStatus
import com.yahpz.domain.FillMode
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.ResponderFillDraft
import com.yahpz.domain.buildAvailabilityWrite
import com.yahpz.domain.israelToday
import com.yahpz.domain.mapTreatedPlateRows
import com.yahpz.domain.parsedOdometer
import com.yahpz.domain.passwordStrengthError
import com.yahpz.domain.plateDigits
import com.yahpz.domain.plateNumberForSave
import com.yahpz.domain.validateResponderFillDraft
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
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

class APIException(override val message: String) : Exception(message)

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
