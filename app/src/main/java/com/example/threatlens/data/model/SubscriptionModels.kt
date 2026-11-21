package com.example.threatlens.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val categories: List<String> = emptyList(),
    val actors: List<String> = emptyList(),
    @SerialName("ransomware_families") val ransomwareFamilies: List<String> = emptyList(),
    val products: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val techniques: List<String> = emptyList(),
    val vendors: List<String> = emptyList(),
    @SerialName("target_sectors") val targetSectors: List<String> = emptyList(),
    @SerialName("target_countries") val targetCountries: List<String> = emptyList(),
    val cadence: String,
    @SerialName("cadence_minute") val cadenceMinute: Int? = null,
    @SerialName("cadence_time") val cadenceTime: String? = null,
    @SerialName("cadence_day_of_week") val cadenceDayOfWeek: Int? = null,
    @SerialName("cadence_day_of_month") val cadenceDayOfMonth: Int? = null,
    @SerialName("cadence_month") val cadenceMonth: Int? = null,
    val timezone: String = "UTC",
    @SerialName("next_fire_at") val nextFireAt: String? = null,
    @SerialName("is_paused") val isPaused: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CreateSubscriptionRequest(
    val name: String,
    val categories: List<String>? = null,
    val actors: List<String>? = null,
    @SerialName("ransomware_families") val ransomwareFamilies: List<String>? = null,
    val products: List<String>? = null,
    val tools: List<String>? = null,
    val techniques: List<String>? = null,
    val vendors: List<String>? = null,
    @SerialName("target_sectors") val targetSectors: List<String>? = null,
    @SerialName("target_countries") val targetCountries: List<String>? = null,
    val cadence: String,
    @SerialName("cadence_minute") val cadenceMinute: Int? = null,
    @SerialName("cadence_time") val cadenceTime: String? = null,
    @SerialName("cadence_day_of_week") val cadenceDayOfWeek: Int? = null,
    @SerialName("cadence_day_of_month") val cadenceDayOfMonth: Int? = null,
    @SerialName("cadence_month") val cadenceMonth: Int? = null,
    val timezone: String = "UTC"
)

@Serializable
data class PatchSubscriptionRequest(
    val name: String? = null,
    val categories: List<String>? = null,
    val actors: List<String>? = null,
    @SerialName("ransomware_families") val ransomwareFamilies: List<String>? = null,
    val products: List<String>? = null,
    val tools: List<String>? = null,
    val techniques: List<String>? = null,
    val vendors: List<String>? = null,
    @SerialName("target_sectors") val targetSectors: List<String>? = null,
    @SerialName("target_countries") val targetCountries: List<String>? = null,
    val cadence: String? = null,
    @SerialName("cadence_minute") val cadenceMinute: Int? = null,
    @SerialName("cadence_time") val cadenceTime: String? = null,
    @SerialName("cadence_day_of_week") val cadenceDayOfWeek: Int? = null,
    @SerialName("cadence_day_of_month") val cadenceDayOfMonth: Int? = null,
    @SerialName("cadence_month") val cadenceMonth: Int? = null,
    val timezone: String? = null,
    @SerialName("is_paused") val isPaused: Boolean? = null
)

@Serializable
data class WindowMatchesResponse(
    val count: Int,
    val page: Int,
    val size: Int,
    @SerialName("total_pages") val totalPages: Int,
    val limit: Int,
    val offset: Int,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_previous") val hasPrevious: Boolean,
    val window: PushWindow,
    val items: List<NewsCard> = emptyList()
)

@Serializable
data class PushWindow(
    val id: String,
    @SerialName("window_start_at") val windowStartAt: String,
    @SerialName("window_end_at") val windowEndAt: String,
    @SerialName("matched_count") val matchedCount: Int,
    @SerialName("sent_at") val sentAt: String
)
