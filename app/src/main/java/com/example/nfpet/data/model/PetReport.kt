package com.example.nfpet.data.model

import org.json.JSONObject
import java.util.UUID

data class PetReport(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val photoUri: String?,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val reporterName: String,
    val phoneNumber: String,
    val status: String = "LOST", // "LOST" or "FOUND"
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("photoUri", photoUri ?: "")
            put("latitude", latitude)
            put("longitude", longitude)
            put("city", city)
            put("reporterName", reporterName)
            put("phoneNumber", phoneNumber)
            put("status", status)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): PetReport {
            val photoVal = json.optString("photoUri", "")
            return PetReport(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.getString("description"),
                photoUri = if (photoVal.isEmpty()) null else photoVal,
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                city = json.getString("city"),
                reporterName = json.getString("reporterName"),
                phoneNumber = json.getString("phoneNumber"),
                status = json.optString("status", "LOST"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}
