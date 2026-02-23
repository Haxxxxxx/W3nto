package com.w3nto.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.w3nto.app.data.model.FavoritePage
import com.w3nto.app.data.repository.AuthRepository
import com.w3nto.app.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<FavoritePage> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val requiresAuth: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
        viewModelScope.launch {
            authRepository.authStateFlow()
                .drop(1)
                .distinctUntilChanged { old, new -> old?.uid == new?.uid }
                .collect { loadFavorites() }
        }
    }

    fun refresh() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            val result = favoritesRepository.getPageFavorites(userId)
            result.fold(
                onSuccess = { favorites ->
                    _uiState.value = _uiState.value.copy(favorites = favorites, isRefreshing = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = error.message ?: "Failed to refresh favorites"
                    )
                }
            )
        }
    }

    fun loadFavorites() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = FavoritesUiState(isLoading = false, requiresAuth = true)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, requiresAuth = false)
            val result = favoritesRepository.getPageFavorites(userId)
            result.fold(
                onSuccess = { favorites ->
                    _uiState.value = _uiState.value.copy(
                        favorites = favorites,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load favorites"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun removeFavorite(username: String, pageId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // Optimistically remove from list
            _uiState.value = _uiState.value.copy(
                favorites = _uiState.value.favorites.filter {
                    !(it.username == username && it.pageId == pageId)
                }
            )
            val result = favoritesRepository.removePageFavorite(userId, username, pageId)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Failed to remove favorite"
                )
                loadFavorites()
            }
        }
    }
}
