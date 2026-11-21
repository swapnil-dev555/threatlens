package com.example.threatlens.data.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugSettings @Inject constructor() {

    private val _latencyMs = MutableStateFlow(com.example.threatlens.common.AppConstants.DEFAULT_DEBUG_LATENCY_MS)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _force401State = MutableStateFlow(false)
    val force401State: StateFlow<Boolean> = _force401State.asStateFlow()
    val force401: Boolean get() = _force401State.value

    private val _force500State = MutableStateFlow(false)
    val force500State: StateFlow<Boolean> = _force500State.asStateFlow()
    val force500: Boolean get() = _force500State.value

    private val _forceTimeoutState = MutableStateFlow(false)
    val forceTimeoutState: StateFlow<Boolean> = _forceTimeoutState.asStateFlow()
    val forceTimeout: Boolean get() = _forceTimeoutState.value

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setLatency(ms: Long) { _latencyMs.value = ms.coerceIn(0, com.example.threatlens.common.AppConstants.MAX_DEBUG_LATENCY_MS) }
    fun setForce401(enabled: Boolean) { _force401State.value = enabled }
    fun setForce500(enabled: Boolean) { _force500State.value = enabled }
    fun setForceTimeout(enabled: Boolean) { _forceTimeoutState.value = enabled }
    fun setDarkMode(enabled: Boolean) { _isDarkMode.value = enabled }
    fun toggleDarkMode() { _isDarkMode.value = !_isDarkMode.value }

    fun resetAll() {
        _latencyMs.value = com.example.threatlens.common.AppConstants.DEFAULT_DEBUG_LATENCY_MS
        _force401State.value = false
        _force500State.value = false
        _forceTimeoutState.value = false
        _isDarkMode.value = true
    }
}
