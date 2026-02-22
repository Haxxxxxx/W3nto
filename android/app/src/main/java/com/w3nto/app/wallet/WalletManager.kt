package com.w3nto.app.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.w3nto.app.data.repository.AuthRepository
import com.w3nto.app.wallet.phantom.PhantomWallet
import com.w3nto.app.wallet.saga.SagaSeedVault
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectedWallet(
    val publicKey: String,
    val provider: WalletProvider
)

enum class WalletProvider {
    PHANTOM, SAGA_SEED_VAULT
}

@Singleton
class WalletManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phantomWallet: PhantomWallet,
    private val sagaSeedVault: SagaSeedVault,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    private val _connectedWallet = MutableStateFlow<ConnectedWallet?>(null)
    val connectedWallet: StateFlow<ConnectedWallet?> = _connectedWallet.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Temporary state for the two-step Phantom flow (connect → sign)
    private var pendingPublicKey: String? = null
    private var pendingPhantomEncryptionPubKey: String? = null
    private var pendingSession: String? = null
    private var pendingAuthMessage: String? = null

    val isSolanaDevice: Boolean
        get() {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val model = Build.MODEL.lowercase()
            val brand = Build.BRAND.lowercase()
            return manufacturer.contains("solana") || brand.contains("solana") ||
                model.contains("saga") || model.contains("seeker")
        }

    // --- Phantom Flow ---

    fun connectPhantom() {
        _isConnecting.value = true
        _error.value = null
        phantomWallet.clearSession()
        val connectUri = phantomWallet.buildConnectUri()
        val intent = Intent(Intent.ACTION_VIEW, connectUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun handlePhantomCallback(uri: Uri) {
        val errorMessage = uri.getQueryParameter("errorMessage")
        if (errorMessage != null) {
            _isConnecting.value = false
            _error.value = errorMessage
            return
        }

        val phantomEncPubKey = uri.getQueryParameter("phantom_encryption_public_key")
        val nonce = uri.getQueryParameter("nonce")
        val data = uri.getQueryParameter("data")

        if (phantomEncPubKey != null && nonce != null && data != null) {
            try {
                val decrypted = phantomWallet.decryptPayload(data, nonce, phantomEncPubKey)
                val json = JSONObject(decrypted)
                val publicKey = json.getString("public_key")
                val session = json.getString("session")

                pendingPublicKey = publicKey
                pendingPhantomEncryptionPubKey = phantomEncPubKey
                pendingSession = session

                val authMessage = generateAuthMessage(publicKey)
                pendingAuthMessage = authMessage
                val messageBytes = authMessage.toByteArray(Charsets.UTF_8)

                val signUri = phantomWallet.buildSignMessageUri(messageBytes, session, phantomEncPubKey)
                val intent = Intent(Intent.ACTION_VIEW, signUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                _isConnecting.value = false
                _error.value = "Failed to process Phantom connect response: ${e.message}"
                Log.e("WalletManager", "Phantom connect error", e)
            }
        } else {
            val publicKey = uri.getQueryParameter("public_key")
            if (publicKey != null) {
                _isConnecting.value = false
                _error.value = "Phantom version does not support encrypted session. Please update Phantom."
            } else {
                _isConnecting.value = false
                _error.value = "Failed to get public key from Phantom"
            }
        }
    }

    suspend fun handlePhantomSignMessageCallback(uri: Uri) {
        val errorMessage = uri.getQueryParameter("errorMessage")
        if (errorMessage != null) {
            _isConnecting.value = false
            _error.value = errorMessage
            return
        }

        val nonce = uri.getQueryParameter("nonce")
        val data = uri.getQueryParameter("data")
        val phantomEncPubKey = pendingPhantomEncryptionPubKey
        val publicKey = pendingPublicKey

        if (nonce == null || data == null || phantomEncPubKey == null || publicKey == null) {
            _isConnecting.value = false
            _error.value = "Missing sign message response data"
            return
        }

        try {
            val decrypted = phantomWallet.decryptPayload(data, nonce, phantomEncPubKey)
            val json = JSONObject(decrypted)
            val signature = json.getString("signature")
            val message = pendingAuthMessage ?: throw IllegalStateException("Missing auth message")

            authenticateWithFirebase(publicKey, signature, message, WalletProvider.PHANTOM)
        } catch (e: Exception) {
            _isConnecting.value = false
            _error.value = "Failed to process sign response: ${e.message}"
            Log.e("WalletManager", "Phantom sign error", e)
        } finally {
            pendingPublicKey = null
            pendingPhantomEncryptionPubKey = null
            pendingSession = null
            pendingAuthMessage = null
        }
    }

    // --- Saga MWA Flow ---

    suspend fun connectSaga(sender: ActivityResultSender) {
        _isConnecting.value = true
        _error.value = null
        try {
            val publicKey = sagaSeedVault.authorize(sender)
            val authMessage = generateAuthMessage(publicKey)
            val messageBytes = authMessage.toByteArray(Charsets.UTF_8)
            val signature = sagaSeedVault.signMessage(sender, messageBytes)

            authenticateWithFirebase(publicKey, signature, authMessage, WalletProvider.SAGA_SEED_VAULT)
        } catch (e: Exception) {
            _isConnecting.value = false
            _error.value = e.message ?: "Failed to connect Saga Seed Vault"
            Log.e("WalletManager", "Saga MWA error", e)
        }
    }

    // --- Firebase Auth Pipeline (Cloud Function custom token) ---

    private suspend fun authenticateWithFirebase(
        publicKey: String,
        signature: String,
        message: String,
        provider: WalletProvider
    ) {
        try {
            val data = hashMapOf(
                "publicKey" to publicKey,
                "signature" to signature,
                "message" to message
            )
            val result = functions
                .getHttpsCallable("verifyWalletSignature")
                .call(data)
                .await()

            val resultData = result.getData() as? Map<*, *>
                ?: throw IllegalStateException("Invalid response from verifyWalletSignature")
            val token = resultData["token"] as? String
                ?: throw IllegalStateException("Missing token in auth response")

            authRepository.signInWithCustomToken(token).getOrThrow()

            _connectedWallet.value = ConnectedWallet(
                publicKey = publicKey,
                provider = provider
            )
        } catch (e: Exception) {
            _error.value = "Firebase authentication failed: ${e.message}"
            Log.e("WalletManager", "Firebase auth error", e)
        } finally {
            _isConnecting.value = false
        }
    }

    fun disconnect() {
        authRepository.signOut()
        _connectedWallet.value = null
        phantomWallet.clearSession()
    }

    internal fun generateAuthMessage(publicKey: String): String {
        return "Sign in to W3nto with wallet: $publicKey\nTimestamp: ${System.currentTimeMillis()}"
    }

    fun clearError() {
        _error.value = null
    }
}
