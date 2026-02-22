package com.w3nto.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.w3nto.app.data.model.BentoPage

@Composable
fun PageSelector(
    pages: List<BentoPage>,
    selectedPageId: String,
    onPageSelected: (BentoPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pages.forEach { page ->
            val isSelected = page.id == selectedPageId
            FilterChip(
                selected = isSelected,
                onClick = { onPageSelected(page) },
                label = { Text(page.name) },
                leadingIcon = if (page.isFavorite) {
                    {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}
