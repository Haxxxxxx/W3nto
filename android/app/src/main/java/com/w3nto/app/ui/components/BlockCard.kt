package com.w3nto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.BlockType
import com.w3nto.app.data.model.Theme
import com.w3nto.app.ui.components.blocks.*

@Composable
fun BlockCard(
    block: Block,
    theme: Theme,
    onClick: () -> Unit = {}
) {
    val cornerRadius = theme.borderRadius.dp
    val bgColor = parseColor(block.bgColor ?: theme.cardBackground)
    val shape = RoundedCornerShape(cornerRadius)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Layer 1: Background (solid color, gradient, or image)
            when (block.bgType) {
                "gradient" -> {
                    val gradient = block.bgGradient
                    if (gradient != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            parseColor(gradient.from),
                                            parseColor(gradient.to)
                                        )
                                    )
                                )
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(bgColor))
                    }
                }
                "image" -> {
                    if (!block.bgImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(block.bgImage)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Scrim overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(bgColor))
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize().background(bgColor))
                }
            }

            // Layer 2: Content
            val textColor = parseColor(block.textColor ?: theme.textColor)
            val contentPadding = when (block.type) {
                BlockType.IMAGE, BlockType.NFT -> 0.dp
                else -> 12.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                when (block.type) {
                    BlockType.LINK -> LinkBlock(block = block, textColor = textColor)
                    BlockType.SOCIAL -> SocialBlock(block = block, textColor = textColor)
                    BlockType.TEXT -> TextBlock(block = block, textColor = textColor)
                    BlockType.IMAGE -> ImageBlock(block = block)
                    BlockType.VIDEO -> VideoBlock(block = block, textColor = textColor)
                    BlockType.NFT -> NFTBlock(block = block, textColor = textColor)
                    BlockType.TOKEN -> TokenBlock(block = block, textColor = textColor)
                    BlockType.CHART -> ChartBlock(block = block, textColor = textColor)
                    BlockType.MUSIC -> MusicBlock(block = block, textColor = textColor)
                }
            }
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorLong = cleanHex.toLong(16)
        when (cleanHex.length) {
            6 -> Color(0xFF000000 or colorLong)
            8 -> Color(colorLong)
            else -> Color.Gray
        }
    } catch (_: Exception) {
        Color.Gray
    }
}
