# Ralph's Mission: W3nto — Round 5 Wiring & Rendering Polish

## Your Goal
A fifth audit focused on frontend-backend wiring and rendering correctness found 14 issues across Android and Web. Work through all 5 phases in order. Verify builds after each phase. Commit once at the end.

Read `CLAUDE.md` for full architecture context.

---

## What Was Already Fixed (DO NOT RE-DO)
Rounds 1–4 (commits `2812f48` through `ddabbee`) already completed these. Skip them entirely:
- Firestore rules: uid check on create for profiles and wallets
- Cloud Function: unconditional timestamp check, publicKey/signature byte-length validation
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
- parseColor wrapped in remember() in PublicProfileScreen, ProfileHeader, PageSelector
- Wallet mapping write format aligned (username + uid + updatedAt)
- CLAUDE.md auth message format updated
- onProfileSnapshot dead function deleted from firestore.ts
- BentoGrid: guard against zero availableWidth
- FirestoreDataSource: `doc.data!!` force-unwrap fixed in getPages and getBlocks
- WalletManager: safe cast for Cloud Function response
- SagaSeedVault: guard against empty signedPayloads
- Legacy favorites methods removed
- searchProfiles: fixed to use FieldPath.documentId()
- Firestore rules: username format validation added
- Firebase Hosting: security headers added
- Cloud Function: publicKey and signature byte-length validation
- ConnectWalletScreen: removed double-navigation LaunchedEffect
- FavoritesScreen: fixed double Scaffold padding
- ProfileViewModel: error shown when unauthenticated user taps favorite
- NavGraph: deep link navigation wrapped in try/catch with 100ms delay
- DiscoverRepository: @Volatile replaced with Mutex
- WalletContext: wallet adapters wired, autoConnect=false, disconnect awaits signOut
- UserProfile: false "not found" for zero-page profiles separated
- Dashboard: stale closure in debounced save fixed with saveVersionRef
- Dashboard: saveTimeoutRef cleanup on unmount added
- Dashboard: duplicate page slugs fixed (Date.now() slug)
- Dashboard: window.confirm() before block delete
- Dashboard: block position renormalization after delete
- Dashboard: user-visible error feedback for delete failures (toast.error)
- socialPlatforms.ts: React import and ComponentType typed correctly
- ProfileEditor: avatar URL validation (https:// only)
- AndroidManifest: allowBackup set to false
- MainActivity: deep link path segment validation and length clamping
- ConnectWalletScreen: removed unused onConnected callback parameter
- WalletManager: removed unused firestore constructor parameter, @Volatile on pending fields
- ConnectWalletScreen: clearError() called at start of wallet button clicks
- FavoritesScreen/NavGraph: malformed route fixed when pageSlug is empty
- ProfileViewModel: isLoading set during page switch with proper clear
- ProfileViewModel: error auto-cleared after 3 seconds
- TextBlock: maxLines = 6 added
- PhantomWallet: removed unused android.util.Base64 import
- AuthRepository: dead signInAnonymously() method removed
- StorageRepository and StorageDataSource deleted (viewer-only app)
- ProfileRepository: write-only dead methods removed
- PageRepository: savePage and deletePage removed
- BlockRepository: saveBlock and deleteBlock removed
- FirestoreDataSource: write methods with no remaining callers removed
- AppModule: redundant Context binding deleted
- ImageBlock: block.content removed from URL fallback, null placeholder added
- SagaSeedVault: MWA 2.0 field name verified/fixed
- NavGraph: back navigation falls back to Discover for deep-link entries
- FavoritesViewModel: reacts to auth state changes
- DiscoverViewModel: loadMore() Job guard added
- NavGraph: 100ms delay replaced with currentDestination guard
- Dashboard: handleDeleteBlock/handleDeletePage await Firestore before state update
- firestore.ts: pre-read getDoc removed from saveBlock/savePage
- WalletContext: userInitiatedRef reset on picker close
- ProfileEditor: originalUsernameRef synced on save
- ProfileEditor: reserved username guard moved to visual-only validation
- ProfileEditor: min-length visual validation for username
- Dashboard: page slug dedup on rename
- UserProfile: empty displayName avatar placeholder fallback
- Landing: false social proof copy replaced
- Landing: dead social links section removed
- package.json: dead dependencies removed (react-dnd, motion)
- TokenChart: unused chainId removed from destructured props

---

## Current State
The codebase has been hardened through four full rounds. This fifth audit focuses specifically on **rendering correctness** and **frontend-backend wiring gaps** — places where the UI doesn't match the data model or where data flows break under real usage conditions.

For tasks marked **"VERIFY FIRST"** — read the file before editing and skip the task if the fix is already applied.

---

## Phase 1: Android Rendering Fixes (4 tasks)

### Task 1.1: BlockCard — Support gradient directions (P0)
**File:** `android/app/src/main/java/com/w3nto/app/ui/components/BlockCard.kt`

The gradient background always renders as `Brush.horizontalGradient()` regardless of the `gradient.direction` field from Firestore. The web app supports `to-b` (top-to-bottom), `to-br` (top-left to bottom-right), `to-tr` (bottom-left to top-right), and `radial`. The Android app ignores direction entirely.

Read the file and find the gradient rendering code. It currently looks like:

```kotlin
"gradient" -> {
    val gradient = block.bgGradient
    if (gradient != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            parseColor(gradient.from),
                            parseColor(gradient.to)
                        )
                    )
                )
        )
    } else {
        Box(modifier = Modifier.fillMaxSize().background(bgColor))
    }
}
```

Replace the gradient rendering with direction-aware brushes:

```kotlin
"gradient" -> {
    val gradient = block.bgGradient
    if (gradient != null) {
        val colors = listOf(parseColor(gradient.from), parseColor(gradient.to))
        val brush = when (gradient.direction) {
            "to-b" -> Brush.verticalGradient(colors)
            "to-br" -> Brush.linearGradient(
                colors = colors,
                start = Offset.Zero,
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
            "to-tr" -> Brush.linearGradient(
                colors = colors,
                start = Offset(0f, Float.POSITIVE_INFINITY),
                end = Offset(Float.POSITIVE_INFINITY, 0f)
            )
            "radial" -> Brush.radialGradient(colors)
            else -> Brush.horizontalGradient(colors)
        }
        Box(modifier = Modifier.fillMaxSize().background(brush = brush))
    } else {
        Box(modifier = Modifier.fillMaxSize().background(bgColor))
    }
}
```

Check the data model — find the `Gradient` data class (or equivalent) in the Block model. Verify it has a `direction` field. If it does not, add the field:
- Find `Block.kt` or wherever `Gradient`/`BgGradient` is defined
- Add `val direction: String = "to-r"` with a default value
- Ensure `fromMap()` reads `direction` from the Firestore map

Check imports — `Offset` requires `androidx.compose.ui.geometry.Offset`. Add if missing. `Brush.radialGradient` and `Brush.linearGradient` are in `androidx.compose.ui.graphics.Brush` which should already be imported.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.2: ProfileViewModel — Fallback to default page when pageSlug is invalid
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/profile/ProfileViewModel.kt`

When a deep link contains an invalid `pageSlug` (e.g., a deleted page or typo), `pages.find { it.slug == pageSlug }` returns null and `currentPage` is null. The UI shows an empty profile with no blocks and no page selector.

Read the file and find the page resolution logic in `loadProfile`. It currently looks like:

```kotlin
val currentPage = if (pageSlug != null) {
    pages.find { it.slug == pageSlug }
} else {
    pages.find { it.id == profile.defaultPageId } ?: pages.firstOrNull()
}
```

Replace with a fallback chain:

```kotlin
val currentPage = if (pageSlug != null) {
    pages.find { it.slug == pageSlug }
        ?: pages.find { it.id == profile.defaultPageId }
        ?: pages.firstOrNull()
} else {
    pages.find { it.id == profile.defaultPageId } ?: pages.firstOrNull()
}
```

This ensures an invalid slug falls back to the default page, then to any available page.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.3: FirestoreDataSource — Make getPages ordering resilient to missing createdAt
**File:** `android/app/src/main/java/com/w3nto/app/data/remote/FirestoreDataSource.kt`

`getPages` orders by `createdAt` ASCENDING, but pages created from the web app may not have a `createdAt` field (the web's `savePage` uses `merge: true` and may not always set it). Firestore silently excludes documents that lack the `orderBy` field, making those pages invisible.

Read the file and find the `getPages` method:

```kotlin
suspend fun getPages(username: String): List<BentoPage> {
    val snapshot = firestore.collection("profiles")
        .document(username)
        .collection("pages")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .get()
        .await()
    return snapshot.documents.mapNotNull { doc ->
        doc.data?.let { BentoPage.fromMap(doc.id, it) }
    }
}
```

Replace with ordering by `position` (which the web app always sets), falling back to document order:

```kotlin
suspend fun getPages(username: String): List<BentoPage> {
    val snapshot = firestore.collection("profiles")
        .document(username)
        .collection("pages")
        .get()
        .await()
    return snapshot.documents
        .mapNotNull { doc -> doc.data?.let { BentoPage.fromMap(doc.id, it) } }
        .sortedBy { it.position ?: Int.MAX_VALUE }
}
```

Check the `BentoPage` data class — verify it has a `position: Int?` field. If it doesn't, check what ordering field is available (could be `order`, `sortOrder`, etc.). Use whichever field exists. If no ordering field exists, add `val position: Int? = null` to `BentoPage` and parse it in `fromMap()`.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 1.4: DiscoverScreen — Fix "No creators yet" showing alongside loading spinner
**File:** `android/app/src/main/java/com/w3nto/app/ui/screens/discover/DiscoverScreen.kt`

**VERIFY FIRST:** Read the file and check the conditional logic for the empty state.

The empty state text "No creators yet. Check back soon!" can appear even when `isLoading` is true during the initial load, because the `when` block may not be mutually exclusive with the LazyColumn's empty-list check.

Find the empty state item inside the LazyColumn:

```kotlin
if (uiState.profiles.isEmpty() && !uiState.isLoading) {
    item {
        // "No creators yet" text
    }
}
```

If the `&& !uiState.isLoading` guard is already present, this is fixed — skip this task. If it's missing, add it.

Also verify the `when` block at the top properly guards — the `isLoading` branch should prevent the LazyColumn from rendering at all during initial load:

```kotlin
when {
    uiState.isLoading -> {
        // spinner only, LazyColumn not shown
    }
    // ... other branches
}
```

If the LazyColumn is always rendered (not inside a `when` branch), then the `!uiState.isLoading` guard on the empty state item is the critical fix.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

**Phase 1 Build Check:** Run `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug`. Must pass before continuing to Phase 2.

---

## Phase 2: Web Rendering Fixes (4 tasks)

### Task 2.1: Dashboard — Username change must migrate Firestore data (P0)
**File:** `src/app/services/firestore.ts`

When a user changes their username, `saveProfile()` writes to `profiles/{newUsername}` but never deletes `profiles/{oldUsername}`. All pages and blocks under the old username are orphaned permanently. The wallet reverse lookup also points to the old username.

Add a `renameProfile` function to `firestore.ts`:

```ts
export async function renameProfile(oldUsername: string, newUsername: string, uid: string): Promise<void> {
  const oldRef = doc(db, "profiles", oldUsername);
  const newRef = doc(db, "profiles", newUsername);

  // 1. Check new username isn't taken
  const newSnap = await getDoc(newRef);
  if (newSnap.exists()) throw new Error("Username already taken");

  // 2. Read old profile
  const oldSnap = await getDoc(oldRef);
  if (!oldSnap.exists()) throw new Error("Profile not found");
  const profileData = oldSnap.data();
  if (profileData.uid !== uid) throw new Error("Not authorized");

  // 3. Copy profile doc to new username
  await setDoc(newRef, { ...profileData, username: newUsername, updatedAt: serverTimestamp() });

  // 4. Copy all pages and their blocks
  const pagesSnap = await getDocs(collection(db, "profiles", oldUsername, "pages"));
  for (const pageDoc of pagesSnap.docs) {
    const pageRef = doc(db, "profiles", newUsername, "pages", pageDoc.id);
    await setDoc(pageRef, pageDoc.data());
    const blocksSnap = await getDocs(collection(db, "profiles", oldUsername, "pages", pageDoc.id, "blocks"));
    for (const blockDoc of blocksSnap.docs) {
      await setDoc(doc(db, "profiles", newUsername, "pages", pageDoc.id, "blocks", blockDoc.id), blockDoc.data());
    }
  }

  // 5. Update wallet reverse lookup
  if (profileData.walletAddress) {
    await setDoc(doc(db, "wallets", profileData.walletAddress), {
      username: newUsername, uid, updatedAt: serverTimestamp()
    }, { merge: true });
  }

  // 6. Delete old profile and subcollections
  for (const pageDoc of pagesSnap.docs) {
    const blocksSnap = await getDocs(collection(db, "profiles", oldUsername, "pages", pageDoc.id, "blocks"));
    for (const blockDoc of blocksSnap.docs) {
      await deleteDoc(blockDoc.ref);
    }
    await deleteDoc(pageDoc.ref);
  }
  await deleteDoc(oldRef);
}
```

Check that all Firestore imports used above are already imported at the top of the file (`getDoc`, `setDoc`, `getDocs`, `deleteDoc`, `collection`, `doc`, `serverTimestamp`). Add any missing ones.

**File:** `src/app/pages/Dashboard.tsx`

Now wire the rename into the Dashboard. Find where the username save happens — look for where `saveProfile` is called and the username may have changed. This is likely in `debouncedSave` or `handleSave`.

Add a check before `saveProfile`:

```ts
// Before calling saveProfile, check if username changed
if (profile.username !== originalUsernameRef.current && originalUsernameRef.current) {
  try {
    await firestoreService.renameProfile(originalUsernameRef.current, profile.username, user.uid);
    originalUsernameRef.current = profile.username;
  } catch (err: any) {
    toast.error(err.message || "Failed to rename profile");
    return;
  }
} else {
  await firestoreService.saveProfile(profile);
}
```

Check if `originalUsernameRef` already exists in Dashboard.tsx. If not, add it:
```ts
const originalUsernameRef = useRef<string | null>(null);
```
And set it when the profile first loads:
```ts
useEffect(() => {
  if (profile?.username && !originalUsernameRef.current) {
    originalUsernameRef.current = profile.username;
  }
}, [profile?.username]);
```

**Verify:** `npm run build` passes.

---

### Task 2.2: UserProfile — Add basic video embed for VIDEO blocks
**File:** `src/app/pages/UserProfile.tsx`

VIDEO blocks currently show a static emoji placeholder. If the block has a `url` field containing a video URL, render an actual video player.

Find the VIDEO block rendering (the section with `block.type === "video"`):

```tsx
// OLD:
if (block.type === "video" && block.url) {
  return (
    // emoji + title + content (no actual video)
  );
}
```

Replace with:

```tsx
if (block.type === "video") {
  const videoUrl = block.url;
  if (videoUrl) {
    // Check for YouTube/Vimeo embeds
    const youtubeMatch = videoUrl.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([a-zA-Z0-9_-]+)/);
    const vimeoMatch = videoUrl.match(/vimeo\.com\/(\d+)/);
    if (youtubeMatch) {
      return (
        <iframe
          src={`https://www.youtube.com/embed/${youtubeMatch[1]}`}
          className="w-full h-full rounded-xl"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
          title={block.title || "Video"}
        />
      );
    }
    if (vimeoMatch) {
      return (
        <iframe
          src={`https://player.vimeo.com/video/${vimeoMatch[1]}`}
          className="w-full h-full rounded-xl"
          allow="autoplay; fullscreen; picture-in-picture"
          allowFullScreen
          title={block.title || "Video"}
        />
      );
    }
    // Direct video URL
    return (
      <video
        src={videoUrl}
        controls
        className="w-full h-full object-cover rounded-xl"
        title={block.title || "Video"}
      />
    );
  }
  // No URL — show placeholder
  return (
    <div className="flex flex-col items-center justify-center h-full text-center p-4 opacity-60">
      <span className="text-3xl mb-2">🎥</span>
      <span className="text-sm font-medium">{block.title || "Video"}</span>
    </div>
  );
}
```

**Verify:** `npm run build` passes.

---

### Task 2.3: UserProfile — Add basic audio embed for MUSIC blocks
**File:** `src/app/pages/UserProfile.tsx`

MUSIC blocks show a static emoji. If the block has a `url` field, render an audio player or Spotify/SoundCloud embed.

Find the MUSIC block rendering:

```tsx
if (block.type === "music") {
  return (
    // emoji + title (no actual audio)
  );
}
```

Replace with:

```tsx
if (block.type === "music") {
  const musicUrl = block.url;
  if (musicUrl) {
    // Spotify embed
    const spotifyMatch = musicUrl.match(/open\.spotify\.com\/(track|album|playlist)\/([a-zA-Z0-9]+)/);
    if (spotifyMatch) {
      return (
        <iframe
          src={`https://open.spotify.com/embed/${spotifyMatch[1]}/${spotifyMatch[2]}`}
          className="w-full h-full rounded-xl"
          allow="autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture"
          allowFullScreen
          title={block.title || "Music"}
        />
      );
    }
    // SoundCloud embed
    if (musicUrl.includes("soundcloud.com")) {
      return (
        <iframe
          src={`https://w.soundcloud.com/player/?url=${encodeURIComponent(musicUrl)}&color=%23ff5500&auto_play=false&hide_related=true&show_comments=false&show_user=true&show_reposts=false&show_teaser=false&visual=true`}
          className="w-full h-full rounded-xl"
          allow="autoplay"
          title={block.title || "Music"}
        />
      );
    }
    // Direct audio URL
    return (
      <div className="flex flex-col items-center justify-center h-full p-4">
        <span className="text-2xl mb-3">🎵</span>
        <span className="text-sm font-medium mb-2">{block.title || "Music"}</span>
        <audio src={musicUrl} controls className="w-full max-w-[200px]" />
      </div>
    );
  }
  // No URL — placeholder
  return (
    <div className="flex flex-col items-center justify-center h-full text-center p-4 opacity-60">
      <span className="text-3xl mb-2">🎵</span>
      <span className="text-sm font-medium">{block.title || "Music"}</span>
    </div>
  );
}
```

**Verify:** `npm run build` passes.

---

### Task 2.4: firestore.ts — Fetch block subcollections in parallel instead of sequentially
**File:** `src/app/services/firestore.ts`

`getPages()` fetches blocks for each page in a sequential `for` loop. With 5 pages, this is 5 sequential Firestore queries that could run in parallel.

Find the `getPages` function:

```ts
// OLD (sequential):
for (const pageDoc of snap.docs) {
    const blocks = await getBlocks(username, pageDoc.id);
    // ... build page object
}
```

Replace the sequential loop with `Promise.all`:

```ts
// NEW (parallel):
const pages = await Promise.all(
  snap.docs.map(async (pageDoc) => {
    const blocks = await getBlocks(username, pageDoc.id);
    const data = pageDoc.data();
    return {
      id: pageDoc.id,
      name: data.name || "",
      slug: data.slug || pageDoc.id,
      theme: data.theme || null,
      isFavorite: data.isFavorite || false,
      position: data.position ?? 0,
      blocks,
    } as BentoPage;
  })
);
return pages.sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
```

Read the file carefully — the BentoPage construction may have different field names. Match exactly what's currently there, just change the sequential loop to `Promise.all`. Preserve the existing page object shape.

**Verify:** `npm run build` passes.

---

**Phase 2 Build Check:** `npm run build` must pass before continuing to Phase 3.

---

## Phase 3: Web Save Optimization (2 tasks)

### Task 3.1: Dashboard — Only save changed pages/blocks (dirty tracking)
**File:** `src/app/pages/Dashboard.tsx`

The auto-save writes ALL pages and ALL blocks on every 2-second debounce tick, even when only one block changed. This wastes Firestore writes.

Read the file and find the `debouncedSave` or `saveProfile` logic. The fix is to track which pages and blocks have been modified and only save those.

Add a dirty-tracking ref:

```ts
const dirtyPagesRef = useRef<Set<string>>(new Set());
const dirtyBlocksRef = useRef<Set<string>>(new Set()); // stores "pageId::blockId"
```

When a page is modified (find `handleRenamePage`, page theme changes, etc.), mark it dirty:
```ts
dirtyPagesRef.current.add(pageId);
```

When a block is modified (find block update handlers), mark it dirty:
```ts
dirtyBlocksRef.current.add(`${currentPageId}::${blockId}`);
```

Then in the save function, only write dirty items:

```ts
// Save only dirty pages
for (const pageId of dirtyPagesRef.current) {
  const page = profileToSave.pages.find(p => p.id === pageId);
  if (page) await firestoreService.savePage(profileToSave.username, page);
}

// Save only dirty blocks
for (const key of dirtyBlocksRef.current) {
  const [pageId, blockId] = key.split("::");
  const page = profileToSave.pages.find(p => p.id === pageId);
  const block = page?.blocks.find(b => b.id === blockId);
  if (block) await firestoreService.saveBlock(profileToSave.username, pageId, block);
}

// Always save profile metadata (it's one write, cheap)
await firestoreService.saveProfile(profileToSave);

// Clear dirty sets after successful save
dirtyPagesRef.current.clear();
dirtyBlocksRef.current.clear();
```

**Important:** Read the file carefully. The current save might call `saveProfile` which internally saves everything. If that's the case, you need to:
1. Keep the profile-level save (metadata only)
2. Add targeted page/block saves for dirty items only
3. Ensure `saveProfile` does NOT re-save all pages/blocks (check `firestore.ts`)

If `saveProfile` in `firestore.ts` saves pages/blocks in addition to profile metadata, split it: keep a `saveProfileMetadata` that only writes the profile doc, and use `savePage`/`saveBlock` for individual items.

**Verify:** `npm run build` passes.

---

### Task 3.2: Dashboard — Mark items dirty on every mutation
**File:** `src/app/pages/Dashboard.tsx`

This task depends on Task 3.1. Find EVERY place in Dashboard.tsx where pages or blocks are modified and add the dirty-tracking calls.

Places to mark pages dirty:
- `handleRenamePage` — `dirtyPagesRef.current.add(pageId)`
- `handleAddPage` — `dirtyPagesRef.current.add(newPage.id)`
- Any page theme/slug/position change

Places to mark blocks dirty:
- `handleUpdateBlock` / block field changes — `dirtyBlocksRef.current.add(\`${currentPageId}::${blockId}\`)`
- `handleAddBlock` — `dirtyBlocksRef.current.add(\`${currentPageId}::${newBlock.id}\`)`
- Block position changes (reorder) — mark all reordered blocks dirty

Read the file and find all mutation handlers. Add the appropriate dirty call to each.

**Verify:** `npm run build` passes.

---

**Phase 3 Build Check:** `npm run build` must pass before continuing to Phase 4.

---

## Phase 4: Android Process Safety (2 tasks)

### Task 4.1: WalletManager — Persist pending auth state across process death (P0)
**File:** `android/app/src/main/java/com/w3nto/app/wallet/WalletManager.kt`

The Phantom wallet flow is two-step: connect → sign. Between these steps, the app opens Phantom (another app) and the user may be gone for seconds to minutes. Android can kill the W3nto process during this time. The `@Volatile` fields `pendingPublicKey`, `pendingPhantomEncryptionPubKey`, `pendingSession`, and `pendingAuthMessage` are lost, and the sign callback fails silently.

Read the file. The fix is to persist these fields to SharedPreferences:

1. Add SharedPreferences to the constructor:

```kotlin
@Singleton
class WalletManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phantomWallet: PhantomWallet,
    private val sagaSeedVault: SagaSeedVault,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth
) {
    private val prefs = context.getSharedPreferences("wallet_pending", Context.MODE_PRIVATE)
```

2. Replace the `@Volatile` fields with property delegates backed by SharedPreferences:

```kotlin
private var pendingPublicKey: String?
    get() = prefs.getString("pendingPublicKey", null)
    set(value) = prefs.edit().apply { if (value != null) putString("pendingPublicKey", value) else remove("pendingPublicKey") }.apply()

private var pendingPhantomEncryptionPubKey: String?
    get() = prefs.getString("pendingPhantomEncryptionPubKey", null)
    set(value) = prefs.edit().apply { if (value != null) putString("pendingPhantomEncryptionPubKey", value) else remove("pendingPhantomEncryptionPubKey") }.apply()

private var pendingSession: String?
    get() = prefs.getString("pendingSession", null)
    set(value) = prefs.edit().apply { if (value != null) putString("pendingSession", value) else remove("pendingSession") }.apply()

private var pendingAuthMessage: String?
    get() = prefs.getString("pendingAuthMessage", null)
    set(value) = prefs.edit().apply { if (value != null) putString("pendingAuthMessage", value) else remove("pendingAuthMessage") }.apply()
```

3. Remove the `@Volatile` annotations on these fields (they're now property delegates, not backing fields).

4. The existing `finally` block in `handlePhantomSignMessageCallback` that sets all four to `null` already handles cleanup — verify it still works with the property delegate pattern.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

### Task 4.2: NavGraph — Fix deep link + auth race condition
**File:** `android/app/src/main/java/com/w3nto/app/ui/navigation/NavGraph.kt`

Two competing `LaunchedEffect` blocks can fight: the auth-state effect navigates to Discover when `firebaseUser` becomes non-null, and the deep-link effect tries to navigate to the profile route. If both fire in the same frame, the user bounces between screens.

Read the file and find both `LaunchedEffect` blocks. The fix is to make the auth-state effect respect pending deep links:

```kotlin
// Auth state effect — skip auto-navigate if deep link is pending
LaunchedEffect(firebaseUser) {
    if (firebaseUser != null) {
        val route = navController.currentDestination?.route
        if (route == Screen.Landing.route || route == Screen.ConnectWallet.route) {
            if (deepLinkRoute == null) {  // <-- ADD THIS GUARD
                navController.navigate(Screen.Discover.route) {
                    popUpTo(Screen.Landing.route) { inclusive = true }
                }
            }
        }
    }
}
```

The deep link effect should already handle navigation to the profile — it just needs to win the race. Adding the `deepLinkRoute == null` guard ensures the auth effect defers when a deep link is pending.

Check what `deepLinkRoute` is in this context — it may be a parameter, a state variable, or collected from a StateFlow. Use whatever form is already available in scope.

**Verify:** `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug` passes.

---

**Phase 4 Build Check:** Run `cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug`. Must pass before continuing to Phase 5.

---

## Phase 5: Final Verification and Commit (2 tasks)

### Task 5.1: Final Build Verification

Run BOTH builds and confirm they pass:

```bash
cd /Users/charlesvincent/Desktop/GitHub/W3nto && npm run build
cd /Users/charlesvincent/Desktop/GitHub/W3nto/android && ./gradlew assembleDebug
```

Both must succeed with zero errors. Fix any compilation errors before committing.

---

### Task 5.2: Commit All Changes

Stage all modified files and commit with this exact message:

```
fix: round 5 — gradient rendering, process death safety, username migration, media embeds, save optimization
```

Use the Conventional Commits format. Co-author line required:
```
Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

Use a HEREDOC for the commit message to preserve formatting exactly.

---

## Execution Order

1. Phase 1 (Tasks 1.1-1.4) — Android rendering fixes
2. Phase 2 (Tasks 2.1-2.4) — Web rendering fixes
3. Phase 3 (Tasks 3.1-3.2) — Web save optimization
4. Phase 4 (Tasks 4.1-4.2) — Android process safety
5. Phase 5 (Tasks 5.1-5.2) — Final builds and commit

## Important Notes
- **Read every file before editing** — line numbers and code snippets are approximate
- **VERIFY FIRST tasks** — read the file and skip if already applied
- **Check imports** before adding new ones — many may already exist
- **Run the build** after each phase to catch errors early
- **Do not touch** anything already fixed in previous rounds (listed in "What Was Already Fixed")
- You have full Bash access — run npm, gradle, git, any command needed
- Total tasks: 14 across 5 phases (some may be skipped if verify-first check passes)
- Working directory: `/Users/charlesvincent/Desktop/GitHub/W3nto`
