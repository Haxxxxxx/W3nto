import { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate, Link } from "react-router";
import { Profile } from "../types";
import { useWallet } from "../contexts/WalletContext";
import { createDefaultProfile, saveProfile, getProfile, logout } from "../utils/storage";
import * as firestoreService from "../services/firestore";
import {
  Grid3x3,
  User,
  Palette,
  Eye,
  Save,
  LogOut,
  Edit,
  Trash2,
  Link as LinkIcon,
  Image as ImageIcon,
  Type,
  Coins,
  TrendingUp,
  Image as NFTIcon,
  Copy,
  Layers,
  PlayCircle,
  Music
} from "lucide-react";
import { Block, BentoPage } from "../types";
import BlockEditor from "../components/BlockEditor";
import ProfileEditor from "../components/ProfileEditor";
import ThemeEditor from "../components/ThemeEditor";
import PageManager from "../components/PageManager";
import ShareModal from "../components/ShareModal";
import W3ntoLogo from "../components/W3ntoLogo";
import { socialPlatforms } from "../utils/socialPlatforms";
import { toast } from "sonner";

export default function Dashboard() {
  const navigate = useNavigate();
  const { address, isConnected, isAuthenticated, isAuthenticating, authError, disconnect, retryAuth } = useWallet();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [currentPageId, setCurrentPageId] = useState<string>("");
  const [activeTab, setActiveTab] = useState<"blocks" | "pages" | "profile" | "theme">("blocks");
  const [editingBlock, setEditingBlock] = useState<Block | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [shareUrl, setShareUrl] = useState<string | null>(null);
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout>>();
  const saveVersionRef = useRef(0);

  // Load profile from Firestore
  useEffect(() => {
    if (isAuthenticating) return;

    if (!isConnected || !address) {
      navigate("/");
      return;
    }

    if (!isAuthenticated) return;

    let cancelled = false;

    async function loadProfile() {
      setIsLoading(true);
      try {
        let userProfile = await getProfile(address!);

        if (!userProfile) {
          userProfile = createDefaultProfile(address!);
          await saveProfile(userProfile);
        }

        // Migrate old profiles without pages
        if (!userProfile.pages || userProfile.pages.length === 0) {
          const defaultPage: BentoPage = {
            id: crypto.randomUUID(),
            name: "Main",
            slug: "main",
            blocks: (userProfile as any).blocks || [],
            theme: userProfile.theme,
            isFavorite: true,
            createdAt: Date.now(),
            updatedAt: Date.now(),
          };
          userProfile.pages = [defaultPage];
          userProfile.defaultPageId = defaultPage.id;
          await saveProfile(userProfile);
        }

        if (!cancelled) {
          setProfile(userProfile);
          setCurrentPageId(userProfile.defaultPageId || userProfile.pages[0]?.id || "");
        }
      } catch (err) {
        console.error("Failed to load profile:", err);
        setSaveError("Failed to load profile");
        toast.error("Failed to load profile");
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    loadProfile();
    return () => { cancelled = true; };
  }, [address, isConnected, isAuthenticated, isAuthenticating, navigate]);

  // Debounced auto-save
  const debouncedSave = useCallback((profileToSave: Profile) => {
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    const version = ++saveVersionRef.current;
    saveTimeoutRef.current = setTimeout(async () => {
      if (version !== saveVersionRef.current) return; // superseded by newer save
      try {
        await saveProfile(profileToSave);
      } catch (err) {
        console.error("Auto-save failed:", err);
        toast.error("Auto-save failed");
      }
    }, 2000);
  }, []);

  // Cleanup: clear pending debounce timer on unmount
  useEffect(() => {
    return () => {
      if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    };
  }, []);

  const currentPage = profile?.pages.find((p) => p.id === currentPageId);

  const handleSave = async () => {
    if (!profile) return;
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    setIsSaving(true);
    setSaveError(null);
    try {
      await saveProfile(profile);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Save failed";
      setSaveError(msg);
      toast.error("Save failed", { description: msg });
    } finally {
      setIsSaving(false);
    }
  };

  const updateProfile = (updatedProfile: Profile) => {
    setProfile(updatedProfile);
    debouncedSave(updatedProfile);
  };

  const handleCreatePage = () => {
    if (!profile) return;
    const newPage: BentoPage = {
      id: crypto.randomUUID(),
      name: `Page ${profile.pages.length + 1}`,
      slug: `page-${Date.now()}`,
      blocks: [],
      theme: profile.theme,
      isFavorite: false,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };
    updateProfile({ ...profile, pages: [...profile.pages, newPage] });
    setCurrentPageId(newPage.id);
  };

  const handleDeletePage = async (pageId: string) => {
    if (!profile || !window.confirm("Delete this page?")) return;
    try {
      await firestoreService.deletePage(profile.username, pageId);
    } catch (err) {
      console.error(err);
      toast.error("Failed to delete page");
      return;
    }
    const updatedPages = profile.pages.filter((p) => p.id !== pageId);
    updateProfile({
      ...profile,
      pages: updatedPages,
      defaultPageId: profile.defaultPageId === pageId ? updatedPages[0]?.id : profile.defaultPageId,
    });
    if (currentPageId === pageId) {
      setCurrentPageId(updatedPages[0]?.id || "");
    }
  };

  const handleToggleFavorite = (pageId: string) => {
    if (!profile) return;
    updateProfile({
      ...profile,
      pages: profile.pages.map((p) =>
        p.id === pageId ? { ...p, isFavorite: !p.isFavorite } : p
      ),
    });
  };

  const handleRenamePage = async (pageId: string, name: string) => {
    if (!profile) return;
    const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, "-");
    const existingSlugs = profile.pages.filter(p => p.id !== pageId).map(p => p.slug);
    const finalSlug = existingSlugs.includes(slug) ? `${slug}-${Date.now()}` : slug;
    const updated = {
      ...profile,
      pages: profile.pages.map((p) =>
        p.id === pageId ? { ...p, name, slug: finalSlug, updatedAt: Date.now() } : p
      ),
    };
    setProfile(updated);
    // Save immediately — don't rely on debounce for renames
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    try {
      await saveProfile(updated);
    } catch (err) {
      toast.error("Failed to rename page");
    }
  };

  const handleSharePage = (page: BentoPage) => {
    const url = `${window.location.origin}/${profile?.username}/${page.slug}`;
    setShareUrl(url);
  };

  const handleAddBlock = (type: Block["type"]) => {
    if (!profile || !currentPage) return;
    const newBlock: Block = {
      id: crypto.randomUUID(),
      type,
      title: `New ${type}`,
      content: "",
      url: "",
      icon: getDefaultIcon(type),
      bgType: "color",
      bgColor: getDefaultColor(type),
      textColor: "#ffffff",
      width: type === "text" ? 2 : 1,
      height: 1,
      position: currentPage.blocks.length,
    };
    updateProfile({
      ...profile,
      pages: profile.pages.map((p) =>
        p.id === currentPageId
          ? { ...p, blocks: [...p.blocks, newBlock], updatedAt: Date.now() }
          : p
      ),
    });
  };

  const getDefaultIcon = (type: Block["type"]): string => {
    const icons: Record<Block["type"], string> = {
      link: "🔗", social: "🌐", text: "📝", image: "🖼️",
      video: "🎥", nft: "🎨", token: "💰", chart: "📊", music: "🎵",
    };
    return icons[type];
  };

  const getDefaultColor = (type: Block["type"]): string => {
    const colors: Record<Block["type"], string> = {
      link: "#8b5cf6", social: "#1da1f2", text: "#6366f1", image: "#ec4899",
      video: "#f59e0b", nft: "#8b5cf6", token: "#10b981", chart: "#3b82f6", music: "#ef4444",
    };
    return colors[type];
  };

  const handleUpdateBlock = (updatedBlock: Block) => {
    if (!profile) return;
    updateProfile({
      ...profile,
      pages: profile.pages.map((p) =>
        p.id === currentPageId
          ? {
              ...p,
              blocks: p.blocks.map((b) => (b.id === updatedBlock.id ? updatedBlock : b)),
              updatedAt: Date.now(),
            }
          : p
      ),
    });
    setEditingBlock(null);
  };

  const handleDeleteBlock = async (blockId: string) => {
    if (!profile || !window.confirm("Delete this block?")) return;
    try {
      await firestoreService.deleteBlock(profile.username, currentPageId, blockId);
    } catch (err) {
      console.error(err);
      toast.error("Failed to delete block");
      return;
    }
    updateProfile({
      ...profile,
      pages: profile.pages.map((p) =>
        p.id === currentPageId
          ? {
              ...p,
              blocks: p.blocks
                .filter((b) => b.id !== blockId)
                .map((b, idx) => ({ ...b, position: idx })),
              updatedAt: Date.now(),
            }
          : p
      ),
    });
  };

  const handlePreview = () => {
    if (profile && currentPage) {
      window.open(`/${profile.username}/${currentPage.slug}`, "_blank");
    }
  };

  const handleLogout = () => {
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    logout();
    disconnect();
    navigate("/");
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

  if (isAuthenticating) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="size-12 border-4 border-gray-200 border-t-purple-500 rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">Authenticating...</p>
        </div>
      </div>
    );
  }

  if (isConnected && !isAuthenticated && !isAuthenticating) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <p className="text-gray-800 text-lg mb-2">Authentication failed</p>
          <p className="text-gray-500 text-sm mb-4">{authError || "Could not verify your wallet signature."}</p>
          <div className="flex gap-3 justify-center">
            <button
              onClick={() => retryAuth()}
              className="px-6 py-2.5 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
            >
              Retry
            </button>
            <button
              onClick={() => { disconnect(); navigate("/"); }}
              className="px-6 py-2.5 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Disconnect
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (isLoading || !profile) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="size-12 border-4 border-gray-200 border-t-purple-500 rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your profile...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
              <W3ntoLogo size={40} />
              <span className="text-xl">
                <span className="font-semibold">W3nto</span>
              </span>
            </Link>
            <div className="h-8 w-px bg-gray-200"></div>
            <div className="flex items-center gap-2 text-sm">
              <span className="px-3 py-1 bg-gray-100 rounded-full font-mono">
                {address?.slice(0, 6)}...{address?.slice(-4)}
              </span>
            </div>
          </div>
          <div className="flex items-center gap-3">
            {saveError && (
              <span className="text-sm text-red-500">{saveError}</span>
            )}
            <button
              onClick={handlePreview}
              className="px-4 py-2 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
            >
              <Eye className="size-4" />
              Preview
            </button>
            <button
              onClick={handleSave}
              disabled={isSaving}
              className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center gap-2 disabled:opacity-50"
            >
              <Save className="size-4" />
              {isSaving ? "Saving..." : "Save"}
            </button>
            <button
              onClick={handleLogout}
              className="p-2 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <LogOut className="size-4" />
            </button>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-6 py-8">
        <div className="grid lg:grid-cols-[300px_1fr] gap-8">
          {/* Sidebar */}
          <div className="space-y-4">
            <div className="bg-white rounded-xl border border-gray-200 p-4">
              <div className="space-y-2">
                <button
                  onClick={() => setActiveTab("blocks")}
                  className={`w-full px-4 py-3 rounded-lg text-left flex items-center gap-3 transition-colors ${
                    activeTab === "blocks" ? "bg-purple-50 text-purple-600" : "hover:bg-gray-50"
                  }`}
                >
                  <Grid3x3 className="size-5" />
                  Blocks
                </button>
                <button
                  onClick={() => setActiveTab("pages")}
                  className={`w-full px-4 py-3 rounded-lg text-left flex items-center gap-3 transition-colors ${
                    activeTab === "pages" ? "bg-purple-50 text-purple-600" : "hover:bg-gray-50"
                  }`}
                >
                  <Layers className="size-5" />
                  Pages
                </button>
                <button
                  onClick={() => setActiveTab("profile")}
                  className={`w-full px-4 py-3 rounded-lg text-left flex items-center gap-3 transition-colors ${
                    activeTab === "profile" ? "bg-purple-50 text-purple-600" : "hover:bg-gray-50"
                  }`}
                >
                  <User className="size-5" />
                  Profile
                </button>
                <button
                  onClick={() => setActiveTab("theme")}
                  className={`w-full px-4 py-3 rounded-lg text-left flex items-center gap-3 transition-colors ${
                    activeTab === "theme" ? "bg-purple-50 text-purple-600" : "hover:bg-gray-50"
                  }`}
                >
                  <Palette className="size-5" />
                  Theme
                </button>
              </div>
            </div>

            {activeTab === "blocks" && (
              <div className="bg-white rounded-xl border border-gray-200 p-4">
                <h3 className="text-sm mb-3 text-gray-600">Add Block</h3>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => handleAddBlock("link")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <LinkIcon className="size-5" />
                    <span className="text-xs">Link</span>
                  </button>
                  <button
                    onClick={() => handleAddBlock("text")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <Type className="size-5" />
                    <span className="text-xs">Text</span>
                  </button>
                  <button
                    onClick={() => handleAddBlock("image")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <ImageIcon className="size-5" />
                    <span className="text-xs">Image</span>
                  </button>
                  <button
                    onClick={() => handleAddBlock("social")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <User className="size-5" />
                    <span className="text-xs">Social</span>
                  </button>
                  <button
                    onClick={() => handleAddBlock("nft")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <NFTIcon className="size-5" />
                    <span className="text-xs">NFT</span>
                  </button>
                  <button
                    onClick={() => handleAddBlock("token")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <Coins className="size-5" />
                    <span className="text-xs">Token</span>
                  </button>
                  <button
                    onClick={() => handleAddBlock("chart")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <TrendingUp className="size-5" />
                    <span className="text-xs">Chart</span>
                  </button>
                  <button
                    onClick={() => handleAddBlock("video")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <PlayCircle className="size-5" />
                    <span className="text-xs">Video</span>
                  </button>
                  <button
                    onClick={() => handleAddBlock("music")}
                    className="p-3 border border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-colors flex flex-col items-center gap-2"
                  >
                    <Music className="size-5" />
                    <span className="text-xs">Music</span>
                  </button>
                </div>
              </div>
            )}

            {activeTab === "pages" && (
              <div className="bg-white rounded-xl border border-gray-200 p-4">
                <PageManager
                  pages={profile.pages}
                  currentPageId={currentPageId}
                  onSelectPage={setCurrentPageId}
                  onCreatePage={handleCreatePage}
                  onDeletePage={handleDeletePage}
                  onToggleFavorite={handleToggleFavorite}
                  onRenamePage={handleRenamePage}
                  onSharePage={handleSharePage}
                  username={profile.username}
                />
              </div>
            )}
          </div>

          {/* Main Content */}
          <div className="bg-white rounded-xl border border-gray-200 p-8">
            {activeTab === "blocks" && currentPage && (
              <div>
                <div className="flex items-center justify-between mb-6">
                  <h2 className="text-2xl">
                    {currentPage.name}
                    <span className="text-sm text-gray-500 ml-3">/{currentPage.slug}</span>
                  </h2>
                  <button
                    onClick={() => handleSharePage(currentPage)}
                    className="px-3 py-2 text-sm border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
                  >
                    <Copy className="size-4" />
                    Share
                  </button>
                </div>

                {currentPage.blocks.length === 0 ? (
                  <div className="text-center py-16">
                    <Grid3x3 className="size-16 text-gray-300 mx-auto mb-4" />
                    <p className="text-gray-600 mb-2">No blocks yet</p>
                    <p className="text-sm text-gray-500">Add your first block from the sidebar!</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4 auto-rows-[100px] md:auto-rows-[120px]">
                    {currentPage.blocks.map((block) => (
                      <div
                        key={block.id}
                        className={`relative group rounded-xl p-4 flex flex-col justify-center items-center text-center cursor-pointer transition-transform hover:scale-105 overflow-hidden`}
                        style={{
                          gridColumn: `span ${block.width}`,
                          gridRow: `span ${block.height}`,
                          ...getBlockBackground(block),
                          color: block.textColor,
                        }}
                        onClick={() => setEditingBlock(block)}
                      >
                        {block.bgType === "image" && block.bgImage && (
                          <div className="absolute inset-0 bg-black/30"></div>
                        )}
                        <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1 z-10">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setEditingBlock(block);
                            }}
                            className="p-1.5 bg-white/90 rounded-lg hover:bg-white transition-colors"
                          >
                            <Edit className="size-3 text-gray-700" />
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteBlock(block.id);
                            }}
                            className="p-1.5 bg-white/90 rounded-lg hover:bg-white transition-colors"
                          >
                            <Trash2 className="size-3 text-red-600" />
                          </button>
                        </div>
                        <div className="relative z-10">
                          {block.iconImage && <img src={block.iconImage} alt="icon" className="size-8 rounded-lg object-cover mb-2 mx-auto" />}
                          {!block.iconImage && block.type === "social" && block.socialPlatform && block.socialPlatform !== "custom" && (() => {
                            const Icon = socialPlatforms[block.socialPlatform].icon;
                            return <Icon className="size-8 mb-2 mx-auto" />;
                          })()}
                          {!block.iconImage && !(block.type === "social" && block.socialPlatform) && block.icon && <div className="text-3xl mb-2">{block.icon}</div>}
                          <div className="text-sm">{block.title}</div>
                          {block.content && <div className="text-xs opacity-75 mt-1 line-clamp-2">{block.content}</div>}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === "pages" && (
              <div>
                <h2 className="text-2xl mb-6">Manage Your Pages</h2>
                <p className="text-gray-600 mb-6">
                  Create multiple Bento pages for different purposes. Each page has its own unique URL and set of blocks.
                </p>
                <div className="grid md:grid-cols-2 gap-4">
                  {profile.pages.map((page) => (
                    <div
                      key={page.id}
                      className="p-6 border-2 border-gray-200 rounded-xl hover:border-purple-500 transition-colors cursor-pointer"
                      onClick={() => {
                        setCurrentPageId(page.id);
                        setActiveTab("blocks");
                      }}
                    >
                      <div className="flex items-start justify-between mb-3">
                        <h3 className="text-lg">{page.name}</h3>
                        {page.isFavorite && (
                          <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-1 rounded">
                            Favorite
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-600 mb-2">/{page.slug}</p>
                      <p className="text-sm text-gray-500">{page.blocks.length} blocks</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {activeTab === "profile" && (
              <ProfileEditor profile={profile} onUpdate={updateProfile} />
            )}

            {activeTab === "theme" && currentPage && (
              <ThemeEditor
                profile={profile}
                onUpdate={(updatedProfile) => {
                  updateProfile({
                    ...updatedProfile,
                    pages: updatedProfile.pages.map((p) =>
                      p.id === currentPageId ? { ...p, theme: updatedProfile.theme } : p
                    ),
                  });
                }}
              />
            )}
          </div>
        </div>
      </div>

      {/* Block Editor Modal */}
      {editingBlock && (
        <BlockEditor
          block={editingBlock}
          onSave={handleUpdateBlock}
          onClose={() => setEditingBlock(null)}
        />
      )}

      {/* Share Modal */}
      {shareUrl && (
        <ShareModal url={shareUrl} onClose={() => setShareUrl(null)} />
      )}
    </div>
  );
}
