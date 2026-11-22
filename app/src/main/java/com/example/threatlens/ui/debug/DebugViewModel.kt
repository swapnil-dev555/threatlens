package com.example.threatlens.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.threatlens.data.api.CyberNewsApi
import com.example.threatlens.data.debug.DebugSettings
import com.example.threatlens.data.fake.FakeBackendInterceptor
import com.example.threatlens.data.fcm.DeviceTokenManager
import com.example.threatlens.data.fcm.NotificationHelper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebugUiState(
    val latencyMs: Long = 300L,
    val force401: Boolean = false,
    val force500: Boolean = false,
    val forceTimeout: Boolean = false,
    val isDarkMode: Boolean = true,
    val fcmToken: String? = null,
    val deviceId: String? = null,
    val subscriptionId: String = FakeBackendInterceptor.SEEDED_SUB_ID,
    val windowId: String = FakeBackendInterceptor.SEEDED_WINDOW_ID
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val debugSettings: DebugSettings,
    private val deviceTokenManager: DeviceTokenManager,
    private val notificationHelper: NotificationHelper,
    private val api: CyberNewsApi,
    private val fakeBackendInterceptor: FakeBackendInterceptor
) : ViewModel() {

    val uiState: StateFlow<DebugUiState> = combine(
        debugSettings.latencyMs,
        debugSettings.force401State,
        debugSettings.force500State,
        debugSettings.forceTimeoutState,
        debugSettings.isDarkMode,
        deviceTokenManager.fcmToken,
        deviceTokenManager.deviceId
    ) { values ->
        DebugUiState(
            latencyMs = values[0] as Long,
            force401 = values[1] as Boolean,
            force500 = values[2] as Boolean,
            forceTimeout = values[3] as Boolean,
            isDarkMode = values[4] as Boolean,
            fcmToken = values[5] as String?,
            deviceId = values[6] as String?,
            subscriptionId = FakeBackendInterceptor.SEEDED_SUB_ID,
            windowId = FakeBackendInterceptor.SEEDED_WINDOW_ID
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebugUiState())

    fun setLatency(ms: Long) = debugSettings.setLatency(ms)
    fun setForce401(enabled: Boolean) = debugSettings.setForce401(enabled)
    fun setForce500(enabled: Boolean) = debugSettings.setForce500(enabled)
    fun setForceTimeout(enabled: Boolean) = debugSettings.setForceTimeout(enabled)
    fun setDarkMode(enabled: Boolean) = debugSettings.setDarkMode(enabled)
    fun toggleDarkMode() = debugSettings.toggleDarkMode()
    fun resetAll() = debugSettings.resetAll()

    fun triggerPushNotification() {
        viewModelScope.launch {
            try {
                val response = api.getNews(limit = 60)
                val stories = response.items
                val pickedStory = stories.randomOrNull() ?: stories.firstOrNull()
                if (pickedStory != null) {
                    notificationHelper.showNotification(
                        title = pickedStory.title,
                        body = pickedStory.atAGlance.ifBlank { pickedStory.title },
                        subscriptionId = FakeBackendInterceptor.SEEDED_SUB_ID,
                        windowId = FakeBackendInterceptor.SEEDED_WINDOW_ID,
                        activityKey = pickedStory.activityKey,
                        matchedCount = 1
                    )
                    return@launch
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            val fallbackStory = fakeBackendInterceptor.getSeededStory()
            notificationHelper.showNotification(
                title = fallbackStory?.title ?: "Critical Alert: Ransomware Outbreak",
                body = fallbackStory?.summary ?: "New threat intelligence matching your subscription.",
                subscriptionId = FakeBackendInterceptor.SEEDED_SUB_ID,
                windowId = FakeBackendInterceptor.SEEDED_WINDOW_ID,
                activityKey = fallbackStory?.activityKey,
                matchedCount = 1
            )
        }
    }

    fun forceCrash() {
        throw RuntimeException("Test crash triggered from debug screen")
    }

    fun forceNonFatal() {
        viewModelScope.launch {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("debug_non_fatal", true)
                recordException(Exception("Test non-fatal from debug screen"))
            }
        }
    }
}
