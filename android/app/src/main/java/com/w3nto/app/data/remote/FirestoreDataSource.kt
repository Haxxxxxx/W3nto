package com.w3nto.app.data.remote

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.w3nto.app.data.model.BentoPage
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.FavoritePage
import com.w3nto.app.data.model.Profile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // ── Profiles ──

    suspend fun getProfile(username: String): Profile? {
        val doc = firestore.collection("profiles").document(username).get().await()
        return if (doc.exists()) Profile.fromMap(username, doc.data!!) else null
    }

    // ── Pages ──

    suspend fun getPages(username: String): List<BentoPage> {
        val snapshot = firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { doc -> doc.data?.let { BentoPage.fromMap(doc.id, it) } }
            .sortedBy { it.position ?: Int.MAX_VALUE }
    }

    suspend fun getPage(username: String, pageId: String): BentoPage? {
        val doc = firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .document(pageId)
            .get()
            .await()
        return if (doc.exists()) BentoPage.fromMap(doc.id, doc.data!!) else null
    }

    // ── Blocks ──

    suspend fun getBlocks(username: String, pageId: String): List<Block> {
        val snapshot = firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .document(pageId)
            .collection("blocks")
            .orderBy("position", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Block.fromMap(doc.id, it) }
        }
    }

    // ── Discover ──

    suspend fun getDiscoverProfiles(
        pageSize: Int = 20,
        lastDocumentId: String? = null
    ): List<Profile> {
        var query = firestore.collection("profiles")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocumentId != null) {
            val lastDoc = firestore.collection("profiles").document(lastDocumentId).get().await()
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc)
            }
        }

        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Profile.fromMap(doc.id, it) }
        }
    }

    suspend fun searchProfiles(searchQuery: String): List<Profile> {
        val query = searchQuery.lowercase()
        val end = query + "\uf8ff"
        val snapshot = firestore.collection("profiles")
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), query)
            .whereLessThanOrEqualTo(FieldPath.documentId(), end)
            .limit(20)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            Profile.fromMap(doc.id, doc.data ?: return@mapNotNull null)
        }
    }

    // ── Profile Writes ──

    suspend fun saveProfile(username: String, profile: Profile) {
        firestore.collection("profiles")
            .document(username)
            .set(profile.toMap())
            .await()
    }

    suspend fun getProfileByUid(uid: String): Profile? {
        val snapshot = firestore.collection("profiles")
            .whereEqualTo("uid", uid)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.let { doc ->
            doc.data?.let { Profile.fromMap(doc.id, it) }
        }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        val doc = firestore.collection("profiles").document(username).get().await()
        return !doc.exists()
    }

    suspend fun createWalletLookup(walletAddress: String, username: String, uid: String) {
        firestore.collection("wallets")
            .document(walletAddress)
            .set(
                mapOf(
                    "username" to username,
                    "uid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    // ── Page Writes ──

    suspend fun createPage(username: String, page: BentoPage): String {
        val ref = firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .add(page.toMap())
            .await()
        return ref.id
    }

    suspend fun updatePage(username: String, pageId: String, page: BentoPage) {
        firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .document(pageId)
            .set(page.toMap())
            .await()
    }

    suspend fun deletePage(username: String, pageId: String) {
        val blocks = firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .document(pageId)
            .collection("blocks")
            .get()
            .await()
        val batch = firestore.batch()
        blocks.documents.forEach { batch.delete(it.reference) }
        batch.delete(
            firestore.collection("profiles")
                .document(username)
                .collection("pages")
                .document(pageId)
        )
        batch.commit().await()
    }

    // ── Block Writes ──

    suspend fun createBlock(username: String, pageId: String, block: Block): String {
        val ref = firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .document(pageId)
            .collection("blocks")
            .add(block.toMap())
            .await()
        return ref.id
    }

    suspend fun updateBlock(username: String, pageId: String, block: Block) {
        firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .document(pageId)
            .collection("blocks")
            .document(block.id)
            .set(block.toMap())
            .await()
    }

    suspend fun deleteBlock(username: String, pageId: String, blockId: String) {
        firestore.collection("profiles")
            .document(username)
            .collection("pages")
            .document(pageId)
            .collection("blocks")
            .document(blockId)
            .delete()
            .await()
    }

    // ── Page-Level Favorites ──

    suspend fun getPageFavorites(userId: String): List<FavoritePage> {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { FavoritePage.fromMap(it) }
        }
    }

    suspend fun addPageFavorite(userId: String, favorite: FavoritePage) {
        val docId = "${favorite.username}:::${favorite.pageId}"
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(docId)
            .set(favorite.toMap())
            .await()
    }

    suspend fun removePageFavorite(userId: String, username: String, pageId: String) {
        val docId = "${username}:::${pageId}"
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(docId)
            .delete()
            .await()
    }

    suspend fun isPageFavorited(userId: String, username: String, pageId: String): Boolean {
        val docId = "${username}:::${pageId}"
        val doc = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(docId)
            .get()
            .await()
        return doc.exists()
    }
}
