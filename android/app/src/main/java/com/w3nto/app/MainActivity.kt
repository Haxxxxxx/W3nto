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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var authRepository: AuthRepository

    private val scope = MainScope()
    private lateinit var activityResultSender: ActivityResultSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must register before STARTED state
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
                        activityResultSender = activityResultSender
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
        if (uri.scheme != "w3nto") return

        when (uri.host) {
            "onConnect" -> walletManager.handlePhantomCallback(uri)
            "onSignMessage" -> scope.launch {
                walletManager.handlePhantomSignMessageCallback(uri)
            }
        }
    }
}
