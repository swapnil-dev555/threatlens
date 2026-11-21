package com.example.threatlens.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val platform: String = "android",
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("device_label") val deviceLabel: String = "Android Device"
)

@Serializable
data class DeviceResponse(
    val id: String,
    val platform: String,
    @SerialName("device_label") val deviceLabel: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_seen_at") val lastSeenAt: String,
    @SerialName("revoked_at") val revokedAt: String? = null
)

@Serializable
data class RevokeResponse(val status: String)
