package com.w3nto.app.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.w3nto.app.data.model.BentoPage
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.repository.BlockRepository
import com.w3nto.app.data.repository.FavoritesRepository
import com.w3nto.app.data.repository.PageRepository
import com.w3nto.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicProfileUiState(
    val profile: Profile? = null,
    val pages: List<BentoPage> = emptyList(),
    val currentPage: BentoPage? = null,
    val blocks: List<Block> = emptyList(),
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val isFavorited: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val pageRepository: PageRepository,
    private val blockRepository: BlockRepository,
    private val favoritesRepository: FavoritesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val username: String = savedStateHandle["username"] ?: ""
    private val pageSlug: String? = savedStateHandle["pageSlug"]

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val result = profileRepository.getProfile(username)
            val profile = result.getOrNull()

            if (profile == null) {
                _uiState.value = PublicProfileUiState(isLoading = false, notFound = true)
                return@launch
            }

            _uiState.value = _uiState.value.copy(profile = profile)

            val pagesResult = pageRepository.getPages(username)
            val pages = pagesResult.getOrDefault(emptyList())

            val currentPage = if (pageSlug != null) {
                pages.find { it.slug == pageSlug }
            } else {
                pages.find { it.id == profile.defaultPageId } ?: pages.firstOrNull()
            }

            _uiState.value = _uiState.value.copy(pages = pages, currentPage = currentPage)

            if (currentPage != null) {
                val blocksResult = blockRepository.getBlocks(username, currentPage.id)
                _uiState.value = _uiState.value.copy(
                    blocks = blocksResult.getOrDefault(emptyList()),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }

            // Check favorite status for authenticated users
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val isFavorited = favoritesRepository.isFavorited(userId, username)
                    .getOrDefault(false)
                _uiState.value = _uiState.value.copy(isFavorited = isFavorited)
            }
        }
    }

    fun selectPage(page: BentoPage) {
        _uiState.value = _uiState.value.copy(currentPage = page, isLoading = true)
        viewModelScope.launch {
            val blocksResult = blockRepository.getBlocks(username, page.id)
            _uiState.value = _uiState.value.copy(
                blocks = blocksResult.getOrDefault(emptyList()),
                isLoading = false
            )
        }
    }

    fun toggleFavorite() {
        val userId = auth.currentUser?.uid ?: return
        val currentlyFavorited = _uiState.value.isFavorited

        // Optimistic update
        _uiState.value = _uiState.value.copy(isFavorited = !currentlyFavorited)

        viewModelScope.launch {
            val result = if (currentlyFavorited) {
                favoritesRepository.removeFavorite(userId, username)
            } else {
                favoritesRepository.addFavorite(userId, username)
            }
            result.onFailure {
                // Revert on failure
                _uiState.value = _uiState.value.copy(isFavorited = currentlyFavorited)
            }
        }
    }
}
