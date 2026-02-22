package com.w3nto.app.data.remote

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { BentoPage.fromMap(doc.id, it) }
        }
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
        val end = searchQuery + "\uf8ff"
        val snapshot = firestore.collection("profiles")
            .orderBy(FieldPath.documentId())
            .startAt(searchQuery.lowercase())
            .endAt(end.lowercase())
            .limit(20)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.data?.let { Profile.fromMap(doc.id, it) }
        }
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
