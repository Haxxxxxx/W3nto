package com.w3nto.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BentoPage(
    val id: String = "",
    val name: String = "Main",
    val slug: String = "main",
    @Transient val theme: Theme? = null,
    val isFavorite: Boolean = false,
    val position: Int? = null,
    @Transient val blocks: List<Block> = emptyList(),
    @Transient val createdAt: Timestamp? = null,
    @Transient val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("name", name)
        put("slug", slug)
        put("theme", theme?.toMap())
        put("isFavorite", isFavorite)
        put("position", position)
        put("createdAt", createdAt ?: FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): BentoPage = BentoPage(
            id = id,
            name = map["name"] as? String ?: "Main",
            slug = map["slug"] as? String ?: "main",
            theme = (map["theme"] as? Map<String, Any?>)?.let { Theme.fromMap(it) },
            isFavorite = map["isFavorite"] as? Boolean ?: false,
            position = (map["position"] as? Number)?.toInt(),
            createdAt = map["createdAt"] as? Timestamp,
            updatedAt = map["updatedAt"] as? Timestamp
        )
    }
}
