package com.w3nto.app.ui.components.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.w3nto.app.data.model.Block

private data class MusicPlatformInfo(val name: String, val color: Color)

private fun detectMusicPlatform(url: String?): MusicPlatformInfo? {
    if (url.isNullOrBlank()) return null
    return when {
        "spotify.com" in url -> MusicPlatformInfo("Spotify", Color(0xFF1DB954))
        "soundcloud.com" in url -> MusicPlatformInfo("SoundCloud", Color(0xFFFF5500))
        "music.apple.com" in url || "itunes.apple.com" in url ->
            MusicPlatformInfo("Apple Music", Color(0xFFFC3C44))
        "music.youtube.com" in url -> MusicPlatformInfo("YouTube Music", Color(0xFFFF0000))
        "tidal.com" in url -> MusicPlatformInfo("Tidal", Color(0xFF04EDFF))
        "deezer.com" in url -> MusicPlatformInfo("Deezer", Color(0xFFEF5466))
        else -> null
    }
}

@Composable
fun MusicBlock(block: Block, textColor: Color) {
    val platform = detectMusicPlatform(block.url)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            platform != null -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = platform.color
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = platform.name,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            else -> {
                Text(text = "\uD83C\uDFB5", fontSize = 36.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = block.title.ifEmpty { platform?.name ?: "Music" },
            color = textColor,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (platform != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = platform.name,
                color = platform.color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        } else if (block.content.isNotEmpty()) {
            Text(
                text = block.content,
                color = textColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
