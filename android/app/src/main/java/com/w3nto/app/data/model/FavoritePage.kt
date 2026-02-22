package com.w3nto.app.data.model

import com.google.firebase.Timestamp

data class FavoritePage(
    val username: String = "",
    val pageId: String = "",
    val pageName: String = "",
    val pageSlug: String = "",
    val profileDisplayName: String = "",
    val profileAvatar: String = "",
    val savedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("username", username)
        put("pageId", pageId)
        put("pageName", pageName)
        put("pageSlug", pageSlug)
        put("profileDisplayName", profileDisplayName)
        put("profileAvatar", profileAvatar)
        put("savedAt", savedAt ?: Timestamp.now())
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): FavoritePage = FavoritePage(
            username = map["username"] as? String ?: "",
            pageId = map["pageId"] as? String ?: "",
            pageName = map["pageName"] as? String ?: "",
            pageSlug = map["pageSlug"] as? String ?: "",
            profileDisplayName = map["profileDisplayName"] as? String ?: "",
            profileAvatar = map["profileAvatar"] as? String ?: "",
            savedAt = map["savedAt"] as? Timestamp
        )
    }
}
