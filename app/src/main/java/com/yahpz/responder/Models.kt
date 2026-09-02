package com.yahpz.responder

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import com.yahpz.domain.AdminUserSearchInput
import com.yahpz.domain.AdminUserSortKey
import com.yahpz.domain.AssignableProfile
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.BroadcastAudience
import com.yahpz.domain.BroadcastChannel
import com.yahpz.domain.BroadcastLogEntry
import com.yahpz.domain.BroadcastSendResult
import com.yahpz.domain.ClosedListItem
import com.yahpz.domain.CockpitEventInput
import com.yahpz.domain.CockpitResponderInput
import com.yahpz.domain.ContactSearchFields
import com.yahpz.domain.DuplicateParticipation
import com.yahpz.domain.EventDraft
import com.yahpz.domain.EventFreezeFlags
import com.yahpz.domain.EventResponderDraft
import com.yahpz.domain.EventStatus
import com.yahpz.domain.EventsByResponderEventInput
import com.yahpz.domain.EventsByResponderResponderInput
import com.yahpz.domain.KmDiscrepancyEventInput
import com.yahpz.domain.KmDiscrepancyResponderInput
import com.yahpz.domain.KmExceptionEventInput
import com.yahpz.domain.KmExceptionResponderInput
import com.yahpz.domain.LookupOption
import com.yahpz.domain.MineSearchFields
import com.yahpz.domain.MineShiftItem
import com.yahpz.domain.OpenDocEventInput
import com.yahpz.domain.OpenDocResponderInput
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.ResponderFillDraft
import com.yahpz.domain.SHIFT_KIND_LABELS
import com.yahpz.domain.ShiftDraft
import com.yahpz.domain.ShiftStatus
import com.yahpz.domain.TreatedPlateRowInput
import com.yahpz.domain.TreatedVehicleDraft
import com.yahpz.domain.VEHICLE_TYPE_LABELS
import com.yahpz.domain.formatNumber
import com.yahpz.domain.returnDateToInput
import com.yahpz.domain.toTimeInput
import com.yahpz.domain.FuelQuarterRow as DomainFuelQuarterRow
import com.yahpz.domain.formatDate
import com.yahpz.domain.formatPlate

@Serializable
data class Named(val name: String? = null)

@Serializable
data class PersonName(
    @SerialName("full_name") val fullName: String? = null,
    val callsign: String? = null,
) {
    val display: String?
        get() {
            val name = fullName?.trim().orEmpty()
            val sign = callsign?.trim().orEmpty()
            return when {
                name.isEmpty() && sign.isEmpty() -> null
                name.isEmpty() -> sign
                sign.isEmpty() -> name
                else -> "$name · $sign"
            }
        }
}

@Serializable
data class PlateRef(
    @SerialName("plate_number") val plateNumber: String? = null,
)

@Serializable
data class ShiftSummary(
    @SerialName("shift_date") val shiftDate: String? = null,
    @SerialName("shift_kind") val shiftKind: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    @SerialName("personal_vehicle")
    @Serializable(with = OptionalPlateRefSerializer::class)
    val personalVehicle: PlateRef? = null,
)

@Serializable
data class ResponderSummary(
    val id: String,
    @SerialName("responder_id") val responderId: String,
    @Serializable(with = ParticipationStatusSerializer::class)
    val status: ParticipationStatus,
    @SerialName("fill_completable_at") val fillCompletableAt: String? = null,
    @Serializable(with = OptionalPersonNameSerializer::class)
    val profile: PersonName? = null,
)

@Serializable
data class TreatedVehicleKindRow(
    val quantity: Int? = null,
    val kind: Named? = null,
)

@Serializable
data class UnitEventDetailResponderRow(
    val id: String,
    @SerialName("responder_id") val responderId: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("vehicle_plate") val vehiclePlate: String? = null,
    @SerialName("odometer_start")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerStart: Double? = null,
    @SerialName("odometer_end")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerEnd: Double? = null,
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
    val route: String? = null,
    @SerialName("treatment_detail") val treatmentDetail: String? = null,
    @SerialName("treatment_notes") val treatmentNotes: String? = null,
    @SerialName("emergency_means") val emergencyMeans: Boolean = false,
    @Serializable(with = ParticipationStatusSerializer::class)
    val status: ParticipationStatus,
    @Serializable(with = OptionalPersonNameSerializer::class)
    val profile: PersonName? = null,
    val treated: List<TreatedVehicleKindRow> = emptyList(),
    @SerialName("treated_plates") val treatedPlates: List<EventTreatedPlateRow> = emptyList(),
)

@Serializable
data class EventListItem(
    val id: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("police_event_id") val policeEventId: String? = null,
    val location: String? = null,
    @Serializable(with = EventStatusSerializer::class)
    val status: EventStatus,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("bus_lane") val busLane: Boolean = false,
    val origin: String? = null,
    @SerialName("shift_lead_id") val shiftLeadId: String? = null,
    @SerialName("shift_id") val shiftId: String? = null,
    @SerialName("frozen_over_60km") val frozenOver60km: Boolean = false,
    @SerialName("frozen_suspicious_duplicate") val frozenSuspiciousDuplicate: Boolean = false,
    @SerialName("event_type")
    @Serializable(with = OptionalNamedSerializer::class)
    val eventType: Named? = null,
    @Serializable(with = OptionalNamedSerializer::class)
    val road: Named? = null,
    @SerialName("shift_lead")
    @Serializable(with = OptionalPersonNameSerializer::class)
    val shiftLead: PersonName? = null,
    @Serializable(with = OptionalShiftSummarySerializer::class)
    val shift: ShiftSummary? = null,
    val responders: List<ResponderSummary> = emptyList(),
) {
    fun ownParticipation(userId: String): ParticipationStatus? =
        responders.firstOrNull { it.responderId == userId }?.status

    fun ownFillCompletableAt(userId: String): String? =
        responders.firstOrNull { it.responderId == userId }?.fillCompletableAt

    val freeze: EventFreezeFlags
        get() = EventFreezeFlags(frozenOver60km, frozenSuspiciousDuplicate)

    val typeLabel: String
        get() {
            val name = eventType?.name?.trim().orEmpty()
            return if (origin == "shift") {
                if (name.isEmpty()) "(משמרת)" else "$name (משמרת)"
            } else {
                name
            }
        }

    val shiftGroupTitle: String
        get() {
            val shift = shift ?: return "משמרת"
            val parts = mutableListOf("משמרת")
            shift.shiftDate?.let { parts += formatDate(it) }
            shift.shiftKind?.let { kind -> SHIFT_KIND_LABELS[kind]?.let { parts += it } }
            shift.vehicleType?.let { vehicle ->
                val label = VEHICLE_TYPE_LABELS[vehicle] ?: vehicle
                val plate = shift.personalVehicle?.plateNumber
                if (vehicle == "personal" && !plate.isNullOrEmpty()) {
                    parts += "$label ${formatPlate(plate)}"
                } else {
                    parts += label
                }
            }
            return parts.joinToString(" · ")
        }

    val searchFields: MineSearchFields
        get() = MineSearchFields(policeEventId, road?.name, location)

    /** Unit-wide lists also search by event type and the אחמ״ש who owns the event. */
    val unitSearchFields: List<String?>
        get() = listOf(policeEventId, road?.name, location, eventType?.name, shiftLead?.display, formatDate(eventDate))
}

@Serializable
data class CockpitResponderRow(
    val id: String,
    @SerialName("responder_id") val responderId: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @Serializable(with = ParticipationStatusSerializer::class)
    val status: ParticipationStatus = ParticipationStatus.PENDING,
) {
    val asInput: CockpitResponderInput
        get() = CockpitResponderInput(
            id = id,
            responderId = responderId,
            endedAt = endedAt,
            status = status,
        )
}

@Serializable
data class CockpitEventListItem(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("police_event_id") val policeEventId: String? = null,
    @Serializable(with = EventStatusSerializer::class)
    val status: EventStatus,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    val location: String? = null,
    @SerialName("frozen_over_60km") val frozenOver60km: Boolean = false,
    @SerialName("frozen_suspicious_duplicate") val frozenSuspiciousDuplicate: Boolean = false,
    @SerialName("location_lat")
    @Serializable(with = OptionalDoubleSerializer::class)
    val locationLat: Double? = null,
    @SerialName("location_lng")
    @Serializable(with = OptionalDoubleSerializer::class)
    val locationLng: Double? = null,
    @SerialName("event_type")
    @Serializable(with = OptionalNamedSerializer::class)
    val eventType: Named? = null,
    @Serializable(with = OptionalNamedSerializer::class)
    val road: Named? = null,
    @SerialName("shift_lead")
    @Serializable(with = OptionalPersonNameSerializer::class)
    val shiftLead: PersonName? = null,
    val responders: List<CockpitResponderRow> = emptyList(),
) {
    val freeze: EventFreezeFlags
        get() = EventFreezeFlags(frozenOver60km, frozenSuspiciousDuplicate)

    val asInput: CockpitEventInput
        get() = CockpitEventInput(
            id = id,
            createdAt = createdAt,
            policeEventId = policeEventId,
            status = status,
            isCancelled = isCancelled,
            location = location,
            locationLat = locationLat,
            locationLng = locationLng,
            eventTypeName = eventType?.name,
            roadName = road?.name,
            leadFullName = shiftLead?.fullName,
            leadCallsign = shiftLead?.callsign,
            responders = responders.map { it.asInput },
        )
}

@Serializable
data class VehicleOption(
    val id: String? = null,
    @SerialName("plate_number") val plateNumber: String,
    val model: String? = null,
    val archived: Boolean? = null,
    @SerialName("is_default") val isDefault: Boolean? = null,
)

@Serializable
data class CrewVehicleRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("plate_number") val plateNumber: String,
    val model: String? = null,
    val archived: Boolean? = null,
)

data class ResponderVehicle(val plate: String, val model: String) {
    val label: String
        get() {
            val modelText = model.trim()
            return if (modelText.isEmpty()) formatPlate(plate) else "${formatPlate(plate)} · $modelText"
        }
}

@Serializable
data class EventTreatedPlateRow(
    @SerialName("plate_number") val plateNumber: String? = null,
    val model: String? = null,
    val color: String? = null,
    @SerialName("left_where") val leftWhere: String? = null,
    val manufacturer: String? = null,
    @SerialName("logo_slug") val logoSlug: String? = null,
    @SerialName("sort_order")
    @Serializable(with = OptionalIntSerializer::class)
    val sortOrder: Int? = null,
) {
    val asInput: TreatedPlateRowInput
        get() = TreatedPlateRowInput(
            plateNumber = plateNumber,
            model = model,
            color = color,
            leftWhere = leftWhere,
            manufacturer = manufacturer,
            logoSlug = logoSlug,
            sortOrder = sortOrder,
        )
}

@Serializable
data class TreatedPlateWrite(
    @SerialName("event_responder_id") val eventResponderId: String,
    @SerialName("plate_number") val plateNumber: String,
    val model: String? = null,
    val color: String? = null,
    @SerialName("left_where") val leftWhere: String? = null,
    val manufacturer: String? = null,
    @SerialName("logo_slug") val logoSlug: String? = null,
    @SerialName("sort_order") val sortOrder: Int,
)

@Serializable
data class EventMediaPlateLink(
    @SerialName("treated_plate_id") val treatedPlateId: String,
)

@Serializable
data class EventMediaRow(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("uploaded_by") val uploadedBy: String,
    val caption: String? = null,
    @SerialName("taken_when") val takenWhen: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("mime_type") val mimeType: String = "image/jpeg",
    @SerialName("byte_size") val byteSize: Int,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("created_at") val createdAt: String,
    @Serializable(with = OptionalPersonNameSerializer::class)
    val uploader: PersonName? = null,
    val plates: List<EventMediaPlateLink> = emptyList(),
)

@Serializable
data class MyActiveEventPrefRow(
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
    val kind: String,
)

@Serializable
data class UserFeedbackAttachmentJson(
    val path: String,
    val mime: String,
    val size: Int,
    val name: String,
)

data class FeedbackAttachmentUpload(
    val name: String,
    val mime: String,
    val bytes: ByteArray,
)

@Serializable
data class UserFeedbackInsert(
    val id: String,
    @SerialName("user_id") val userId: String,
    val kind: String,
    val body: String? = null,
    @SerialName("page_path") val pagePath: String? = null,
    val status: String = "open",
    @SerialName("audio_storage_path") val audioStoragePath: String? = null,
    @SerialName("audio_mime_type") val audioMimeType: String? = null,
    @SerialName("audio_byte_size") val audioByteSize: Int? = null,
    val attachments: List<UserFeedbackAttachmentJson>? = null,
)

@Serializable
data class EventMediaInsert(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("uploaded_by") val uploadedBy: String,
    val caption: String? = null,
    @SerialName("taken_when") val takenWhen: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("mime_type") val mimeType: String = "image/jpeg",
    @SerialName("byte_size") val byteSize: Int,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class EventMediaUpdate(
    @SerialName("taken_when") val takenWhen: String,
    val caption: String? = null,
)

@Serializable
data class EventMediaPlateWrite(
    @SerialName("media_id") val mediaId: String,
    @SerialName("treated_plate_id") val treatedPlateId: String,
)

@Serializable
data class EventMediaPlateOptionRow(
    val id: String,
    @SerialName("plate_number") val plateNumber: String? = null,
    val model: String? = null,
    val color: String? = null,
    @SerialName("logo_slug") val logoSlug: String? = null,
)

@Serializable
data class FillAssignmentRow(
    val id: String,
    @SerialName("responder_id") val responderId: String,
    @SerialName("vehicle_plate") val vehiclePlate: String? = null,
    @SerialName("odometer_start")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerStart: Double? = null,
    @SerialName("odometer_end")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerEnd: Double? = null,
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
    val route: String? = null,
    @SerialName("treatment_detail") val treatmentDetail: String? = null,
    @SerialName("treatment_notes") val treatmentNotes: String? = null,
    @Serializable(with = ParticipationStatusSerializer::class)
    val status: ParticipationStatus,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("treated_plates") val treatedPlates: List<EventTreatedPlateRow> = emptyList(),
)

@Serializable
data class FillEventRow(
    val id: String,
    @Serializable(with = EventStatusSerializer::class)
    val status: EventStatus,
    @SerialName("event_date") val eventDate: String,
    @SerialName("police_event_id") val policeEventId: String? = null,
    val location: String? = null,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("event_type")
    @Serializable(with = OptionalNamedSerializer::class)
    val eventType: Named? = null,
    @Serializable(with = OptionalNamedSerializer::class)
    val road: Named? = null,
    @SerialName("shift_lead")
    @Serializable(with = OptionalPersonNameSerializer::class)
    val shiftLead: PersonName? = null,
    val responders: List<FillAssignmentRow> = emptyList(),
)

data class FillContext(
    val eventId: String,
    val assignmentId: String,
    val eventStatus: EventStatus,
    val eventDate: String,
    val policeEventId: String?,
    val eventTypeName: String?,
    val isCancelled: Boolean,
    val roadName: String?,
    val location: String?,
    val shiftLeadName: String?,
    val totalKm: Double?,
    val participationStatus: ParticipationStatus,
    val updatedAt: String?,
    val draft: ResponderFillDraft,
    val vehicles: List<ResponderVehicle>,
    val endedAt: String?,
)

@Serializable
data class ProfileRecord(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    val callsign: String = "",
    val phone: String? = null,
    val active: Boolean = true,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
    @Serializable(with = AvailabilityStatusSerializer::class)
    val availability: AvailabilityStatus = AvailabilityStatus.AVAILABLE,
    @SerialName("available_from") val availableFrom: String? = null,
    @SerialName("lifetime_event_count")
    @Serializable(with = OptionalIntSerializer::class)
    val lifetimeEventCount: Int? = 0,
    @SerialName("lifetime_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val lifetimeKm: Double? = 0.0,
    @SerialName("lifetime_stats_updated_at") val lifetimeStatsUpdatedAt: String? = null,
) {
    val eventCount: Int get() = lifetimeEventCount ?: 0
    val km: Double get() = lifetimeKm ?: 0.0
}

@Serializable
data class UnitContact(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val callsign: String = "",
    val phone: String? = null,
    val email: String = "",
) {
    val searchFields: ContactSearchFields
        get() = ContactSearchFields(fullName, callsign, email, phone)
}

@Serializable
data class AdminProfileRow(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    val callsign: String = "",
    val phone: String? = null,
    val active: Boolean = true,
    @SerialName("invite_pending") val invitePending: Boolean = false,
    @SerialName("otp_login_enabled") val otpLoginEnabled: Boolean = false,
    @SerialName("otp_users_page_enabled") val otpUsersPageEnabled: Boolean = false,
    @Serializable(with = AvailabilityStatusSerializer::class)
    val availability: AvailabilityStatus = AvailabilityStatus.AVAILABLE,
    @SerialName("available_from") val availableFrom: String? = null,
    @SerialName("volunteer_status") val volunteerStatus: String? = null,
)

@Serializable
data class AdminRoleRow(
    @SerialName("user_id") val userId: String,
    val role: String,
)

@Serializable
data class AdminVehicleRow(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("plate_number") val plateNumber: String = "",
    val model: String = "",
    val archived: Boolean? = null,
)

@Serializable
data class AdminAddressRow(
    @SerialName("user_id") val userId: String,
    val kind: String = "",
    val label: String? = null,
    @SerialName("formatted_address") val formattedAddress: String = "",
)

data class AdminVehicleItem(
    val id: String,
    val plateNumber: String,
    val model: String,
    val archived: Boolean,
)

data class AdminAddressItem(
    val kind: String,
    val label: String?,
    val formattedAddress: String,
)

data class AdminUserListItem(
    val id: String,
    val fullName: String,
    val email: String,
    val callsign: String,
    val phone: String?,
    val active: Boolean,
    val invitePending: Boolean = false,
    val otpLoginEnabled: Boolean = false,
    val otpUsersPageEnabled: Boolean = false,
    val availability: AvailabilityStatus,
    val availableFrom: String?,
    val volunteerStatus: String?,
    val roles: List<String>,
    val vehicles: List<AdminVehicleItem> = emptyList(),
    val addresses: List<AdminAddressItem> = emptyList(),
) {
    val vehicleCount: Int
        get() = vehicles.count { !it.archived }

    val searchFields: ContactSearchFields
        get() = ContactSearchFields(fullName, callsign, email, phone)

    val searchInput: AdminUserSearchInput
        get() = AdminUserSearchInput(
            fullName = fullName,
            callsign = callsign,
            email = email,
            volunteerStatus = volunteerStatus,
            availability = availability,
            availableFrom = availableFrom,
            active = active,
            invitePending = invitePending,
        )

    val sortKey: AdminUserSortKey
        get() = AdminUserSortKey(fullName = fullName, active = active, invitePending = invitePending)
}

data class AdminUsersActionResult(
    val error: String? = null,
    val message: String? = null,
    val userId: String? = null,
    val actionLink: String? = null,
) {
    val ok: Boolean get() = error == null
}

@Serializable
data class BroadcastProfileRow(
    val id: String,
    val email: String? = null,
    val phone: String? = null,
    val active: Boolean = true,
    @SerialName("invite_pending") val invitePending: Boolean = false,
)

@Serializable
data class UnitBroadcastRow(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @Serializable(with = BroadcastChannelSerializer::class)
    val channel: BroadcastChannel,
    @Serializable(with = BroadcastAudienceSerializer::class)
    val audience: BroadcastAudience,
    val subject: String? = null,
    val body: String = "",
    @SerialName("recipient_count")
    @Serializable(with = OptionalIntSerializer::class)
    val recipientCount: Int? = null,
    @SerialName("push_count")
    @Serializable(with = OptionalIntSerializer::class)
    val pushCount: Int? = null,
    @SerialName("push_failed_count")
    @Serializable(with = OptionalIntSerializer::class)
    val pushFailedCount: Int? = null,
    @Serializable(with = OptionalPersonNameSerializer::class)
    val sender: PersonName? = null,
) {
    val asEntry: BroadcastLogEntry
        get() = BroadcastLogEntry(
            id = id,
            createdAt = createdAt,
            channel = channel,
            audience = audience,
            subject = subject.orEmpty(),
            body = body,
            recipientCount = recipientCount ?: 0,
            pushCount = pushCount ?: 0,
            pushFailedCount = pushFailedCount ?: 0,
            senderName = sender?.fullName,
            senderCallsign = sender?.callsign,
        )
}

@Serializable
data class NotifyFillReadyCall(
    val action: String = "notify_fill_ready",
    @SerialName("event_responder_ids") val eventResponderIds: List<String>,
)

@Serializable
data class NotifyFillReadyResponse(
    val error: String? = null,
    val sent: List<String> = emptyList(),
)

@Serializable
data class BroadcastSendCall(
    val action: String = "send",
    val channel: String,
    val audience: String,
    val subject: String,
    val body: String,
)

@Serializable
data class BroadcastSendResponse(
    val error: String? = null,
    @SerialName("recipient_count") val recipientCount: Int? = null,
    @SerialName("skipped_no_phone") val skippedNoPhone: Int? = null,
    @SerialName("skipped_no_email") val skippedNoEmail: Int? = null,
    @SerialName("failed_count") val failedCount: Int? = null,
    @SerialName("push_count") val pushCount: Int? = null,
    @SerialName("push_failed_count") val pushFailedCount: Int? = null,
) {
    val asResult: BroadcastSendResult
        get() = BroadcastSendResult(
            recipientCount = recipientCount ?: 0,
            skippedNoPhone = skippedNoPhone ?: 0,
            skippedNoEmail = skippedNoEmail ?: 0,
            failedCount = failedCount ?: 0,
            pushCount = pushCount ?: 0,
            pushFailedCount = pushFailedCount ?: 0,
        )
}

@Serializable
data class AdminInviteVehicle(
    @SerialName("plate_number") val plateNumber: String,
    val model: String,
)

@Serializable
data class AdminInviteCall(
    val action: String = "invite",
    @SerialName("full_name") val fullName: String,
    val email: String,
    val callsign: String,
    val phone: String? = null,
    @SerialName("volunteer_status") val volunteerStatus: String,
    val roles: List<String>,
    val vehicles: List<AdminInviteVehicle> = emptyList(),
)

@Serializable
data class AdminSetActiveCall(
    val action: String,
    @SerialName("user_id") val userId: String,
)

@Serializable
data class AdminInviteLinkCall(
    val action: String,
    @SerialName("user_id") val userId: String,
    @SerialName("send_email") val sendEmail: Boolean,
)

@Serializable
data class AdminUsersResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val message: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("action_link") val actionLink: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val target: ImpersonationTargetSummary? = null,
)

@Serializable
data class ImpersonationTargetSummary(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val callsign: String,
)

@Serializable
data class ImpersonateCall(
    val action: String,
    @SerialName("target_user_id") val targetUserId: String,
)

@Serializable
data class AdminProfileSaveRow(
    @SerialName("full_name") val fullName: String,
    val callsign: String,
    val phone: String?,
    @SerialName("volunteer_status") val volunteerStatus: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class UserRoleWrite(
    @SerialName("user_id") val userId: String,
    val role: String,
)

@Serializable
data class AdminVehicleInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("plate_number") val plateNumber: String,
    val model: String,
    val archived: Boolean = false,
)

@Serializable
data class AdminVehicleArchivedWrite(
    val archived: Boolean,
)

@Serializable
data class AdminVehiclePlateWrite(
    @SerialName("plate_number") val plateNumber: String,
    val model: String,
    val archived: Boolean = false,
)

@Serializable
data class VolunteerStatusWrite(
    @SerialName("volunteer_status") val volunteerStatus: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class SetOtpLoginCall(
    val action: String = "set_otp_flags",
    @SerialName("user_id") val userId: String,
    @SerialName("otp_login_enabled") val otpLoginEnabled: Boolean,
)

@Serializable
data class SetOtpUsersPageCall(
    val action: String = "set_otp_flags",
    @SerialName("user_id") val userId: String,
    @SerialName("otp_users_page_enabled") val otpUsersPageEnabled: Boolean,
)

@Serializable
data class PhoneOtpResponse(
    val error: String? = null,
    val message: String? = null,
)

@Serializable
data class VehiclePlateRef(
    @SerialName("vehicle_plate") val vehiclePlate: String? = null,
)

@Serializable
data class SetDefaultVehicleCall(
    @SerialName("p_vehicle_id") val vehicleId: String,
)

@Serializable
data class ReportAndroidSessionCall(
    @SerialName("p_version_code") val versionCode: Int,
    @SerialName("p_version_name") val versionName: String,
)

@Serializable
data class KmDiscrepancyResponderRow(
    val id: String,
    @Serializable(with = ParticipationStatusSerializer::class)
    val status: ParticipationStatus,
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
    @SerialName("odometer_start")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerStart: Double? = null,
    @SerialName("odometer_end")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerEnd: Double? = null,
    @Serializable(with = OptionalPersonNameSerializer::class)
    val profile: PersonName? = null,
)

@Serializable
data class KmDiscrepancyEventRow(
    val id: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("police_event_id") val policeEventId: String? = null,
    val location: String? = null,
    @Serializable(with = OptionalNamedSerializer::class)
    val road: Named? = null,
    @SerialName("shift_lead")
    @Serializable(with = OptionalPersonNameSerializer::class)
    val shiftLead: PersonName? = null,
    val responders: List<KmDiscrepancyResponderRow> = emptyList(),
) {
    val asInput: KmDiscrepancyEventInput
        get() = KmDiscrepancyEventInput(
            id = id,
            eventDate = eventDate,
            isCancelled = isCancelled,
            policeEventId = policeEventId,
            location = location,
            roadName = road?.name,
            leadName = shiftLead?.fullName,
            leadCallsign = shiftLead?.callsign,
            responders = responders.map { row ->
                KmDiscrepancyResponderInput(
                    assignmentId = row.id,
                    status = row.status,
                    totalKm = row.totalKm,
                    odometerStart = row.odometerStart,
                    odometerEnd = row.odometerEnd,
                    name = row.profile?.fullName,
                    callsign = row.profile?.callsign,
                )
            },
        )
}

@Serializable
data class LeadKmRow(
    val id: String,
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
    @SerialName("odometer_start")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerStart: Double? = null,
    @SerialName("odometer_end")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerEnd: Double? = null,
)

@Serializable
data class LeadKmWrite(
    @SerialName("total_km") val totalKm: Double,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class DuplicateResponderRow(
    @SerialName("responder_id") val responderId: String,
    @SerialName("started_at") val startedAt: String? = null,
    @Serializable(with = OptionalPersonNameSerializer::class)
    val profile: PersonName? = null,
)

@Serializable
data class DuplicateEventRow(
    val id: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("police_event_id") val policeEventId: String? = null,
    val location: String? = null,
    @SerialName("event_type")
    @Serializable(with = OptionalNamedSerializer::class)
    val eventType: Named? = null,
    @Serializable(with = OptionalNamedSerializer::class)
    val road: Named? = null,
    val responders: List<DuplicateResponderRow> = emptyList(),
) {
    val asParticipations: List<DuplicateParticipation>
        get() = responders.map { row ->
            DuplicateParticipation(
                eventId = id,
                responderId = row.responderId,
                eventDate = eventDate,
                location = location,
                startedAt = row.startedAt,
                isCancelled = isCancelled,
                policeEventId = policeEventId,
                eventTypeName = eventType?.name,
                roadName = road?.name,
                name = row.profile?.fullName,
                callsign = row.profile?.callsign,
            )
        }
}

@Serializable
data class OpenDocResponderRow(
    @SerialName("responder_id") val responderId: String,
    @Serializable(with = ParticipationStatusSerializer::class)
    val status: ParticipationStatus,
    @Serializable(with = OptionalPersonNameSerializer::class)
    val profile: PersonName? = null,
)

@Serializable
data class OpenDocEventRow(
    val id: String,
    @SerialName("event_date") val eventDate: String,
    @Serializable(with = EventStatusSerializer::class)
    val status: EventStatus,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("police_event_id") val policeEventId: String? = null,
    val location: String? = null,
    @SerialName("shift_lead_id") val shiftLeadId: String? = null,
    @Serializable(with = OptionalNamedSerializer::class)
    val road: Named? = null,
    @SerialName("shift_lead")
    @Serializable(with = OptionalPersonNameSerializer::class)
    val shiftLead: PersonName? = null,
    val responders: List<OpenDocResponderRow> = emptyList(),
) {
    val asInput: OpenDocEventInput
        get() = OpenDocEventInput(
            id = id,
            eventDate = eventDate,
            status = status,
            isCancelled = isCancelled,
            policeEventId = policeEventId,
            location = location,
            roadName = road?.name,
            shiftLeadId = shiftLeadId,
            leadName = shiftLead?.fullName,
            leadCallsign = shiftLead?.callsign,
            responders = responders.map { row ->
                OpenDocResponderInput(
                    responderId = row.responderId,
                    status = row.status,
                    name = row.profile?.fullName,
                    callsign = row.profile?.callsign,
                )
            },
        )
}

@Serializable
data class LookupRow(
    val id: String,
    val name: String = "",
    val code: String? = null,
) {
    val asOption: LookupOption get() = LookupOption(id = id, name = name, code = code)
}

@Serializable
data class ClosedListItemRow(
    val id: String,
    val name: String = "",
    val active: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val code: String? = null,
) {
    val asItem: ClosedListItem
        get() = ClosedListItem(
            id = id,
            name = name,
            active = active,
            sortOrder = sortOrder,
            code = code,
        )
}

@Serializable
data class ClosedListInsert(
    val name: String,
    @SerialName("sort_order") val sortOrder: Int,
    val active: Boolean = true,
)

@Serializable
data class ClosedListNameUpdate(
    val name: String,
)

data class EventLookups(
    val districts: List<LookupOption> = emptyList(),
    val eventTypes: List<LookupOption> = emptyList(),
    val roads: List<LookupOption> = emptyList(),
    val vehicleKinds: List<LookupOption> = emptyList(),
) {
    val isEmpty: Boolean get() = eventTypes.isEmpty() && roads.isEmpty()
}

@Serializable
data class AssignableProfileRow(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val callsign: String = "",
) {
    val asProfile: AssignableProfile get() = AssignableProfile(id = id, fullName = fullName, callsign = callsign)
}

@Serializable
data class ReportResponderRow(
    @SerialName("responder_id") val responderId: String = "",
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
    @Serializable(with = OptionalPersonNameSerializer::class)
    val profile: PersonName? = null,
)

/** One select serves both the by-responder and the km-exception reports. */
@Serializable
data class ReportEventRow(
    val id: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("police_event_id") val policeEventId: String? = null,
    val location: String? = null,
    @SerialName("event_type")
    @Serializable(with = OptionalNamedSerializer::class)
    val eventType: Named? = null,
    @Serializable(with = OptionalNamedSerializer::class)
    val district: Named? = null,
    @Serializable(with = OptionalNamedSerializer::class)
    val road: Named? = null,
    @SerialName("shift_lead")
    @Serializable(with = OptionalPersonNameSerializer::class)
    val shiftLead: PersonName? = null,
    val responders: List<ReportResponderRow> = emptyList(),
) {
    val asEventsByResponderInput: EventsByResponderEventInput
        get() = EventsByResponderEventInput(
            id = id,
            eventDate = eventDate,
            isCancelled = isCancelled,
            policeEventId = policeEventId,
            location = location,
            eventTypeName = eventType?.name,
            districtName = district?.name,
            roadName = road?.name,
            leadName = shiftLead?.fullName,
            leadCallsign = shiftLead?.callsign,
            responders = responders.map { row ->
                EventsByResponderResponderInput(
                    responderId = row.responderId,
                    totalKm = row.totalKm,
                    name = row.profile?.fullName,
                    callsign = row.profile?.callsign,
                )
            },
        )

    val asKmExceptionInput: KmExceptionEventInput
        get() = KmExceptionEventInput(
            id = id,
            eventDate = eventDate,
            isCancelled = isCancelled,
            policeEventId = policeEventId,
            location = location,
            eventTypeName = eventType?.name,
            roadName = road?.name,
            leadName = shiftLead?.fullName,
            leadCallsign = shiftLead?.callsign,
            responders = responders.map { row ->
                KmExceptionResponderInput(
                    totalKm = row.totalKm,
                    name = row.profile?.fullName,
                    callsign = row.profile?.callsign,
                )
            },
        )
}

@Serializable
data class FuelParticipationRow(
    @SerialName("responder_id") val responderId: String = "",
    @SerialName("event_id") val eventId: String = "",
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
)

@Serializable
data class FuelEventFreezeRow(
    val id: String,
    @SerialName("frozen_over_60km") val frozenOver60km: Boolean = false,
    @SerialName("frozen_suspicious_duplicate") val frozenSuspiciousDuplicate: Boolean = false,
) {
    val isFrozen: Boolean get() = frozenOver60km || frozenSuspiciousDuplicate
}

@Serializable
data class FuelShiftRow(
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
    @Serializable(with = OptionalVehicleOwnerSerializer::class)
    val vehicles: VehicleOwner? = null,
)

@Serializable
data class VehicleOwner(@SerialName("user_id") val userId: String? = null)

object OptionalVehicleOwnerSerializer : OneOrNullSerializer<VehicleOwner>(VehicleOwner.serializer())

@Serializable
data class EventInsert(
    @SerialName("event_date") val eventDate: String,
    @SerialName("police_event_id") val policeEventId: String? = null,
    @SerialName("district_id") val districtId: String? = null,
    @SerialName("patrol_callsign") val patrolCallsign: String? = null,
    @SerialName("event_type_id") val eventTypeId: String? = null,
    @SerialName("road_id") val roadId: String? = null,
    val location: String? = null,
    val notes: String? = null,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("bus_lane") val busLane: Boolean = false,
    val status: String,
    @SerialName("shift_lead_id") val shiftLeadId: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class EventResponderInsert(
    @SerialName("event_id") val eventId: String,
    @SerialName("responder_id") val responderId: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("total_km") val totalKm: Double? = null,
    @SerialName("emergency_means") val emergencyMeans: Boolean = false,
    val status: String = ParticipationStatus.PENDING.raw,
)

@Serializable
data class EventResponderLeadWrite(
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("total_km") val totalKm: Double? = null,
    @SerialName("emergency_means") val emergencyMeans: Boolean = false,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class EventTreatedVehicleInsert(
    @SerialName("event_responder_id") val eventResponderId: String,
    @SerialName("vehicle_kind_id") val vehicleKindId: String,
    val quantity: Int,
)

@Serializable
data class EventCancelWrite(
    @SerialName("is_cancelled") val isCancelled: Boolean,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class EventFormTreatedRow(
    @SerialName("vehicle_kind_id") val vehicleKindId: String,
    val quantity: Int = 0,
)

@Serializable
data class EventFormDetail(
    val id: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("police_event_id") val policeEventId: String? = null,
    @SerialName("district_id") val districtId: String? = null,
    @SerialName("patrol_callsign") val patrolCallsign: String? = null,
    @SerialName("event_type_id") val eventTypeId: String? = null,
    @SerialName("road_id") val roadId: String? = null,
    val location: String? = null,
    val notes: String? = null,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("bus_lane") val busLane: Boolean = false,
    @Serializable(with = EventStatusSerializer::class)
    val status: EventStatus = EventStatus.DRAFT,
    val responders: List<EventFormResponderRow> = emptyList(),
) {
    fun toDraft(vehicleOwnerIds: Set<String>): EventDraft = EventDraft(
        eventDate = returnDateToInput(eventDate),
        policeEventId = policeEventId.orEmpty(),
        patrolCallsign = patrolCallsign.orEmpty(),
        eventTypeId = eventTypeId.orEmpty(),
        roadId = roadId.orEmpty(),
        districtId = districtId.orEmpty(),
        location = location.orEmpty(),
        notes = notes.orEmpty(),
        responders = responders.map { it.toDraft(vehicleOwnerIds.contains(it.responderId)) },
        isCancelled = isCancelled,
        busLane = busLane,
    )
}

@Serializable
data class EventFormResponderRow(
    val id: String,
    @SerialName("responder_id") val responderId: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
    @SerialName("emergency_means") val emergencyMeans: Boolean = false,
    @Serializable(with = ParticipationStatusSerializer::class)
    val status: ParticipationStatus = ParticipationStatus.PENDING,
    val treated: List<EventFormTreatedRow> = emptyList(),
) {
    fun toDraft(hasVehicle: Boolean): EventResponderDraft = EventResponderDraft(
        responderId = responderId,
        assignmentId = id,
        startTime = toTimeInput(startedAt),
        endTime = toTimeInput(endedAt),
        totalKm = totalKm?.let { formatNumber(it) }.orEmpty(),
        emergencyMeans = emergencyMeans,
        treated = treated.map { TreatedVehicleDraft(vehicleKindId = it.vehicleKindId, quantity = it.quantity) },
        status = status,
        hasVehicle = hasVehicle,
    )
}

@Serializable
data class EventUpdateWrite(
    @SerialName("event_date") val eventDate: String,
    @SerialName("police_event_id") val policeEventId: String? = null,
    @SerialName("district_id") val districtId: String? = null,
    @SerialName("patrol_callsign") val patrolCallsign: String? = null,
    @SerialName("event_type_id") val eventTypeId: String? = null,
    @SerialName("road_id") val roadId: String? = null,
    val location: String? = null,
    val notes: String? = null,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("bus_lane") val busLane: Boolean = false,
    val status: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ShiftFormDetail(
    val id: String,
    @SerialName("shift_date") val shiftDate: String,
    @SerialName("shift_kind") val shiftKind: String,
    @SerialName("vehicle_type") val vehicleType: String,
    val notes: String? = null,
    @SerialName("personal_vehicle_id") val personalVehicleId: String? = null,
    val responders: List<ShiftCrewRow> = emptyList(),
) {
    fun toDraft(): ShiftDraft = ShiftDraft(
        shiftDate = returnDateToInput(shiftDate),
        shiftKind = shiftKind,
        vehicleType = vehicleType,
        notes = notes.orEmpty(),
        responderIds = responders.map { it.responderId },
        personalVehicleId = personalVehicleId,
    )
}

@Serializable
data class ShiftUpdateWrite(
    @SerialName("shift_date") val shiftDate: String,
    @SerialName("shift_kind") val shiftKind: String,
    @SerialName("vehicle_type") val vehicleType: String,
    @SerialName("personal_vehicle_id") val personalVehicleId: String? = null,
    val notes: String? = null,
    @SerialName("last_saved_by") val lastSavedBy: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class FuelQuarterInsert(
    val year: Int,
    val quarter: Int,
    val status: String = "draft",
)

@Serializable
data class FuelQuarterRowDb(
    val id: String,
    val year: Int,
    val quarter: Int,
    val status: String,
)

@Serializable
data class FuelQuarterDistributionRow(
    @SerialName("responder_id") val responderId: String,
    val cards: Int? = null,
    @SerialName("card_numbers") val cardNumbers: String? = null,
    @SerialName("remaining_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val remainingKm: Double? = null,
)

@Serializable
data class FuelQuarterParticipationRow(
    @SerialName("responder_id") val responderId: String,
    @SerialName("total_km")
    @Serializable(with = OptionalDoubleSerializer::class)
    val totalKm: Double? = null,
    @Serializable(with = OptionalFuelQuarterEventSerializer::class)
    val events: FuelQuarterEventEmbed? = null,
)

@Serializable
data class FuelQuarterEventEmbed(
    @SerialName("created_at") val createdAt: String,
    val status: String? = null,
)

object OptionalFuelQuarterEventSerializer : OneOrNullSerializer<FuelQuarterEventEmbed>(
    FuelQuarterEventEmbed.serializer(),
)

@Serializable
data class FuelQuarterProfileRow(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val callsign: String,
    val active: Boolean = true,
)

data class FuelQuarterWorkbook(
    val quarterId: String,
    val year: Int,
    val quarter: Int,
    val status: String,
    val monthLabels: List<String>,
    val rows: List<DomainFuelQuarterRow>,
)

@Serializable
data class ShiftInsert(
    @SerialName("shift_date") val shiftDate: String,
    @SerialName("shift_kind") val shiftKind: String,
    @SerialName("vehicle_type") val vehicleType: String,
    @SerialName("personal_vehicle_id") val personalVehicleId: String? = null,
    @SerialName("odometer_start") val odometerStart: Double? = null,
    @SerialName("odometer_end") val odometerEnd: Double? = null,
    @SerialName("total_km") val totalKm: Double? = null,
    val notes: String? = null,
    val status: String = ShiftStatus.DRAFT.raw,
    @SerialName("shift_lead_id") val shiftLeadId: String,
    @SerialName("last_saved_by") val lastSavedBy: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ShiftResponderInsert(
    @SerialName("shift_id") val shiftId: String,
    @SerialName("responder_id") val responderId: String,
)

@Serializable
data class RoleRow(val role: String)

@Serializable
data class AssignmentIdRow(@SerialName("event_id") val eventId: String)

@Serializable
data class ShiftAssignmentIdRow(@SerialName("shift_id") val shiftId: String)

@Serializable
data class ShiftCrewRow(
    val id: String,
    @SerialName("responder_id") val responderId: String,
)

@Serializable
data class ShiftBornEvent(
    val id: String,
    @SerialName("event_date") val eventDate: String,
    @SerialName("police_event_id") val policeEventId: String? = null,
    @Serializable(with = EventStatusSerializer::class)
    val status: EventStatus,
    @SerialName("event_type")
    @Serializable(with = OptionalNamedSerializer::class)
    val eventType: Named? = null,
)

@Serializable
data class ShiftListItem(
    val id: String,
    @SerialName("shift_date") val shiftDate: String,
    @SerialName("shift_kind") val shiftKind: String,
    @SerialName("vehicle_type") val vehicleType: String,
    @Serializable(with = ShiftStatusSerializer::class)
    val status: ShiftStatus,
    @SerialName("odometer_start")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerStart: Double? = null,
    @SerialName("odometer_end")
    @Serializable(with = OptionalDoubleSerializer::class)
    val odometerEnd: Double? = null,
    @SerialName("personal_vehicle")
    @Serializable(with = OptionalPlateRefSerializer::class)
    val personalVehicle: PlateRef? = null,
    @SerialName("shift_lead")
    @Serializable(with = OptionalPersonNameSerializer::class)
    val shiftLead: PersonName? = null,
    val responders: List<ShiftCrewRow> = emptyList(),
    @SerialName("born_events") val bornEvents: List<ShiftBornEvent> = emptyList(),
) {
    val title: String
        get() {
            val kind = SHIFT_KIND_LABELS[shiftKind] ?: shiftKind
            val vehicle = VEHICLE_TYPE_LABELS[vehicleType] ?: vehicleType
            val plate = personalVehicle?.plateNumber
            return if (vehicleType == "personal" && !plate.isNullOrEmpty()) {
                "$kind · $vehicle · ${formatPlate(plate)}"
            } else {
                "$kind · $vehicle"
            }
        }

    val mineItem: MineShiftItem
        get() = MineShiftItem(id, shiftDate, status, odometerStart, odometerEnd)

    val unitSearchFields: List<String?>
        get() = listOf(
            formatDate(shiftDate),
            SHIFT_KIND_LABELS[shiftKind],
            VEHICLE_TYPE_LABELS[vehicleType],
            shiftLead?.display,
            personalVehicle?.plateNumber,
        )
}

@Serializable
data class IdRow(val id: String)

@Serializable
data class EventOwnerRow(
    val id: String,
    @SerialName("shift_lead_id") val shiftLeadId: String? = null,
)

@Serializable
data class TrackLoadResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val ended: Boolean? = null,
    @SerialName("event_type") val eventType: String? = null,
    val road: String? = null,
    val location: String? = null,
)

object EventStatusSerializer : EnumRawSerializer<EventStatus>(EventStatus.entries, EventStatus::raw, EventStatus.IN_PROGRESS)
object ParticipationStatusSerializer : EnumRawSerializer<ParticipationStatus>(ParticipationStatus.entries, ParticipationStatus::raw, ParticipationStatus.PENDING)
object ShiftStatusSerializer : EnumRawSerializer<ShiftStatus>(ShiftStatus.entries, ShiftStatus::raw, ShiftStatus.DRAFT)
object AvailabilityStatusSerializer : EnumRawSerializer<AvailabilityStatus>(AvailabilityStatus.entries, AvailabilityStatus::raw, AvailabilityStatus.AVAILABLE)
object BroadcastChannelSerializer : EnumRawSerializer<BroadcastChannel>(BroadcastChannel.entries, BroadcastChannel::raw, BroadcastChannel.BOTH)
object BroadcastAudienceSerializer : EnumRawSerializer<BroadcastAudience>(BroadcastAudience.entries, BroadcastAudience::raw, BroadcastAudience.ALL)

open class EnumRawSerializer<T : Enum<T>>(
    private val values: List<T>,
    private val raw: (T) -> String,
    private val fallback: T,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("EnumRaw", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(raw(value))

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeString()
        return values.find { raw(it) == value } ?: fallback
    }
}

@OptIn(ExperimentalSerializationApi::class)
object OptionalDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("OptionalDouble", kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE).nullable

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
    }

    override fun deserialize(decoder: Decoder): Double? {
        val json = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        return when (val el = json.decodeJsonElement()) {
            JsonNull -> null
            is JsonPrimitive -> el.doubleOrNull ?: el.intOrNull?.toDouble() ?: el.content.toDoubleOrNull()
            else -> null
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object OptionalIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("OptionalInt", kotlinx.serialization.descriptors.PrimitiveKind.INT).nullable

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int? {
        val json = decoder as? JsonDecoder ?: return decoder.decodeInt()
        return when (val el = json.decodeJsonElement()) {
            JsonNull -> null
            is JsonPrimitive -> el.intOrNull ?: el.doubleOrNull?.toInt() ?: el.content.toIntOrNull()
            else -> null
        }
    }
}

object OptionalNamedSerializer : OneOrNullSerializer<Named>(Named.serializer())
object OptionalPersonNameSerializer : OneOrNullSerializer<PersonName>(PersonName.serializer())
object OptionalPlateRefSerializer : OneOrNullSerializer<PlateRef>(PlateRef.serializer())
object OptionalShiftSummarySerializer : OneOrNullSerializer<ShiftSummary>(ShiftSummary.serializer())

@OptIn(ExperimentalSerializationApi::class)
open class OneOrNullSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<T?> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor.nullable

    override fun serialize(encoder: Encoder, value: T?) {
        if (value == null) encoder.encodeNull() else encoder.encodeSerializableValue(dataSerializer, value)
    }

    override fun deserialize(decoder: Decoder): T? {
        val json = decoder as? JsonDecoder ?: return decoder.decodeSerializableValue(dataSerializer)
        return when (val el = json.decodeJsonElement()) {
            JsonNull -> null
            is JsonArray -> el.firstOrNull()?.let { json.json.decodeFromJsonElement(dataSerializer, it) }
            else -> json.json.decodeFromJsonElement(dataSerializer, el)
        }
    }
}

fun String.nilIfEmpty(): String? = trim().ifEmpty { null }
