package com.w3nto.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.w3nto.app.data.model.Block
import com.w3nto.app.data.model.Theme

private const val GRID_COLUMNS = 4
private val ROW_HEIGHT = 120.dp
private val GAP = 8.dp

@Composable
fun BentoGrid(
    blocks: List<Block>,
    theme: Theme,
    modifier: Modifier = Modifier,
    onBlockClick: (Block) -> Unit = {}
) {
    val sortedBlocks = remember(blocks) { blocks.sortedBy { it.position } }
    val blockSpans = remember(sortedBlocks) {
        sortedBlocks.map { block ->
            block.width.coerceIn(1, GRID_COLUMNS) to block.height.coerceIn(1, 4)
        }
    }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { ROW_HEIGHT.roundToPx() }
    val gapPx = with(density) { GAP.roundToPx() }

    Layout(
        modifier = modifier
            .padding(horizontal = 8.dp),
        content = {
            sortedBlocks.forEach { block ->
                BlockCard(
                    block = block,
                    theme = theme,
                    onClick = { onBlockClick(block) }
                )
            }
        }
    ) { measurables, constraints ->
        val availableWidth = constraints.maxWidth
        if (availableWidth <= 0) {
            return@Layout layout(0, 0) {}
        }
        val colWidth = (availableWidth - (GRID_COLUMNS - 1) * gapPx) / GRID_COLUMNS

        // Track occupied cells: grid[row][col] = true if occupied
        val occupied = mutableListOf<BooleanArray>()
        fun ensureRows(count: Int) {
            while (occupied.size < count) {
                occupied.add(BooleanArray(GRID_COLUMNS))
            }
        }

        data class Placement(val col: Int, val row: Int, val spanW: Int, val spanH: Int)

        val placements = mutableListOf<Placement>()

        // Place each block using first-fit algorithm (like CSS Grid auto-placement)
        for (i in measurables.indices) {
            val (spanW, spanH) = blockSpans[i]

            var placed = false
            var searchRow = 0

            while (!placed) {
                ensureRows(searchRow + spanH)
                for (col in 0..GRID_COLUMNS - spanW) {
                    // Check if all cells in this span are free
                    var fits = true
                    for (r in searchRow until searchRow + spanH) {
                        for (c in col until col + spanW) {
                            if (occupied[r][c]) {
                                fits = false
                                break
                            }
                        }
                        if (!fits) break
                    }
                    if (fits) {
                        // Mark cells as occupied
                        for (r in searchRow until searchRow + spanH) {
                            for (c in col until col + spanW) {
                                occupied[r][c] = true
                            }
                        }
                        placements.add(Placement(col, searchRow, spanW, spanH))
                        placed = true
                        break
                    }
                }
                if (!placed) searchRow++
            }
        }

        // Measure each child with its computed size
        val placeables = measurables.mapIndexed { index, measurable ->
            val p = placements[index]
            val childWidth = p.spanW * colWidth + (p.spanW - 1) * gapPx
            val childHeight = p.spanH * rowHeightPx + (p.spanH - 1) * gapPx
            measurable.measure(
                Constraints.fixed(childWidth.coerceAtLeast(0), childHeight.coerceAtLeast(0))
            )
        }

        // Calculate total height
        val totalRows = occupied.size
        val totalHeight = if (totalRows > 0) {
            totalRows * rowHeightPx + (totalRows - 1) * gapPx
        } else 0

        layout(availableWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val p = placements[index]
                val x = p.col * (colWidth + gapPx)
                val y = p.row * (rowHeightPx + gapPx)
                placeable.placeRelative(x, y)
            }
        }
    }
}
