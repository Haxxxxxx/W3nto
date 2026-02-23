package com.w3nto.app.ui.screens.myprofile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.w3nto.app.R
import coil.compose.AsyncImage
import com.w3nto.app.data.model.BentoPage
import com.w3nto.app.ui.components.BentoGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onViewProfile: (username: String) -> Unit,
    viewModel: MyProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.error?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    uiState.successMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_profile_title)) },
                actions = {
                    if (uiState.profile != null && !uiState.showCreateProfile) {
                        if (uiState.isEditing) {
                            IconButton(onClick = { viewModel.cancelEditing() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(end = 8.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { viewModel.saveProfile() }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save")
                                }
                            }
                        } else {
                            IconButton(onClick = { viewModel.startEditing() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.requiresAuth -> {
                    AuthRequiredState()
                }

                uiState.showCreateProfile -> {
                    CreateProfileForm(
                        username = uiState.createUsername,
                        isChecking = uiState.isCheckingUsername,
                        isCreating = uiState.isCreatingProfile,
                        error = uiState.usernameError,
                        onUsernameChange = viewModel::updateCreateUsername,
                        onCreate = viewModel::createProfile
                    )
                }

                else -> {
                    ProfileContent(
                        uiState = uiState,
                        onViewProfile = onViewProfile,
                        onSelectPage = viewModel::selectPage,
                        onShowCreatePage = viewModel::showCreatePageDialog,
                        onDisplayNameChange = viewModel::updateDisplayName,
                        onBioChange = viewModel::updateBio,
                        onShowWalletChange = viewModel::updateShowWallet
                    )
                }
            }
        }
    }

    if (uiState.showCreatePage) {
        CreatePageDialog(
            pageName = uiState.newPageName,
            onNameChange = viewModel::updateNewPageName,
            onCreate = viewModel::createPage,
            onDismiss = viewModel::hideCreatePageDialog
        )
    }
}

@Composable
private fun AuthRequiredState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connect your wallet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign in to view and edit your profile",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateProfileForm(
    username: String,
    isChecking: Boolean,
    isCreating: Boolean,
    error: String?,
    onUsernameChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Create Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose a unique username for your W3nto profile",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.label_username)) },
            placeholder = { Text(stringResource(R.string.placeholder_username)) },
            singleLine = true,
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                } else {
                    Text(stringResource(R.string.hint_username))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreate,
            enabled = username.isNotEmpty() && !isChecking && !isCreating,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isChecking || isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isChecking) stringResource(R.string.status_checking) else stringResource(R.string.status_creating))
            } else {
                Text(stringResource(R.string.action_create_profile))
            }
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: MyProfileUiState,
    onViewProfile: (String) -> Unit,
    onSelectPage: (BentoPage) -> Unit,
    onShowCreatePage: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onShowWalletChange: (Boolean) -> Unit
) {
    val profile = uiState.profile ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Avatar + info card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        if (profile.avatar.isNotEmpty()) {
                            AsyncImage(
                                model = profile.avatar,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (profile.displayName.firstOrNull()
                                        ?: profile.username.firstOrNull() ?: '?')
                                        .uppercaseChar()
                                        .toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "@${profile.username}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = profile.displayName.ifEmpty { profile.username },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (uiState.isEditing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.editDisplayName,
                            onValueChange = onDisplayNameChange,
                            label = { Text(stringResource(R.string.label_display_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.editBio,
                            onValueChange = onBioChange,
                            label = { Text(stringResource(R.string.label_bio)) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Show wallet address",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = uiState.editShowWallet,
                                onCheckedChange = onShowWalletChange
                            )
                        }
                    } else {
                        if (profile.bio.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = profile.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onViewProfile(profile.username) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.action_view_public_profile))
                        }
                    }
                }
            }
        }

        // Pages section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onShowCreatePage) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_new_page))
                }
            }
        }

        if (uiState.pages.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.pages, key = { it.id }) { page ->
                        val selected = uiState.selectedPage?.id == page.id
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectPage(page) },
                            label = { Text(page.name) }
                        )
                    }
                }
            }

            // Animated blocks grid — slides in/out when page changes
            item {
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedContent(
                    targetState = uiState.selectedPage?.id to uiState.blocks,
                    transitionSpec = {
                        (slideInHorizontally { width -> width / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width / 4 } + fadeOut())
                    },
                    label = "page_blocks_transition"
                ) { (_, blocks) ->
                    if (blocks.isNotEmpty()) {
                        BentoGrid(
                            blocks = blocks,
                            theme = uiState.selectedPage?.theme ?: uiState.profile?.theme
                                ?: com.w3nto.app.data.model.Theme(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No blocks on this page yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pages yet — tap \"New Page\" to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePageDialog(
    pageName: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_new_page)) },
        text = {
            OutlinedTextField(
                value = pageName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.label_page_name)) },
                placeholder = { Text(stringResource(R.string.placeholder_page_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = pageName.isNotBlank()
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
