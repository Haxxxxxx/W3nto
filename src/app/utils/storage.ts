import { Profile, Block, Theme, BentoPage } from "../types";

const PROFILES_KEY = "web3bento_profiles";
const CURRENT_PROFILE_KEY = "web3bento_current";

export function getProfiles(): Record<string, Profile> {
  const data = localStorage.getItem(PROFILES_KEY);
  return data ? JSON.parse(data) : {};
}

export function getProfile(walletAddress: string): Profile | null {
  const profiles = getProfiles();
  return profiles[walletAddress] || null;
}

export function getProfileByUsername(username: string): Profile | null {
  const profiles = getProfiles();
  return Object.values(profiles).find(p => p.username.toLowerCase() === username.toLowerCase()) || null;
}

export function saveProfile(profile: Profile): void {
  const profiles = getProfiles();
  profiles[profile.walletAddress] = profile;
  localStorage.setItem(PROFILES_KEY, JSON.stringify(profiles));
  localStorage.setItem(CURRENT_PROFILE_KEY, profile.walletAddress);
}

export function getCurrentProfile(walletAddress?: string): Profile | null {
  const addr = walletAddress || localStorage.getItem(CURRENT_PROFILE_KEY);
  if (!addr) return null;
  return getProfile(addr);
}

export function logout(): void {
  localStorage.removeItem(CURRENT_PROFILE_KEY);
}

export function createDefaultProfile(walletAddress: string): Profile {
  const username = `user${walletAddress.slice(2, 8)}`;
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
    id: Date.now().toString(),
    name: "Main",
    slug: "main",
    isFavorite: true,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    theme: defaultTheme,
    blocks: [
      {
        id: "1",
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
        id: "2",
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
        id: "3",
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
    blocks: [],
    pages: [defaultPage],
    defaultPageId: defaultPage.id,
    showWallet: true,
  };
}
