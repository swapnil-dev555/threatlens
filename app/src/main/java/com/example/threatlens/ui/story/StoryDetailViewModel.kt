package com.example.threatlens.ui.story

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.threatlens.data.model.NewsStory
import com.example.threatlens.data.repository.NewsRepository
import com.example.threatlens.data.repository.SubscriptionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryDetailUiState(
    val story: NewsStory? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSubscriptionMatch: Boolean = false,
    val subscriptionId: String? = null
)

@HiltViewModel
class StoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newsRepository: NewsRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val activityKey: String = savedStateHandle["activityKey"] ?: ""
    val subscriptionId: String? = savedStateHandle["subscriptionId"]

    private val _uiState = MutableStateFlow(
        StoryDetailUiState(
            isSubscriptionMatch = !subscriptionId.isNullOrBlank(),
            subscriptionId = subscriptionId
        )
    )
    val uiState: StateFlow<StoryDetailUiState> = _uiState.asStateFlow()

    init {
        loadStory()
    }

    fun retry() = loadStory()

    private fun loadStory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (!subscriptionId.isNullOrBlank()) {
                val subResult = subscriptionRepository.getSubscriptionMatchStory(subscriptionId, activityKey)
                if (subResult.isFailure) {
                    newsRepository.getStory(activityKey)
                } else {
                    subResult
                }
            } else {
                newsRepository.getStory(activityKey)
            }
            result.onSuccess { story ->
                _uiState.update { it.copy(story = story, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to load story", isLoading = false) }
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }
}
