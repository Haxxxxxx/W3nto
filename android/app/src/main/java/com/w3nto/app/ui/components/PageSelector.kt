package com.w3nto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.w3nto.app.data.model.BentoPage
import com.w3nto.app.data.model.Theme

@Composable
fun PageSelector(
    pages: List<BentoPage>,
    selectedPageId: String,
    onPageSelected: (BentoPage) -> Unit,
    theme: Theme = Theme(),
    modifier: Modifier = Modifier
) {
    val textColor = remember(theme.textColor) { parseColor(theme.textColor) }
    val cardBg = remember(theme.cardBackground) { parseColor(theme.cardBackground) }
    val buttonBg = remember(theme.buttonBackground) { parseColor(theme.buttonBackground) }
    val buttonText = remember(theme.buttonText) { parseColor(theme.buttonText) }
    val buttonRadius = if (theme.buttonStyle == "rounded") 20.dp else theme.borderRadius.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pages.forEach { page ->
            val isSelected = page.id == selectedPageId
            val bg = if (isSelected) buttonBg else cardBg
            val fg = if (isSelected) buttonText else textColor.copy(alpha = 0.7f)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(buttonRadius))
                    .background(bg)
                    .clickable { onPageSelected(page) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = page.name,
                    color = fg,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
