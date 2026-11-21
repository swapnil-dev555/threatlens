package com.example.threatlens.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations using Kotlin Serialization.
 */
sealed interface Screen {
    @Serializable
    data object NewsFeed : Screen

    @Serializable
    data class StoryDetail(
        val activityKey: String,
        val subscriptionId: String? = null
    ) : Screen

    @Serializable
    data object SubscriptionsList : Screen

    @Serializable
    data object CreateSubscription : Screen

    @Serializable
    data class EditSubscription(val subscriptionId: String) : Screen

    @Serializable
    data class SubscriptionDetail(val subscriptionId: String) : Screen

    @Serializable
    data class WindowMatches(
        val subscriptionId: String,
        val windowId: String
    ) : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object Debug : Screen
}
