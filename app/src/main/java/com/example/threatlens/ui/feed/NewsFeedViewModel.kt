package com.example.threatlens.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.threatlens.data.model.NewsCard
import com.example.threatlens.data.model.Section
import com.example.threatlens.data.repository.NewsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewsFeedUiState(
    val stories: List<NewsCard> = emptyList(),
    val sections: List<Section> = emptyList(),
    val selectedSection: Section? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val totalCount: Int = 0
)

@OptIn(FlowPreview::class)
@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsFeedUiState())
    val uiState: StateFlow<NewsFeedUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var currentOffset = 0
    private var loadJob: Job? = null
    private var searchVersion = 0L // Monotonic version to prevent stale search results

    init {
        loadSections()
        loadNews(reset = true)

        // Debounced search: 400ms debounce, only the latest query wins
        viewModelScope.launch {
            _searchQuery
                .debounce(com.example.threatlens.common.AppConstants.SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { query ->
                    _uiState.update { it.copy(searchQuery = query) }
                    loadNews(reset = true)
                }
        }
    }

    private fun loadSections() {
        viewModelScope.launch {
            newsRepository.getSections().onSuccess { response ->
                _uiState.update { it.copy(sections = response.items) }
            }.onFailure { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSectionSelected(section: Section?) {
        _uiState.update { it.copy(selectedSection = section) }
        loadNews(reset = true)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadNews(reset = true)
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        loadNews(reset = false)
    }

    private fun loadNews(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (reset) {
                currentOffset = 0
                _uiState.update {
                    it.copy(isLoading = it.stories.isEmpty(), error = null)
                }
            } else {
                _uiState.update { it.copy(isLoadingMore = true) }
            }

            val version = ++searchVersion // Capture current version

            newsRepository.getNews(
                limit = 20,
                offset = currentOffset,
                sectionKey = _uiState.value.selectedSection?.key,
                query = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            ).onSuccess { response ->
                // Only apply if this is still the latest search
                if (version == searchVersion) {
                    _uiState.update { state ->
                        val newStories = if (reset) response.items else state.stories + response.items
                        state.copy(
                            stories = newStories,
                            isLoading = false,
                            isLoadingMore = false,
                            isRefreshing = false,
                            hasMore = response.hasNext,
                            error = null,
                            totalCount = response.count
                        )
                    }
                    currentOffset += response.items.size
                }
            }.onFailure { e ->
                if (version == searchVersion) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            isRefreshing = false,
                            error = e.message ?: "Failed to load news"
                        )
                    }
                }
                FirebaseCrashlytics.getInstance().apply {
                    setCustomKey("feed_error", e.message ?: "unknown")
                    recordException(e)
                }
            }
        }
    }
}
