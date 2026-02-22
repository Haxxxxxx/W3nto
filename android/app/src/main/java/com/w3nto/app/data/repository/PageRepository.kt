package com.w3nto.app.data.repository

import com.w3nto.app.data.model.BentoPage
import com.w3nto.app.data.remote.FirestoreDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    suspend fun getPages(username: String): Result<List<BentoPage>> = runCatching {
        firestoreDataSource.getPages(username)
    }

    suspend fun getPage(username: String, pageId: String): Result<BentoPage?> = runCatching {
        firestoreDataSource.getPage(username, pageId)
    }
}
