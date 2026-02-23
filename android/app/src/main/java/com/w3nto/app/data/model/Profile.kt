package com.w3nto.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Profile(
    val username: String = "",
    val walletAddress: String = "",
    val uid: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatar: String = "",
    @Transient val theme: Theme = Theme(),
    val defaultPageId: String = "",
    val showWallet: Boolean = true,
    @Transient val pages: List<BentoPage> = emptyList(),
    @Transient val createdAt: Timestamp? = null,
    @Transient val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("uid", uid)
        put("walletAddress", walletAddress)
        put("displayName", displayName)
        put("bio", bio)
        put("avatar", avatar)
        put("theme", theme.toMap())
        put("defaultPageId", defaultPageId)
        put("showWallet", showWallet)
        put("createdAt", createdAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(username: String, map: Map<String, Any?>): Profile = Profile(
            username = username,
            walletAddress = map["walletAddress"] as? String ?: "",
            uid = map["uid"] as? String ?: "",
            displayName = map["displayName"] as? String ?: "",
            bio = map["bio"] as? String ?: "",
            avatar = map["avatar"] as? String ?: "",
            theme = (map["theme"] as? Map<String, Any?>)?.let { Theme.fromMap(it) } ?: Theme(),
            defaultPageId = map["defaultPageId"] as? String ?: "",
            showWallet = map["showWallet"] as? Boolean ?: true,
            createdAt = map["createdAt"] as? Timestamp,
            updatedAt = map["updatedAt"] as? Timestamp
        )
    }
}
