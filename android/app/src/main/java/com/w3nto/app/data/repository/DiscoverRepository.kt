package com.w3nto.app.data.repository

import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.remote.FirestoreDataSource
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoverRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    private val paginationMutex = kotlinx.coroutines.sync.Mutex()
    private var lastDocumentId: String? = null

    fun resetPagination() {
        lastDocumentId = null
    }

    suspend fun getProfiles(pageSize: Int = 20): Result<List<Profile>> = runCatching {
        paginationMutex.withLock {
            val profiles = firestoreDataSource.getDiscoverProfiles(
                pageSize = pageSize,
                lastDocumentId = lastDocumentId
            )
            if (profiles.isNotEmpty()) {
                lastDocumentId = profiles.last().username
            }
            profiles
        }
    }

    suspend fun searchProfiles(query: String): Result<List<Profile>> = runCatching {
        firestoreDataSource.searchProfiles(query)
    }
}
