package com.w3nto.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.w3nto.app.data.repository.AuthRepository
import com.w3nto.app.wallet.WalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val walletAddress: String = "",
    val walletProvider: String = "",
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val walletManager: WalletManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                authRepository.authStateFlow(),
                walletManager.connectedWallet
            ) { user, wallet ->
                SettingsUiState(
                    walletAddress = wallet?.publicKey ?: user?.uid ?: "",
                    walletProvider = wallet?.provider?.name?.let { p ->
                        if (p == "SAGA_SEED_VAULT") "Saga Seed Vault"
                        else p.lowercase().replaceFirstChar { it.uppercase() }
                    } ?: "",
                    isAuthenticated = user != null
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun signOut() {
        walletManager.disconnect()
    }
}
