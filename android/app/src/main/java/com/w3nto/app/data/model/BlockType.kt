package com.w3nto.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BlockType {
    @SerialName("link") LINK,
    @SerialName("social") SOCIAL,
    @SerialName("text") TEXT,
    @SerialName("image") IMAGE,
    @SerialName("video") VIDEO,
    @SerialName("nft") NFT,
    @SerialName("token") TOKEN,
    @SerialName("chart") CHART,
    @SerialName("music") MUSIC;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
