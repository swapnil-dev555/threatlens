package com.example.threatlens.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val count: Int,
    val page: Int,
    val size: Int,
    @SerialName("total_pages") val totalPages: Int,
    val limit: Int,
    val offset: Int,
    @SerialName("has_next") val hasNext: Boolean,
    @SerialName("has_previous") val hasPrevious: Boolean,
    @SerialName("is_expand") val isExpand: Boolean = false,
    val items: List<T> = emptyList()
)

@Serializable
data class NewsCard(
    @SerialName("activity_key") val activityKey: String,
    val title: String,
    @SerialName("at_a_glance") val atAGlance: String = "",
    @SerialName("published_ts") val publishedTs: String,
    @SerialName("section_label") val sectionLabel: String = "",
    @SerialName("read_time_minutes") val readTimeMinutes: Int = 0
)

@Serializable
data class NewsStory(
    @SerialName("activity_key") val activityKey: String,
    val title: String,
    val summary: String = "",
    val source: String = "",
    @SerialName("source_url") val sourceUrl: String = "",
    val tlp: String = "",
    @SerialName("is_ransomware") val isRansomware: Boolean = false,
    @SerialName("actor_names") val actorNames: List<String> = emptyList(),
    @SerialName("section_label") val sectionLabel: String = "",
    val malware: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val techniques: List<String> = emptyList(),
    val cves: List<String> = emptyList(),
    val vendors: List<String> = emptyList(),
    val products: List<String> = emptyList(),
    @SerialName("target_sectors") val targetSectors: List<String> = emptyList(),
    @SerialName("target_countries") val targetCountries: List<String> = emptyList(),
    @SerialName("published_ts") val publishedTs: String,
    @SerialName("icounter_observation") val icounterObservation: String = "",
    @SerialName("at_a_glance") val atAGlance: String = "",
    @SerialName("how_the_attack_works") val howTheAttackWorks: String = "",
    @SerialName("read_time_minutes") val readTimeMinutes: Int = 0
)

@Serializable
data class Section(val key: String, val label: String)

@Serializable
data class SectionsResponse(val items: List<Section>)

@Serializable
data class FilterOption(val value: String, val label: String)

@Serializable
data class FilterOptionsResponse(val items: List<FilterOption>)
