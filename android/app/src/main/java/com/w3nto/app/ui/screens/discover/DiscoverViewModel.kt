package com.w3nto.app.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.repository.DiscoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val profiles: List<Profile> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val hasMore: Boolean = true
)

@OptIn(FlowPreview::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        loadProfiles()
        observeSearch()
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .drop(1)
                .debounce(400L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        refresh()
                    } else {
                        searchProfiles(query)
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            discoverRepository.resetPagination()
            val result = discoverRepository.getProfiles()
            result.fold(
                onSuccess = { profiles ->
                    _uiState.value = _uiState.value.copy(
                        profiles = profiles,
                        isLoading = false,
                        hasMore = profiles.size == 20
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load profiles"
                    )
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            discoverRepository.resetPagination()
            val result = discoverRepository.getProfiles()
            result.fold(
                onSuccess = { profiles ->
                    _uiState.value = _uiState.value.copy(
                        profiles = profiles,
                        isRefreshing = false,
                        hasMore = profiles.size == 20
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = error.message ?: "Failed to refresh profiles"
                    )
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.searchQuery.isNotBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val result = discoverRepository.getProfiles()
            result.fold(
                onSuccess = { newProfiles ->
                    _uiState.value = _uiState.value.copy(
                        profiles = _uiState.value.profiles + newProfiles,
                        isLoadingMore = false,
                        hasMore = newProfiles.size == 20
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = error.message ?: "Failed to load more profiles"
                    )
                }
            )
        }
    }

    private fun searchProfiles(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = discoverRepository.searchProfiles(query)
            result.fold(
                onSuccess = { profiles ->
                    _uiState.value = _uiState.value.copy(
                        profiles = profiles,
                        isLoading = false,
                        hasMore = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Search failed"
                    )
                }
            )
        }
    }
}
