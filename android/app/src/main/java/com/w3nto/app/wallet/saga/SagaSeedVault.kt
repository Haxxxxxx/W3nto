package com.w3nto.app.wallet.saga

import android.net.Uri
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.RpcCluster
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.AdapterOperations
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient.AuthorizationResult
import com.w3nto.app.wallet.phantom.PhantomWallet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SagaSeedVault @Inject constructor(
    private val phantomWallet: PhantomWallet
) {

    companion object {
        private const val APP_IDENTITY_NAME = "W3nto"
        private val APP_IDENTITY_URI = Uri.parse("https://w3nto.app")
        private val APP_IDENTITY_ICON = Uri.parse("https://w3nto.app/favicon.ico")
    }

    private val connectionIdentity = ConnectionIdentity(
        identityUri = APP_IDENTITY_URI,
        iconUri = APP_IDENTITY_ICON,
        identityName = APP_IDENTITY_NAME
    )

    private val mwa = MobileWalletAdapter(connectionIdentity)

    /**
     * Authorize with the Saga Seed Vault via MWA.
     * Returns the base58-encoded public key.
     */
    suspend fun authorize(sender: ActivityResultSender): String {
        mwa.rpcCluster = RpcCluster.Devnet

        val block: suspend (AdapterOperations, AuthorizationResult) -> ByteArray =
            { _, authResult -> authResult.publicKey }

        val result = mwa.transact(sender, null, block)

        return when (result) {
            is TransactionResult.Success -> phantomWallet.base58Encode(result.payload)
            is TransactionResult.NoWalletFound -> throw IllegalStateException("No MWA-compatible wallet found")
            is TransactionResult.Failure -> throw IllegalStateException("MWA authorization failed: ${result.e.message}")
        }
    }

    /**
     * Sign a message with the authorized wallet via MWA.
     * Returns the base58-encoded signature.
     */
    suspend fun signMessage(sender: ActivityResultSender, message: ByteArray): String {
        mwa.rpcCluster = RpcCluster.Devnet

        val block: suspend (AdapterOperations, AuthorizationResult) -> ByteArray =
            { ops, authResult ->
                val signResult = ops.signMessages(
                    arrayOf(message),
                    arrayOf(authResult.publicKey)
                )
                signResult.signedPayloads[0]
            }

        val result = mwa.transact(sender, null, block)

        return when (result) {
            is TransactionResult.Success -> phantomWallet.base58Encode(result.payload)
            is TransactionResult.NoWalletFound -> throw IllegalStateException("No MWA-compatible wallet found")
            is TransactionResult.Failure -> throw IllegalStateException("MWA sign failed: ${result.e.message}")
        }
    }
}
