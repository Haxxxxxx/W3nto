package com.w3nto.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GradientConfig(
    val from: String = "#6c5ce7",
    val to: String = "#a29bfe",
    val direction: String = "to-r"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "from" to from,
        "to" to to,
        "direction" to direction
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): GradientConfig = GradientConfig(
            from = map["from"] as? String ?: "#6c5ce7",
            to = map["to"] as? String ?: "#a29bfe",
            direction = map["direction"] as? String ?: "to-r"
        )
    }
}

@Serializable
data class Block(
    val id: String = "",
    val type: BlockType = BlockType.LINK,
    val title: String = "",
    val content: String = "",
    val url: String? = null,
    val icon: String? = null,
    val iconImage: String? = null,
    val bgType: String = "color",
    val bgColor: String? = null,
    val bgGradient: GradientConfig? = null,
    val bgImage: String? = null,
    val textColor: String? = null,
    val width: Int = 1,
    val height: Int = 1,
    val position: Int = 0,
    val tokenAddress: String? = null,
    val chainId: String? = null,
    val nftCollection: String? = null,
    val nftTokenId: String? = null,
    val nftName: String? = null,
    val nftImage: String? = null,
    val socialPlatform: SocialPlatform? = null,
    val imageUrl: String? = null
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("type", type.name.lowercase())
        put("title", title)
        put("content", content)
        put("url", url)
        put("icon", icon)
        put("iconImage", iconImage)
        put("bgType", bgType)
        put("bgColor", bgColor)
        put("bgGradient", bgGradient?.toMap())
        put("bgImage", bgImage)
        put("textColor", textColor)
        put("width", width)
        put("height", height)
        put("position", position)
        put("tokenAddress", tokenAddress)
        put("chainId", chainId)
        put("nftCollection", nftCollection)
        put("nftTokenId", nftTokenId)
        put("nftName", nftName)
        put("nftImage", nftImage)
        put("socialPlatform", socialPlatform?.name?.lowercase())
        put("imageUrl", imageUrl)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): Block = Block(
            id = id,
            type = BlockType.entries.find { it.name.equals(map["type"] as? String, ignoreCase = true) } ?: BlockType.LINK,
            title = map["title"] as? String ?: "",
            content = map["content"] as? String ?: "",
            url = map["url"] as? String,
            icon = map["icon"] as? String,
            iconImage = map["iconImage"] as? String,
            bgType = map["bgType"] as? String ?: "color",
            bgColor = map["bgColor"] as? String,
            bgGradient = (map["bgGradient"] as? Map<String, Any?>)?.let { GradientConfig.fromMap(it) },
            bgImage = map["bgImage"] as? String,
            textColor = map["textColor"] as? String,
            width = (map["width"] as? Number)?.toInt() ?: 1,
            height = (map["height"] as? Number)?.toInt() ?: 1,
            position = (map["position"] as? Number)?.toInt() ?: 0,
            tokenAddress = map["tokenAddress"] as? String,
            chainId = map["chainId"] as? String,
            nftCollection = map["nftCollection"] as? String,
            nftTokenId = map["nftTokenId"] as? String,
            nftName = map["nftName"] as? String,
            nftImage = map["nftImage"] as? String,
            socialPlatform = SocialPlatform.entries.find { it.name.equals(map["socialPlatform"] as? String, ignoreCase = true) },
            imageUrl = map["imageUrl"] as? String
        )
    }
}
