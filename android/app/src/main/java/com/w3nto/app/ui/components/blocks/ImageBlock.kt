package com.w3nto.app.ui.components.blocks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.w3nto.app.data.model.Block

@Composable
fun ImageBlock(block: Block) {
    val imageUrl = block.url ?: block.bgImage ?: block.content
    AsyncImage(
        model = imageUrl,
        contentDescription = block.title,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
}
