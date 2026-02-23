package com.w3nto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.w3nto.app.data.model.Profile
import com.w3nto.app.data.model.Theme

@Composable
fun ProfileHeader(
    profile: Profile,
    theme: Theme = Theme(),
    modifier: Modifier = Modifier
) {
    val textColor = remember(theme.textColor) { parseColor(theme.textColor) }
    val cardBg = remember(theme.cardBackground) { parseColor(theme.cardBackground) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        val avatarBorderColor = textColor.copy(alpha = 0.2f)
        if (profile.avatar.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profile.avatar)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(96.dp)
                    .shadow(8.dp, CircleShape)
                    .border(4.dp, avatarBorderColor, CircleShape)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(8.dp, CircleShape)
                    .border(4.dp, avatarBorderColor, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                remember(theme.primaryColor) { parseColor(theme.primaryColor) },
                                remember(theme.secondaryColor) { parseColor(theme.secondaryColor) }
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.displayName.firstOrNull()?.toString() ?: "?",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = remember(theme.buttonText) { parseColor(theme.buttonText) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display name
        Text(
            text = profile.displayName.ifEmpty { profile.username },
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        // Bio
        if (profile.bio.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.bio,
                fontSize = 16.sp,
                color = textColor.copy(alpha = 0.8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // .skr identity badge
        Box(
            modifier = Modifier
                .background(cardBg, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${profile.username}.skr",
                color = textColor,
                fontSize = 13.sp
            )
        }
    }
}
