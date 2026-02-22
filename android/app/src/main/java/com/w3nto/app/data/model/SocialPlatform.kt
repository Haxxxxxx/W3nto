package com.w3nto.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SocialPlatform(val displayName: String, val baseUrl: String) {
    @SerialName("twitter") TWITTER("Twitter / X", "https://x.com/"),
    @SerialName("instagram") INSTAGRAM("Instagram", "https://instagram.com/"),
    @SerialName("tiktok") TIKTOK("TikTok", "https://tiktok.com/@"),
    @SerialName("youtube") YOUTUBE("YouTube", "https://youtube.com/@"),
    @SerialName("linkedin") LINKEDIN("LinkedIn", "https://linkedin.com/in/"),
    @SerialName("github") GITHUB("GitHub", "https://github.com/"),
    @SerialName("discord") DISCORD("Discord", "https://discord.gg/"),
    @SerialName("telegram") TELEGRAM("Telegram", "https://t.me/"),
    @SerialName("twitch") TWITCH("Twitch", "https://twitch.tv/"),
    @SerialName("snapchat") SNAPCHAT("Snapchat", "https://snapchat.com/add/"),
    @SerialName("facebook") FACEBOOK("Facebook", "https://facebook.com/"),
    @SerialName("reddit") REDDIT("Reddit", "https://reddit.com/u/"),
    @SerialName("medium") MEDIUM("Medium", "https://medium.com/@"),
    @SerialName("behance") BEHANCE("Behance", "https://behance.net/"),
    @SerialName("dribbble") DRIBBBLE("Dribbble", "https://dribbble.com/"),
    @SerialName("spotify") SPOTIFY("Spotify", "https://open.spotify.com/user/"),
    @SerialName("soundcloud") SOUNDCLOUD("SoundCloud", "https://soundcloud.com/"),
    @SerialName("custom") CUSTOM("Custom", "");
}
