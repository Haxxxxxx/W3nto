package com.w3nto.app.ui.components.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.SocialPlatform

@Composable
fun SocialBlock(block: Block, textColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon: iconImage > icon emoji > platform icon
        when {
            !block.iconImage.isNullOrEmpty() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(block.iconImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            !block.icon.isNullOrEmpty() -> {
                Text(text = block.icon, fontSize = 36.sp)
            }
            else -> {
                val brandColor = getSocialBrandColor(block.socialPlatform)
                Icon(
                    imageVector = getSocialIcon(block.socialPlatform),
                    contentDescription = block.socialPlatform?.displayName,
                    tint = brandColor ?: textColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = block.title.ifEmpty { block.socialPlatform?.displayName ?: "Social" },
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getSocialBrandColor(platform: SocialPlatform?): Color? = when (platform) {
    SocialPlatform.TWITTER -> Color(0xFF1DA1F2)
    SocialPlatform.INSTAGRAM -> Color(0xFFE1306C)
    SocialPlatform.TIKTOK -> Color(0xFF010101)
    SocialPlatform.YOUTUBE -> Color(0xFFFF0000)
    SocialPlatform.LINKEDIN -> Color(0xFF0A66C2)
    SocialPlatform.GITHUB -> Color(0xFF6E40C9)
    SocialPlatform.DISCORD -> Color(0xFF5865F2)
    SocialPlatform.TELEGRAM -> Color(0xFF2CA5E0)
    SocialPlatform.TWITCH -> Color(0xFF9146FF)
    SocialPlatform.SNAPCHAT -> Color(0xFFFFFC00)
    SocialPlatform.FACEBOOK -> Color(0xFF1877F2)
    SocialPlatform.REDDIT -> Color(0xFFFF4500)
    SocialPlatform.MEDIUM -> Color(0xFF02B875)
    SocialPlatform.SPOTIFY -> Color(0xFF1DB954)
    SocialPlatform.SOUNDCLOUD -> Color(0xFFFF5500)
    SocialPlatform.BEHANCE -> Color(0xFF1769FF)
    SocialPlatform.DRIBBBLE -> Color(0xFFEA4C89)
    SocialPlatform.CUSTOM, null -> null
}

private fun getSocialIcon(platform: SocialPlatform?): ImageVector = when (platform) {
    SocialPlatform.GITHUB -> Icons.Default.Code
    SocialPlatform.YOUTUBE -> Icons.Default.PlayCircle
    SocialPlatform.DISCORD -> Icons.Default.Chat
    SocialPlatform.TELEGRAM -> Icons.Default.Send
    SocialPlatform.REDDIT -> Icons.Default.Forum
    SocialPlatform.LINKEDIN -> Icons.Default.Work
    SocialPlatform.MEDIUM -> Icons.Default.Article
    SocialPlatform.TWITCH -> Icons.Default.Videocam
    SocialPlatform.SPOTIFY -> Icons.Default.MusicNote
    SocialPlatform.SOUNDCLOUD -> Icons.Default.GraphicEq
    SocialPlatform.FACEBOOK -> Icons.Default.ThumbUp
    SocialPlatform.SNAPCHAT -> Icons.Default.CameraAlt
    SocialPlatform.BEHANCE -> Icons.Default.Palette
    SocialPlatform.DRIBBBLE -> Icons.Default.SportsSoccer
    SocialPlatform.TWITTER -> Icons.Default.Tag
    SocialPlatform.INSTAGRAM -> Icons.Default.PhotoCamera
    SocialPlatform.TIKTOK -> Icons.Default.MusicVideo
    SocialPlatform.CUSTOM, null -> Icons.Default.Person
}
