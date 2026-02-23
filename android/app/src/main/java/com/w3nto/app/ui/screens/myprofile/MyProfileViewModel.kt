package com.w3nto.app.ui.screens.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.w3nto.app.data.model.BentoPage
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.repository.AuthRepository
import com.w3nto.app.data.repository.BlockRepository
import com.w3nto.app.data.repository.PageRepository
import com.w3nto.app.data.repository.ProfileRepository
import com.w3nto.app.data.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyProfileUiState(
    val profile: Profile? = null,
    val pages: List<BentoPage> = emptyList(),
    val selectedPage: BentoPage? = null,
    val blocks: List<Block> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val requiresAuth: Boolean = false,
    // Edit profile fields
    val isEditing: Boolean = false,
    val editDisplayName: String = "",
    val editBio: String = "",
    val editShowWallet: Boolean = true,
    // Create profile flow
    val showCreateProfile: Boolean = false,
    val createUsername: String = "",
    val isCheckingUsername: Boolean = false,
    val usernameError: String? = null,
    val isCreatingProfile: Boolean = false,
    // Create page dialog
    val showCreatePage: Boolean = false,
    val newPageName: String = ""
)

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val pageRepository: PageRepository,
    private val blockRepository: BlockRepository,
    @Suppress("unused") private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProfileUiState())
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authStateFlow().collect { user ->
                if (user == null) {
                    _uiState.update {
                        it.copy(
                            requiresAuth = true,
                            isLoading = false,
                            profile = null,
                            pages = emptyList(),
                            blocks = emptyList()
                        )
                    }
                } else {
                    loadProfile(user.uid)
                }
            }
        }
    }

    private suspend fun loadProfile(uid: String) {
        _uiState.update { it.copy(isLoading = true, error = null, requiresAuth = false) }
        profileRepository.getProfileByUid(uid).fold(
            onSuccess = { profile ->
                if (profile != null) {
                    _uiState.update { it.copy(profile = profile, isLoading = false, showCreateProfile = false) }
                    loadPages(profile.username)
                } else {
                    _uiState.update { it.copy(profile = null, isLoading = false, showCreateProfile = true) }
                }
            },
            onFailure = { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        )
    }

    private suspend fun loadPages(username: String) {
        pageRepository.getPages(username).fold(
            onSuccess = { pages ->
                val defaultPage = pages.firstOrNull()
                _uiState.update { it.copy(pages = pages, selectedPage = defaultPage) }
                defaultPage?.let { page -> loadBlocks(username, page.id) }
            },
            onFailure = { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        )
    }

    private suspend fun loadBlocks(username: String, pageId: String) {
        blockRepository.getBlocks(username, pageId).fold(
            onSuccess = { blocks ->
                _uiState.update { it.copy(blocks = blocks) }
            },
            onFailure = { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        )
    }

    fun selectPage(page: BentoPage) {
        val username = _uiState.value.profile?.username ?: return
        _uiState.update { it.copy(selectedPage = page, blocks = emptyList()) }
        viewModelScope.launch { loadBlocks(username, page.id) }
    }

    // ── Edit Profile ──

    fun startEditing() {
        val profile = _uiState.value.profile ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editDisplayName = profile.displayName,
                editBio = profile.bio,
                editShowWallet = profile.showWallet
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(editDisplayName = name) }
    }

    fun updateBio(bio: String) {
        _uiState.update { it.copy(editBio = bio) }
    }

    fun updateShowWallet(show: Boolean) {
        _uiState.update { it.copy(editShowWallet = show) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val profile = state.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = profile.copy(
                displayName = state.editDisplayName,
                bio = state.editBio,
                showWallet = state.editShowWallet
            )
            profileRepository.saveProfile(profile.username, updated).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            profile = updated,
                            isSaving = false,
                            isEditing = false,
                            successMessage = "Profile saved"
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }

    // ── Create Profile ──

    fun updateCreateUsername(username: String) {
        _uiState.update { it.copy(createUsername = username, usernameError = null) }
    }

    fun createProfile() {
        val state = _uiState.value
        val username = state.createUsername.trim().lowercase()
        val uid = authRepository.currentUser?.uid ?: return

        if (!username.matches(Regex("^[a-z0-9_]{1,30}$"))) {
            _uiState.update { it.copy(usernameError = "Username must be 1-30 lowercase letters, numbers, or underscores") }
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
                            profileRepository.createWalletLookup(uid, username)
                            _uiState.update {
                                it.copy(
                                    profile = newProfile,
                                    isCreatingProfile = false,
                                    showCreateProfile = false,
                                    successMessage = "Profile created!"
                                )
                            }
                            loadPages(username)
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isCreatingProfile = false, error = e.message) }
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isCheckingUsername = false, error = e.message) }
                }
            )
        }
    }

    // ── Page Management ──

    fun showCreatePageDialog() {
        _uiState.update { it.copy(showCreatePage = true, newPageName = "") }
    }

    fun hideCreatePageDialog() {
        _uiState.update { it.copy(showCreatePage = false) }
    }

    fun updateNewPageName(name: String) {
        _uiState.update { it.copy(newPageName = name) }
    }

    fun createPage() {
        val username = _uiState.value.profile?.username ?: return
        val name = _uiState.value.newPageName.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            val position = _uiState.value.pages.size
            val newPage = BentoPage(name = name, slug = slug, position = position)
            pageRepository.createPage(username, newPage).fold(
                onSuccess = { pageId ->
                    val created = newPage.copy(id = pageId)
                    _uiState.update {
                        it.copy(
                            pages = it.pages + created,
                            selectedPage = created,
                            blocks = emptyList(),
                            showCreatePage = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, showCreatePage = false) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
