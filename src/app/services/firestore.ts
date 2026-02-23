import {
  doc,
  getDoc,
  getDocs,
  setDoc,
  deleteDoc,
  collection,
  serverTimestamp,
} from "firebase/firestore";
import { db, auth } from "../config/firebase";
import type { Profile, BentoPage, Block } from "../types";

// --- Profile ---

export async function getProfile(username: string): Promise<Profile | null> {
  const snap = await getDoc(doc(db, "profiles", username));
  if (!snap.exists()) return null;

  const data = snap.data();
  const pages = await getPages(username);

  return {
    username: snap.id,
    walletAddress: data.walletAddress ?? "",
    displayName: data.displayName ?? "",
    bio: data.bio ?? "",
    avatar: data.avatar ?? "",
    theme: data.theme ?? defaultTheme(),
    pages,
    defaultPageId: data.defaultPageId,
    showWallet: data.showWallet ?? true,
  };
}

export async function getProfileByWallet(walletAddress: string): Promise<Profile | null> {
  const walletSnap = await getDoc(doc(db, "wallets", walletAddress));
  if (!walletSnap.exists()) return null;
  const { username } = walletSnap.data();
  if (!username) return null;
  return getProfile(username);
}

export async function getProfileByUsername(username: string): Promise<Profile | null> {
  return getProfile(username);
}

export async function isUsernameAvailable(username: string): Promise<boolean> {
  const snap = await getDoc(doc(db, "profiles", username));
  return !snap.exists();
}

export async function saveProfile(profile: Profile): Promise<void> {
  const uid = auth.currentUser?.uid;
  if (!uid) throw new Error("Not authenticated");

  const profileRef = doc(db, "profiles", profile.username);
  const existing = await getDoc(profileRef);

  const profileData: Record<string, unknown> = {
    walletAddress: profile.walletAddress,
    displayName: profile.displayName,
    bio: profile.bio,
    avatar: profile.avatar,
    theme: profile.theme,
    defaultPageId: profile.defaultPageId,
    showWallet: profile.showWallet,
    uid,
    updatedAt: serverTimestamp(),
  };

  if (!existing.exists()) {
    profileData.createdAt = serverTimestamp();
  }

  await setDoc(profileRef, profileData, { merge: true });

  // Wallet reverse lookup
  await setDoc(
    doc(db, "wallets", profile.walletAddress),
    { username: profile.username, uid, updatedAt: serverTimestamp() },
    { merge: true }
  );
}

export async function deleteProfile(username: string): Promise<void> {
  await deleteDoc(doc(db, "profiles", username));
}

// --- Pages ---

export async function getPages(username: string): Promise<BentoPage[]> {
  const snap = await getDocs(collection(db, "profiles", username, "pages"));
  const pages: BentoPage[] = [];

  for (const pageDoc of snap.docs) {
    const data = pageDoc.data();
    const blocks = await getBlocks(username, pageDoc.id);
    pages.push({
      id: pageDoc.id,
      name: data.name ?? "",
      slug: data.slug ?? "",
      blocks,
      theme: data.theme ?? defaultTheme(),
      isFavorite: data.isFavorite ?? false,
      createdAt: data.createdAt?.toMillis?.() ?? Date.now(),
      updatedAt: data.updatedAt?.toMillis?.() ?? Date.now(),
    });
  }

  return pages;
}

export async function savePage(username: string, page: BentoPage): Promise<void> {
  const uid = auth.currentUser?.uid;
  if (!uid) throw new Error("Not authenticated");

  const pageRef = doc(db, "profiles", username, "pages", page.id);

  const pageData: Record<string, unknown> = {
    name: page.name,
    slug: page.slug,
    theme: page.theme,
    isFavorite: page.isFavorite,
    updatedAt: serverTimestamp(),
  };

  await setDoc(pageRef, pageData, { merge: true });

  // Save all blocks for this page
  for (const block of page.blocks) {
    await saveBlock(username, page.id, block);
  }
}

export async function deletePage(username: string, pageId: string): Promise<void> {
  // Delete blocks first
  const blocksSnap = await getDocs(
    collection(db, "profiles", username, "pages", pageId, "blocks")
  );
  for (const blockDoc of blocksSnap.docs) {
    await deleteDoc(blockDoc.ref);
  }
  await deleteDoc(doc(db, "profiles", username, "pages", pageId));
}

// --- Blocks ---

export async function getBlocks(username: string, pageId: string): Promise<Block[]> {
  const snap = await getDocs(
    collection(db, "profiles", username, "pages", pageId, "blocks")
  );

  return snap.docs
    .map((d) => {
      const data = d.data();
      return {
        id: d.id,
        type: data.type ?? "link",
        title: data.title ?? "",
        content: data.content ?? "",
        url: data.url,
        imageUrl: data.imageUrl,
        icon: data.icon,
        iconImage: data.iconImage,
        bgType: data.bgType ?? "color",
        bgColor: data.bgColor,
        bgGradient: data.bgGradient,
        bgImage: data.bgImage,
        textColor: data.textColor,
        width: data.width ?? 1,
        height: data.height ?? 1,
        position: data.position ?? 0,
        tokenAddress: data.tokenAddress,
        chainId: data.chainId,
        nftCollection: data.nftCollection,
        nftTokenId: data.nftTokenId,
        nftName: data.nftName,
        nftImage: data.nftImage,
        socialPlatform: data.socialPlatform,
      } as Block;
    })
    .sort((a, b) => a.position - b.position);
}

export async function saveBlock(
  username: string,
  pageId: string,
  block: Block
): Promise<void> {
  const blockRef = doc(db, "profiles", username, "pages", pageId, "blocks", block.id);

  const blockData: Record<string, unknown> = {
    type: block.type,
    title: block.title,
    content: block.content,
    url: block.url ?? null,
    imageUrl: block.imageUrl ?? null,
    icon: block.icon ?? null,
    iconImage: block.iconImage ?? null,
    bgType: block.bgType,
    bgColor: block.bgColor ?? null,
    bgGradient: block.bgGradient ?? null,
    bgImage: block.bgImage ?? null,
    textColor: block.textColor ?? null,
    width: block.width,
    height: block.height,
    position: block.position,
    tokenAddress: block.tokenAddress ?? null,
    chainId: block.chainId ?? null,
    nftCollection: block.nftCollection ?? null,
    nftTokenId: block.nftTokenId ?? null,
    nftName: block.nftName ?? null,
    nftImage: block.nftImage ?? null,
    socialPlatform: block.socialPlatform ?? null,
    updatedAt: serverTimestamp(),
  };

  await setDoc(blockRef, blockData, { merge: true });
}

export async function deleteBlock(
  username: string,
  pageId: string,
  blockId: string
): Promise<void> {
  await deleteDoc(
    doc(db, "profiles", username, "pages", pageId, "blocks", blockId)
  );
}

// --- Helpers ---

function defaultTheme() {
  return {
    backgroundColor: "#ffffff",
    cardBackground: "#f9fafb",
    primaryColor: "#8b5cf6",
    secondaryColor: "#ec4899",
    textColor: "#111827",
    borderRadius: "16px",
    fontFamily: "Inter",
    buttonStyle: "rounded" as const,
    buttonBackground: "#8b5cf6",
    buttonText: "#ffffff",
  };
}
