package com.w3nto.app.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadImage(path: String, uri: Uri): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteImage(path: String) {
        storage.reference.child(path).delete().await()
    }

    fun getImageRef(path: String) = storage.reference.child(path)
}
