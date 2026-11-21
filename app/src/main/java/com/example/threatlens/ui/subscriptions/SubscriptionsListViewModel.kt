package com.example.threatlens.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.threatlens.data.model.PatchSubscriptionRequest
import com.example.threatlens.data.model.Subscription
import com.example.threatlens.data.repository.SubscriptionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class SubscriptionsListUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val deletedSubscription: Subscription? = null // For undo
)

@HiltViewModel
class SubscriptionsListViewModel @Inject constructor(
    private val repository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsListUiState())
    val uiState: StateFlow<SubscriptionsListUiState> = _uiState.asStateFlow()

    // Mutex per subscription to serialize rapid pause/resume taps
    private val pauseMutexes = mutableMapOf<String, Mutex>()
    private val pauseJobs = mutableMapOf<String, Job>()

    init {
        loadSubscriptions()
    }

    fun loadSubscriptions(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh || _uiState.value.subscriptions.isEmpty()) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            repository.getSubscriptions().onSuccess { response ->
                _uiState.update {
                    it.copy(subscriptions = response.items, isLoading = false, error = null)
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load subscriptions", isLoading = false)
                }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isLoading && _uiState.value.subscriptions.isEmpty()) {
            return
        }
        loadSubscriptions(isRefresh = true)
    }

    /**
     * Toggle pause/resume. Implements optimistic update:
     * - Immediately flips the UI.
     * - Sends the request.
     * - Reverts if the request fails.
     * - Uses a per-subscription Mutex so rapid taps serialize correctly.
     */
    fun togglePause(subscription: Subscription) {
        val mutex = pauseMutexes.getOrPut(subscription.id) { Mutex() }

        // Cancel any pending job for this subscription
        pauseJobs[subscription.id]?.cancel()

        pauseJobs[subscription.id] = viewModelScope.launch {
            mutex.withLock {
                val newPaused = !subscription.isPaused

                // Optimistic update
                updateSubscriptionInList(subscription.id) { it.copy(isPaused = newPaused) }

                // Send to server
                repository.patchSubscription(
                    subscription.id,
                    PatchSubscriptionRequest(isPaused = newPaused)
                ).onSuccess { updated ->
                    // Apply server's authoritative state
                    updateSubscriptionInList(subscription.id) { updated }
                }.onFailure { e ->
                    // Revert optimistic update
                    updateSubscriptionInList(subscription.id) { subscription }
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            // Remove from list optimistically, store for undo
            _uiState.update { state ->
                state.copy(
                    subscriptions = state.subscriptions.filter { it.id != subscription.id },
                    deletedSubscription = subscription
                )
            }

            repository.deleteSubscription(subscription.id).onFailure { e ->
                // Restore on failure
                _uiState.update { state ->
                    state.copy(
                        subscriptions = state.subscriptions + subscription,
                        deletedSubscription = null
                    )
                }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun undoDelete() {
        val deleted = _uiState.value.deletedSubscription ?: return
        _uiState.update { state ->
            state.copy(
                subscriptions = state.subscriptions + deleted,
                deletedSubscription = null
            )
        }
        // Note: in a real app, we'd need to re-create the subscription on the backend.
        // Since we soft-delete, we'd need a restore endpoint or just re-create.
    }

    fun clearDeletedSubscription() {
        _uiState.update { it.copy(deletedSubscription = null) }
    }

    private fun updateSubscriptionInList(id: String, transform: (Subscription) -> Subscription) {
        _uiState.update { state ->
            state.copy(
                subscriptions = state.subscriptions.map { if (it.id == id) transform(it) else it }
            )
        }
    }
}

/** Utility to format a cadence into a human-readable string */
fun Subscription.cadenceSummary(): String {
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    return when (cadence) {
        "realtime" -> "Real-time, $timezone"
        "hourly" -> "Hourly at :${(cadenceMinute ?: 0).toString().padStart(2, '0')}, $timezone"
        "daily" -> "Daily at ${cadenceTime ?: "00:00"}, $timezone"
        "weekly" -> {
            val day = cadenceDayOfWeek?.let { dayNames.getOrNull(it) } ?: "Monday"
            "Weekly, ${day}s ${cadenceTime ?: "09:00"}, $timezone"
        }
        "monthly" -> "Monthly, day ${cadenceDayOfMonth ?: 1} at ${cadenceTime ?: "09:00"}, $timezone"
        "yearly" -> {
            val month = cadenceMonth?.let { monthNames.getOrNull(it - 1) } ?: "Jan"
            "Yearly, $month ${cadenceDayOfMonth ?: 1} at ${cadenceTime ?: "09:00"}, $timezone"
        }
        else -> cadence
    }
}

fun Subscription.nextFireFormatted(): String {
    return try {
        val instant = java.time.Instant.parse(nextFireAt)
        val zoned = instant.atZone(java.time.ZoneId.of(timezone))
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")
        "Next: ${zoned.format(formatter)}"
    } catch (e: Exception) {
        nextFireAt ?: "N/A"
    }
}
