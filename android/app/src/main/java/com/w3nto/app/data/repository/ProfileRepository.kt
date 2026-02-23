package com.w3nto.app.data.repository

import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.remote.FirestoreDataSource
import com.w3nto.app.data.remote.SlugInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    suspend fun getProfile(username: String): Result<Profile?> = runCatching {
        firestoreDataSource.getProfile(username)
    }

    suspend fun getProfileByUid(uid: String): Result<Profile?> = runCatching {
        firestoreDataSource.getProfileByUid(uid)
    }

    suspend fun saveProfile(username: String, profile: Profile): Result<Unit> = runCatching {
        firestoreDataSource.saveProfile(username, profile)
    }

    suspend fun isUsernameAvailable(username: String): Result<Boolean> = runCatching {
        firestoreDataSource.isUsernameAvailable(username)
    }

    suspend fun createWalletLookup(walletAddress: String, username: String, uid: String): Result<Unit> = runCatching {
        firestoreDataSource.createWalletLookup(walletAddress, username, uid)
    }

    /**
     * Resolves a URL slug to (username, pageId?).
     * Tries slug registry first, falls back to treating slug as a username.
     * Returns null if neither match is found.
     */
    suspend fun resolveSlug(slug: String): Result<Pair<String, String?>?> = runCatching {
        val slugInfo: SlugInfo? = firestoreDataSource.getSlugInfo(slug)
        if (slugInfo != null) {
            Pair(slugInfo.username, slugInfo.pageId)
        } else {
            val profile = firestoreDataSource.getProfile(slug)
            if (profile != null) Pair(slug, null) else null
        }
    }
}
