package com.w3nto.app.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favorites: List<Profile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = favoritesRepository.getFavorites(userId)
            result.fold(
                onSuccess = { profiles ->
                    _uiState.value = _uiState.value.copy(
                        favorites = profiles,
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

    fun removeFavorite(username: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // Optimistically remove from list immediately
            _uiState.value = _uiState.value.copy(
                favorites = _uiState.value.favorites.filter { it.username != username }
            )
            val result = favoritesRepository.removeFavorite(userId, username)
            result.onFailure { error ->
                // On failure, reload the real state from Firestore
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Failed to remove favorite"
                )
                loadFavorites()
            }
        }
    }
}
