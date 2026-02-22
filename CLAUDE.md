# W3nto — Project Context

## What Is W3nto
Web3 Bento profile builder — Linktree meets Bento.me for Web3 creators. Two products:
- **Web dashboard** (Vite + React + TypeScript) — creator tool for building profiles
- **Android viewer app** (Kotlin + Jetpack Compose) — mobile app for discovering and viewing profiles

## Firebase Project
- **Project ID:** `w3nto-app`
- **Android App ID:** `1:1038988861206:android:b0ab1f6a2761fcf9bf3a37`
- **Web App ID:** `1:1038988861206:web:a6953605b188d9c7bf3a37`
- **Android Package:** `com.w3nto.app`

---

## Data Model (Firestore)

```
profiles/{username}           → Profile (displayName, bio, avatar, walletAddress, uid, theme, defaultPageId, showWallet)
  └── pages/{pageId}          → BentoPage (name, slug, theme, isFavorite)
        └── blocks/{blockId}  → Block (type, title, content, url, icon, bgType, bgColor, textColor, width, height, position, ...)

wallets/{walletAddress}       → { username } (reverse lookup)

users/{uid}
  └── favorites/{docId}       → FavoritePage (username, pageId, pageName, pageSlug, profileDisplayName, profileAvatar, savedAt)
                                 docId format: "{username}:::{pageId}"
```

### Block Types (9)
LINK, SOCIAL, TEXT, IMAGE, VIDEO, NFT, TOKEN, CHART, MUSIC

### Security Rules
- Profiles: public read, auth create, owner update/delete (via `uid` field match)
- Pages/Blocks: public read, owner write (verified via parent profile's `uid`)
- Wallets: public read, auth create, owner update
- Favorites: owner-only read/write (`uid` path match)
- Storage: `profile_images/{userId}/` — public read, owner write, 5MB max, images only

---

## Cloud Functions
- `verifyWalletSignature` — Callable function: receives `{publicKey, signature, message}`, verifies ed25519 sig with tweetnacl, checks 5-min timestamp freshness, returns Firebase custom auth token with UID = wallet public key

---

## Auth Flow
1. Wallet connects (Phantom deep link or Saga MWA)
2. App generates message: `"Sign in to W3nto\nWallet: {publicKey}\nTimestamp: {ms}"`
3. Wallet signs the message
4. App calls `verifyWalletSignature` Cloud Function with publicKey + signature + message
5. Cloud Function verifies ed25519 → creates Firebase custom token → returns it
6. App calls `signInWithCustomToken(token)` → Firebase Auth session established

---

## Android App Architecture

### Stack
- **Language:** Kotlin, JVM target 17
- **UI:** Jetpack Compose (Material3, BOM 2024.10.01)
- **DI:** Hilt 2.52
- **Navigation:** Compose Navigation 2.8.4
- **Firebase:** BOM 33.6.0 (Firestore, Auth, Storage, Functions)
- **Crypto:** lazysodium-android 5.1.0 (X25519 for Phantom), MWA clientlib 2.0.3 (Saga)
- **Images:** Coil Compose 2.5.0
- **Serialization:** kotlinx-serialization-json 1.6.2
- **Min SDK:** 26, **Target SDK:** 36, **Compile SDK:** 36, **Gradle:** 8.9

### Package Structure
```
com.w3nto.app/
├── W3ntoApplication.kt          — @HiltAndroidApp entry
├── MainActivity.kt               — Deep link handling (Phantom w3nto:// + HTTPS w3nto.app/)
├── di/
│   ├── AppModule.kt              — Application context
│   ├── FirebaseModule.kt         — Firebase singletons
│   ├── WalletModule.kt           — PhantomWallet + SagaSeedVault providers
│   └── RepositoryModule.kt       — Repository bindings
├── wallet/
│   ├── WalletManager.kt          — Orchestrates Phantom + Saga auth flows
│   ├── phantom/PhantomWallet.kt  — X25519 encryption, deep link URI builders
│   └── saga/SagaSeedVault.kt     — MWA authorize + signMessage
├── data/
│   ├── model/
│   │   ├── Profile.kt            — User profile
│   │   ├── BentoPage.kt          — Page with slug + theme
│   │   ├── Block.kt              — Block with 9 types
│   │   ├── BlockType.kt          — @Serializable enum
│   │   ├── Theme.kt              — Design theme (3 presets: LIGHT, DARK, PINK)
│   │   ├── FavoritePage.kt       — Denormalized page favorite
│   │   └── SocialPlatform.kt     — 17 social platform enum
│   ├── remote/
│   │   ├── FirestoreDataSource.kt — Raw Firestore CRUD (profiles, pages, blocks, discover, favorites)
│   │   └── StorageDataSource.kt   — Firebase Storage image upload/delete
│   └── repository/                — 7 repos, all return Result<T>
│       ├── AuthRepository.kt
│       ├── ProfileRepository.kt
│       ├── PageRepository.kt
│       ├── BlockRepository.kt
│       ├── StorageRepository.kt
│       ├── DiscoverRepository.kt
│       └── FavoritesRepository.kt
└── ui/
    ├── theme/ (Color.kt, Type.kt, W3ntoTheme.kt)
    ├── navigation/
    │   ├── Screen.kt              — Sealed class routes
    │   └── NavGraph.kt            — NavHost + deep link consumption
    ├── components/
    │   ├── BottomNavBar.kt        — Discover + Favorites tabs
    │   ├── BentoGrid.kt           — Responsive block grid
    │   ├── BlockCard.kt           — Block display
    │   ├── ProfileHeader.kt       — Avatar, name, bio
    │   ├── PageSelector.kt        — Multi-page nav
    │   ├── ImageUploader.kt       — Storage upload
    │   ├── ColorPickerDialog.kt
    │   └── blocks/ (9 block-type composables)
    └── screens/
        ├── landing/ (LandingScreen + VM)
        ├── auth/ (ConnectWalletScreen + AuthViewModel)
        ├── discover/ (DiscoverScreen + DiscoverViewModel)
        ├── favorites/ (FavoritesScreen + FavoritesViewModel)
        └── profile/ (PublicProfileScreen + ProfileViewModel)
```

### Key Patterns
| Pattern | Details |
|---------|---------|
| **MVVM** | ViewModels expose `StateFlow<UiState>` data classes |
| **Result<T>** | All repositories wrap Firestore calls in `runCatching {}` |
| **Optimistic UI** | Favorites toggle updates UI immediately, rolls back on error |
| **Pagination** | Discover uses cursor-based Firestore pagination |
| **Debounced Search** | 400ms debounce on search query changes |
| **Deep Links** | MainActivity captures URI → `pendingDeepLinkRoute` StateFlow → NavGraph navigates |
| **Dual Wallet** | Phantom (deep links + X25519 encryption) + Saga (MWA standard) |

### Navigation Routes
- `landing` — Welcome screen
- `connect_wallet` — Wallet connection (Phantom/Saga picker)
- `discover` — Browse profiles with search + pagination
- `favorites` — Saved page favorites (swipe-to-delete)
- `profile/{username}?page={pageSlug}` — Public profile viewer

### Deep Links
- `w3nto://onConnect` — Phantom connect callback
- `w3nto://onSignMessage` — Phantom sign callback
- `https://w3nto.app/{username}` — HTTPS App Link to profile
- `https://w3nto.app/{username}/{pageSlug}` — HTTPS App Link to specific page

---

## Web App

### Stack
- Vite + React + TypeScript
- Tailwind CSS
- Solana wallet-adapter for wallet connection
- Firebase SDK (same auth flow via Cloud Function)

### Key Files
- `src/app/pages/Dashboard.tsx` — Creator dashboard (block editor, page manager, theme editor)
- `src/app/pages/UserProfile.tsx` — Public profile viewer
- `src/app/components/ShareModal.tsx` — QR code sharing modal (qrcode.react)
- `src/app/config/firebase.ts` — Firebase config from VITE_* env vars
- `src/app/services/firestore.ts` — Firestore CRUD service
- `src/app/contexts/WalletContext.tsx` — Solana wallet adapter context

### Firebase Hosting
- `firebase.json` has `hosting.public: "dist"` with SPA rewrite
- `public/.well-known/assetlinks.json` — Android App Links verification
- Deploy: `npm run build && firebase deploy --only hosting`

---

## Development Commands

### Web
```bash
npm run dev          # Vite dev server
npm run build        # Production build → dist/
```

### Android
```bash
cd android && ./gradlew assembleDebug    # Debug APK
```

### Firebase
```bash
firebase deploy --only hosting           # Deploy web app
firebase deploy --only functions         # Deploy Cloud Functions
firebase deploy --only firestore:rules   # Deploy Firestore rules
firebase deploy --only storage           # Deploy Storage rules
```

### Deep Link Testing
```bash
adb shell am start -W -a android.intent.action.VIEW \
  -d "https://w3nto.app/testuser" com.w3nto.app
```

---

## Conventions
- TypeScript strict mode (web)
- Kotlin with Compose (Android)
- Functional patterns, no class-based components
- Conventional Commits: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`
- Never hardcode keys/secrets — use env vars or Firebase config
- Default to devnet for all Solana operations
- All models have `toMap()`/`fromMap()` for Firestore serialization
