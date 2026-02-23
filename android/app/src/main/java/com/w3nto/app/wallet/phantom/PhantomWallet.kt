package com.w3nto.app.wallet.phantom

import android.net.Uri
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.Box
import com.goterl.lazysodium.utils.KeyPair
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhantomWallet @Inject constructor() {

    companion object {
        private const val PHANTOM_CONNECT_URL = "https://phantom.app/ul/v1/connect"
        private const val PHANTOM_SIGN_MESSAGE_URL = "https://phantom.app/ul/v1/signMessage"
        private const val APP_URL = "https://w3nto.app"
        private const val REDIRECT_SCHEME = "w3nto"
        private const val REDIRECT_HOST_CONNECT = "onConnect"
        private const val REDIRECT_HOST_SIGN = "onSignMessage"
    }

    private val lazySodium = LazySodiumAndroid(SodiumAndroid())
    private var sessionKeyPair: KeyPair? = null
    private var sharedSecret: ByteArray? = null

    /** X25519 keypair generated fresh each session */
    fun getOrCreateSessionKeyPair(): KeyPair {
        return sessionKeyPair ?: lazySodium.cryptoBoxKeypair().also {
            sessionKeyPair = it
        }
    }

    fun buildConnectUri(): Uri {
        val kp = getOrCreateSessionKeyPair()
        val dappPubKeyBase58 = base58Encode(kp.publicKey.asBytes)

        return Uri.parse(PHANTOM_CONNECT_URL).buildUpon()
            .appendQueryParameter("app_url", APP_URL)
            .appendQueryParameter("dapp_encryption_public_key", dappPubKeyBase58)
            .appendQueryParameter("redirect_link", "$REDIRECT_SCHEME://$REDIRECT_HOST_CONNECT")
            .appendQueryParameter("cluster", "mainnet-beta")
            .build()
    }

    fun buildSignMessageUri(
        message: ByteArray,
        session: String,
        phantomEncryptionPubKey: String
    ): Uri {
        val kp = getOrCreateSessionKeyPair()
        val secret = getOrComputeSharedSecret(phantomEncryptionPubKey)

        // Encrypt the payload: { message: base58(message), session: session }
        val payload = """{"message":"${base58Encode(message)}","session":"$session"}"""
        val nonce = lazySodium.randomBytesBuf(Box.NONCEBYTES)

        val encrypted = encryptPayload(payload.toByteArray(Charsets.UTF_8), nonce, secret)

        return Uri.parse(PHANTOM_SIGN_MESSAGE_URL).buildUpon()
            .appendQueryParameter("dapp_encryption_public_key", base58Encode(kp.publicKey.asBytes))
            .appendQueryParameter("nonce", base58Encode(nonce))
            .appendQueryParameter("redirect_link", "$REDIRECT_SCHEME://$REDIRECT_HOST_SIGN")
            .appendQueryParameter("payload", base58Encode(encrypted))
            .build()
    }

    fun decryptPayload(data: String, nonce: String, phantomEncryptionPubKey: String): String {
        val secret = getOrComputeSharedSecret(phantomEncryptionPubKey)
        val dataBytes = base58Decode(data)
        val nonceBytes = base58Decode(nonce)

        val decrypted = ByteArray(dataBytes.size - Box.MACBYTES)
        val success = lazySodium.cryptoBoxOpenEasyAfterNm(
            decrypted,
            dataBytes,
            dataBytes.size.toLong(),
            nonceBytes,
            secret
        )
        if (!success) throw IllegalStateException("Failed to decrypt Phantom payload")
        return String(decrypted, Charsets.UTF_8)
    }

    private fun getOrComputeSharedSecret(phantomEncryptionPubKey: String): ByteArray {
        return sharedSecret ?: run {
            val kp = getOrCreateSessionKeyPair()
            val phantomPubBytes = base58Decode(phantomEncryptionPubKey)
            val secret = ByteArray(Box.BEFORENMBYTES)
            lazySodium.cryptoBoxBeforeNm(
                secret,
                phantomPubBytes,
                kp.secretKey.asBytes
            )
            sharedSecret = secret
            secret
        }
    }

    private fun encryptPayload(data: ByteArray, nonce: ByteArray, secret: ByteArray): ByteArray {
        val encrypted = ByteArray(data.size + Box.MACBYTES)
        lazySodium.cryptoBoxEasyAfterNm(
            encrypted,
            data,
            data.size.toLong(),
            nonce,
            secret
        )
        return encrypted
    }

    fun clearSession() {
        sessionKeyPair = null
        sharedSecret = null
    }

    // Base58 encode/decode using Bitcoin's alphabet
    private val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun base58Encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var bi = java.math.BigInteger(1, input)
        val sb = StringBuilder()
        val base = java.math.BigInteger.valueOf(58)
        while (bi > java.math.BigInteger.ZERO) {
            val divRem = bi.divideAndRemainder(base)
            bi = divRem[0]
            sb.append(BASE58_ALPHABET[divRem[1].toInt()])
        }
        // Preserve leading zeros
        for (b in input) {
            if (b.toInt() == 0) sb.append('1') else break
        }
        return sb.reverse().toString()
    }

    fun base58Decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        var bi = java.math.BigInteger.ZERO
        val base = java.math.BigInteger.valueOf(58)
        for (c in input) {
            val index = BASE58_ALPHABET.indexOf(c)
            if (index < 0) throw IllegalArgumentException("Invalid Base58 character: $c")
            bi = bi.multiply(base).add(java.math.BigInteger.valueOf(index.toLong()))
        }
        val bytes = bi.toByteArray()
        // Remove leading zero byte if present (from BigInteger sign bit)
        val stripped = if (bytes.size > 1 && bytes[0].toInt() == 0) bytes.copyOfRange(1, bytes.size) else bytes
        // Count leading '1's
        val leadingZeros = input.takeWhile { it == '1' }.length
        return ByteArray(leadingZeros) + stripped
    }
}
