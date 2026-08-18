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
import com.yahpz.domain.AvailabilityStatus
import com.yahpz.domain.EventStatus
import com.yahpz.domain.MineSearchFields
import com.yahpz.domain.MineShiftItem
import com.yahpz.domain.ParticipationStatus
import com.yahpz.domain.ResponderFillDraft
import com.yahpz.domain.SHIFT_KIND_LABELS
import com.yahpz.domain.ShiftStatus
import com.yahpz.domain.TreatedPlateRowInput
import com.yahpz.domain.VEHICLE_TYPE_LABELS
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
    val origin: String? = null,
    @SerialName("shift_id") val shiftId: String? = null,
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
}

@Serializable
data class VehicleOption(
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
    @SerialName("sort_order")
    @Serializable(with = OptionalIntSerializer::class)
    val sortOrder: Int? = null,
) {
    val asInput: TreatedPlateRowInput
        get() = TreatedPlateRowInput(
            plateNumber = plateNumber,
            model = model,
            color = color,
            sortOrder = sortOrder,
        )
}

@Serializable
data class TreatedPlateWrite(
    @SerialName("event_responder_id") val eventResponderId: String,
    @SerialName("plate_number") val plateNumber: String,
    val model: String? = null,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int,
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
        get() = MineShiftItem(id, shiftDate, odometerStart, odometerEnd)
}

@Serializable
data class IdRow(val id: String)

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
