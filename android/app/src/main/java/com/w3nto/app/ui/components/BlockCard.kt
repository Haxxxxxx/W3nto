package com.w3nto.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.BlockType
import com.w3nto.app.data.model.Theme
import com.w3nto.app.ui.components.blocks.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlockCard(
    block: Block,
    theme: Theme,
    isEditMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val cornerRadius = theme.borderRadius.dp
    val bgColor = parseColor(block.bgColor ?: theme.cardBackground)
    val shape = RoundedCornerShape(cornerRadius)

    val bgModifier = when (block.bgType) {
        "gradient" -> {
            val gradient = block.bgGradient
            if (gradient != null) {
                Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            parseColor(gradient.from),
                            parseColor(gradient.to)
                        )
                    ),
                    shape = shape
                )
            } else {
                Modifier.background(bgColor, shape)
            }
        }
        else -> Modifier.background(bgColor, shape)
    }

    val height = (block.height * 80).dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (isEditMode) onLongClick else null
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .padding(12.dp)
        ) {
            val textColor = parseColor(block.textColor ?: theme.textColor)

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
