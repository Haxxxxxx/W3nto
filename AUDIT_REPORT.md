# W3nto — Production Readiness Audit Report

**Date:** 2026-02-22
**Auditor:** Claude Code deep-dive audit (Android + Web + Firebase)

---

## Executive Summary

The W3nto codebase is architecturally solid — MVVM, Hilt DI, Repository pattern, and the wallet auth pipeline all work correctly. The main gaps fall into three categories:

1. **Cross-stack data mismatches** (Android Block model missing fields the web writes)
2. **Security issues** (Firestore rules too permissive, unvalidated URLs, Cloud Function bypass)
3. **UX/Production polish** (navigation edge cases, ProGuard rules, responsive grid, dead code)

**Bottom line:** With ~15 targeted fixes, this is demo-ready. With ~25 fixes, it's production-ready.

---

## P0 BLOCKERS (Will crash, lose data, or block demo)

### Android

| # | Issue | File | Fix |
|---|-------|------|-----|
| 1 | `imageUrl` field missing from Block model — web writes IMAGE blocks with `imageUrl`, Android never reads it. Images created via web dashboard render blank on Android. | `data/model/Block.kt` | Add `val imageUrl: String? = null` to Block, add `imageUrl = map["imageUrl"] as? String` in `fromMap()`, update `ImageBlock.kt` fallback chain. |
| 2 | Deep link route never cleared — `pendingDeepLinkRoute` StateFlow is never set to null after navigation. Re-navigates on every recomposition. | `NavGraph.kt` lines 52-59 | Pass `onDeepLinkConsumed: () -> Unit` lambda to NavGraph, call after navigate. Wire to `MainActivity.consumeDeepLinkRoute()`. |
| 3 | `FavoritesViewModel.loadFavorites()` returns silently when user not authed — `isLoading` stays `true` forever, infinite spinner. | `FavoritesViewModel.kt` line 35 | Set `isLoading = false` on early return, show "sign in to view favorites" state. |
| 4 | ProGuard rules missing for Lazysodium/JNA, Firebase, Hilt — release build will crash on Phantom encryption. | `proguard-rules.pro` | Add keep rules for `com.sun.jna.**`, Firebase, and Hilt. |

### Web

| # | Issue | File | Fix |
|---|-------|------|-----|
| 5 | Block/page deletions don't persist — `saveProfile` never calls `deleteBlock`/`deletePage`, deleted items reappear on reload. | `storage.ts` line 86 | Call `deletePage`/`deleteBlock` at deletion time, not in save cascade. |
| 6 | Open redirect via unvalidated `block.url` — `javascript:` protocol URLs execute in `window.open`. | `UserProfile.tsx` line 96 | Validate URL starts with `https://` or `http://` before `window.open`. |
| 7 | Username change orphans old profile document — no cleanup, old username stays publicly accessible. | `firestore.ts` line 58 | Track original username, delete old doc + subcollections, migrate data. |
| 8 | Cloud Function timestamp check is conditional — replay attack with messages lacking `Timestamp:`. | `verifyWallet.ts` line 62 | Throw `invalid-argument` if `timestampMatch` is null. |

### Firebase Rules

| # | Issue | File | Fix |
|---|-------|------|-----|
| 9 | Profile `create` allows any authenticated user to claim any username — no UID match check. | `firestore.rules` line 9 | Add `&& request.resource.data.uid == request.auth.uid` to create rule. |
| 10 | Anonymous auth is enabled — combined with rule #9, unlimited profile spam. | `firebase.json` line 31 | Disable anonymous auth or require wallet-format UID in rules. |

---

## P1 IMPORTANT (Visible quality issues)

### Android

| # | Issue | File | Fix |
|---|-------|------|-----|
| 11 | `Theme.borderRadius` stored as `"16px"` string by web — Android casts as Number, silently falls back. | `Theme.kt` line 38 | Handle both: `(map["borderRadius"] as? Number)?.toInt() ?: (map["borderRadius"] as? String)?.filter { it.isDigit() }?.toIntOrNull() ?: 16` |
| 12 | `MainScope()` leaks on Activity recreation — never cancelled. | `MainActivity.kt` line 33 | Replace with `lifecycleScope`. |
| 13 | `SagaSeedVault.signMessage` passes empty `ByteArray(0)` as address — may fail on strict wallets. | `SagaSeedVault.kt` line 64 | Pass the authorized public key bytes. |
| 14 | `APP_IDENTITY_ICON` is relative URI `"favicon.ico"` — wallet can't fetch it. | `SagaSeedVault.kt` line 23 | Change to `Uri.parse("https://w3nto.app/favicon.ico")`. |
| 15 | `generateAuthMessage` is `public` — should be `internal` or `private`. | `WalletManager.kt` line 228 | Change visibility to `internal`. |
| 16 | ConnectWallet has no back-nav guard — auth can complete while user is on Landing with no feedback. | `NavGraph.kt` line 97 | Observe `authStateFlow()` at NavGraph top level, auto-navigate if authed. |
| 17 | IMAGE/NFT blocks render inside BlockCard's 12dp padding — not full-bleed like web. | `BlockCard.kt` line 102 | Render IMAGE/NFT in the background layer, or remove padding for those types. |
| 18 | `searchProfiles` only searches `username` prefix — "Search creators..." implies displayName search. | `FirestoreDataSource.kt` line 164 | Change placeholder to "Search by username..." or add displayName index. |
| 19 | `BentoGrid` placement algorithm recalculates every measure pass — no caching. | `BentoGrid.kt` | Cache placements with `remember(sortedBlocks, constraints.maxWidth)`. |
| 20 | `parseColor` called without `remember` — allocates Color objects on every recomposition. | `BlockCard.kt` line 123 | Wrap in `remember(hex) { parseColor(hex) }` at call sites. |

### Web

| # | Issue | File | Fix |
|---|-------|------|-----|
| 21 | Bento grid is not responsive — hardcoded 4 columns, broken on mobile. | `UserProfile.tsx` line 314 | Add `sm:grid-cols-2 md:grid-cols-4` breakpoints. |
| 22 | `autoConnect` triggers unexpected signature prompts on page load for returning users. | `WalletContext.tsx` line 203 | Set `autoConnect={false}` or improve `userInitiatedRef` guard. |
| 23 | `handleDeletePage` doesn't delete from Firestore — same orphan bug as finding #5. | `Dashboard.tsx` line 156 | Call `firestoreService.deletePage()` directly. |
| 24 | `video` and `music` block types missing from "Add Block" UI. | `Dashboard.tsx` lines 446-497 | Add buttons for video and music block types. |
| 25 | Landing page copy mentions MetaMask/WalletConnect — app only supports Solana. | `Landing.tsx` line 155 | Update copy to reference Phantom, Solflare, etc. |

---

## P2 NICE-TO-HAVE (Polish)

### Android
| # | Issue | Fix |
|---|-------|-----|
| 26 | Dead code: `AuthViewModel`, `LandingViewModel` never used | Delete both files |
| 27 | Dead code: `ColorPickerDialog`, `ImageUploader` — editor components in viewer app | Delete + remove unused CAMERA/READ_MEDIA_IMAGES permissions from manifest |
| 28 | `combinedClickable` with `onLongClick = null` — unnecessary ExperimentalFoundationApi opt-in | Simplify to `.clickable { onClick() }` |
| 29 | `Theme.fontFamily` parsed but never applied to Compose text | Document as known gap or implement font mapping |
| 30 | Compose BOM 2024.10.01 and Firebase BOM 33.6.0 are behind stable | Batch update deps |
| 31 | `lastDocumentId` in `DiscoverRepository` is not thread-safe | Add `@Volatile` or `AtomicReference` |

### Web
| # | Issue | Fix |
|---|-------|-----|
| 32 | `Date.now().toString()` as block/page IDs — collision on rapid clicks | Use `crypto.randomUUID()` |
| 33 | Default blocks use hardcoded IDs "1", "2", "3" | Use `crypto.randomUUID()` |
| 34 | `clipboard.writeText` in ShareModal has no error handling | Wrap in try/catch |
| 35 | `onProfileSnapshot` does N+1 reads (unused but latent) | Refactor or delete if unused |
| 36 | No Cache-Control headers in `firebase.json` hosting config | Add headers for static assets |
| 37 | `ThemeEditor.updateTheme` uses `any` type | Change to `string \| number` |
| 38 | `assetlinks.json` has debug keystore SHA-256 — needs release fingerprint | Update before production deploy |

### Cross-Stack
| # | Issue | Fix |
|---|-------|-----|
| 39 | Android `setWalletMapping` writes `{ username }` only; web writes `{ username, uid, updatedAt }` | Align write format |
| 40 | Auth message format in CLAUDE.md doesn't match actual code | Update CLAUDE.md |

---

## Recommended Fix Order for Demo

**Phase A — Data integrity (do first):**
1. Fix Block model `imageUrl` field (#1)
2. Fix deep link clearing (#2)
3. Fix FavoritesVM auth guard (#3)
4. Fix ProGuard rules (#4)
5. Fix `Theme.borderRadius` parsing (#11)

**Phase B — Security (before any public traffic):**
6. Firestore rules UID check (#9)
7. Disable anonymous auth (#10)
8. URL validation (#6)
9. Cloud Function timestamp guard (#8)

**Phase C — UX polish (before demo):**
10. IMAGE/NFT full-bleed fix (#17)
11. Replace leaked MainScope (#12)
12. Fix SagaSeedVault identity icon (#14)
13. Cache BentoGrid placements (#19)
14. Remove dead code (#26, #27)

**Phase D — Web fixes (before public launch):**
15. Block/page deletion persistence (#5, #23)
16. Responsive grid (#21)
17. Missing block types in UI (#24)
18. Landing page copy (#25)
