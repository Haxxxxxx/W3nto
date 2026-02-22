package com.w3nto.app.data.repository

import com.w3nto.app.data.model.FavoritePage
import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.remote.FirestoreDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    // Legacy profile-level favorites (kept for backward compat)
    suspend fun getFavorites(userId: String): Result<List<Profile>> = runCatching {
        firestoreDataSource.getFavorites(userId)
    }

    suspend fun addFavorite(userId: String, username: String): Result<Unit> = runCatching {
        firestoreDataSource.addFavorite(userId, username)
    }

    suspend fun removeFavorite(userId: String, username: String): Result<Unit> = runCatching {
        firestoreDataSource.removeFavorite(userId, username)
    }

    suspend fun isFavorited(userId: String, username: String): Result<Boolean> = runCatching {
        firestoreDataSource.isFavorited(userId, username)
    }

    // Page-level favorites
    suspend fun getPageFavorites(userId: String): Result<List<FavoritePage>> = runCatching {
        firestoreDataSource.getPageFavorites(userId)
    }

    suspend fun addPageFavorite(userId: String, favorite: FavoritePage): Result<Unit> = runCatching {
        firestoreDataSource.addPageFavorite(userId, favorite)
    }

    suspend fun removePageFavorite(userId: String, username: String, pageId: String): Result<Unit> = runCatching {
        firestoreDataSource.removePageFavorite(userId, username, pageId)
    }

    suspend fun isPageFavorited(userId: String, username: String, pageId: String): Result<Boolean> = runCatching {
        firestoreDataSource.isPageFavorited(userId, username, pageId)
    }
}
