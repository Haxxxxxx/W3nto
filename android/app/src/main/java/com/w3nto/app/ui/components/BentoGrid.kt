package com.w3nto.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.Theme

@Composable
fun BentoGrid(
    blocks: List<Block>,
    theme: Theme,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onBlockClick: (Block) -> Unit = {},
    onBlockLongClick: (Block) -> Unit = {}
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(4),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(
            items = blocks.sortedBy { it.position },
            key = { it.id }
        ) { block ->
            BlockCard(
                block = block,
                theme = theme,
                isEditMode = isEditMode,
                onClick = { onBlockClick(block) },
                onLongClick = { onBlockLongClick(block) }
            )
        }
    }
}
