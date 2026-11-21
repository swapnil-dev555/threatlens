package com.example.threatlens.data.fcm

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.threatlens.data.model.RegisterDeviceRequest
import com.example.threatlens.data.repository.DeviceRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(name = com.example.threatlens.common.AppConstants.PREFS_DEVICE_NAME)

/**
 * Manages FCM token registration with the backend.
 * - Persists device_id so we can revoke later.
 * - Registers the token even if the component that received it is torn down
 *   (uses an application-scoped coroutine).
 * - If the user is not signed in yet, queues the token and registers once they are.
 */
@Singleton
class DeviceTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: DeviceRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _deviceId = MutableStateFlow<String?>(null)
    val deviceId: StateFlow<String?> = _deviceId.asStateFlow()

    companion object {
        private val KEY_DEVICE_ID = stringPreferencesKey(com.example.threatlens.common.AppConstants.PREF_KEY_DEVICE_ID)
        private val KEY_FCM_TOKEN = stringPreferencesKey(com.example.threatlens.common.AppConstants.PREF_KEY_FCM_TOKEN)
        private val KEY_PENDING_TOKEN = stringPreferencesKey(com.example.threatlens.common.AppConstants.PREF_KEY_PENDING_FCM_TOKEN)
    }

    init {
        scope.launch {
            // Load persisted values
            val prefs = context.deviceDataStore.data.first()
            _deviceId.value = prefs[KEY_DEVICE_ID]
            _fcmToken.value = prefs[KEY_FCM_TOKEN]
        }
    }

    /**
     * Called when FCM delivers a new token. This can happen at any time.
     * Registration is fire-and-forget from an app-scoped coroutine so it
     * survives component teardown.
     */
    fun onNewToken(token: String) {
        _fcmToken.value = token
        scope.launch {
            registerToken(token)
        }
    }

    private suspend fun registerToken(token: String) {
        try {
            val result = deviceRepository.registerDevice(
                RegisterDeviceRequest(
                    platform = "android",
                    fcmToken = token,
                    deviceLabel = android.os.Build.MODEL
                )
            )
            result.onSuccess { device ->
                _deviceId.value = device.id
                context.deviceDataStore.edit { prefs ->
                    prefs[KEY_DEVICE_ID] = device.id
                    prefs[KEY_FCM_TOKEN] = token
                    prefs.remove(KEY_PENDING_TOKEN)
                }
                FirebaseCrashlytics.getInstance().setCustomKey("device_id", device.id)
            }.onFailure { e ->
                // Save as pending so we can retry later
                context.deviceDataStore.edit { prefs ->
                    prefs[KEY_PENDING_TOKEN] = token
                }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        } catch (e: Exception) {
            context.deviceDataStore.edit { prefs ->
                prefs[KEY_PENDING_TOKEN] = token
            }
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /** Retry registering any pending token (e.g., after sign-in). */
    fun retryPendingRegistration() {
        scope.launch {
            val prefs = context.deviceDataStore.data.first()
            val pending = prefs[KEY_PENDING_TOKEN]
            if (pending != null) {
                registerToken(pending)
            }
        }
    }
}
