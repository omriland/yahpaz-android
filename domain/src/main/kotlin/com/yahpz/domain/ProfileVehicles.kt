package com.yahpz.domain

data class VehicleRowInput(
    val plateRaw: String,
    val modelRaw: String?,
    val archived: Boolean?,
)

data class ProfileVehicle(
    val plate: String,
    val model: String,
)

fun visibleProfileVehicles(rows: List<VehicleRowInput>): List<ProfileVehicle> {
    return rows.mapNotNull { row ->
        val plate = plateDigits(row.plateRaw)
        if (plate.isEmpty()) return@mapNotNull null
        if (row.archived == true) return@mapNotNull null
        ProfileVehicle(
            plate = plate,
            model = row.modelRaw?.trim().orEmpty(),
        )
    }
}
