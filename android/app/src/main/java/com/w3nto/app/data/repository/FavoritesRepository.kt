package com.w3nto.app.data.repository

import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.remote.FirestoreDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
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
}
