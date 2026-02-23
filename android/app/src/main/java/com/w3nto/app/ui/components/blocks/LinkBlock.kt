package com.w3nto.app.ui.components.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.net.URL

private fun extractDomain(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return try {
        val parsed = URL(url)
        parsed.host.removePrefix("www.")
    } catch (_: Exception) {
        null
    }
}

private fun faviconUrl(domain: String): String {
    return "https://www.google.com/s2/favicons?domain=$domain&sz=64"
}

@Composable
fun LinkBlock(block: Block, textColor: Color) {
    val domain = extractDomain(block.url)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon display: iconImage > icon emoji > favicon from URL > no icon
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
                Spacer(modifier = Modifier.height(8.dp))
            }
            !block.icon.isNullOrEmpty() -> {
                Text(text = block.icon, fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            domain != null -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(faviconUrl(domain))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Text(
            text = block.title.ifEmpty { "Link" },
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (block.content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = block.content,
                color = textColor.copy(alpha = 0.75f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else if (domain != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = domain,
                color = textColor.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
