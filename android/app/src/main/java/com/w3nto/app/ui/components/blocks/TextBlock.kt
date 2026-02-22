package com.w3nto.app.ui.components.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.w3nto.app.data.model.Block

@Composable
fun TextBlock(block: Block, textColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        if (block.title.isNotEmpty()) {
            Text(
                text = block.title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = block.content,
            color = textColor.copy(alpha = 0.85f),
            fontSize = 13.sp,
            overflow = TextOverflow.Ellipsis
        )
    }
}
