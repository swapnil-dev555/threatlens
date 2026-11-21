package com.example.threatlens.ui.subscriptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.threatlens.data.model.NewsCard
import com.example.threatlens.data.model.PatchSubscriptionRequest
import com.example.threatlens.data.model.Subscription
import com.example.threatlens.data.repository.SubscriptionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decision: When one of the two parallel requests (subscription detail + matches)
 * fails and the other succeeds, we show whichever data we have with an inline
 * warning for the failed portion. E.g., if subscription loads but matches fail,
 * we show the subscription details with a "Could not load matches" message and retry button.
 * If subscription fails but matches load, we show matches with a warning that details are unavailable.
 */
data class SubscriptionDetailUiState(
    val subscription: Subscription? = null,
    val matches: List<NewsCard> = emptyList(),
    val isLoadingSubscription: Boolean = true,
    val isLoadingMatches: Boolean = true,
    val subscriptionError: String? = null,
    val matchesError: String? = null,
    val matchesHasMore: Boolean = false,
    val matchesOffset: Int = 0
)

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SubscriptionRepository
) : ViewModel() {

    val subscriptionId: String = savedStateHandle["subscriptionId"] ?: ""

    private val _uiState = MutableStateFlow(SubscriptionDetailUiState())
    val uiState: StateFlow<SubscriptionDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        if (_uiState.value.isLoadingSubscription && _uiState.value.subscription == null) {
            return
        }
        loadData()
    }

    fun retry() = loadData()

    fun retrySubscription() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSubscription = true, subscriptionError = null) }
            repository.getSubscriptions(limit = 50).onSuccess { response ->
                val sub = response.items.find { it.id == subscriptionId }
                _uiState.update {
                    it.copy(
                        subscription = sub,
                        isLoadingSubscription = false,
                        subscriptionError = if (sub == null) "Subscription not found" else null
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoadingSubscription = false, subscriptionError = e.message ?: "Failed to load subscription")
                }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun retryMatches() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMatches = true, matchesError = null) }
            repository.getSubscriptionMatches(subscriptionId).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        matches = response.items,
                        isLoadingMatches = false,
                        matchesHasMore = response.hasNext,
                        matchesOffset = response.items.size
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoadingMatches = false, matchesError = e.message ?: "Failed to load matches")
                }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingSubscription = true,
                    isLoadingMatches = true,
                    subscriptionError = null,
                    matchesError = null
                )
            }

            // Load subscription and matches IN PARALLEL
            val subDeferred = async {
                repository.getSubscriptions(limit = 50)
            }
            val matchesDeferred = async {
                repository.getSubscriptionMatches(subscriptionId)
            }

            val subResult = subDeferred.await()
            val matchesResult = matchesDeferred.await()

            subResult.onSuccess { response ->
                val sub = response.items.find { it.id == subscriptionId }
                _uiState.update {
                    it.copy(
                        subscription = sub,
                        isLoadingSubscription = false,
                        subscriptionError = if (sub == null) "Subscription not found" else null
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoadingSubscription = false, subscriptionError = e.message ?: "Failed to load subscription")
                }
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            matchesResult.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        matches = response.items,
                        isLoadingMatches = false,
                        matchesHasMore = response.hasNext,
                        matchesOffset = response.items.size
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoadingMatches = false, matchesError = e.message ?: "Failed to load matches")
                }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun togglePause() {
        val sub = _uiState.value.subscription ?: return
        val newPaused = !sub.isPaused
        _uiState.update { it.copy(subscription = sub.copy(isPaused = newPaused)) }
        viewModelScope.launch {
            repository.patchSubscription(sub.id, PatchSubscriptionRequest(isPaused = newPaused)).onSuccess { updated ->
                _uiState.update { it.copy(subscription = updated) }
            }.onFailure { e ->
                _uiState.update { it.copy(subscription = sub) }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun deleteSubscription(onDeleted: () -> Unit) {
        val sub = _uiState.value.subscription ?: return
        viewModelScope.launch {
            repository.deleteSubscription(sub.id).onSuccess {
                onDeleted()
            }.onFailure { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
