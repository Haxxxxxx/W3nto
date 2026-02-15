import { useParams, useNavigate, Link } from "react-router";
import { useEffect, useState } from "react";
import { Profile, BentoPage, Block } from "../types";
import { getProfileByUsername } from "../utils/storage";
import { ExternalLink, Grid3x3, ArrowLeft } from "lucide-react";
import W3ntoLogo from "../components/W3ntoLogo";
import TokenChart from "../components/TokenChart";
import { socialPlatforms, chainLogos } from "../utils/socialPlatforms";

export default function UserProfile() {
  const { username, pageSlug } = useParams();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [currentPage, setCurrentPage] = useState<BentoPage | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (username) {
      const userProfile = getProfileByUsername(username);
      setProfile(userProfile);

      if (userProfile) {
        let page: BentoPage | undefined;
        if (pageSlug) {
          page = userProfile.pages.find((p) => p.slug === pageSlug);
        } else {
          page = userProfile.pages.find((p) => p.id === userProfile.defaultPageId) || userProfile.pages[0];
        }
        setCurrentPage(page || null);
      }

      setLoading(false);
    }
  }, [username, pageSlug]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="size-12 border-4 border-gray-200 border-t-purple-500 rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">Loading profile...</p>
        </div>
      </div>
    );
  }

  if (!profile || !currentPage) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center max-w-md">
          <div className="size-20 bg-gray-200 rounded-full flex items-center justify-center mx-auto mb-6">
            <Grid3x3 className="size-10 text-gray-400" />
          </div>
          <h1 className="text-3xl mb-3">Profile not found</h1>
          <p className="text-gray-600 mb-8">
            The profile "{username}" doesn't exist or hasn't been created yet.
          </p>
          <button
            onClick={() => navigate("/")}
            className="px-6 py-3 bg-black text-white rounded-full hover:bg-gray-800 transition-colors inline-flex items-center gap-2"
          >
            <ArrowLeft className="size-4" />
            Go Home
          </button>
        </div>
      </div>
    );
  }

  const handleBlockClick = (block: Block) => {
    if (block.url) {
      window.open(block.url, "_blank", "noopener,noreferrer");
    }
  };

  const getBlockBackground = (block: Block) => {
    if (block.bgType === "gradient" && block.bgGradient) {
      return {
        background: `linear-gradient(${block.bgGradient.direction}, ${block.bgGradient.from}, ${block.bgGradient.to})`,
      };
    }
    if (block.bgType === "image" && block.bgImage) {
      return {
        backgroundImage: `url(${block.bgImage})`,
        backgroundSize: "cover",
        backgroundPosition: "center",
      };
    }
    return { backgroundColor: block.bgColor };
  };

  const getIconDisplay = (block: Block) => {
    if (block.iconImage) {
      return <img src={block.iconImage} alt="icon" className="size-12 rounded-lg object-cover mb-2" />;
    }
    if (block.type === "social" && block.socialPlatform && block.socialPlatform !== "custom") {
      const Icon = socialPlatforms[block.socialPlatform].icon;
      return <Icon className="size-12 mb-2" />;
    }
    if (block.icon) {
      return <div className="text-4xl mb-2">{block.icon}</div>;
    }
    return null;
  };

  const renderBlockContent = (block: Block) => {
    // NFT Block
    if (block.type === "nft" && block.nftImage) {
      return (
        <div className="absolute inset-0">
          <img src={block.nftImage} alt={block.nftName || "NFT"} className="size-full object-cover" />
          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent"></div>
          <div className="absolute bottom-4 left-4 right-4 text-white">
            <div className="text-lg mb-1">{block.nftName || block.title}</div>
            <div className="text-sm opacity-90">{block.nftCollection}</div>
          </div>
        </div>
      );
    }

    // Chart Block
    if (block.type === "chart") {
      return (
        <div className="absolute inset-0 p-4 flex flex-col">
          <div className="mb-2">
            <div className="text-sm font-medium mb-1">{block.title}</div>
            {block.tokenAddress && (
              <div className="flex items-center gap-2 flex-wrap">
                <div className="text-xs font-mono opacity-75">
                  {block.tokenAddress.slice(0, 6)}...{block.tokenAddress.slice(-4)}
                </div>
                {block.chainId && (
                  <div className="text-xs px-2 py-0.5 bg-white/20 backdrop-blur-sm rounded-full">
                    {chainLogos[block.chainId]}
                  </div>
                )}
              </div>
            )}
          </div>
          <div className="flex-1 min-h-0">
            <TokenChart
              tokenAddress={block.tokenAddress}
              chainId={block.chainId}
              textColor={block.textColor}
            />
          </div>
        </div>
      );
    }

    // Image Block
    if (block.type === "image" && block.imageUrl) {
      return (
        <div className="absolute inset-0">
          <img src={block.imageUrl} alt={block.title} className="size-full object-cover" />
          <div className="absolute inset-0 bg-black/20 group-hover:bg-black/30 transition-colors"></div>
        </div>
      );
    }

    // Default Content (Link, Social, Text, Token)
    return (
      <div className="relative z-10 text-center px-4">
        {block.bgType === "image" && block.bgImage && (
          <div className="absolute inset-0 bg-black/30 -z-10"></div>
        )}
        {getIconDisplay(block)}
        <div className="text-base md:text-lg mb-1">
          {block.title}
        </div>
        {block.content && (
          <div className="text-sm opacity-75 line-clamp-2">
            {block.content}
          </div>
        )}

        {/* Token Block - Show chain */}
        {block.type === "token" && block.chainId && (
          <div className="mt-2 inline-flex items-center gap-1 px-2 py-1 bg-white/20 backdrop-blur-sm rounded-full text-xs">
            <span>{chainLogos[block.chainId]}</span>
          </div>
        )}
      </div>
    );
  };

  const buttonRadius = currentPage.theme.buttonStyle === "rounded" ? "9999px" : currentPage.theme.borderRadius;

  return (
    <div
      className="min-h-screen py-12 px-6"
      style={{
        backgroundColor: currentPage.theme.backgroundColor,
        color: currentPage.theme.textColor,
        fontFamily: currentPage.theme.fontFamily,
      }}
    >
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-12 text-center">
          {profile.avatar ? (
            <img
              src={profile.avatar}
              alt={profile.displayName}
              className="size-24 md:size-32 rounded-full object-cover mx-auto mb-6 border-4 shadow-lg"
              style={{ borderColor: currentPage.theme.cardBackground }}
            />
          ) : (
            <div
              className="size-24 md:size-32 rounded-full mx-auto mb-6 border-4 shadow-lg flex items-center justify-center text-4xl bg-gradient-to-br from-purple-400 to-pink-400"
              style={{ borderColor: currentPage.theme.cardBackground }}
            >
              {profile.displayName.charAt(0)}
            </div>
          )}
          <h1 className="text-4xl mb-3">{profile.displayName}</h1>
          <p className="text-lg opacity-80 mb-4 max-w-2xl mx-auto">{profile.bio}</p>
          <div className="flex items-center justify-center gap-3 flex-wrap">
            <span
              className="px-4 py-2 rounded-full text-sm"
              style={{ backgroundColor: currentPage.theme.cardBackground }}
            >
              @{profile.username}
            </span>
            {profile.showWallet && (
              <span
                className="px-4 py-2 rounded-full text-sm font-mono"
                style={{ backgroundColor: currentPage.theme.cardBackground }}
              >
                {profile.walletAddress.slice(0, 6)}...{profile.walletAddress.slice(-4)}
              </span>
            )}
          </div>

          {/* Page Navigation if multiple pages */}
          {profile.pages.length > 1 && (
            <div className="flex items-center justify-center gap-2 mt-6 flex-wrap">
              {profile.pages.map((page) => (
                <Link
                  key={page.id}
                  to={`/${profile.username}/${page.slug}`}
                  className="px-4 py-2 rounded-full text-sm transition-all"
                  style={{
                    backgroundColor: page.id === currentPage.id ? currentPage.theme.buttonBackground : currentPage.theme.cardBackground,
                    color: page.id === currentPage.id ? currentPage.theme.buttonText : currentPage.theme.textColor,
                    opacity: page.id === currentPage.id ? 1 : 0.7,
                    borderRadius: buttonRadius,
                  }}
                >
                  {page.name}
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Bento Grid */}
        {currentPage.blocks.length === 0 ? (
          <div className="text-center py-16">
            <p className="opacity-60">No blocks added yet</p>
          </div>
        ) : (
          <div className="grid grid-cols-4 gap-4 auto-rows-[120px]">
            {currentPage.blocks.map((block) => (
              <div
                key={block.id}
                className={`group relative overflow-hidden transition-all hover:scale-105 hover:shadow-xl ${
                  block.url ? "cursor-pointer" : ""
                }`}
                style={{
                  gridColumn: `span ${block.width}`,
                  gridRow: `span ${block.height}`,
                  ...getBlockBackground(block),
                  color: block.textColor,
                  borderRadius: currentPage.theme.borderRadius,
                }}
                onClick={() => handleBlockClick(block)}
              >
                {renderBlockContent(block)}

                {block.url && (
                  <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                    <div className="size-8 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center">
                      <ExternalLink className="size-4" />
                    </div>
                  </div>
                )}

                {/* Hover Effect */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none"></div>
              </div>
            ))}
          </div>
        )}

        {/* Footer */}
        <div className="mt-16 text-center">
          <p className="text-sm opacity-60 mb-4">Create your own W3nto profile</p>
          <Link
            to="/"
            className="px-6 py-3 inline-flex items-center gap-2 transition-all hover:scale-105"
            style={{
              backgroundColor: currentPage.theme.buttonBackground,
              color: currentPage.theme.buttonText,
              borderRadius: buttonRadius,
            }}
          >
            <W3ntoLogo size={20} />
            Get Started Free
          </Link>
        </div>
      </div>
    </div>
  );
}
