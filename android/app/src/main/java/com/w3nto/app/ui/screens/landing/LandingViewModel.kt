package com.w3nto.app.ui.screens.landing

import androidx.lifecycle.ViewModel
import com.w3nto.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    val isAuthenticated: Boolean
        get() = authRepository.isAuthenticated
}
