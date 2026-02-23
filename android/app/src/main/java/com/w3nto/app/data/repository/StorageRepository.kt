package com.w3nto.app.data.repository

import com.w3nto.app.data.remote.StorageDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storageDataSource: StorageDataSource
) {
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> = runCatching {
        storageDataSource.uploadAvatar(userId, imageBytes)
    }

    suspend fun deleteAvatar(userId: String): Result<Unit> = runCatching {
        storageDataSource.deleteAvatar(userId)
    }
}
