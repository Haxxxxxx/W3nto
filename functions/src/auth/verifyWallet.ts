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
  async (data: VerifyWalletRequest, context) => {
    const { publicKey, signature, message } = data;

    if (!publicKey || !signature || !message) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Missing publicKey, signature, or message"
      );
    }

    // Rate limiting: max 10 auth attempts per IP per 5 minutes
    const ip = context.rawRequest?.ip || "unknown";
    const rateLimitRef = admin.firestore().collection("_rateLimits").doc(ip);
    const rateLimitDoc = await rateLimitRef.get();
    const now = Date.now();
    const windowMs = 5 * 60 * 1000;

    if (rateLimitDoc.exists) {
      const rateData = rateLimitDoc.data()!;
      if (now - rateData.windowStart < windowMs && rateData.count >= 10) {
        throw new functions.https.HttpsError(
          "resource-exhausted",
          "Too many auth attempts. Try again in a few minutes."
        );
      }
      if (now - rateData.windowStart >= windowMs) {
        await rateLimitRef.set({ windowStart: now, count: 1 });
      } else {
        await rateLimitRef.update({ count: admin.firestore.FieldValue.increment(1) });
      }
    } else {
      await rateLimitRef.set({ windowStart: now, count: 1 });
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

    // Validate Solana public key length (32 bytes)
    if (publicKeyBytes.length !== 32) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Invalid public key: must be a 32-byte Solana address"
      );
    }
    // Validate ed25519 signature length (64 bytes)
    if (signatureBytes.length !== 64) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Invalid signature: must be 64 bytes"
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

    // Verify the message contains the correct wallet address
    const walletMatch = message.match(/[Ww]allet:\s*([a-zA-Z0-9]+)/);
    if (!walletMatch || walletMatch[1] !== publicKey) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Message wallet address does not match publicKey"
      );
    }

    // Create or get the Firebase custom token
    // UID is the wallet public key (base58)
    const customToken = await admin.auth().createCustomToken(publicKey);

    return { token: customToken };
  }
);
