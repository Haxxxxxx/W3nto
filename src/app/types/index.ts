export type BlockType =
  | "link"
  | "social"
  | "text"
  | "image"
  | "video"
  | "nft"
  | "token"
  | "chart"
  | "music";

export type BackgroundType = "color" | "gradient" | "image";

export type SocialPlatform =
  | "twitter"
  | "instagram"
  | "tiktok"
  | "youtube"
  | "linkedin"
  | "github"
  | "discord"
  | "telegram"
  | "twitch"
  | "snapchat"
  | "facebook"
  | "reddit"
  | "medium"
  | "behance"
  | "dribbble"
  | "spotify"
  | "soundcloud"
  | "custom";

export interface Block {
  id: string;
  type: BlockType;
  title: string;
  content: string;
  url?: string;
  imageUrl?: string;
  icon?: string;
  iconImage?: string;
  bgType: BackgroundType;
  bgColor?: string;
  bgGradient?: {
    from: string;
    to: string;
    direction: string;
  };
  bgImage?: string;
  textColor?: string;
  width: number;
  height: number;
  position: number;
  tokenAddress?: string;
  chainId?: string;
  nftCollection?: string;
  nftTokenId?: string;
  nftName?: string;
  nftImage?: string;
  socialPlatform?: SocialPlatform;
}

export interface BentoPage {
  id: string;
  name: string;
  slug: string;
  blocks: Block[];
  theme: Theme;
  isFavorite: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Profile {
  username: string;
  walletAddress: string;
  displayName: string;
  bio: string;
  avatar: string;
  theme: Theme;
  pages: BentoPage[];
  defaultPageId?: string;
  showWallet: boolean;
}

export interface Theme {
  backgroundColor: string;
  cardBackground: string;
  primaryColor: string;
  secondaryColor: string;
  textColor: string;
  borderRadius: string;
  fontFamily: string;
  buttonStyle: "rounded" | "squared";
  buttonBackground: string;
  buttonText: string;
}
