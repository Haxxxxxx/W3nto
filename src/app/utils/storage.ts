import { Profile, Theme, BentoPage } from "../types";
import * as firestoreService from "../services/firestore";
import { toast } from "sonner";

const PROFILES_KEY = "web3bento_profiles";
const CURRENT_PROFILE_KEY = "web3bento_current";

// --- localStorage cache helpers ---

function getLocalProfiles(): Record<string, Profile> {
  try {
    const data = localStorage.getItem(PROFILES_KEY);
    return data ? JSON.parse(data) : {};
  } catch {
    return {};
  }
}

function saveLocalProfile(profile: Profile): void {
  const profiles = getLocalProfiles();
  profiles[profile.walletAddress] = profile;
  localStorage.setItem(PROFILES_KEY, JSON.stringify(profiles));
  localStorage.setItem(CURRENT_PROFILE_KEY, profile.walletAddress);
}

// --- Public API (Firestore-first, localStorage as cache) ---

export async function getProfile(walletAddress: string): Promise<Profile | null> {
  // Try Firestore first
  try {
    const profile = await firestoreService.getProfileByWallet(walletAddress);
    if (profile) {
      saveLocalProfile(profile); // cache locally
      return profile;
    }
  } catch {
    const cached = getLocalProfiles()[walletAddress];
    if (cached) {
      toast.warning("Loaded from cache", { description: "Changes may not save until connection is restored." });
      return cached;
    }
    return null;
  }

  // No Firestore result, no cache
  const profiles = getLocalProfiles();
  return profiles[walletAddress] || null;
}

export async function getProfileByUsername(username: string): Promise<Profile | null> {
  try {
    const profile = await firestoreService.getProfileByUsername(username);
    if (profile) {
      saveLocalProfile(profile);
      return profile;
    }
  } catch {
    const cached = Object.values(getLocalProfiles()).find(
      (p) => p.username.toLowerCase() === username.toLowerCase()
    );
    if (cached) {
      toast.warning("Loaded from cache", { description: "Changes may not save until connection is restored." });
      return cached;
    }
    return null;
  }

  // Fallback
  const profiles = getLocalProfiles();
  return Object.values(profiles).find(
    (p) => p.username.toLowerCase() === username.toLowerCase()
  ) || null;
}

export async function saveProfile(profile: Profile): Promise<void> {
  // Save to localStorage immediately (optimistic)
  saveLocalProfile(profile);

  // Persist to Firestore
  try {
    await firestoreService.saveProfile(profile);

    // Save pages + blocks
    for (const page of profile.pages) {
      await firestoreService.savePage(profile.username, page);
    }
  } catch (err) {
    throw err;
  }
}

export function getCurrentProfileSync(walletAddress?: string): Profile | null {
  const addr = walletAddress || localStorage.getItem(CURRENT_PROFILE_KEY);
  if (!addr) return null;
  const profiles = getLocalProfiles();
  return profiles[addr] || null;
}

export function logout(): void {
  localStorage.removeItem(CURRENT_PROFILE_KEY);
}

export function createDefaultProfile(walletAddress: string): Profile {
  const username = `user${walletAddress.slice(0, 6)}`;
  const defaultTheme: Theme = {
    backgroundColor: "#ffffff",
    cardBackground: "#f9fafb",
    primaryColor: "#8b5cf6",
    secondaryColor: "#ec4899",
    textColor: "#111827",
    borderRadius: "16px",
    fontFamily: "Inter",
    buttonStyle: "rounded",
    buttonBackground: "#8b5cf6",
    buttonText: "#ffffff",
  };

  const defaultPage: BentoPage = {
    id: crypto.randomUUID(),
    name: "Main",
    slug: "main",
    isFavorite: true,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    theme: defaultTheme,
    blocks: [
      {
        id: crypto.randomUUID(),
        type: "link",
        title: "My Website",
        content: "Check out my portfolio",
        url: "https://example.com",
        icon: "🌐",
        bgType: "color",
        bgColor: "#8b5cf6",
        textColor: "#ffffff",
        width: 2,
        height: 1,
        position: 0,
      },
      {
        id: crypto.randomUUID(),
        type: "social",
        title: "Twitter",
        content: "@username",
        url: "https://twitter.com",
        icon: "🐦",
        socialPlatform: "twitter",
        bgType: "color",
        bgColor: "#1da1f2",
        textColor: "#ffffff",
        width: 1,
        height: 1,
        position: 1,
      },
      {
        id: crypto.randomUUID(),
        type: "social",
        title: "GitHub",
        content: "@username",
        url: "https://github.com",
        icon: "💻",
        socialPlatform: "github",
        bgType: "color",
        bgColor: "#24292e",
        textColor: "#ffffff",
        width: 1,
        height: 1,
        position: 2,
      },
    ],
  };

  return {
    username,
    walletAddress,
    displayName: "Web3 Creator",
    bio: "Building the future of the decentralized web",
    avatar: "",
    theme: defaultTheme,
    pages: [defaultPage],
    defaultPageId: defaultPage.id,
    showWallet: true,
  };
}

const RESERVED_USERNAMES = new Set([
  "dashboard", "login", "signup", "register", "admin", "api",
  "settings", "profile", "explore", "discover", "search",
  "about", "help", "support", "terms", "privacy", "404",
]);

export function isReservedUsername(username: string): boolean {
  return RESERVED_USERNAMES.has(username.toLowerCase());
}

export async function isUsernameAvailable(username: string): Promise<boolean> {
  if (isReservedUsername(username)) return false;
  return firestoreService.isUsernameAvailable(username);
}
