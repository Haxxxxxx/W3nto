import type { ComponentType } from "react";
import { SocialPlatform } from "../types";
import {
  Twitter,
  Instagram,
  Youtube,
  Linkedin,
  Github,
  Facebook,
  Music,
  MessageCircle,
  Video,
  Camera,
  Music2,
  Cloud,
  Dribbble,
  ExternalLink
} from "lucide-react";

export interface SocialPlatformInfo {
  name: string;
  icon: ComponentType<{ className?: string }>;
  color: string;
  urlPrefix: string;
}

export const socialPlatforms: Record<SocialPlatform, SocialPlatformInfo> = {
  twitter: {
    name: "Twitter / X",
    icon: Twitter,
    color: "#1da1f2",
    urlPrefix: "https://twitter.com/",
  },
  instagram: {
    name: "Instagram",
    icon: Instagram,
    color: "#E4405F",
    urlPrefix: "https://instagram.com/",
  },
  tiktok: {
    name: "TikTok",
    icon: Music,
    color: "#000000",
    urlPrefix: "https://tiktok.com/@",
  },
  youtube: {
    name: "YouTube",
    icon: Youtube,
    color: "#FF0000",
    urlPrefix: "https://youtube.com/@",
  },
  linkedin: {
    name: "LinkedIn",
    icon: Linkedin,
    color: "#0077B5",
    urlPrefix: "https://linkedin.com/in/",
  },
  github: {
    name: "GitHub",
    icon: Github,
    color: "#333333",
    urlPrefix: "https://github.com/",
  },
  discord: {
    name: "Discord",
    icon: MessageCircle,
    color: "#5865F2",
    urlPrefix: "https://discord.com/users/",
  },
  telegram: {
    name: "Telegram",
    icon: MessageCircle,
    color: "#0088cc",
    urlPrefix: "https://t.me/",
  },
  twitch: {
    name: "Twitch",
    icon: Video,
    color: "#9146FF",
    urlPrefix: "https://twitch.tv/",
  },
  snapchat: {
    name: "Snapchat",
    icon: Camera,
    color: "#FFFC00",
    urlPrefix: "https://snapchat.com/add/",
  },
  facebook: {
    name: "Facebook",
    icon: Facebook,
    color: "#1877F2",
    urlPrefix: "https://facebook.com/",
  },
  reddit: {
    name: "Reddit",
    icon: MessageCircle,
    color: "#FF4500",
    urlPrefix: "https://reddit.com/u/",
  },
  medium: {
    name: "Medium",
    icon: ExternalLink,
    color: "#000000",
    urlPrefix: "https://medium.com/@",
  },
  behance: {
    name: "Behance",
    icon: ExternalLink,
    color: "#1769FF",
    urlPrefix: "https://behance.net/",
  },
  dribbble: {
    name: "Dribbble",
    icon: Dribbble,
    color: "#EA4C89",
    urlPrefix: "https://dribbble.com/",
  },
  spotify: {
    name: "Spotify",
    icon: Music2,
    color: "#1DB954",
    urlPrefix: "https://open.spotify.com/user/",
  },
  soundcloud: {
    name: "SoundCloud",
    icon: Cloud,
    color: "#FF3300",
    urlPrefix: "https://soundcloud.com/",
  },
  custom: {
    name: "Custom",
    icon: ExternalLink,
    color: "#6366f1",
    urlPrefix: "",
  },
};

export const chainLogos: Record<string, string> = {
  "1": "ETH",
  "137": "MATIC",
  "56": "BNB",
  "42161": "ARB",
  "10": "OP",
  "8453": "BASE",
  "sol": "SOL",
  "btc": "BTC",
};

export const chainNames: Record<string, string> = {
  "1": "Ethereum",
  "137": "Polygon",
  "56": "BSC",
  "42161": "Arbitrum",
  "10": "Optimism",
  "8453": "Base",
  "sol": "Solana",
  "btc": "Bitcoin",
};
