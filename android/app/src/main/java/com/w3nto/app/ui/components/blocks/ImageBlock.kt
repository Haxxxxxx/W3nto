package com.w3nto.app.ui.components.blocks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.w3nto.app.data.model.Block

@Composable
fun ImageBlock(block: Block) {
    val imageUrl = block.imageUrl ?: block.bgImage ?: block.url ?: block.content
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = block.title,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}
