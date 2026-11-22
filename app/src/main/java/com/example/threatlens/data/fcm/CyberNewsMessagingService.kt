package com.example.threatlens.data.fcm

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CyberNewsMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var deviceTokenManager: DeviceTokenManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseCrashlytics.getInstance().setCustomKey("fcm_token_updated", true)
        deviceTokenManager.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val windowId = data["window_id"]
        val subscriptionId = data["subscription_id"]
        val matchedCount = data["matched_count"]?.toIntOrNull() ?: 0
        val title = message.notification?.title ?: data["title"] ?: "CyberSentinel Alert"
        val body = message.notification?.body ?: data["body"] ?: "New threat intelligence available"

        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("last_push_window_id", windowId ?: "")
            setCustomKey("last_push_subscription_id", subscriptionId ?: "")
            log("Push received: windowId=$windowId, subId=$subscriptionId, matches=$matchedCount")
        }

        val activityKey = data["activity_key"] ?: data["story_activity_key"]

        if (subscriptionId != null && windowId != null) {
            notificationHelper.showNotification(
                title = title,
                body = body,
                subscriptionId = subscriptionId,
                windowId = windowId,
                activityKey = activityKey,
                matchedCount = matchedCount
            )
        }
    }
}
