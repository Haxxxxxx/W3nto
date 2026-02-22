package com.w3nto.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.w3nto.app.ui.theme.DarkBackground
import com.w3nto.app.ui.theme.DarkSurfaceVariant
import com.w3nto.app.ui.theme.Error
import com.w3nto.app.ui.theme.Purple300
import com.w3nto.app.ui.theme.Purple500
import com.w3nto.app.wallet.WalletManager
import kotlinx.coroutines.launch

@Composable
fun ConnectWalletScreen(
    walletManager: WalletManager,
    activityResultSender: ActivityResultSender,
    onConnected: () -> Unit
) {
    val isConnecting by walletManager.isConnecting.collectAsState()
    val error by walletManager.error.collectAsState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurfaceVariant, DarkBackground)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Connect Your Wallet",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose your preferred wallet to get started",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // On Seeker: show Seed Vault first as primary
            if (walletManager.isSolanaDevice) {
                // Seed Vault — primary on Seeker
                Button(
                    onClick = { scope.launch { walletManager.connectSaga(activityResultSender) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isConnecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple500
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Seed Vault",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Recommended for Seeker",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Phantom — secondary on Seeker
                OutlinedButton(
                    onClick = { walletManager.connectPhantom() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isConnecting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Purple300
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Connect with Phantom",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            } else {
                // Non-Seeker: Phantom is primary
                Button(
                    onClick = { walletManager.connectPhantom() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isConnecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple500
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Connect with Phantom",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Loading
            if (isConnecting) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Purple500
                )
            }

            // Error
            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Error.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error ?: "",
                        color = Error,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
