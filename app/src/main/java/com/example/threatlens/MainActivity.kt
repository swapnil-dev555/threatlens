package com.example.threatlens

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.threatlens.data.debug.DebugSettings
import com.example.threatlens.data.fcm.DeviceTokenManager
import com.example.threatlens.data.fcm.NotificationHelper.Companion.EXTRA_STORY_ACTIVITY_KEY
import com.example.threatlens.data.fcm.NotificationHelper.Companion.EXTRA_SUBSCRIPTION_ID
import com.example.threatlens.data.fcm.NotificationHelper.Companion.EXTRA_WINDOW_ID
import com.example.threatlens.ui.navigation.AppNavGraph
import com.example.threatlens.ui.splash.SplashScreen
import com.example.threatlens.ui.theme.CyberSentinelTheme
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var deviceTokenManager: DeviceTokenManager

    @Inject
    lateinit var debugSettings: DebugSettings

    private var pushStoryActivityKey by mutableStateOf<String?>(null)
    private var pushSubscriptionId by mutableStateOf<String?>(null)
    private var pushWindowId by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        FirebaseCrashlytics.getInstance().setCustomKey(com.example.threatlens.common.AppConstants.NOTIFICATION_PERMISSION_KEY, granted)
        // If denied, the app still works — push will silently fail on Android 13+
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pushStoryActivityKey = intent.getStringExtra(EXTRA_STORY_ACTIVITY_KEY)
        pushSubscriptionId = intent.getStringExtra(EXTRA_SUBSCRIPTION_ID)
        pushWindowId = intent.getStringExtra(EXTRA_WINDOW_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Fetch FCM token and register
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            deviceTokenManager.onNewToken(token)
        }

        // Retry any pending registrations
        deviceTokenManager.retryPendingRegistration()

        // Extract push deep link extras
        pushStoryActivityKey = intent.getStringExtra(EXTRA_STORY_ACTIVITY_KEY)
        pushSubscriptionId = intent.getStringExtra(EXTRA_SUBSCRIPTION_ID)
        pushWindowId = intent.getStringExtra(EXTRA_WINDOW_ID)

        setContent {
            val isDark by debugSettings.isDarkMode.collectAsStateWithLifecycle()
            val hasDeepLink = !pushStoryActivityKey.isNullOrBlank() ||
                    (!pushSubscriptionId.isNullOrBlank() && !pushWindowId.isNullOrBlank())

            var showSplash by remember {
                mutableStateOf(!hasDeepLink)
            }

            LaunchedEffect(pushStoryActivityKey, pushSubscriptionId, pushWindowId) {
                if (!pushStoryActivityKey.isNullOrBlank() || (!pushSubscriptionId.isNullOrBlank() && !pushWindowId.isNullOrBlank())) {
                    showSplash = false
                }
            }

            CyberSentinelTheme(darkTheme = isDark) {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(com.example.threatlens.common.AppConstants.SPLASH_CROSSFADE_MS),
                    label = "splashCrossfade"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(onSplashComplete = { showSplash = false })
                    } else {
                        AppNavGraph(
                            deepLinkStoryActivityKey = pushStoryActivityKey,
                            deepLinkSubscriptionId = pushSubscriptionId,
                            deepLinkWindowId = pushWindowId,
                            onDeepLinkConsumed = {
                                pushStoryActivityKey = null
                                pushSubscriptionId = null
                                pushWindowId = null
                            }
                        )
                    }
                }
            }
        }
    }
}
