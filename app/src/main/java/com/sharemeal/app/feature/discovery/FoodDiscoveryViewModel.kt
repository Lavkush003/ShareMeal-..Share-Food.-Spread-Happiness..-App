package com.sharemeal.app.feature.discovery

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharemeal.app.R
import com.sharemeal.app.core.common.AppError
import com.sharemeal.app.domain.model.FoodDiscoveryItem
import com.sharemeal.app.domain.model.FoodDiscoveryQuery
import com.sharemeal.app.domain.repository.FoodDiscoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodDiscoveryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val query: FoodDiscoveryQuery = FoodDiscoveryQuery(),
    val items: List<FoodDiscoveryItem> = emptyList(),
    val activeFilterCount: Int = 0,
    @StringRes val errorMessageRes: Int? = null
)

@HiltViewModel
class FoodDiscoveryViewModel @Inject constructor(
    private val foodDiscoveryRepository: FoodDiscoveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodDiscoveryUiState())
    val uiState: StateFlow<FoodDiscoveryUiState> = _uiState.asStateFlow()

    init {
        observeDiscoveryItems()
    }

    private fun observeDiscoveryItems() {
        viewModelScope.launch {
            foodDiscoveryRepository.discoveryItems.collect { _ ->
                applyFilters(_uiState.value.query)
            }
        }
    }

    fun onSearchQueryChange(search: String) {
        val updatedQuery = _uiState.value.query.copy(searchQuery = search)
        updateQuery(updatedQuery)
    }

    fun updateQuery(query: FoodDiscoveryQuery) {
        val count = calculateActiveFilterCount(query)
        _uiState.update {
            it.copy(
                query = query,
                activeFilterCount = count
            )
        }
        applyFilters(query)
    }

    fun clearFilters() {
        val defaultQuery = FoodDiscoveryQuery(searchQuery = _uiState.value.query.searchQuery)
        updateQuery(defaultQuery)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessageRes = null) }
            val result = foodDiscoveryRepository.refresh()
            if (result is com.sharemeal.app.core.common.AppResult.Failure) {
                _uiState.update { it.copy(errorMessageRes = result.error.messageRes()) }
            }
            applyFilters(_uiState.value.query)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun applyFilters(query: FoodDiscoveryQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
            try {
                val filteredItems = foodDiscoveryRepository.filterDonations(query)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = filteredItems
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessageRes = R.string.error_generic
                    )
                }
            }
        }
    }

    private fun calculateActiveFilterCount(query: FoodDiscoveryQuery): Int {
        var count = 0
        if (query.category != null) count++
        if (query.classification != null) count++
        if (query.minQuantity != null) count++
        if (query.maxDistanceKm != null) count++
        if (query.maxDeadlineHours != null) count++
        if (query.sortBy != com.sharemeal.app.domain.model.FoodSortOrder.NEWEST) count++
        return count
    }

    private fun AppError.messageRes(): Int = when (this) {
        is AppError.Network -> R.string.error_network
        else -> R.string.error_generic
    }
}
