package com.w3nto.app.data.remote

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String {
        val ref = storage.reference.child("profile_images/$userId/avatar.jpg")
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        ref.putBytes(imageBytes, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteAvatar(userId: String) {
        runCatching {
            storage.reference.child("profile_images/$userId/avatar.jpg").delete().await()
        }
    }
}
