import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import nacl from "tweetnacl";
import bs58 from "bs58";

if (!admin.apps.length) {
  admin.initializeApp();
}

interface VerifyWalletRequest {
  publicKey: string;
  signature: string;
  message: string;
}

/**
 * Cloud Function: verifyWalletSignature
 *
 * Receives a Solana wallet public key + signed message,
 * verifies the signature using ed25519, and returns
 * a Firebase custom auth token with UID = walletAddress.
 */
export const verifyWalletSignature = functions.https.onCall(
  async (data: VerifyWalletRequest) => {
    const { publicKey, signature, message } = data;

    if (!publicKey || !signature || !message) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Missing publicKey, signature, or message"
      );
    }

    // Decode from base58
    let publicKeyBytes: Uint8Array;
    let signatureBytes: Uint8Array;
    try {
      publicKeyBytes = bs58.decode(publicKey);
      signatureBytes = bs58.decode(signature);
    } catch {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Invalid base58 encoding for publicKey or signature"
      );
    }

    // Verify the ed25519 signature
    const messageBytes = new TextEncoder().encode(message);
    const isValid = nacl.sign.detached.verify(
      messageBytes,
      signatureBytes,
      publicKeyBytes
    );

    if (!isValid) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "Invalid wallet signature"
      );
    }

    // Verify message freshness (within 5 minutes)
    const timestampMatch = message.match(/Timestamp:\s*(\d+)/);
    if (!timestampMatch) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Message must include a timestamp"
      );
    }
    {
      const timestamp = parseInt(timestampMatch[1], 10);
      const now = Date.now();
      const fiveMinutes = 5 * 60 * 1000;
      if (Math.abs(now - timestamp) > fiveMinutes) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Signature expired"
        );
      }
    }

    // Create or get the Firebase custom token
    // UID is the wallet public key (base58)
    const customToken = await admin.auth().createCustomToken(publicKey);

    return { token: customToken };
  }
);
