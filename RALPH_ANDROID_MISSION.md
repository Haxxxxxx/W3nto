# Ralph's Mission: W3nto — Round 2 Audit Fixes

## Your Goal
Three comprehensive audits (Android, Web, Security) identified new bugs beyond what the first round of fixes addressed. Work through all 5 phases in order. Verify builds after each phase. Commit once at the end.

Read `CLAUDE.md` for full architecture context.

---

## What Was Already Fixed (DO NOT RE-DO)
Commit `2812f48` already completed these tasks. Skip them:
- Firestore rules: uid check on create for profiles and wallets
- Cloud Function: unconditional timestamp check (throws if no timestamp in message)
- URL validation on block click in UserProfile.tsx
- Anonymous auth disabled in firebase.json
- Block/page deletion now calls Firestore (handleDeleteBlock, handleDeletePage)
- Responsive bento grid (grid-cols-2 md:grid-cols-4)
- Video and music buttons in Add Block UI
- Landing page copy fixed (Phantom/Solflare instead of MetaMask)
- crypto.randomUUID() for IDs in Dashboard.tsx and storage.ts
- Clipboard error handling in ShareModal.tsx
- Cache-Control headers in firebase.json
- ThemeEditor `any` type fixed
- SagaSeedVault: uses authResult.publicKey (not ByteArray(0))
- DiscoverRepository: @Volatile on lastDocumentId
- parseColor wrapped in remember() in PublicProfileScreen, ProfileHeader, PageSelector
- Wallet mapping write format aligned (username + uid + updatedAt)
- CLAUDE.md auth message format updated
- onProfileSnapshot dead function deleted from firestore.ts

---

## Current State
The codebase is in a solid state after round 1. The new findings from three deep audits are categorized below. All are net-new issues not touched by round 1.

---

## Phase 1: P0 Crashes and Data Corruption (Android)

### Task 1.1: BentoGrid — Guard against zero availableWidth crash
**File:** `android/app/src/main/java/com/w3nto/app/ui/components/BentoGrid.kt`

The layout lambda calculates `colWidth = (availableWidth - ...) / GRID_COLUMNS` without checking that `availableWidth > 0`. When the composable is first placed in an unbounded container or during first composition, this can produce zero or negative child widths that crash on certain API levels.

Find the `Layout { measurables, constraints ->` block. At the very top of the layout lambda, before the `colWidth` calculation, add:

```kotlin
) { measurables, constraints ->
    val availableWidth = constraints.maxWidth
    if (availableWidth <= 0) {
        layout(0, 0) {}
        return@Layout
    }
    val colWidth = (availableWidth - (GRID_COLUMNS - 1) * gapPx) / GRID_COLUMNS
```

The current code has:
```kotlin
) { measurables, constraints ->
    val availableWidth = constraints.maxWidth
    val colWidth = (availableWidth - (GRID_COLUMNS - 1) * gapPx) / GRID_COLUMNS
```

Replace those two lines with the three-line version that includes the early return.

**Verify:** Code compiles — `cd android && ./gradlew assembleDebug` passes.

---

### Task 1.2: FirestoreDataSource — Fix `doc.data!!` force-unwrap in getPages and getBlocks
**File:** `android/app/src/main/java/com/w3nto/app/data/remote/FirestoreDataSource.kt`

`getPages` and `getBlocks` both use `it.data!!` without null-checking. Tombstoned documents can briefly have null data during Firestore operations, which crashes the app.

In `getPages`, replace:
```kotlin
return snapshot.documents.map { BentoPage.fromMap(it.id, it.data!!) }
```
With:
```kotlin
return snapshot.documents.mapNotNull { doc ->
    doc.data?.let { BentoPage.fromMap(doc.id, it) }
}
```

In `getBlocks`, replace:
```kotlin
return snapshot.documents.map { Block.fromMap(it.id, it.data!!) }
```
With:
```kotlin
return snapshot.documents.mapNotNull { doc ->
    doc.data?.let { Block.fromMap(doc.id, it) }
}
```

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 1.3: WalletManager — Safe cast for Cloud Function response
**File:** `android/app/src/main/java/com/w3nto/app/wallet/WalletManager.kt`

In `authenticateWithFirebase`, the current code uses unchecked casts:
```kotlin
@Suppress("UNCHECKED_CAST")
val resultData = result.getData() as Map<String, Any>
val token = resultData["token"] as String
```

If the Cloud Function returns an unexpected shape, `resultData["token"]` is null and `as String` throws `NullPointerException` with a confusing message.

Replace those two lines with:
```kotlin
val resultData = result.getData() as? Map<*, *>
    ?: throw IllegalStateException("Invalid response from verifyWalletSignature")
val token = resultData["token"] as? String
    ?: throw IllegalStateException("Missing token in auth response")
```

Remove the `@Suppress("UNCHECKED_CAST")` annotation since it is no longer needed.

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 1.4: SagaSeedVault — Guard against empty signedPayloads
**File:** `android/app/src/main/java/com/w3nto/app/wallet/saga/SagaSeedVault.kt`

The MWA `signMessages` result can have fewer payloads than requested if the wallet partially rejects signing. The current code accesses `signResult.signedPayloads[0]` directly, which throws `ArrayIndexOutOfBoundsException` if the list is empty.

Find the line:
```kotlin
signResult.signedPayloads[0]
```

Replace it with:
```kotlin
signResult.signedPayloads.firstOrNull()
    ?: throw IllegalStateException("Wallet did not return a signed payload")
```

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 1.5: FirestoreDataSource — Remove legacy favorites methods (data corruption risk)
**File:** `android/app/src/main/java/com/w3nto/app/data/remote/FirestoreDataSource.kt`
**File:** `android/app/src/main/java/com/w3nto/app/data/repository/FavoritesRepository.kt`

The legacy `getFavorites`, `addFavorite`, `removeFavorite`, and `isFavorited` methods in `FirestoreDataSource` write the OLD favorites schema `{ username, savedAt }` to the same `users/{uid}/favorites` collection that the new page-level favorites use. The old `addFavorite` writes documents WITHOUT the `pageId` field, so any document written via the old path will produce a `FavoritePage` with empty `pageId` when read by `getPageFavorites`. This creates broken favorites that navigate to `profile/username?page=` (blank slug).

These methods are never called by any screen — only by `FavoritesRepository`, which is only called via the legacy repo methods that are also dead code.

**In `FirestoreDataSource.kt`, delete these four methods entirely:**
- `getFavorites(userId: String): List<Profile>` (lines ~188-200)
- `addFavorite(userId: String, username: String)` (lines ~202-212)
- `removeFavorite(userId: String, username: String)` (lines ~214-221)
- `isFavorited(userId: String, username: String): Boolean` (lines ~223-231)

**In `FavoritesRepository.kt`, delete these four methods entirely:**
- `getFavorites(userId: String): Result<List<Profile>>`
- `addFavorite(userId: String, username: String): Result<Unit>`
- `removeFavorite(userId: String, username: String): Result<Unit>`
- `isFavorited(userId: String, username: String): Result<Boolean>`

Also remove `import com.w3nto.app.data.model.Profile` from `FavoritesRepository.kt` if it is no longer used after these deletions (check first).

**Verify:** `cd android && ./gradlew assembleDebug` passes without compilation errors.

---

### Task 1.6: FirestoreDataSource — Fix broken searchProfiles (queries non-existent field)
**File:** `android/app/src/main/java/com/w3nto/app/data/remote/FirestoreDataSource.kt`

`searchProfiles` queries `.orderBy("username")` but `Profile.toMap()` does NOT include a `"username"` field — it is only the document ID. This query returns zero results for all users in production.

The fix is to search on `FieldPath.documentId()` instead of a field named `"username"`:

Replace the entire `searchProfiles` function:
```kotlin
suspend fun searchProfiles(searchQuery: String): List<Profile> {
    val end = searchQuery + "\uf8ff"
    val snapshot = firestore.collection("profiles")
        .orderBy("username")
        .startAt(searchQuery.lowercase())
        .endAt(end.lowercase())
        .limit(20)
        .get()
        .await()
    return snapshot.documents.mapNotNull { doc ->
        doc.data?.let { Profile.fromMap(doc.id, it) }
    }
}
```

With:
```kotlin
suspend fun searchProfiles(searchQuery: String): List<Profile> {
    val end = searchQuery + "\uf8ff"
    val snapshot = firestore.collection("profiles")
        .orderBy(com.google.firebase.firestore.FieldPath.documentId())
        .startAt(searchQuery.lowercase())
        .endAt(end.lowercase())
        .limit(20)
        .get()
        .await()
    return snapshot.documents.mapNotNull { doc ->
        doc.data?.let { Profile.fromMap(doc.id, it) }
    }
}
```

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

**Phase 1 Build Check:** Run `cd android && ./gradlew assembleDebug`. Must pass before continuing.

---

## Phase 2: P0 Security Fixes

### Task 2.1: Firestore Rules — Add username format validation
**File:** `firestore.rules`

The profile create rule currently allows any authenticated user to create a profile with any string as a username, including empty strings, 1-character names, extremely long names, or special characters. This enables spam and routing bugs.

The current create rule is:
```
allow create: if request.auth != null
  && request.resource.data.uid == request.auth.uid;
```

Replace it with:
```
allow create: if request.auth != null
  && request.resource.data.uid == request.auth.uid
  && username.matches('^[a-z0-9_]{3,30}$');
```

**Verify:** Run `firebase emulators:start --only firestore` briefly to confirm rules load, OR just verify the file syntax is correct by reading it back.

---

### Task 2.2: Firebase Hosting — Add security headers
**File:** `firebase.json`

The current `firebase.json` has no security headers beyond caching. Add these headers to protect against clickjacking, MIME sniffing, and improve security posture.

In the `hosting.headers` array, add a new entry at the BEGINNING (before the assetlinks.json entry):

```json
{
  "source": "**",
  "headers": [
    {
      "key": "X-Frame-Options",
      "value": "DENY"
    },
    {
      "key": "X-Content-Type-Options",
      "value": "nosniff"
    },
    {
      "key": "Referrer-Policy",
      "value": "strict-origin-when-cross-origin"
    },
    {
      "key": "Strict-Transport-Security",
      "value": "max-age=31536000; includeSubDomains"
    }
  ]
},
```

Do NOT add a CSP header — the web app uses inline styles and Tailwind which would require a complex CSP. The other four headers are safe to add without risking breakage.

**Verify:** `npm run build` passes and `firebase.json` is valid JSON (check with `python3 -m json.tool firebase.json`).

---

### Task 2.3: Cloud Function — Add publicKey length validation
**File:** `functions/src/auth/verifyWallet.ts`

The function never validates that `publicKey` is a valid Solana public key length. A valid Solana public key is a 32-byte value encoded in base58 (always 44 characters when base58-encoded). Add this validation after the base58 decode:

Find the block that decodes from base58:
```typescript
try {
  publicKeyBytes = bs58.decode(publicKey);
  signatureBytes = bs58.decode(signature);
} catch {
  throw new functions.https.HttpsError(
    "invalid-argument",
    "Invalid base58 encoding for publicKey or signature"
  );
}
```

After the `try/catch` block, add:
```typescript
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
```

**Verify:** `cd functions && npm run build` passes (or check if there's a build step — if `functions/` has a `tsconfig.json`, run `npx tsc --noEmit` from the functions directory).

---

**Phase 2 Build Check:** `npm run build` must pass. Verify `firebase.json` is valid JSON.

---

## Phase 3: P1 Broken Features (Android)

### Task 3.1: ConnectWalletScreen — Remove double-navigation LaunchedEffect
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/auth/ConnectWalletScreen.kt`

`ConnectWalletScreen` has a `LaunchedEffect(firebaseUser)` that calls `onConnected()` whenever `firebaseUser` becomes non-null. Combined with the `LaunchedEffect(firebaseUser)` in `NavGraph.kt` that also navigates to Discover on auth, this produces two simultaneous `navController.navigate(discover)` calls, leaving an inconsistent back stack.

Find and delete this entire block:
```kotlin
// Observe Firebase auth state for navigation
val firebaseUser by authRepository.authStateFlow().collectAsState(initial = authRepository.currentUser)

LaunchedEffect(firebaseUser) {
    if (firebaseUser != null) {
        onConnected()
    }
}
```

The auth-state-driven navigation belongs solely in `NavGraph`. The `onConnected` callback in `ConnectWalletScreen` is used by the UI button flow — do not remove the `onConnected` parameter or the button handler.

After deletion, check if `authRepository` is still used in `ConnectWalletScreen`. If it is only used in the deleted block, also remove the `authRepository` parameter from the function signature, the import, and update the call site in `NavGraph.kt` accordingly. Read the full file first to check all usages.

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 3.2: FavoritesScreen — Fix double Scaffold padding
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/favorites/FavoritesScreen.kt`

`FavoritesScreen` is already rendered inside the outer `Scaffold` in `NavGraph.kt` which applies `innerPadding`. The inner `Scaffold` in `FavoritesScreen` creates a second padding layer, causing double bottom-nav padding. The `SnackbarHost` also renders offset from the screen edge.

Find the inner Scaffold:
```kotlin
Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
        // ... content ...
    }
}
```

Replace the entire `Scaffold { innerPadding -> Box(modifier = Modifier.padding(innerPadding)...` wrapper with a plain `Box` that positions the `SnackbarHost` at the bottom center:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // ... (all the existing content: loading state, auth required state, empty state, LazyColumn) ...
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}
```

Remove the `innerPadding` variable and the `Scaffold` wrapper. Keep all the content inside the Box. Make sure `Alignment` is imported from `androidx.compose.ui.Alignment`.

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 3.3: ProfileViewModel — Show error when unauthenticated user taps favorite
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/profile/ProfileViewModel.kt`

Currently `toggleFavorite()` silently returns when `auth.currentUser` is null — no feedback to the user.

Find:
```kotlin
fun toggleFavorite() {
    val userId = auth.currentUser?.uid ?: return
```

Replace with:
```kotlin
fun toggleFavorite() {
    val userId = auth.currentUser?.uid ?: run {
        _uiState.value = _uiState.value.copy(error = "Connect your wallet to save favorites")
        return
    }
```

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 3.4: NavGraph — Fix NavHost initialization race for deep links
**File:** `android/app/src/main/java/com/w3nto/app/ui/navigation/NavGraph.kt`

The current `LaunchedEffect(deepLinkRoute)` fires on first composition, but `NavHost` may not have completed its graph initialization yet when the app is cold-launched via a deep link. This produces `IllegalStateException: NavController is not set`.

Find the deep link LaunchedEffect:
```kotlin
// Handle deep link navigation
LaunchedEffect(deepLinkRoute) {
    val route = deepLinkRoute ?: return@LaunchedEffect
    navController.navigate(route) {
        launchSingleTop = true
    }
    onDeepLinkConsumed()
}
```

Replace it with a version that delays by one frame to ensure NavHost is ready:
```kotlin
// Handle deep link navigation — delay one frame to ensure NavHost graph is initialized
LaunchedEffect(deepLinkRoute) {
    val route = deepLinkRoute ?: return@LaunchedEffect
    // Wait for NavHost to complete its first composition
    kotlinx.coroutines.delay(100)
    try {
        navController.navigate(route) {
            launchSingleTop = true
        }
    } catch (e: Exception) {
        android.util.Log.e("NavGraph", "Deep link navigation failed: ${e.message}")
    }
    onDeepLinkConsumed()
}
```

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 3.5: DiscoverRepository — Move pagination cursor to Mutex-protected state
**File:** `android/app/src/main/java/com/w3nto/app/data/repository/DiscoverRepository.kt`

`@Volatile` ensures visibility but NOT atomicity. Concurrent `loadMore()` calls from rapid scroll events can race. Replace `@Volatile` with a `Mutex` to serialize access:

Find the current file content:
```kotlin
@Singleton
class DiscoverRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    @Volatile
    private var lastDocumentId: String? = null

    fun resetPagination() {
        lastDocumentId = null
    }

    suspend fun getProfiles(pageSize: Int = 20): Result<List<Profile>> = runCatching {
        val profiles = firestoreDataSource.getDiscoverProfiles(
            pageSize = pageSize,
            lastDocumentId = lastDocumentId
        )
        if (profiles.isNotEmpty()) {
            lastDocumentId = profiles.last().username
        }
        profiles
    }
```

Replace with:
```kotlin
@Singleton
class DiscoverRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    private val paginationMutex = kotlinx.coroutines.sync.Mutex()
    private var lastDocumentId: String? = null

    fun resetPagination() {
        lastDocumentId = null
    }

    suspend fun getProfiles(pageSize: Int = 20): Result<List<Profile>> = runCatching {
        paginationMutex.withLock {
            val profiles = firestoreDataSource.getDiscoverProfiles(
                pageSize = pageSize,
                lastDocumentId = lastDocumentId
            )
            if (profiles.isNotEmpty()) {
                lastDocumentId = profiles.last().username
            }
            profiles
        }
    }
```

Add the import at the top:
```kotlin
import kotlinx.coroutines.sync.withLock
```

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

**Phase 3 Build Check:** `cd android && ./gradlew assembleDebug` must pass.

---

## Phase 4: P1 Broken Features (Web)

### Task 4.1: WalletContext — Add wallet adapters (walletAdapters is empty array)
**File:** `src/app/contexts/WalletContext.tsx`

The current code:
```typescript
const walletAdapters = useMemo(() => [], []);
```

...initializes `SolanaWalletProvider` with zero adapters. On browsers without an installed wallet, the picker shows nothing. The `@solana/wallet-adapter-wallets` package is already installed but unused.

Replace:
```typescript
const walletAdapters = useMemo(() => [], []);
```

With:
```typescript
import { PhantomWalletAdapter, SolflareWalletAdapter } from "@solana/wallet-adapter-wallets";

const walletAdapters = useMemo(
  () => [new PhantomWalletAdapter(), new SolflareWalletAdapter()],
  []
);
```

Add the import at the top of the file with the other imports. Check if it already exists before adding.

**Verify:** `npm run build` passes (TypeScript strict mode check).

---

### Task 4.2: UserProfile — Fix false "not found" for profiles with zero pages
**File:** `src/app/pages/UserProfile.tsx`

The current condition:
```typescript
if (notFound || !profile || !currentPage) {
```

...shows "Profile not found" when a profile exists but has zero pages (e.g., a brand-new user who just signed up). `currentPage` is null when `profile.pages` is empty, so the not-found screen shows "The profile 'username' doesn't exist" for a real user.

Find this condition (it may be slightly different in the actual file — read the file first). Replace the combined condition with separate handling:

```typescript
if (notFound || !profile) {
  return (
    // ... existing not-found JSX ...
  );
}

if (!currentPage) {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <p className="text-2xl font-bold mb-2">{profile.displayName || profile.username}</p>
        <p className="text-gray-500">This profile has no pages yet.</p>
      </div>
    </div>
  );
}
```

Read the actual file first to find the exact existing not-found JSX, then split the condition appropriately.

**Verify:** `npm run build` passes.

---

### Task 4.3: Dashboard — Fix stale closure in debounced save
**File:** `src/app/pages/Dashboard.tsx`

The debounced save has a race condition: if a save is already in-flight when a new change comes in, the in-flight save may write stale state (capturing old profile data). Add a version counter to abort superseded saves.

Find the `debouncedSave` function in Dashboard.tsx. Read the file to see its exact current form. It will look something like:

```typescript
const saveTimeoutRef = useRef<ReturnType<typeof setTimeout>>();

const debouncedSave = useCallback((profileToSave: Profile) => {
  if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
  saveTimeoutRef.current = setTimeout(async () => {
    // ... save logic ...
  }, 2000);
}, []);
```

Add a `saveVersionRef` just after `saveTimeoutRef`:
```typescript
const saveVersionRef = useRef(0);
```

Then modify `debouncedSave` to capture and check the version:
```typescript
const debouncedSave = useCallback((profileToSave: Profile) => {
  if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
  const version = ++saveVersionRef.current;
  saveTimeoutRef.current = setTimeout(async () => {
    if (version !== saveVersionRef.current) return; // superseded by newer save
    // ... rest of the existing save logic unchanged ...
  }, 2000);
}, []);
```

Read the file carefully before editing to preserve the exact existing save logic (error handling, setIsSaving, etc.).

**Verify:** `npm run build` passes.

---

### Task 4.4: Dashboard — Add missing saveTimeoutRef cleanup on unmount
**File:** `src/app/pages/Dashboard.tsx`

When the Dashboard component unmounts (user navigates away without logging out), the pending debounce timer is never cleared. This can cause state updates on unmounted components.

Find the existing `useEffect` blocks in Dashboard. Add a cleanup effect. If there is already a `useEffect(() => { return () => { ... } }, [])` cleanup, add the timeout clear to it. Otherwise add a new one:

```typescript
// Cleanup: clear pending debounce timer on unmount
useEffect(() => {
  return () => {
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
  };
}, []);
```

**Verify:** `npm run build` passes.

---

### Task 4.5: Dashboard — Fix duplicate page slugs
**File:** `src/app/pages/Dashboard.tsx`

`handleCreatePage` uses `profile.pages.length + 1` for the slug. If a user creates page 3, deletes page 2, then creates another page, the new page gets slug `page-3` which duplicates the existing one. Duplicate slugs cause the wrong page to load.

Find `handleCreatePage` and the slug assignment. It will look like:
```typescript
slug: `page-${profile.pages.length + 1}`,
```

Replace with a timestamp-based slug that is guaranteed unique:
```typescript
slug: `page-${Date.now()}`,
```

**Verify:** `npm run build` passes.

---

### Task 4.6: WalletContext — Await signOut in disconnect
**File:** `src/app/contexts/WalletContext.tsx`

The `disconnect` function calls `signOut(auth)` without awaiting it. If signOut fails silently, the Firebase auth state persists.

Find:
```typescript
const disconnect = useCallback(() => {
  authedWalletRef.current = null;
  walletDisconnect();
  signOut(auth);
  setAuthError(null);
}, [walletDisconnect]);
```

Replace with:
```typescript
const disconnect = useCallback(() => {
  authedWalletRef.current = null;
  walletDisconnect();
  signOut(auth).catch((err) => console.error("Sign out error:", err));
  setAuthError(null);
}, [walletDisconnect]);
```

**Verify:** `npm run build` passes.

---

### Task 4.7: Web — Fix socialPlatforms.ts missing React import
**File:** `src/app/utils/socialPlatforms.ts`

The file uses `React.ComponentType<any>` as a type but never imports `React`. With strict TypeScript this is fragile.

Find the type definition:
```typescript
icon: React.ComponentType<any>;
```

Replace with a proper import and typed reference. At the top of the file add:
```typescript
import type { ComponentType } from "react";
```

Then change the field type:
```typescript
icon: ComponentType<{ className?: string }>;
```

**Verify:** `npm run build` passes.

---

**Phase 4 Build Check:** `npm run build` must pass. Fix any TypeScript errors before proceeding.

---

## Phase 5: P2 Polish, Final Builds, and Commit

### Task 5.1: ProfileEditor — Validate avatar URL input
**File:** `src/app/components/ProfileEditor.tsx`

The avatar URL text input accepts any string including `javascript:` URIs and base64 data URIs (which could push Firestore documents toward the 1MB limit).

Read the file to find the avatar URL input's onChange handler. It will be updating `profile.avatar`. Add protocol validation:

```typescript
onChange={(e) => {
  const val = e.target.value;
  // Only allow http/https URLs or empty string
  if (val && !val.startsWith('https://') && !val.startsWith('http://')) return;
  onUpdate({ ...profile, avatar: val });
}}
```

**Verify:** `npm run build` passes.

---

### Task 5.2: Dashboard — Block position renormalization after delete
**File:** `src/app/pages/Dashboard.tsx`

When a block is deleted, the remaining blocks keep their old positions (e.g., 0, 2, 3 after deleting position 1). If two blocks end up with the same position value, their sort order is undefined. Renormalize after deletes.

Find `handleDeleteBlock`. After filtering out the deleted block, renormalize positions:
```typescript
const updatedBlocks = p.blocks
  .filter((b) => b.id !== blockId)
  .map((b, idx) => ({ ...b, position: idx }));
```

Read the file to find the exact current filter syntax and integrate the `.map` renormalization into it.

**Verify:** `npm run build` passes.

---

### Task 5.3: Android — Fix `allowBackup` in AndroidManifest
**File:** `android/app/src/main/AndroidManifest.xml`

The manifest has `android:allowBackup="true"` which allows cached auth tokens to be extracted via ADB on rooted devices.

Find:
```xml
android:allowBackup="true"
```

Replace with:
```xml
android:allowBackup="false"
```

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 5.4: Android — Validate deep link path segments in MainActivity
**File:** `android/app/src/main/java/com/w3nto/app/MainActivity.kt`

The deep link handler takes `pathSegments[0]` directly as a username without sanitization. While Firestore handles this safely, extremely long or malformed usernames could cause display issues.

Read the file to find the deep link path segment extraction. It will look like:
```kotlin
val username = pathSegments[0]
val pageSlug = pathSegments.getOrNull(1)
```

Add validation before constructing the route:
```kotlin
val username = pathSegments[0].lowercase().take(30)
val pageSlug = pathSegments.getOrNull(1)?.lowercase()?.take(50)
// Only proceed if username has valid format
if (!username.matches(Regex("^[a-z0-9_]{1,30}$"))) return
```

**Verify:** `cd android && ./gradlew assembleDebug` passes.

---

### Task 5.5: Final Build Verification

Run BOTH builds and confirm they pass:

```bash
npm run build
cd android && ./gradlew assembleDebug
```

Both must succeed with zero errors. Fix any errors before committing.

---

### Task 5.6: Commit All Changes

Stage all modified files and commit with:
```
fix: round 2 audit fixes — crash guards, nav safety, search fix, security headers, web polish
```

Use the exact Conventional Commits format. Co-author line required:
```
Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

---

## Execution Order

1. Phase 1 (Tasks 1.1-1.6) — Android P0 crashes and data corruption
2. Phase 2 (Tasks 2.1-2.3) — Security hardening
3. Phase 3 (Tasks 3.1-3.5) — Android P1 broken features
4. Phase 4 (Tasks 4.1-4.7) — Web P1 broken features
5. Phase 5 (Tasks 5.1-5.6) — Polish, final builds, commit

## Important Notes
- **Read every file before editing** — line numbers in this document are approximate
- **Check imports** before adding new ones — many may already exist
- **Run the build** after each phase to catch errors early
- **Do not touch** anything already fixed in commit `2812f48`
- You have full Bash access — run npm, gradle, git, any command needed
- Total tasks: 24 across 5 phases
