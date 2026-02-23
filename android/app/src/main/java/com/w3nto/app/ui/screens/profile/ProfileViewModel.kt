package com.w3nto.app.ui.screens.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.w3nto.app.data.model.BentoPage
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.FavoritePage
import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.repository.BlockRepository
import com.w3nto.app.data.repository.FavoritesRepository
import com.w3nto.app.data.repository.PageRepository
import com.w3nto.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicProfileUiState(
    val profile: Profile? = null,
    val pages: List<BentoPage> = emptyList(),
    val currentPage: BentoPage? = null,
    val blocks: List<Block> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val notFound: Boolean = false,
    val error: String? = null,
    val isFavorited: Boolean = false,
    val isOwnProfile: Boolean = false
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

    // The raw URL slug (may be a username or a page slug)
    private val slug: String = savedStateHandle["slug"] ?: ""

    // Resolved after slug lookup: actual profile username and optional target page ID
    private var resolvedUsername: String = ""
    private var targetPageId: String? = null

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun retryLoad() {
        _uiState.value = PublicProfileUiState()
        resolvedUsername = ""
        targetPageId = null
        loadProfile()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        val username = resolvedUsername.ifEmpty { return }
        _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
        viewModelScope.launch {
            val result = profileRepository.getProfile(username)

            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Failed to refresh profile"
                )
                return@launch
            }

            val profile = result.getOrNull()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, notFound = true)
                return@launch
            }

            val pagesResult = pageRepository.getPages(username)
            val pages = pagesResult.getOrDefault(emptyList())

            val currentPageId = _uiState.value.currentPage?.id ?: profile.defaultPageId
            val currentPage = pages.find { it.id == currentPageId }
                ?: pages.find { it.id == profile.defaultPageId }
                ?: pages.firstOrNull()

            val blocks = if (currentPage != null) {
                blockRepository.getBlocks(username, currentPage.id).getOrDefault(emptyList())
            } else {
                emptyList()
            }

            _uiState.value = _uiState.value.copy(
                profile = profile,
                pages = pages,
                currentPage = currentPage,
                blocks = blocks,
                isRefreshing = false,
                isOwnProfile = profile.uid == auth.currentUser?.uid
            )

            if (currentPage != null) {
                checkPageFavorited(currentPage.id)
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            // Step 1: Resolve the slug (page slug → username+pageId, or username fallback)
            val resolveResult = profileRepository.resolveSlug(slug)
            resolveResult.onFailure { e ->
                _uiState.value = PublicProfileUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
                return@launch
            }

            val resolved = resolveResult.getOrNull()
            if (resolved == null) {
                _uiState.value = PublicProfileUiState(isLoading = false, notFound = true)
                return@launch
            }

            resolvedUsername = resolved.first
            targetPageId = resolved.second

            // Step 2: Fetch full profile
            val result = profileRepository.getProfile(resolvedUsername)

            result.onFailure { e ->
                _uiState.value = PublicProfileUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
                return@launch
            }

            val profile = result.getOrNull()
            if (profile == null) {
                _uiState.value = PublicProfileUiState(isLoading = false, notFound = true)
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                profile = profile,
                isOwnProfile = profile.uid == auth.currentUser?.uid
            )

            val pagesResult = pageRepository.getPages(resolvedUsername)
            val pages = pagesResult.getOrDefault(emptyList())

            val currentPage = if (targetPageId != null) {
                pages.find { it.id == targetPageId }
                    ?: pages.find { it.id == profile.defaultPageId }
                    ?: pages.firstOrNull()
            } else {
                pages.find { it.id == profile.defaultPageId } ?: pages.firstOrNull()
            }

            _uiState.value = _uiState.value.copy(pages = pages, currentPage = currentPage)

            if (currentPage != null) {
                val blocksResult = blockRepository.getBlocks(resolvedUsername, currentPage.id)
                _uiState.value = _uiState.value.copy(
                    blocks = blocksResult.getOrDefault(emptyList()),
                    isLoading = false
                )
                checkPageFavorited(currentPage.id)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun checkPageFavorited(pageId: String) {
        val userId = auth.currentUser?.uid ?: return
        val username = resolvedUsername.ifEmpty { return }
        viewModelScope.launch {
            val isFavorited = favoritesRepository.isPageFavorited(userId, username, pageId)
                .getOrDefault(false)
            _uiState.value = _uiState.value.copy(isFavorited = isFavorited)
        }
    }

    fun selectPage(page: BentoPage) {
        val username = resolvedUsername.ifEmpty { return }
        _uiState.value = _uiState.value.copy(currentPage = page, blocks = emptyList(), isFavorited = false, isLoading = true)
        viewModelScope.launch {
            val blocksResult = blockRepository.getBlocks(username, page.id)
            _uiState.value = _uiState.value.copy(
                blocks = blocksResult.getOrDefault(emptyList()),
                isLoading = false
            )
            checkPageFavorited(page.id)
        }
    }

    fun toggleFavorite() {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(error = "Connect your wallet to save favorites")
            viewModelScope.launch {
                delay(3000)
                _uiState.value = _uiState.value.copy(error = null)
            }
            return
        }
        val currentPage = _uiState.value.currentPage ?: return
        val profile = _uiState.value.profile ?: return
        val username = resolvedUsername.ifEmpty { return }
        val currentlyFavorited = _uiState.value.isFavorited

        // Optimistic update
        _uiState.value = _uiState.value.copy(isFavorited = !currentlyFavorited)

        viewModelScope.launch {
            val result = if (currentlyFavorited) {
                favoritesRepository.removePageFavorite(userId, username, currentPage.id)
            } else {
                val favorite = FavoritePage(
                    username = username,
                    pageId = currentPage.id,
                    pageName = currentPage.name,
                    pageSlug = currentPage.slug,
                    profileDisplayName = profile.displayName,
                    profileAvatar = profile.avatar
                )
                favoritesRepository.addPageFavorite(userId, favorite)
            }
            result.onFailure { e ->
                // Revert on failure
                _uiState.value = _uiState.value.copy(
                    isFavorited = currentlyFavorited,
                    error = "Failed to update favorite: ${e.message}"
                )
                // Auto-clear error after 3 seconds
                viewModelScope.launch {
                    delay(3000)
                    if (_uiState.value.error?.startsWith("Failed to update") == true) {
                        _uiState.value = _uiState.value.copy(error = null)
                    }
                }
            }
        }
    }
}
