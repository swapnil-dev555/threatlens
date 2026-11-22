package com.example.threatlens.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.threatlens.MainActivity
import com.example.threatlens.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID_ALERTS = com.example.threatlens.common.AppConstants.CHANNEL_ID_ALERTS
        const val CHANNEL_NAME_ALERTS = com.example.threatlens.common.AppConstants.CHANNEL_NAME_ALERTS
        const val EXTRA_SUBSCRIPTION_ID = com.example.threatlens.common.AppConstants.EXTRA_SUBSCRIPTION_ID
        const val EXTRA_WINDOW_ID = com.example.threatlens.common.AppConstants.EXTRA_WINDOW_ID
        const val EXTRA_STORY_ACTIVITY_KEY = com.example.threatlens.common.AppConstants.EXTRA_STORY_ACTIVITY_KEY
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            CHANNEL_ID_ALERTS,
            CHANNEL_NAME_ALERTS,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Cyber threat intelligence alerts from your subscriptions"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun areNotificationsEnabled(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun openNotificationSettings(targetContext: Context = context) {
        val intent = Intent().apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, targetContext.packageName)
            } else {
                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = android.net.Uri.fromParts("package", targetContext.packageName, null)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        targetContext.startActivity(intent)
    }

    fun showNotification(
        title: String,
        body: String,
        subscriptionId: String,
        windowId: String,
        activityKey: String? = null,
        matchedCount: Int = 1
    ) {
        if (!areNotificationsEnabled()) {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                log("Notification suppressed: notifications not enabled or permission denied")
                setCustomKey("notifications_enabled", false)
            }
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SUBSCRIPTION_ID, subscriptionId)
            putExtra(EXTRA_WINDOW_ID, windowId)
            if (!activityKey.isNullOrBlank()) {
                putExtra(EXTRA_STORY_ACTIVITY_KEY, activityKey)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            windowId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(windowId.hashCode(), notification)
    }
}
