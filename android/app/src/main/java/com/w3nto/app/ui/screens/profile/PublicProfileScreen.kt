package com.w3nto.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.w3nto.app.data.model.Theme
import com.w3nto.app.ui.components.BentoGrid
import com.w3nto.app.ui.components.PageSelector
import com.w3nto.app.ui.components.ProfileHeader
import com.w3nto.app.ui.components.parseColor
import com.w3nto.app.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    username: String,
    pageSlug: String?,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Use page theme if available, otherwise profile theme, otherwise default
    val activeTheme = uiState.currentPage?.theme ?: uiState.profile?.theme ?: Theme()
    val bgColor = remember(activeTheme.backgroundColor) { parseColor(activeTheme.backgroundColor) }
    val textColor = remember(activeTheme.textColor) { parseColor(activeTheme.textColor) }
    val primaryColor = remember(activeTheme.primaryColor) { parseColor(activeTheme.primaryColor) }
    val cardBg = remember(activeTheme.cardBackground) { parseColor(activeTheme.cardBackground) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("@$username", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.isFavorited) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarBorder
                            },
                            contentDescription = if (uiState.isFavorited) "Remove from favorites" else "Add to favorites",
                            tint = if (uiState.isFavorited) primaryColor else textColor
                        )
                    }
                    IconButton(onClick = {
                        val currentPage = uiState.currentPage
                        val shareUrl = if (currentPage != null) {
                            "https://w3nto.app/$username/${currentPage.slug}"
                        } else {
                            "https://w3nto.app/$username"
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareUrl)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share profile"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        when {
            uiState.isLoading -> {
                // Shimmer loading skeleton
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(bgColor),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    // Avatar skeleton
                    ShimmerBox(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        baseColor = cardBg
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Name skeleton
                    ShimmerBox(
                        modifier = Modifier
                            .width(180.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        baseColor = cardBg
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Bio skeleton
                    ShimmerBox(
                        modifier = Modifier
                            .width(240.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        baseColor = cardBg
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    // Grid skeleton
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(2) {
                            ShimmerBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                baseColor = cardBg
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            baseColor = cardBg
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            baseColor = cardBg
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            baseColor = cardBg
                        )
                    }
                }
            }

            uiState.error != null -> {
                // Network / load error — distinct from not-found
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(bgColor),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Error.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Error
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Connection error",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Couldn't load this profile. Check your connection and try again.",
                        fontSize = 15.sp,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.retryLoad() },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }

            uiState.notFound -> {
                // Branded "not found" matching web
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(bgColor),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(cardBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = textColor.copy(alpha = 0.4f)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Profile not found",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\"$username\" doesn't exist or hasn't been created yet.",
                        fontSize = 15.sp,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Go Back")
                    }
                }
            }

            uiState.profile != null -> {
                val profile = uiState.profile!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(bgColor)
                        .verticalScroll(rememberScrollState())
                ) {
                    ProfileHeader(
                        profile = profile,
                        theme = activeTheme
                    )

                    if (uiState.pages.size > 1) {
                        PageSelector(
                            pages = uiState.pages,
                            selectedPageId = uiState.currentPage?.id ?: "",
                            onPageSelected = { viewModel.selectPage(it) },
                            theme = activeTheme
                        )
                    }

                    if (uiState.blocks.isEmpty()) {
                        // Empty state for profile with no blocks
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = textColor.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No blocks yet",
                                    fontSize = 16.sp,
                                    color = textColor.copy(alpha = 0.4f)
                                )
                                Text(
                                    text = "This page is still being set up",
                                    fontSize = 13.sp,
                                    color = textColor.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else {
                        BentoGrid(
                            blocks = uiState.blocks,
                            theme = activeTheme,
                            modifier = Modifier.fillMaxWidth(),
                            onBlockClick = { block ->
                                val url = block.url
                                if (!url.isNullOrEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    baseColor: Color = Color.Gray
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.6f),
            baseColor.copy(alpha = 0.2f),
            baseColor.copy(alpha = 0.6f)
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(modifier = modifier.background(shimmerBrush))
}
