package com.w3nto.app.data.repository

import com.w3nto.app.data.model.Block
import com.w3nto.app.data.remote.FirestoreDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    suspend fun getBlocks(username: String, pageId: String): Result<List<Block>> = runCatching {
        firestoreDataSource.getBlocks(username, pageId)
    }

    suspend fun saveBlock(username: String, pageId: String, block: Block): Result<String> = runCatching {
        firestoreDataSource.setBlock(username, pageId, block)
    }

    suspend fun deleteBlock(username: String, pageId: String, blockId: String): Result<Unit> = runCatching {
        firestoreDataSource.deleteBlock(username, pageId, blockId)
    }
}
