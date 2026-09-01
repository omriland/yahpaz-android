package com.yahpz.domain

data class VehicleRowInput(
    val plateRaw: String,
    val modelRaw: String?,
    val archived: Boolean?,
    val id: String? = null,
    val isDefault: Boolean? = null,
)

data class ProfileVehicle(
    val plate: String,
    val model: String,
    val id: String? = null,
    val archived: Boolean = false,
    val isDefault: Boolean = false,
)

const val SET_DEFAULT_VEHICLE_LABEL = "הגדר כרכב ברירת מחדל"
const val DEFAULT_VEHICLE_LABEL = "רכב ראשי"

sealed class VehicleFieldsResult
data class VehicleFieldsOk(val plateNumber: String, val model: String) : VehicleFieldsResult()
data class VehicleFieldsError(val message: String) : VehicleFieldsResult()

fun vehicleRemoveMode(attached: Boolean): String = if (attached) "archive" else "delete"

fun canChooseDefaultVehicle(vehicles: List<ProfileVehicle>): Boolean =
    vehicles.count { !it.archived } >= 2

fun isProfileVehicleEditing(id: String?, key: String, editingKey: String?): Boolean =
    id == null || key == editingKey

fun vehicleFieldsForSave(plateNumber: String, model: String): VehicleFieldsResult {
    val plate = plateNumberForSave(plateNumber)
    val trimmedModel = model.trim()
    if (plate.isNullOrEmpty() || trimmedModel.isEmpty()) {
        return VehicleFieldsError("יש להזין לוחית רישוי ודגם.")
    }
    return VehicleFieldsOk(plateNumber = plate, model = trimmedModel)
}

fun visibleProfileVehicles(rows: List<VehicleRowInput>): List<ProfileVehicle> {
    return managedProfileVehicles(rows).filter { !it.archived }
}

fun managedProfileVehicles(rows: List<VehicleRowInput>): List<ProfileVehicle> {
    return rows.mapNotNull { row ->
        val plate = plateDigits(row.plateRaw)
        if (plate.isEmpty()) return@mapNotNull null
        val archived = row.archived == true
        ProfileVehicle(
            plate = plate,
            model = row.modelRaw?.trim().orEmpty(),
            id = row.id,
            archived = archived,
            isDefault = row.isDefault == true && !archived,
        )
    }
}
