package com.w3nto.app.data.repository

import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.remote.FirestoreDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    suspend fun getProfile(username: String): Result<Profile?> = runCatching {
        firestoreDataSource.getProfile(username)
    }
}
