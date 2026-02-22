# Ralph's Mission: W3nto Android App — Post-Audit Fix Pass

## Your Goal
Fix all remaining issues from the production readiness audit. Tasks 1-6 from the previous mission (BentoGrid rewrite, block card parity, theme-aware rendering, landing/connect polish, dApp Store prep, edge cases) were **mostly completed**. This mission targets the **bugs and gaps discovered during the deep audit**.

Read `CLAUDE.md` for full architecture context. Read `AUDIT_REPORT.md` for the full findings.

---

## Current State
- BentoGrid: custom Layout composable with first-fit algorithm — DONE
- Block cards: gradient/image/color backgrounds, icon chain — DONE
- Theme cascade: full theme applied to profile screen — DONE
- Landing + ConnectWallet: Seeker polish — DONE
- Build config: release signing, R8 shrinking — DONE
- Shimmer loading, error vs not-found states — DONE
- Coil crossfade on all AsyncImage calls — DONE
- Color tokens (no hardcoded hex) — DONE

---

## Task 1: Fix Block Model — `imageUrl` Field (P0)

**File:** `data/model/Block.kt`

The web app writes IMAGE blocks with `imageUrl` as a top-level Firestore field. Android's `Block` class doesn't have this field — images created via the web dashboard render blank on Android.

**Steps:**
1. Open `Block.kt` and add `val imageUrl: String? = null` to the data class constructor
2. In `fromMap()`, add: `imageUrl = map["imageUrl"] as? String`
3. In `toMap()`, add: `"imageUrl" to imageUrl`
4. Open `ui/components/blocks/ImageBlock.kt` and update the URL resolution:
   ```kotlin
   val imageUrl = block.imageUrl ?: block.bgImage ?: block.url ?: block.content
   ```

**Verify:** Build compiles.

---

## Task 2: Fix Deep Link Route Clearing (P0)

**File:** `ui/navigation/NavGraph.kt`, `MainActivity.kt`

The `pendingDeepLinkRoute` StateFlow is never set to null after navigation. The deep link re-triggers on every recomposition.

**Steps:**
1. In `NavGraph.kt`, add an `onDeepLinkConsumed: () -> Unit` parameter to the `W3ntoNavGraph` composable
2. Inside the `LaunchedEffect` that navigates on deep link, call `onDeepLinkConsumed()` after `navController.navigate(route)`
3. In `MainActivity.kt`, add a public method:
   ```kotlin
   fun consumeDeepLinkRoute() {
       _pendingDeepLinkRoute.value = null
   }
   ```
4. At the NavGraph call site in `MainActivity`, wire: `onDeepLinkConsumed = { consumeDeepLinkRoute() }`

**Verify:** Build compiles. Deep link navigates once and doesn't re-trigger.

---

## Task 3: Fix FavoritesViewModel Auth Guard (P0)

**File:** `ui/screens/favorites/FavoritesViewModel.kt`

When `auth.currentUser` is null, `loadFavorites()` returns silently but `isLoading` stays `true` — infinite spinner.

**Steps:**
1. In `FavoritesUiState`, add: `val requiresAuth: Boolean = false`
2. In `loadFavorites()`, change the early return:
   ```kotlin
   val userId = auth.currentUser?.uid
   if (userId == null) {
       _uiState.value = FavoritesUiState(isLoading = false, requiresAuth = true)
       return
   }
   ```
3. In `FavoritesScreen.kt`, add a `requiresAuth` state handler that shows "Connect your wallet to save favorites" with a CTA.

**Verify:** Build compiles.

---

## Task 4: ProGuard Rules (P0)

**File:** `android/app/proguard-rules.pro`

Release build will crash on Phantom encryption because Lazysodium uses JNA reflection.

**Steps:**
Add to `proguard-rules.pro`:
```
# JNA / Lazysodium — required for Phantom X25519 encryption
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-dontwarn com.sun.jna.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# MWA
-keep class com.solana.mobilewalletadapter.** { *; }
```

**Verify:** `./gradlew assembleRelease` succeeds (if keystore is configured) or at least `assembleDebug` with minify.

---

## Task 5: Fix Theme.borderRadius Parsing (P1)

**File:** `data/model/Theme.kt`

The web's `defaultTheme()` writes `borderRadius: "16px"` (string with px suffix). Android casts as Number, gets null, falls back to 16. Works for default but breaks custom values stored as strings.

**Steps:**
In `Theme.fromMap()`, replace:
```kotlin
borderRadius = (map["borderRadius"] as? Number)?.toInt() ?: 16,
```
with:
```kotlin
borderRadius = (map["borderRadius"] as? Number)?.toInt()
    ?: (map["borderRadius"] as? String)?.filter { it.isDigit() }?.toIntOrNull()
    ?: 16,
```

**Verify:** Build compiles.

---

## Task 6: Fix MainScope Leak (P1)

**File:** `MainActivity.kt`

`MainScope()` is never cancelled on Activity recreation.

**Steps:**
1. Remove `private val scope = MainScope()`
2. Add import: `import androidx.lifecycle.lifecycleScope`
3. Replace all `scope.launch { ... }` with `lifecycleScope.launch { ... }`

**Verify:** Build compiles.

---

## Task 7: Fix SagaSeedVault Identity Icon (P1)

**File:** `wallet/saga/SagaSeedVault.kt`

`APP_IDENTITY_ICON` is `Uri.parse("favicon.ico")` — relative URI, wallet can't fetch it.

**Steps:**
Change line 23:
```kotlin
private val APP_IDENTITY_ICON = Uri.parse("https://w3nto.app/favicon.ico")
```

**Verify:** Build compiles.

---

## Task 8: IMAGE/NFT Full-Bleed Rendering (P1)

**File:** `ui/components/BlockCard.kt`

IMAGE and NFT blocks render inside BlockCard's 12dp padding instead of being full-bleed.

**Steps:**
In `BlockCard.kt`, the content `Box` at the bottom has `padding(12.dp)`. For IMAGE and NFT block types, this padding should be 0.

Replace:
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(12.dp)
) {
    when (block.type) {
```
with:
```kotlin
val contentPadding = when (block.type) {
    BlockType.IMAGE, BlockType.NFT -> 0.dp
    else -> 12.dp
}
Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
) {
    when (block.type) {
```

**Verify:** Build compiles. IMAGE blocks fill their entire card area.

---

## Task 9: Cache BentoGrid Placements (P1)

**File:** `ui/components/BentoGrid.kt`

The grid placement algorithm recalculates on every measure pass.

**Steps:**
Move the placement calculation out of the `Layout` measure lambda. Use `remember` keyed on the block list and available width:

1. Before the `Layout` composable, calculate placements:
```kotlin
// Pre-compute outside Layout to avoid recalculating every measure pass
val blockSpans = remember(sortedBlocks) {
    sortedBlocks.map { block ->
        block.width.coerceIn(1, GRID_COLUMNS) to block.height.coerceIn(1, 4)
    }
}
```

2. Inside the measure lambda, use `blockSpans` instead of re-reading `sortedBlocks[i].width`/`.height`.

The full placement algorithm can stay in the measure lambda since it depends on `constraints.maxWidth` (which changes on rotation), but the block-span extraction should be pre-computed.

**Verify:** Build compiles.

---

## Task 10: Remove Dead Code (P2)

**Steps:**
1. Delete `ui/screens/auth/AuthViewModel.kt` if it exists (never wired to any screen)
2. Delete `ui/screens/landing/LandingViewModel.kt` if it exists (never used)
3. Delete `ui/components/ColorPickerDialog.kt` if it exists (editor component, viewer app)
4. Delete `ui/components/ImageUploader.kt` if it exists (editor component, viewer app)
5. In `AndroidManifest.xml`, remove `CAMERA` and `READ_MEDIA_IMAGES` permissions if they exist and are only used by the deleted components
6. In `BlockCard.kt`, simplify `combinedClickable` to `.clickable { onClick() }` since `isEditMode` is always false. Remove `@OptIn(ExperimentalFoundationApi::class)`.

**Verify:** Build compiles.

---

## Task 11: Fix `generateAuthMessage` Visibility (P1)

**File:** `wallet/WalletManager.kt`

`generateAuthMessage` is `public` but should not be callable from outside the wallet layer.

**Steps:**
Change `fun generateAuthMessage` to `internal fun generateAuthMessage`.

**Verify:** Build compiles.

---

## Task 12: Auth-Aware Navigation Guard (P1)

**File:** `ui/navigation/NavGraph.kt`

If auth completes while user is on Landing (e.g., Phantom callback arrives after back-press), user is stuck with no feedback.

**Steps:**
At the top of `W3ntoNavGraph`, observe auth state:
```kotlin
val firebaseUser by authRepository.authStateFlow().collectAsState(initial = authRepository.currentUser)

LaunchedEffect(firebaseUser) {
    if (firebaseUser != null) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == Screen.Landing.route || currentRoute == Screen.ConnectWallet.route) {
            navController.navigate(Screen.Discover.route) {
                popUpTo(Screen.Landing.route) { inclusive = true }
            }
        }
    }
}
```

This requires `AuthRepository` to be passed into NavGraph (or injected). Check how `NavGraph` currently gets its dependencies.

**Verify:** Build compiles.

---

## Task 13: Search Placeholder Text (P2)

**File:** `ui/screens/discover/DiscoverScreen.kt`

Search only works on `username` prefix but placeholder says "Search creators...".

**Steps:**
Change the search field placeholder to `"Search by username..."`.

**Verify:** Build compiles.

---

## Execution Order

1. **Task 1** (Block model imageUrl) — P0, data integrity
2. **Task 2** (Deep link clearing) — P0, navigation
3. **Task 3** (Favorites auth guard) — P0, UX
4. **Task 4** (ProGuard rules) — P0, release build
5. **Task 5** (borderRadius parsing) — P1, data
6. **Task 6** (MainScope leak) — P1, lifecycle
7. **Task 7** (SagaSeedVault icon) — P1, quick fix
8. **Task 8** (IMAGE/NFT full-bleed) — P1, visual
9. **Task 9** (BentoGrid caching) — P1, performance
10. **Task 10** (Dead code removal) — P2, cleanup
11. **Task 11** (Auth message visibility) — P1, quick fix
12. **Task 12** (Auth navigation guard) — P1, UX
13. **Task 13** (Search placeholder) — P2, quick fix

## Verification

After ALL tasks, the app must:
- Build: `cd android && ./gradlew assembleDebug`
- No crash-inducing patterns (verified by code review)
- All Firestore fields read correctly from web-created data

## Important Notes

- These tasks DO touch the data layer and wallet layer (unlike the previous mission) — the audit found bugs there
- Be careful with `NavGraph.kt` and `MainActivity.kt` changes — test deep link flow mentally
- Always verify build after each task
- Commit working states between tasks: `feat:`, `fix:`, `refactor:`
