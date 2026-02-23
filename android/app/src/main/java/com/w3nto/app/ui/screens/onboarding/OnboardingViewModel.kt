package com.w3nto.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.repository.AuthRepository
import com.w3nto.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val username: String = "",
    val isCheckingUsername: Boolean = false,
    val isCreatingProfile: Boolean = false,
    val usernameError: String? = null,
    val profileCreated: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username, usernameError = null) }
    }

    fun createProfile() {
        val username = _uiState.value.username.trim().lowercase()
        val uid = authRepository.currentUser?.uid ?: return

        if (!username.matches(Regex("^[a-z0-9_]{1,30}$"))) {
            _uiState.update {
                it.copy(usernameError = "Username must be 1–30 lowercase letters, numbers, or underscores")
            }
            return
        }

        if (username in RESERVED_USERNAMES) {
            _uiState.update { it.copy(usernameError = "This username is reserved") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUsername = true, usernameError = null) }
            profileRepository.isUsernameAvailable(username).fold(
                onSuccess = { available ->
                    if (!available) {
                        _uiState.update { it.copy(isCheckingUsername = false, usernameError = "Username is already taken") }
                        return@fold
                    }
                    _uiState.update { it.copy(isCheckingUsername = false, isCreatingProfile = true) }
                    val newProfile = Profile(
                        username = username,
                        walletAddress = uid,
                        uid = uid,
                        displayName = username,
                        bio = "",
                        showWallet = true
                    )
                    profileRepository.saveProfile(username, newProfile).fold(
                        onSuccess = {
                            profileRepository.createWalletLookup(uid, username, uid)
                            _uiState.update { it.copy(isCreatingProfile = false, profileCreated = true) }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isCreatingProfile = false, usernameError = e.message ?: "Failed to create profile") }
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isCheckingUsername = false, usernameError = e.message ?: "Error checking username") }
                }
            )
        }
    }

    fun clearProfileCreated() {
        _uiState.update { it.copy(profileCreated = false) }
    }

    companion object {
        private val RESERVED_USERNAMES = setOf(
            "dashboard", "settings", "admin", "api", "auth",
            "login", "signup", "register", "profile", "discover",
            "favorites", "onboarding", "connect", "wallet",
            "about", "help", "support", "terms", "privacy",
            "app", "www", "mail", "ftp", "cdn", "static"
        )
    }
}
