import { createContext, useContext, useState, useEffect, useCallback, useRef, ReactNode, useMemo } from "react";
import { ConnectionProvider, WalletProvider as SolanaWalletProvider, useWallet as useSolanaWallet, Wallet } from "@solana/wallet-adapter-react";
import { PhantomWalletAdapter, SolflareWalletAdapter } from "@solana/wallet-adapter-wallets";
import { clusterApiUrl } from "@solana/web3.js";
import { signInWithCustomToken, onAuthStateChanged, signOut, User } from "firebase/auth";
import { httpsCallable } from "firebase/functions";
import { auth, functions } from "../config/firebase";
import bs58 from "bs58";
import { toast } from "sonner";

interface WalletContextType {
  address: string | null;
  isConnected: boolean;
  isAuthenticated: boolean;
  isAuthenticating: boolean;
  authError: string | null;
  firebaseUser: User | null;
  connect: () => Promise<void>;
  disconnect: () => void;
  retryAuth: () => Promise<void>;
}

// --- Wallet Picker Modal ---

function WalletPickerModal({
  wallets,
  onSelect,
  onClose,
}: {
  wallets: Wallet[];
  onSelect: (wallet: Wallet) => void;
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-sm mx-4 overflow-hidden">
        <div className="px-6 pt-6 pb-4 border-b border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900">Connect Wallet</h2>
          <p className="text-sm text-gray-500 mt-1">Choose a wallet to sign in</p>
        </div>
        <div className="p-3 max-h-80 overflow-y-auto">
          {wallets.length === 0 ? (
            <div className="text-center py-8 text-gray-500 text-sm">
              No wallets detected. Install a Solana wallet extension.
            </div>
          ) : (
            wallets.map((wallet) => (
              <button
                key={wallet.adapter.name}
                onClick={() => onSelect(wallet)}
                className="w-full flex items-center gap-3 px-4 py-3 rounded-xl hover:bg-gray-50 transition-colors text-left"
              >
                {wallet.adapter.icon && (
                  <img
                    src={wallet.adapter.icon}
                    alt={wallet.adapter.name}
                    className="size-10 rounded-xl"
                  />
                )}
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-gray-900">{wallet.adapter.name}</div>
                  <div className="text-xs text-gray-400">
                    {wallet.readyState === "Installed" ? "Detected" : "Not installed"}
                  </div>
                </div>
                {wallet.readyState === "Installed" && (
                  <div className="size-2 rounded-full bg-green-400 shrink-0" />
                )}
              </button>
            ))
          )}
        </div>
        <div className="px-6 py-4 border-t border-gray-100">
          <button
            onClick={onClose}
            className="w-full py-2.5 text-sm text-gray-500 hover:text-gray-700 transition-colors"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}

const WalletContext = createContext<WalletContextType | undefined>(undefined);

function WalletAuthBridge({ children }: { children: ReactNode }) {
  const { publicKey, connected, signMessage, disconnect: walletDisconnect, select, wallets } = useSolanaWallet();
  const [firebaseUser, setFirebaseUser] = useState<User | null>(null);
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);
  const [showPicker, setShowPicker] = useState(false);
  const authedWalletRef = useRef<string | null>(null);
  const authInProgressRef = useRef(false);
  const userInitiatedRef = useRef(false);

  const address = publicKey?.toBase58() ?? null;
  const isAuthenticated = !!firebaseUser && !!address;

  useEffect(() => {
    return onAuthStateChanged(auth, (user) => setFirebaseUser(user));
  }, []);

  const authenticate = useCallback(async () => {
    if (!publicKey || !signMessage || !connected) return;
    if (authInProgressRef.current) return;
    if (authedWalletRef.current === publicKey.toBase58()) return;

    authInProgressRef.current = true;
    setIsAuthenticating(true);
    setAuthError(null);

    try {
      const message = `Sign in to W3nto\nWallet: ${publicKey.toBase58()}\nTimestamp: ${Date.now()}`;
      const messageBytes = new TextEncoder().encode(message);
      const signatureBytes = await signMessage(messageBytes);

      const verifyWalletSignature = httpsCallable<
        { publicKey: string; signature: string; message: string },
        { token: string }
      >(functions, "verifyWalletSignature");

      const result = await verifyWalletSignature({
        publicKey: publicKey.toBase58(),
        signature: bs58.encode(signatureBytes),
        message,
      });

      await signInWithCustomToken(auth, result.data.token);
      authedWalletRef.current = publicKey.toBase58();
    } catch (err) {
      console.error("Auth failed:", err);
      const msg = err instanceof Error ? err.message : "Authentication failed";
      setAuthError(msg);
      toast.error("Authentication failed", { description: msg });
    } finally {
      authInProgressRef.current = false;
      setIsAuthenticating(false);
    }
  }, [publicKey, signMessage, connected]);

  // Only auto-authenticate if user explicitly clicked connect
  useEffect(() => {
    if (connected && publicKey && !authedWalletRef.current && userInitiatedRef.current) {
      authenticate();
    }
  }, [connected, publicKey, authenticate]);

  const connect = useCallback(async () => {
    userInitiatedRef.current = true;
    if (connected && publicKey && !authedWalletRef.current) {
      authenticate();
    } else {
      setShowPicker(true);
    }
  }, [connected, publicKey, authenticate]);

  const handleWalletSelect = useCallback((wallet: Wallet) => {
    setShowPicker(false);
    select(wallet.adapter.name);
  }, [select]);

  const disconnect = useCallback(() => {
    authedWalletRef.current = null;
    walletDisconnect();
    signOut(auth).catch((err) => console.error("Sign out error:", err));
    setAuthError(null);
  }, [walletDisconnect]);

  return (
    <WalletContext.Provider
      value={{
        address,
        isConnected: connected,
        isAuthenticated,
        isAuthenticating,
        authError,
        firebaseUser,
        connect,
        disconnect,
        retryAuth: authenticate,
      }}
    >
      {children}
      {showPicker && (
        <WalletPickerModal
          wallets={wallets}
          onSelect={handleWalletSelect}
          onClose={() => setShowPicker(false)}
        />
      )}
    </WalletContext.Provider>
  );
}

export function WalletProvider({ children }: { children: ReactNode }) {
  const endpoint = useMemo(() => clusterApiUrl("devnet"), []);
  const walletAdapters = useMemo(
    () => [new PhantomWalletAdapter(), new SolflareWalletAdapter()],
    []
  );

  return (
    <ConnectionProvider endpoint={endpoint}>
      <SolanaWalletProvider wallets={walletAdapters} autoConnect={false}>
        <WalletAuthBridge>{children}</WalletAuthBridge>
      </SolanaWalletProvider>
    </ConnectionProvider>
  );
}

export function useWallet() {
  const context = useContext(WalletContext);
  if (!context) {
    throw new Error("useWallet must be used within WalletProvider");
  }
  return context;
}
