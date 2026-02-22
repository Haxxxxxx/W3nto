package com.w3nto.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.w3nto.app.data.repository.AuthRepository
import com.w3nto.app.ui.navigation.NavGraph
import com.w3nto.app.ui.theme.W3ntoTheme
import com.w3nto.app.wallet.WalletManager
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var activityResultSender: ActivityResultSender

    private val _pendingDeepLinkRoute = MutableStateFlow<String?>(null)
    val pendingDeepLinkRoute = _pendingDeepLinkRoute.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityResultSender = ActivityResultSender(this)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            W3ntoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        walletManager = walletManager,
                        authRepository = authRepository,
                        activityResultSender = activityResultSender,
                        pendingDeepLinkRoute = pendingDeepLinkRoute,
                        onDeepLinkConsumed = { consumeDeepLinkRoute() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return

        when (uri.scheme) {
            "w3nto" -> {
                // Phantom wallet callbacks
                when (uri.host) {
                    "onConnect" -> walletManager.handlePhantomCallback(uri)
                    "onSignMessage" -> lifecycleScope.launch {
                        walletManager.handlePhantomSignMessageCallback(uri)
                    }
                }
            }
            "https", "http" -> {
                // HTTPS deep links: w3nto.app/{username} or w3nto.app/{username}/{pageSlug}
                val pathSegments = uri.pathSegments ?: return
                if (pathSegments.isEmpty()) return
                val username = pathSegments[0]
                val pageSlug = pathSegments.getOrNull(1)
                val route = if (pageSlug != null) {
                    "profile/$username?page=$pageSlug"
                } else {
                    "profile/$username"
                }
                _pendingDeepLinkRoute.value = route
            }
        }
    }

    fun consumeDeepLinkRoute() {
        _pendingDeepLinkRoute.value = null
    }
}
