package com.w3nto.app.data.repository

import android.net.Uri
import com.w3nto.app.data.remote.StorageDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storageDataSource: StorageDataSource
) {
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> = runCatching {
        val path = "profile_images/$userId/avatar.jpg"
        storageDataSource.uploadImage(path, imageUri)
    }

    suspend fun uploadBlockImage(userId: String, blockId: String, imageUri: Uri): Result<String> = runCatching {
        val path = "profile_images/$userId/blocks/$blockId.jpg"
        storageDataSource.uploadImage(path, imageUri)
    }

    suspend fun deleteImage(path: String): Result<Unit> = runCatching {
        storageDataSource.deleteImage(path)
    }
}
