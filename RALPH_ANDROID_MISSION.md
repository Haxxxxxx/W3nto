# Ralph's Mission: W3nto — Round 6 Final Polish

## Your Goal
A sixth audit found 11 bugs remaining after Round 5. These are the last issues blocking production readiness. Work through all 4 phases in order. Verify builds after each phase. Commit once at the end.

Read `CLAUDE.md` for full architecture context.

---

## What Was Already Fixed (DO NOT RE-DO)
Rounds 1–5 already completed all prior fixes. Do NOT re-do any of them. If you encounter code that already handles something, skip it.

---

## Phase 1: Android Critical Fixes (5 tasks)

### Task 1.1: Delete WalletModule entirely — fix duplicate Hilt bindings (P0)
**File:** `android/app/src/main/java/com/w3nto/app/di/WalletModule.kt`

Both `PhantomWallet` and `SagaSeedVault` have `@Singleton @Inject constructor(...)` annotations, meaning Hilt can wire them automatically. `WalletModule` also provides them manually via `@Provides`, creating duplicate bindings that cause a Hilt compile error.

**Fix:** Delete the entire `WalletModule.kt` file. Hilt will auto-wire both classes via their `@Inject constructor` annotations.

After deleting, verify no other file imports from `WalletModule`. Search for:
```
grep -r "WalletModule" android/app/src/main/java/ --include="*.kt" -l
```

If any file references `WalletModule`, remove the reference.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.2: Fix SagaSeedVault MWA transact API signature (P0)
**File:** `android/app/src/main/java/com/w3nto/app/wallet/saga/SagaSeedVault.kt`

The `authorize()` and `signMessage()` methods use a wrong lambda signature for `mwa.transact()`. The current code:

```kotlin
val block: suspend (AdapterOperations, AuthorizationResult) -> ByteArray =
    { _, authResult -> authResult.publicKey }
val result = mwa.transact(sender, null, block)
```

In MWA clientlib 2.0.3, `transact()` takes a single-argument lambda: `suspend (AdapterOperations) -> T`. The `AuthorizationResult` is returned from `ops.authorize()`, not passed as a parameter.

Replace the `authorize()` function body:

```kotlin
suspend fun authorize(sender: ActivityResultSender): String {
    mwa.rpcCluster = RpcCluster.Devnet

    val result = mwa.transact(sender) { ops ->
        val authResult = ops.authorize(
            APP_IDENTITY_URI,
            APP_IDENTITY_ICON,
            APP_IDENTITY_NAME,
            RpcCluster.Devnet
        )
        authResult.publicKey
    }

    return when (result) {
        is TransactionResult.Success -> phantomWallet.base58Encode(result.payload)
        is TransactionResult.NoWalletFound -> throw IllegalStateException("No MWA-compatible wallet found")
        is TransactionResult.Failure -> throw IllegalStateException("MWA authorization failed: ${result.e.message}")
    }
}
```

Replace the `signMessage()` function body:

```kotlin
suspend fun signMessage(sender: ActivityResultSender, message: ByteArray): String {
    mwa.rpcCluster = RpcCluster.Devnet

    val result = mwa.transact(sender) { ops ->
        val authResult = ops.authorize(
            APP_IDENTITY_URI,
            APP_IDENTITY_ICON,
            APP_IDENTITY_NAME,
            RpcCluster.Devnet
        )
        val signResult = ops.signMessages(
            arrayOf(message),
            arrayOf(authResult.publicKey)
        )
        signResult.signedPayloads.firstOrNull()
            ?: throw IllegalStateException("Wallet did not return a signed payload")
    }

    return when (result) {
        is TransactionResult.Success -> phantomWallet.base58Encode(result.payload)
        is TransactionResult.NoWalletFound -> throw IllegalStateException("No MWA-compatible wallet found")
        is TransactionResult.Failure -> throw IllegalStateException("MWA sign failed: ${result.e.message}")
    }
}
```

**Important:** After writing this code, try to compile. The exact API shape depends on the MWA clientlib version. If `ops.authorize()` takes different parameters, check the actual `AdapterOperations` class API. The key change is: lambda takes `(AdapterOperations)` only — NOT `(AdapterOperations, AuthorizationResult)`. Also remove the now-unused import for `AuthorizationResult` if no other code uses it.

Also check if `signResult.signedPayloads` is the correct field name. In some MWA versions it's `signedMessages`. If the build fails on this field, try `signedMessages` instead.

Remove the unused import `import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient.AuthorizationResult` if it's no longer needed. Also remove `import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity` and `import com.solana.mobilewalletadapter.clientlib.AdapterOperations` if they become unused after the refactor.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.3: Fix gradient direction mismatch — Android must parse CSS degrees (P1)
**File:** `android/app/src/main/java/com/w3nto/app/ui/components/BlockCard.kt`

The web app stores gradient directions as CSS degrees (`135deg`, `90deg`, `180deg`, `45deg`) in Firestore. The Round 5 Android fix expects string values like `to-b`, `to-br`, `to-tr`, `radial` — which are never actually stored. The `when` block will always fall through to the `else` (horizontal gradient).

Find the gradient direction `when` block and replace it to handle CSS degree values:

```kotlin
"gradient" -> {
    val gradient = block.bgGradient
    if (gradient != null) {
        val colors = listOf(parseColor(gradient.from), parseColor(gradient.to))
        val brush = when (gradient.direction) {
            "180deg", "to-b" -> Brush.verticalGradient(colors)
            "135deg", "to-br" -> Brush.linearGradient(
                colors = colors,
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
            "45deg", "to-tr" -> Brush.linearGradient(
                colors = colors,
                start = Offset(0f, Float.POSITIVE_INFINITY),
                end = Offset(Float.POSITIVE_INFINITY, 0f)
            )
            "radial" -> Brush.radialGradient(colors)
            else -> Brush.horizontalGradient(colors) // "90deg", "to-r", or any unknown
        }
        Box(modifier = Modifier.fillMaxSize().background(brush = brush))
    } else {
        Box(modifier = Modifier.fillMaxSize().background(bgColor))
    }
}
```

This handles both CSS degree format (what's actually stored) and the `to-*` format (for forward compatibility).

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.4: Fix DiscoverViewModel.loadMore() stale state (P1)
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/discover/DiscoverViewModel.kt`

Line 118 uses the captured `state` snapshot from before the coroutine launched:

```kotlin
fun loadMore() {
    val state = _uiState.value
    if (state.isLoadingMore || !state.hasMore || state.searchQuery.isNotBlank()) return
    if (loadMoreJob?.isActive == true) return
    loadMoreJob = viewModelScope.launch {
        _uiState.value = state.copy(isLoadingMore = true)  // <-- BUG: stale 'state'
```

Replace line 118 with:

```kotlin
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
```

This ensures any state changes that happened between `loadMore()` being called and the coroutine starting are preserved.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.5: Fix NavGraph deep link effect re-firing on every destination change (P1)
**File:** `android/app/src/main/java/com/w3nto/app/ui/navigation/NavGraph.kt`

The deep link `LaunchedEffect` uses `navController.currentDestination` as a key:

```kotlin
LaunchedEffect(deepLinkRoute, navController.currentDestination) {
```

This causes the effect to re-launch every time the user navigates anywhere. If `deepLinkRoute` hasn't been consumed yet (race condition window), the deep link navigation fires again.

Replace the key with only `deepLinkRoute`, and check destination readiness inside the effect:

```kotlin
LaunchedEffect(deepLinkRoute) {
    val route = deepLinkRoute ?: return@LaunchedEffect
    // Wait for NavHost to initialize
    while (navController.currentDestination == null) {
        kotlinx.coroutines.delay(50)
    }
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

This ensures the effect only runs once per deep link route change.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

**Phase 1 Build Check:** Run `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug`. Must pass before continuing to Phase 2.

---

## Phase 2: Web Critical Fixes (4 tasks)

### Task 2.1: BlockEditor — Add URL field for video and music block types (P1)
**File:** `src/app/components/BlockEditor.tsx`

The URL input field is only shown for `link` and `social` types (line 201). Video and music blocks need URLs (YouTube, Spotify, etc.) to render embeds, but creators have no way to enter them.

Find the URL field conditional:

```tsx
{(editedBlock.type === "link" || editedBlock.type === "social") && (
```

Replace with:

```tsx
{(editedBlock.type === "link" || editedBlock.type === "social" || editedBlock.type === "video" || editedBlock.type === "music") && (
```

Also update the placeholder text to be contextual. Replace the placeholder inside the URL input:

```tsx
placeholder={
  editedBlock.type === "video" ? "https://youtube.com/watch?v=..." :
  editedBlock.type === "music" ? "https://open.spotify.com/track/..." :
  "https://example.com"
}
```

**Verify:** `npm run build` passes.

---

### Task 2.2: ProfileEditor — Fix originalUsernameRef resetting on every keystroke (P1)
**File:** `src/app/components/ProfileEditor.tsx`

Lines 42-44 contain a `useEffect` that resets `originalUsernameRef.current` to `profile.username` on every change:

```ts
useEffect(() => {
  originalUsernameRef.current = profile.username;
}, [profile.username]);
```

This makes the availability checker (lines 17-40) always see `username === originalUsernameRef.current` and return early with `"idle"`. The username availability check never fires.

**Fix:** Delete lines 42-44 entirely (the entire `useEffect` block). The `originalUsernameRef` is already initialized correctly on line 15:

```ts
const originalUsernameRef = useRef(profile.username);
```

This captures the username at component mount time, which is the correct behavior — it should remain as the original username for the entire lifecycle of the editor session.

**Verify:** `npm run build` passes.

---

### Task 2.3: Dashboard/PageManager — Remove double confirm dialog on page delete (P1)
**File:** `src/app/pages/Dashboard.tsx`

`handleDeletePage` has a `window.confirm("Delete this page?")` guard. But `PageManager` (which calls `onDeletePage`) ALSO has its own `confirm()` dialog. Users see two confirmation prompts.

Find `handleDeletePage` in Dashboard.tsx. Remove the `window.confirm` from it:

```ts
// OLD:
const handleDeletePage = async (pageId: string) => {
  if (!profile || !window.confirm("Delete this page?")) return;
```

```ts
// NEW:
const handleDeletePage = async (pageId: string) => {
  if (!profile) return;
```

The confirm dialog in `PageManager.tsx` is the one users see first and is contextual (shows the page name). Keep that one.

**Verify:** `npm run build` passes.

---

### Task 2.4: UserProfile — Don't render hover overlay and ExternalLink badge on video/music iframes (P2)
**File:** `src/app/pages/UserProfile.tsx`

Every block gets a hover gradient overlay and an `ExternalLink` badge when it has a `url` field. For video and music blocks that render `<iframe>` elements, this visually covers the embed.

Find the overlay div and the ExternalLink badge. They are rendered for all blocks. Add a guard to skip them for video and music blocks that have working embeds.

Find the hover gradient overlay (look for `bg-gradient-to-t from-black/20`):

```tsx
<div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none"></div>
```

Wrap it in a conditional that excludes video and music blocks:

```tsx
{block.type !== "video" && block.type !== "music" && (
  <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none"></div>
)}
```

Find the ExternalLink badge (look for `ExternalLink` icon with `block.url`):

```tsx
{block.url && (
  // ExternalLink icon badge
)}
```

Add the same guard:

```tsx
{block.url && block.type !== "video" && block.type !== "music" && (
  // ExternalLink icon badge
)}
```

**Verify:** `npm run build` passes.

---

**Phase 2 Build Check:** `npm run build` must pass before continuing to Phase 3.

---

## Phase 3: Cross-Stack Data Integrity (1 task)

### Task 3.1: firestore.ts — Migrate favorites when username changes (P1)
**File:** `src/app/services/firestore.ts`

The `renameProfile` function migrates profiles, pages, blocks, and wallet lookups — but not favorites. Favorites are stored in `users/{uid}/favorites/{docId}` where `docId` is `"{username}:::{pageId}"` and the document contains a `username` field. After a rename, all users who favorited the old username have broken favorites.

Find the `renameProfile` function. After step 5 (wallet reverse lookup update) and before step 6 (delete old data), add a favorites migration step:

```ts
// 5.5. Update favorites across all users that reference the old username
// Note: This requires querying across all users' favorites subcollections.
// Firestore doesn't support collection group queries on subcollections with
// arbitrary parent paths efficiently. For now, we skip this — favorites
// will gracefully fail (profile not found) and users can re-favorite.
// TODO: Implement via Cloud Function with collectionGroup query when needed.
```

This is a known limitation — a proper fix requires a Cloud Function with `collectionGroup("favorites")` query filtering on `username == oldUsername`. For now, add the comment documenting the gap.

However, we CAN fix the Android viewer to handle this gracefully. The favorites screen should handle the case where a favorited profile no longer exists. But that's an Android-side fix, not a web fix. Just add the TODO comment in `renameProfile`.

**Verify:** `npm run build` passes.

---

**Phase 3 Build Check:** `npm run build` must pass before continuing to Phase 4.

---

## Phase 4: Final Verification and Commit (2 tasks)

### Task 4.1: Final Build Verification

Run BOTH builds and confirm they pass:

```bash
cd /Users/charlesvincent/Desktop/GitHub/W3nto && npm run build
cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug
```

Both must succeed with zero errors. Fix any compilation errors before committing.

---

### Task 4.2: Commit All Changes

Stage all modified files (and deleted files) and commit with this exact message:

```
fix: round 6 — Hilt DI cleanup, MWA API fix, gradient direction, video/music editor, availability checker
```

Use the Conventional Commits format. Co-author line required:
```
Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

Use a HEREDOC for the commit message to preserve formatting exactly.

---

## Execution Order

1. Phase 1 (Tasks 1.1-1.5) — Android critical fixes
2. Phase 2 (Tasks 2.1-2.4) — Web critical fixes
3. Phase 3 (Task 3.1) — Cross-stack data integrity
4. Phase 4 (Tasks 4.1-4.2) — Final builds and commit

## Important Notes
- **Read every file before editing** — line numbers and code snippets are approximate
- **Check imports** before adding new ones — many may already exist
- **Run the build** after each phase to catch errors early
- **Task 1.1 deletes a file** — use `rm` or `git rm` for WalletModule.kt
- **Task 1.2 is tricky** — the MWA API may differ from what's documented. Compile and fix iteratively.
- You have full Bash access — run npm, gradle, git, any command needed
- Total tasks: 12 across 4 phases
- Working directory: `/Users/charlesvincent/Desktop/GitHub/W3nto`
