package com.example.threatlens.ui.subscriptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.threatlens.data.model.CreateSubscriptionRequest
import com.example.threatlens.data.model.FilterOption
import com.example.threatlens.data.model.PatchSubscriptionRequest
import com.example.threatlens.data.model.Subscription
import com.example.threatlens.data.repository.NewsRepository
import com.example.threatlens.data.repository.SubscriptionRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

data class SubscriptionFormState(
    val name: String = "",
    val categories: List<String> = emptyList(),
    val actors: List<String> = emptyList(),
    val ransomwareFamilies: List<String> = emptyList(),
    val products: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val techniques: List<String> = emptyList(),
    val vendors: List<String> = emptyList(),
    val targetSectors: List<String> = emptyList(),
    val targetCountries: List<String> = emptyList(),
    val cadence: String = "daily",
    val cadenceMinute: Int = 0,
    val cadenceTime: String = "09:00:00",
    val cadenceDayOfWeek: Int = 0,
    val cadenceDayOfMonth: Int = 1,
    val cadenceMonth: Int = 1,
    val timezone: String = TimeZone.getDefault().id,
    // UI state
    val isEditing: Boolean = false,
    val existingSubscription: Subscription? = null,
    val categoryOptions: List<FilterOption> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class SubscriptionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subscriptionRepository: SubscriptionRepository,
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val subscriptionId: String? = savedStateHandle["subscriptionId"]

    private val _uiState = MutableStateFlow(SubscriptionFormState(isEditing = !subscriptionId.isNullOrBlank()))
    val uiState: StateFlow<SubscriptionFormState> = _uiState.asStateFlow()

    init {
        loadCategoryOptions()
        if (!subscriptionId.isNullOrBlank()) {
            loadExistingSubscription(subscriptionId)
        }
    }

    private fun loadCategoryOptions() {
        viewModelScope.launch {
            newsRepository.getFilterOptions("categories").onSuccess { response ->
                _uiState.update { it.copy(categoryOptions = response.items) }
            }
        }
    }

    private fun loadExistingSubscription(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            subscriptionRepository.getSubscriptions(limit = 50).onSuccess { response ->
                val sub = response.items.find { it.id == id }
                if (sub != null) {
                    _uiState.update {
                        it.copy(
                            name = sub.name,
                            categories = sub.categories,
                            actors = sub.actors,
                            ransomwareFamilies = sub.ransomwareFamilies,
                            products = sub.products,
                            tools = sub.tools,
                            techniques = sub.techniques,
                            vendors = sub.vendors,
                            targetSectors = sub.targetSectors,
                            targetCountries = sub.targetCountries,
                            cadence = sub.cadence,
                            cadenceMinute = sub.cadenceMinute ?: 0,
                            cadenceTime = sub.cadenceTime ?: "09:00:00",
                            cadenceDayOfWeek = sub.cadenceDayOfWeek ?: 0,
                            cadenceDayOfMonth = sub.cadenceDayOfMonth ?: 1,
                            cadenceMonth = sub.cadenceMonth ?: 1,
                            timezone = sub.timezone,
                            existingSubscription = sub,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "Subscription not found", isLoading = false) }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateCadence(cadence: String) = _uiState.update { it.copy(cadence = cadence) }
    fun updateCadenceMinute(minute: Int) = _uiState.update { it.copy(cadenceMinute = minute) }
    fun updateCadenceTime(time: String) = _uiState.update { it.copy(cadenceTime = time) }
    fun updateCadenceDayOfWeek(day: Int) = _uiState.update { it.copy(cadenceDayOfWeek = day) }
    fun updateCadenceDayOfMonth(day: Int) = _uiState.update { it.copy(cadenceDayOfMonth = day) }
    fun updateCadenceMonth(month: Int) = _uiState.update { it.copy(cadenceMonth = month) }
    fun updateTimezone(tz: String) = _uiState.update { it.copy(timezone = tz) }

    fun toggleCategory(value: String) {
        _uiState.update { state ->
            val updated = if (value in state.categories) state.categories - value else state.categories + value
            state.copy(categories = updated)
        }
    }

    fun updateTargetSectors(sectors: List<String>) = _uiState.update { it.copy(targetSectors = sectors) }
    fun updateTargetCountries(countries: List<String>) = _uiState.update { it.copy(targetCountries = countries) }

    fun isFormValid(): Boolean {
        val state = _uiState.value
        val hasFilters = state.categories.isNotEmpty() || state.actors.isNotEmpty() ||
                state.ransomwareFamilies.isNotEmpty() || state.products.isNotEmpty() ||
                state.tools.isNotEmpty() || state.techniques.isNotEmpty() ||
                state.vendors.isNotEmpty() || state.targetSectors.isNotEmpty() ||
                state.targetCountries.isNotEmpty()
        return state.name.isNotBlank() && hasFilters
    }

    fun save() {
        if (!isFormValid()) return
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            if (state.isEditing && !subscriptionId.isNullOrBlank()) {
                val patch = if (state.existingSubscription != null) {
                    buildPatch(state.existingSubscription, state)
                } else {
                    PatchSubscriptionRequest(
                        name = state.name,
                        categories = state.categories,
                        cadence = state.cadence,
                        cadenceMinute = if (state.cadence == "hourly") state.cadenceMinute else null,
                        cadenceTime = if (state.cadence in listOf("daily", "weekly", "monthly", "yearly")) state.cadenceTime else null,
                        cadenceDayOfWeek = if (state.cadence == "weekly") state.cadenceDayOfWeek else null,
                        cadenceDayOfMonth = if (state.cadence in listOf("monthly", "yearly")) state.cadenceDayOfMonth else null,
                        cadenceMonth = if (state.cadence == "yearly") state.cadenceMonth else null,
                        timezone = state.timezone
                    )
                }
                subscriptionRepository.patchSubscription(subscriptionId, patch).onSuccess {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            } else {
                val request = CreateSubscriptionRequest(
                    name = state.name,
                    categories = state.categories.takeIf { it.isNotEmpty() },
                    actors = state.actors.takeIf { it.isNotEmpty() },
                    ransomwareFamilies = state.ransomwareFamilies.takeIf { it.isNotEmpty() },
                    products = state.products.takeIf { it.isNotEmpty() },
                    tools = state.tools.takeIf { it.isNotEmpty() },
                    techniques = state.techniques.takeIf { it.isNotEmpty() },
                    vendors = state.vendors.takeIf { it.isNotEmpty() },
                    targetSectors = state.targetSectors.takeIf { it.isNotEmpty() },
                    targetCountries = state.targetCountries.takeIf { it.isNotEmpty() },
                    cadence = state.cadence,
                    cadenceMinute = if (state.cadence == "hourly") state.cadenceMinute else null,
                    cadenceTime = if (state.cadence in listOf("daily", "weekly", "monthly", "yearly")) state.cadenceTime else null,
                    cadenceDayOfWeek = if (state.cadence == "weekly") state.cadenceDayOfWeek else null,
                    cadenceDayOfMonth = if (state.cadence in listOf("monthly", "yearly")) state.cadenceDayOfMonth else null,
                    cadenceMonth = if (state.cadence == "yearly") state.cadenceMonth else null,
                    timezone = state.timezone
                )
                subscriptionRepository.createSubscription(request).onSuccess {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }
    }

    private fun buildPatch(existing: Subscription, form: SubscriptionFormState): PatchSubscriptionRequest {
        val cadenceChanged = form.cadence != existing.cadence
        return PatchSubscriptionRequest(
            name = form.name.takeIf { it != existing.name },
            categories = form.categories.takeIf { it != existing.categories },
            actors = form.actors.takeIf { it != existing.actors },
            ransomwareFamilies = form.ransomwareFamilies.takeIf { it != existing.ransomwareFamilies },
            products = form.products.takeIf { it != existing.products },
            tools = form.tools.takeIf { it != existing.tools },
            techniques = form.techniques.takeIf { it != existing.techniques },
            vendors = form.vendors.takeIf { it != existing.vendors },
            targetSectors = form.targetSectors.takeIf { it != existing.targetSectors },
            targetCountries = form.targetCountries.takeIf { it != existing.targetCountries },
            cadence = form.cadence.takeIf { cadenceChanged },
            cadenceMinute = if (form.cadence == "hourly") {
                form.cadenceMinute.takeIf { it != existing.cadenceMinute || cadenceChanged }
            } else null,
            cadenceTime = if (form.cadence in listOf("daily", "weekly", "monthly", "yearly")) {
                form.cadenceTime.takeIf { it != existing.cadenceTime || cadenceChanged }
            } else null,
            cadenceDayOfWeek = if (form.cadence == "weekly") {
                form.cadenceDayOfWeek.takeIf { it != existing.cadenceDayOfWeek || cadenceChanged }
            } else null,
            cadenceDayOfMonth = if (form.cadence in listOf("monthly", "yearly")) {
                form.cadenceDayOfMonth.takeIf { it != existing.cadenceDayOfMonth || cadenceChanged }
            } else null,
            cadenceMonth = if (form.cadence == "yearly") {
                form.cadenceMonth.takeIf { it != existing.cadenceMonth || cadenceChanged }
            } else null,
            timezone = form.timezone.takeIf { it != existing.timezone }
        )
    }
}
