package com.example.threatlens.ui.subscriptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.threatlens.data.model.NewsCard
import com.example.threatlens.data.model.PushWindow
import com.example.threatlens.data.repository.SubscriptionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WindowMatchesUiState(
    val window: PushWindow? = null,
    val matches: List<NewsCard> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasMore: Boolean = false
)

@HiltViewModel
class WindowMatchesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SubscriptionRepository
) : ViewModel() {

    val subscriptionId: String = savedStateHandle.get<String>("subscriptionId")?.takeIf { it.isNotBlank() }
        ?: "8127922c-a1f5-47c1-ac3e-9886058a9dce"
    val windowId: String = savedStateHandle.get<String>("windowId")?.takeIf { it.isNotBlank() }
        ?: "0d6b5c0e-9d57-4c8a-bd5a-6f3a7a3f2b10"

    private val _uiState = MutableStateFlow(WindowMatchesUiState())
    val uiState: StateFlow<WindowMatchesUiState> = _uiState.asStateFlow()

    init {
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("push_window_id", windowId)
            setCustomKey("push_subscription_id", subscriptionId)
        }
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getWindowMatches(subscriptionId, windowId).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        window = response.window,
                        matches = response.items,
                        isLoading = false,
                        hasMore = response.hasNext
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
