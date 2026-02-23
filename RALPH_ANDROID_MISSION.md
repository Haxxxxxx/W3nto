# Ralph's Mission: W3nto — Round 4 Audit Fixes

## Your Goal
A fourth audit identified 21 net-new issues beyond what rounds 1, 2, and 3 addressed. Work through all 5 phases in order. Verify builds after each phase. Commit once at the end.

Read `CLAUDE.md` for full architecture context.

---

## What Was Already Fixed (DO NOT RE-DO)
Rounds 1, 2, and 3 (commits `2812f48`, `f91a78f`, and subsequent) already completed these. Skip them entirely:
- Firestore rules: uid check on create for profiles and wallets
- Cloud Function: unconditional timestamp check
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
- DiscoverRepository: @Volatile on lastDocumentId (replaced by Mutex in round 2)
- parseColor wrapped in remember() in PublicProfileScreen, ProfileHeader, PageSelector
- Wallet mapping write format aligned (username + uid + updatedAt)
- CLAUDE.md auth message format updated
- onProfileSnapshot dead function deleted from firestore.ts
- BentoGrid: guard against zero availableWidth
- FirestoreDataSource: `doc.data!!` force-unwrap fixed in getPages and getBlocks
- WalletManager: safe cast for Cloud Function response
- SagaSeedVault: guard against empty signedPayloads
- Legacy favorites methods removed (getFavorites/addFavorite/removeFavorite/isFavorited)
- searchProfiles: fixed to use FieldPath.documentId()
- Firestore rules: username format validation added
- Firebase Hosting: security headers added (X-Frame-Options, HSTS, etc.)
- Cloud Function: publicKey and signature byte-length validation
- ConnectWalletScreen: removed double-navigation LaunchedEffect
- FavoritesScreen: fixed double Scaffold padding
- ProfileViewModel: error shown when unauthenticated user taps favorite
- NavGraph: deep link navigation wrapped in try/catch with 100ms delay
- DiscoverRepository: @Volatile replaced with Mutex
- WalletContext: wallet adapters wired (Phantom + Solflare)
- UserProfile: false "not found" for zero-page profiles separated
- Dashboard: stale closure in debounced save fixed with saveVersionRef
- Dashboard: saveTimeoutRef cleanup on unmount added
- Dashboard: duplicate page slugs fixed (Date.now() slug)
- WalletContext: signOut awaited with .catch()
- socialPlatforms.ts: React import and ComponentType typed correctly
- ProfileEditor: avatar URL validation (https:// only)
- Dashboard: block position renormalization after delete
- AndroidManifest: allowBackup set to false
- MainActivity: deep link path segment validation and length clamping
- ConnectWalletScreen: removed unused onConnected callback parameter
- WalletManager: removed unused firestore constructor parameter
- ConnectWalletScreen: clearError() called at start of wallet button clicks
- FavoritesScreen/NavGraph: malformed route fixed when pageSlug is empty
- ProfileViewModel: isLoading no longer set during page switch (Round 3 version — see Round 4 task 4 which RE-ADDS it intentionally)
- TextBlock: maxLines = 6 added for TextOverflow.Ellipsis
- PhantomWallet: removed unused android.util.Base64 import
- AuthRepository: dead signInAnonymously() method removed
- StorageRepository and StorageDataSource deleted (viewer-only app)
- ProfileRepository: write-only dead methods removed (saveProfile, isUsernameAvailable, getProfileByWallet)
- PageRepository: savePage and deletePage removed
- BlockRepository: saveBlock and deleteBlock removed
- FirestoreDataSource: write methods with no remaining callers removed
- AppModule: redundant Context binding deleted
- Dashboard: user-visible error feedback for delete failures (toast.error)
- UserProfile: "profile not found" separated from "profile has no pages"
- Dashboard: window.confirm() before block delete
- Dashboard: page slug collision after delete fixed
- WalletContext: autoConnect={false}
- WalletContext: disconnect awaits signOut
- Landing: dead href="#" social links removed

---

## Current State
The codebase has been hardened through three full rounds. The new findings from this fourth audit are categorized below. All are net-new issues not addressed in rounds 1, 2, or 3.

For tasks marked **"verify first"** — read the file before editing and skip the task if the fix is already applied.

---

## Phase 1: Android Fixes (9 tasks)

### Task 1.1: SagaSeedVault — Fix MWA 2.0 signed payload field name
**File:** `android/app/src/main/java/com/w3nto/app/wallet/saga/SagaSeedVault.kt`

The current code accesses `signResult.signedPayloads.firstOrNull()` to get the signed message bytes. In MWA 2.0 the field on `SignMessagesResult` may be `signedMessages` instead of `signedPayloads`.

Read the file and find the line that accesses the sign result. Then check what fields actually exist on the `SignMessagesResult` class by looking at the MWA clientlib import or by reading the compiler output. The access site looks like:

```kotlin
// CURRENT (may be wrong field name):
signResult.signedPayloads.firstOrNull()
```

Steps:
1. Read the file and find the exact line referencing `signedPayloads` on the sign result.
2. In Android Studio / Gradle, the MWA clientlib is `com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.3`. Check the actual data class shape. The correct field in MWA 2.0 is `signedMessages`.
3. If the field is `signedPayloads`, change it to `signedMessages`:

```kotlin
// NEW:
signResult.signedMessages.firstOrNull()
```

4. If the field is already `signedMessages`, skip this task.
5. If neither compiles, check the `SignMessagesResult` class in the dependency for the actual field name and use that.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.2: NavGraph — Fix back navigation dropping auth'd users on Landing
**File:** `android/app/src/main/java/com/w3nto/app/ui/navigation/NavGraph.kt`

When a user arrives via a deep link to a profile and then presses back, `popBackStack()` sends them to the Landing screen. An authenticated user has no path forward from Landing without re-authenticating.

Read the file and find the `onBack` lambda passed to `PublicProfileScreen` (or whichever composable handles the back action from the profile screen). It currently looks like:

```kotlin
// OLD:
onBack = { navController.popBackStack() }
```

Replace it so that if `popBackStack()` fails (returns false, meaning no back stack entry exists), the user is navigated to Discover instead:

```kotlin
// NEW:
onBack = {
    if (!navController.popBackStack()) {
        navController.navigate(Screen.Discover.route) {
            popUpTo(0) { inclusive = true }
        }
    }
}
```

Read the file carefully — there may be multiple `onBack` call sites. Apply this fix only to the one inside the `PublicProfileScreen` composable call.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.3: FavoritesViewModel — React to auth state changes
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/favorites/FavoritesViewModel.kt`

If a user opens the Favorites tab before authentication completes, the screen shows "Connect wallet" and stays there forever — it never reacts when auth finishes.

Read the file and check the constructor injection and `init` block. The fix requires:

1. Add `authRepository: AuthRepository` to the `@HiltViewModel` constructor injection (check if it is already injected first — skip adding it if so).
2. In the `init` block, after the existing `loadFavorites()` call, add:

```kotlin
viewModelScope.launch {
    authRepository.authStateFlow()
        .drop(1)
        .distinctUntilChanged { old, new -> old?.uid == new?.uid }
        .collect { loadFavorites() }
}
```

3. Check the import for `distinctUntilChanged` — it is `kotlinx.coroutines.flow.distinctUntilChanged`. Add it if missing.
4. Check the import for `drop` — it is `kotlinx.coroutines.flow.drop`. Add it if missing.
5. Confirm `authRepository.authStateFlow()` exists on `AuthRepository`. If the method is named differently (e.g., `currentUserFlow()`), use the correct name.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.4: ProfileViewModel — Add isLoading=true back to selectPage, clear after blocks load
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/profile/ProfileViewModel.kt`

Round 3 removed `isLoading = true` from `selectPage()` to prevent the full profile shimmer from showing during page switches. However, this left a "No blocks yet" flash because the grid empties with no loading indicator at all. The correct fix is: put `isLoading = true` back into `selectPage()` AND ensure it is set back to `false` after blocks finish loading.

Read the file and find `selectPage()`. It currently looks like:

```kotlin
fun selectPage(page: BentoPage) {
    _uiState.value = _uiState.value.copy(
        currentPage = page, blocks = emptyList(), isFavorited = false
        // isLoading is NOT here after round 3 fix
    )
    loadBlocks(page.id)
}
```

Change it to:

```kotlin
fun selectPage(page: BentoPage) {
    _uiState.value = _uiState.value.copy(
        currentPage = page, blocks = emptyList(), isFavorited = false,
        isLoading = true
    )
    loadBlocks(page.id)
}
```

Then find `loadBlocks()`. At the point where blocks are assigned to `_uiState`, ensure `isLoading = false` is also set. Look for the `.copy(blocks = ...)` call at the end of the blocks load coroutine. It should already have `isLoading = false` there from the initial load path — confirm it does. If it does not, add it:

```kotlin
_uiState.value = _uiState.value.copy(
    blocks = blocksResult.getOrDefault(emptyList()),
    isLoading = false
)
```

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.5: ProfileViewModel — Clear toggleFavorite error after 3 seconds
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/profile/ProfileViewModel.kt`

When an unauthenticated user taps the favorite button, the error message "Connect your wallet to save favorites" (or similar) is set on the `error` field. This error is never cleared — it persists until the user navigates away.

Read the file and find the `toggleFavorite` function. Find the early-return path where the user is not authenticated. It currently looks like:

```kotlin
val userId = auth.currentUser?.uid ?: run {
    _uiState.value = _uiState.value.copy(error = "Connect your wallet to save favorites")
    return
}
```

Replace with a version that auto-clears the error after 3 seconds:

```kotlin
val userId = auth.currentUser?.uid ?: run {
    _uiState.value = _uiState.value.copy(error = "Connect your wallet to save favorites")
    viewModelScope.launch {
        delay(3000)
        _uiState.value = _uiState.value.copy(error = null)
    }
    return
}
```

Check that `delay` is imported (`kotlinx.coroutines.delay`). Add the import if missing.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.6: DiscoverViewModel — Guard loadMore() against concurrent execution
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/discover/DiscoverViewModel.kt`

`loadMore()` has a TOCTOU race — two rapid scroll events can both pass the guard check and fire two concurrent Firestore page loads, leading to duplicate results.

Read the file and find the `loadMore()` function and its class-level fields. The fix requires adding a `Job` guard:

1. At the class level (near other private fields), add:
```kotlin
private var loadMoreJob: Job? = null
```

2. Check that `Job` is imported (`kotlinx.coroutines.Job`). Add the import if missing.

3. In `loadMore()`, add the Job guard at the start of the function, before the existing guard checks OR as the first action after them:

```kotlin
fun loadMore() {
    val state = _uiState.value
    if (state.isLoadingMore || !state.hasMore || state.searchQuery.isNotBlank()) return
    if (loadMoreJob?.isActive == true) return
    loadMoreJob = viewModelScope.launch {
        // ... existing loadMore coroutine body unchanged ...
    }
}
```

Read the existing `loadMore()` carefully — it may already use `viewModelScope.launch { ... }` or it may use a different pattern. Wrap the existing launch in the job assignment. Do not change the inner logic.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.7: NavGraph — Replace 100ms hardcoded delay with currentDestination guard
**File:** `android/app/src/main/java/com/w3nto/app/ui/navigation/NavGraph.kt`

The 100ms delay before consuming a deep link route was added in round 2 as a band-aid. The proper fix is to wait until `navController.currentDestination` is not null before navigating.

Read the file and find the `LaunchedEffect` that handles deep link route consumption. It currently looks like:

```kotlin
LaunchedEffect(deepLinkRoute) {
    val route = deepLinkRoute ?: return@LaunchedEffect
    kotlinx.coroutines.delay(100)
    // ... navigate ...
}
```

Replace the `LaunchedEffect` key and guard:

```kotlin
LaunchedEffect(deepLinkRoute, navController.currentDestination) {
    val route = deepLinkRoute ?: return@LaunchedEffect
    if (navController.currentDestination == null) return@LaunchedEffect
    // ... rest of existing navigation logic unchanged ...
}
```

Remove the `kotlinx.coroutines.delay(100)` line. Keep all other logic inside the effect unchanged.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.8: ImageBlock — Remove block.content from URL fallback, add null placeholder
**File:** `android/app/src/main/java/com/w3nto/app/ui/components/blocks/ImageBlock.kt`

`block.content` is a text field, not a URL. Including it in the image URL fallback chain causes Coil to attempt loading text strings as image URLs, producing errors.

Read the file and find the `imageUrl` (or equivalent) variable assignment. It currently looks like:

```kotlin
// OLD:
val imageUrl = block.imageUrl ?: block.bgImage ?: block.url ?: block.content
```

Replace it with:

```kotlin
// NEW:
val imageUrl = (block.imageUrl ?: block.bgImage ?: block.url)?.takeIf { it.isNotEmpty() }
```

Then find where `imageUrl` is used in the composable. If `imageUrl` is null (no valid URL), show a placeholder instead of attempting to load. The placeholder can be a simple Box with a centered icon:

```kotlin
if (imageUrl == null) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = "No image",
            tint = Color.Gray.copy(alpha = 0.5f)
        )
    }
} else {
    // existing AsyncImage or Image composable using imageUrl
}
```

Check imports — `Icons.Default.Image` requires `androidx.compose.material.icons.Icons` and `androidx.compose.material.icons.filled.Image`. Add them if missing. Also check `Alignment` and `Box` imports.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.9: WalletManager — Add @Volatile to pending* fields
**File:** `android/app/src/main/java/com/w3nto/app/wallet/WalletManager.kt`

`pendingPublicKey`, `pendingPhantomEncryptionPubKey`, `pendingSession`, and `pendingAuthMessage` are mutable fields that can be written from one coroutine context and read from another (e.g., deep link callback). Without `@Volatile`, reads may see stale values due to CPU cache line visibility.

Read the file and find these four fields. They will look like:

```kotlin
private var pendingPublicKey: String? = null
private var pendingPhantomEncryptionPubKey: String? = null
private var pendingSession: String? = null
private var pendingAuthMessage: String? = null
```

Add `@Volatile` to each:

```kotlin
@Volatile private var pendingPublicKey: String? = null
@Volatile private var pendingPhantomEncryptionPubKey: String? = null
@Volatile private var pendingSession: String? = null
@Volatile private var pendingAuthMessage: String? = null
```

If any of these fields do not exist (were renamed or removed in a prior round), skip only that specific field — apply `@Volatile` to whichever ones do exist.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

**Phase 1 Build Check:** Run `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug`. Must pass before continuing to Phase 2.

---

## Phase 2: Web Critical Fixes (3 tasks)

### Task 2.1: Dashboard — Make handleDeleteBlock and handleDeletePage await Firestore before updating state
**File:** `src/app/pages/Dashboard.tsx`

`handleDeleteBlock` fires the Firestore delete async (with `.catch()`) and then immediately updates local state regardless of whether the delete succeeded. If the delete fails, the block disappears from the UI but still exists in Firestore — data is now out of sync.

Read the file and find `handleDeleteBlock`. The current pattern is:

```ts
// OLD pattern (approximate — read the actual file):
firestoreService.deleteBlock(profile.username, currentPageId, blockId).catch(console.error);
setProfile({ ...profile, pages: profile.pages.map(p => ...) });
```

Replace with an async version that awaits the delete and aborts on error:

```ts
// NEW:
const handleDeleteBlock = async (blockId: string) => {
  if (!profile || !window.confirm("Delete this block?")) return;
  try {
    await firestoreService.deleteBlock(profile.username, currentPageId, blockId);
  } catch (err) {
    console.error(err);
    toast.error("Failed to delete block");
    return;
  }
  setProfile({ ...profile, pages: profile.pages.map(p =>
    p.id === currentPageId
      ? { ...p, blocks: p.blocks.filter(b => b.id !== blockId).map((b, idx) => ({ ...b, position: idx })) }
      : p
  )});
};
```

Note: if the `window.confirm` guard was already added in round 3, keep it — do not duplicate it. Read the file to see what is already there and merge cleanly.

Apply the same `await`-first pattern to `handleDeletePage`:

```ts
const handleDeletePage = async (pageId: string) => {
  if (!profile || !window.confirm("Delete this page?")) return;
  try {
    await firestoreService.deletePage(profile.username, pageId);
  } catch (err) {
    console.error(err);
    toast.error("Failed to delete page");
    return;
  }
  // ... existing local state update logic unchanged ...
};
```

Read the file to see the exact current local state update logic in `handleDeletePage` and preserve it exactly — only change the Firestore call from fire-and-forget to await-first.

**Verify:** `npm run build` passes.

---

### Task 2.2: firestore.ts — Remove pre-read getDoc from saveBlock and savePage
**File:** `src/app/services/firestore.ts`

`saveBlock` currently does a `getDoc` read before every write to check if the document exists (to conditionally set `createdAt`). This doubles the Firestore read cost on every block save. `setDoc` with `merge: true` already preserves existing fields including `createdAt` — the pre-read is unnecessary.

Read the file and find `saveBlock`. The current pattern is approximately:

```ts
// OLD:
const existing = await getDoc(blockRef);
const blockData = {
  ...fields,
  ...(existing.exists() ? {} : { createdAt: serverTimestamp() }),
};
await setDoc(blockRef, blockData, { merge: true });
```

Replace with:

```ts
// NEW:
const blockData = { ...fields, updatedAt: serverTimestamp() };
await setDoc(blockRef, blockData, { merge: true });
```

Apply the same fix to `savePage` — find any `getDoc` existence check before the `setDoc` and remove it, keeping only the `setDoc` with `merge: true`.

After the change, check if `getDoc` is still used anywhere else in the file. If `saveBlock` and `savePage` were the only callers, remove the `getDoc` import from the top of the file to avoid an unused import TypeScript warning.

**Verify:** `npm run build` passes.

---

### Task 2.3: WalletContext — Reset userInitiatedRef when wallet picker closes without selection
**File:** `src/app/contexts/WalletContext.tsx`

`userInitiatedRef` is set to `true` when the user clicks "Connect Wallet" but is never reset if the user opens the wallet picker modal and then closes it without selecting a wallet. This means the next auto-connect event (or any wallet state change) will be treated as user-initiated even though the user walked away.

Read the file and find the wallet picker modal's `onClose` handler. It currently looks like:

```ts
// OLD:
onClose={() => setShowPicker(false)}
```

Replace with:

```ts
// NEW:
onClose={() => { setShowPicker(false); userInitiatedRef.current = false; }}
```

If `onClose` is defined as a separate function rather than an inline arrow, add `userInitiatedRef.current = false;` as the first line of that function.

**Verify:** `npm run build` passes.

---

**Phase 2 Build Check:** `npm run build` must pass before continuing to Phase 3.

---

## Phase 3: Web Feature Fixes (7 tasks)

### Task 3.1: ProfileEditor — Sync originalUsernameRef when username becomes available
**File:** `src/app/app/components/ProfileEditor.tsx` (check the actual path — may be `src/app/components/ProfileEditor.tsx`)

`originalUsernameRef` captures the username at mount time but goes stale after a successful save — if the user saves a new username, the ref still holds the old value, breaking the "has username changed?" comparison on subsequent edits.

Read the file and find `originalUsernameRef`. Then find the `useEffect` that tracks username availability (it checks for `"available"` status). Add a sync to the ref inside or alongside that effect:

```ts
useEffect(() => {
  originalUsernameRef.current = profile.username;
}, [profile.username]);
```

Check if an effect with `[profile.username]` as a dependency already exists — if so, add the ref sync inside it rather than creating a duplicate effect.

**Verify:** `npm run build` passes.

---

### Task 3.2: ProfileEditor — Remove reserved username guard from onChange, keep only visual validation
**File:** `src/app/app/components/ProfileEditor.tsx` (check actual path)

The `isReservedUsername` check in the username `onChange` handler silently drops keystrokes when a user types a reserved word. From the user's perspective, the input just stops responding — confusing and hostile UX.

Read the file and find the username input `onChange` handler. It looks like:

```ts
// OLD:
onChange={(e) => {
  const value = e.target.value;
  if (!isReservedUsername(value)) {
    onUpdate({ ...profile, username: value });
  }
}}
```

Replace with:

```ts
// NEW:
onChange={(e) => {
  const value = e.target.value;
  onUpdate({ ...profile, username: value });
}}
```

Keep the `isReservedUsername` function itself — it should still be used for the visual validation message that tells the user the name is reserved. Do not remove any JSX that displays a "reserved username" error message.

**Verify:** `npm run build` passes.

---

### Task 3.3: ProfileEditor — Add min-length visual validation for username
**File:** `src/app/app/components/ProfileEditor.tsx` (check actual path)

Firestore rules require usernames to be at least 3 characters, but the UI does not validate this. Users can type 1-2 character usernames, attempt to save, and get a cryptic Firestore permission error.

Read the file and find where username validation messages are displayed (look for the existing "reserved username" or "taken" error message JSX). After the username input and near the existing validation messages, add:

```tsx
{profile.username.length > 0 && profile.username.length < 3 && (
  <p className="text-sm text-red-500 mt-1">Username must be at least 3 characters</p>
)}
```

Place this immediately after the username `<input>` element, alongside other validation messages. Do not replace any existing validation — add it as an additional conditional.

**Verify:** `npm run build` passes.

---

### Task 3.4: Dashboard — Dedup page slugs on rename
**File:** `src/app/pages/Dashboard.tsx`

`handleRenamePage` generates a slug from the new name but does not check if that slug already exists on another page. Renaming "Work" to match another page's slug creates a collision that breaks page routing.

Read the file and find `handleRenamePage`. Find where the slug is computed. It will look like:

```ts
const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, "-");
```

After that line, add a dedup check:

```ts
const existingSlugs = profile.pages.filter(p => p.id !== pageId).map(p => p.slug);
const finalSlug = existingSlugs.includes(slug) ? `${slug}-${Date.now()}` : slug;
```

Then replace any reference to `slug` in the Firestore save and state update within this function with `finalSlug`.

**Verify:** `npm run build` passes.

---

### Task 3.5: UserProfile — Fix empty displayName avatar placeholder
**File:** `src/app/pages/UserProfile.tsx`

`profile.displayName.charAt(0)` returns an empty string when `displayName` is empty. The avatar placeholder shows nothing.

Read the file and find the avatar placeholder — it is typically an element that shows the first character of the display name when no avatar image is set. It looks like:

```tsx
// OLD:
{profile.displayName.charAt(0)}
```

Replace with:

```tsx
// NEW:
{(profile.displayName || profile.username || "?").charAt(0).toUpperCase()}
```

Also search for the same pattern in `ProfileEditor.tsx` (the editor may have its own avatar placeholder with the same bug). Apply the same fix there if found.

**Verify:** `npm run build` passes.

---

### Task 3.6: Landing — Fix false social proof copy
**File:** Find the landing page file — check `src/app/pages/Landing.tsx`, `src/app/pages/Home.tsx`, or `src/app/pages/LandingPage.tsx`

The text "Join thousands of creators" is false — the product is not yet at that scale. This is a credibility issue.

Read the file and find that string. Replace it:

```tsx
// OLD:
Join thousands of creators
// NEW:
Start building your Web3 profile
```

The replacement should preserve the surrounding JSX exactly — only change the text content of that element.

**Verify:** `npm run build` passes.

---

### Task 3.7: Landing — Remove dead social links section from footer
**VERIFY FIRST:** Read the landing page file. Check if the footer social links section (the one with `href="#"` anchor tags from round 3) was already removed. If it was already removed in round 3, skip this task.

If the social links section still exists but the individual `<a href="#">` anchors were removed (leaving an empty container), remove the entire container element too. The goal is a clean footer with no dead link section at all.

If the entire social links section is already gone, skip.

**Verify:** `npm run build` passes.

---

**Phase 3 Build Check:** `npm run build` must pass before continuing to Phase 4.

---

## Phase 4: Cleanup and Polish (2 tasks)

### Task 4.1: package.json — Remove dead dependencies
**File:** `package.json`

`react-dnd`, `react-dnd-html5-backend`, and `motion` are listed as dependencies but are not used anywhere in the codebase. They add unnecessary bundle weight and install time.

Run:
```bash
cd /Users/charlesvincent/Desktop/GitHub/W3nto && npm uninstall react-dnd react-dnd-html5-backend motion
```

After uninstalling, verify no import of these packages exists anywhere in `src/`:

```bash
grep -r "react-dnd\|motion" /Users/charlesvincent/Desktop/GitHub/W3nto/src --include="*.ts" --include="*.tsx" -l
```

If any files still import from these packages, either remove the imports (if the import is genuinely unused) or do NOT uninstall that package. Do not break working code to satisfy this task.

**Verify:** `npm run build` passes.

---

### Task 4.2: TokenChart — Remove unused chainId from destructured props
**File:** Find the TokenChart component — check `src/app/components/blocks/TokenChart.tsx` or `src/app/components/TokenChart.tsx`

`chainId` is destructured from props but never used in the component body. This produces a TypeScript unused variable warning.

Read the file and find the props destructuring. It looks like:

```ts
// OLD:
const { token, chainId, ... } = props;
// or:
function TokenChart({ token, chainId, ... }) {
```

Remove `chainId` from the destructuring. If `chainId` is part of a typed props interface, also remove it from the interface definition — but only if it is not consumed anywhere in the component and removing it from the interface does not break any callers that pass it in.

If removing `chainId` from the interface would break callers, instead just prefix it with an underscore in the destructuring to suppress the warning:

```ts
const { token, chainId: _chainId, ... } = props;
```

**Verify:** `npm run build` passes with no unused variable warnings for this file.

---

**Phase 4 Build Check:** `npm run build` must pass before continuing to Phase 5.

---

## Phase 5: Final Verification and Commit

### Task 5.1: Final Build Verification

Run BOTH builds and confirm they pass:

```bash
npm run build
cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug
```

Both must succeed with zero errors. Fix any compilation errors before committing.

---

### Task 5.2: Commit All Changes

Stage all modified files and commit with this exact message:

```
fix: round 4 — favorites auth reactivity, delete data integrity, save optimization, UX polish
```

Use the Conventional Commits format. Co-author line required:
```
Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>
```

Use a HEREDOC for the commit message to preserve formatting exactly.

---

## Execution Order

1. Phase 1 (Tasks 1.1-1.9) — Android fixes
2. Phase 2 (Tasks 2.1-2.3) — Web critical fixes
3. Phase 3 (Tasks 3.1-3.7) — Web feature fixes
4. Phase 4 (Tasks 4.1-4.2) — Cleanup and polish
5. Phase 5 (Tasks 5.1-5.2) — Final builds and commit

## Important Notes
- **Read every file before editing** — line numbers and code snippets in this document are approximate
- **Verify-first tasks** — read the file and skip if the fix is already applied (prior rounds may have landed some of these)
- **Check imports** before adding new ones — many may already exist
- **Run the build** after each phase to catch errors early
- **Do not touch** anything already fixed in previous rounds (listed in "What Was Already Fixed" above)
- You have full Bash access — run npm, gradle, git, any command needed
- Total tasks: 21 across 5 phases (some may be skipped if verify-first check passes)
- Working directory: `/Users/charlesvincent/Desktop/GitHub/W3nto`
