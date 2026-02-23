import { useState } from "react";
import { Block, SocialPlatform } from "../types";
import { X, Upload, Image as ImageIcon } from "lucide-react";
import { createImageInput } from "../utils/imageUpload";
import { socialPlatforms, chainNames } from "../utils/socialPlatforms";

interface BlockEditorProps {
  block: Block;
  onSave: (block: Block) => void;
  onClose: () => void;
}

export default function BlockEditor({ block, onSave, onClose }: BlockEditorProps) {
  const [editedBlock, setEditedBlock] = useState<Block>({
    ...block,
    bgType: block.bgType || "color",
    bgColor: block.bgColor || "#8b5cf6",
    textColor: block.textColor || "#ffffff",
    icon: block.icon || "",
    content: block.content || "",
    url: block.url || "",
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave(editedBlock);
  };

  const handleImageUpload = (field: "bgImage" | "imageUrl" | "iconImage" | "nftImage") => {
    createImageInput((dataUrl) => {
      setEditedBlock({ ...editedBlock, [field]: dataUrl });
    });
  };

  const handleSocialPlatformChange = (platform: SocialPlatform) => {
    const platformInfo = socialPlatforms[platform];
    setEditedBlock({
      ...editedBlock,
      socialPlatform: platform,
      bgColor: platformInfo.color,
      title: platformInfo.name,
    });
  };

  const getBackgroundStyle = () => {
    if (editedBlock.bgType === "gradient" && editedBlock.bgGradient) {
      return {
        background: `linear-gradient(${editedBlock.bgGradient.direction}, ${editedBlock.bgGradient.from}, ${editedBlock.bgGradient.to})`,
      };
    }
    if (editedBlock.bgType === "image" && editedBlock.bgImage) {
      return {
        backgroundImage: `url(${editedBlock.bgImage})`,
        backgroundSize: "cover",
        backgroundPosition: "center",
      };
    }
    return { backgroundColor: editedBlock.bgColor };
  };

  const getIconDisplay = () => {
    if (editedBlock.iconImage) {
      return <img src={editedBlock.iconImage} alt="icon" className="size-12 rounded-lg object-cover" />;
    }
    if (editedBlock.type === "social" && editedBlock.socialPlatform && editedBlock.socialPlatform !== "custom") {
      const Icon = socialPlatforms[editedBlock.socialPlatform].icon;
      return <Icon className="size-12" />;
    }
    if (editedBlock.icon) {
      return <div className="text-4xl">{editedBlock.icon}</div>;
    }
    return null;
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-6 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto my-6">
        <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between z-10">
          <h2 className="text-xl">Edit Block</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="size-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Preview */}
          <div>
            <label className="block text-sm mb-2">Preview</label>
            <div
              className="w-full h-32 rounded-xl flex items-center justify-center relative overflow-hidden"
              style={{
                ...getBackgroundStyle(),
                color: editedBlock.textColor,
              }}
            >
              {editedBlock.bgType === "image" && editedBlock.bgImage && (
                <div className="absolute inset-0 bg-black/30"></div>
              )}
              <div className="relative z-10 text-center">
                {getIconDisplay()}
                <div className="text-sm mt-2">{editedBlock.title}</div>
              </div>
            </div>
          </div>

          <div>
            <label className="block text-sm mb-2">Block Type</label>
            <div className="text-sm px-3 py-2 bg-gray-50 rounded-lg capitalize">
              {editedBlock.type}
            </div>
          </div>

          {/* Social Platform Selection */}
          {editedBlock.type === "social" && (
            <div>
              <label className="block text-sm mb-2">Social Platform</label>
              <div className="grid grid-cols-3 gap-2 max-h-64 overflow-y-auto">
                {Object.entries(socialPlatforms).map(([key, platform]) => {
                  const Icon = platform.icon;
                  return (
                    <button
                      key={key}
                      type="button"
                      onClick={() => handleSocialPlatformChange(key as SocialPlatform)}
                      className={`p-3 border-2 rounded-lg transition-all flex flex-col items-center gap-2 ${
                        editedBlock.socialPlatform === key
                          ? "border-purple-500 bg-purple-50"
                          : "border-gray-200 hover:border-gray-300"
                      }`}
                    >
                      <Icon className="size-5" style={{ color: platform.color }} />
                      <span className="text-xs">{platform.name}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Icon Selection */}
          <div>
            <label className="block text-sm mb-2">Icon</label>
            <div className="space-y-3">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={editedBlock.icon || ""}
                  onChange={(e) => setEditedBlock({ ...editedBlock, icon: e.target.value, iconImage: undefined })}
                  className="flex-1 px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="🔗 Enter emoji"
                />
                <button
                  type="button"
                  onClick={() => handleImageUpload("iconImage")}
                  className="px-4 py-2 border border-gray-200 rounded-lg hover:border-purple-500 transition-colors flex items-center gap-2"
                >
                  <Upload className="size-4" />
                  Upload
                </button>
              </div>
              {editedBlock.iconImage && (
                <div className="flex items-center gap-2">
                  <img src={editedBlock.iconImage} alt="icon" className="size-12 rounded-lg object-cover" />
                  <button
                    type="button"
                    onClick={() => setEditedBlock({ ...editedBlock, iconImage: undefined })}
                    className="text-sm text-red-600 hover:underline"
                  >
                    Remove
                  </button>
                </div>
              )}
            </div>
          </div>

          <div>
            <label className="block text-sm mb-2">Title</label>
            <input
              type="text"
              value={editedBlock.title}
              onChange={(e) => setEditedBlock({ ...editedBlock, title: e.target.value })}
              className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
              placeholder="Block title"
              required
            />
          </div>

          <div>
            <label className="block text-sm mb-2">Content / Description</label>
            <textarea
              value={editedBlock.content}
              onChange={(e) => setEditedBlock({ ...editedBlock, content: e.target.value })}
              className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 min-h-[80px]"
              placeholder="Block description"
            />
          </div>

          {(editedBlock.type === "link" || editedBlock.type === "social" || editedBlock.type === "video" || editedBlock.type === "music") && (
            <div>
              <label className="block text-sm mb-2">URL</label>
              <input
                type="url"
                value={editedBlock.url || ""}
                onChange={(e) => setEditedBlock({ ...editedBlock, url: e.target.value })}
                className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                placeholder={
                  editedBlock.type === "video" ? "https://youtube.com/watch?v=..." :
                  editedBlock.type === "music" ? "https://open.spotify.com/track/..." :
                  "https://example.com"
                }
              />
            </div>
          )}

          {editedBlock.type === "image" && (
            <div>
              <label className="block text-sm mb-2">Image</label>
              {editedBlock.imageUrl && (
                <img
                  src={editedBlock.imageUrl}
                  alt="Preview"
                  className="w-full h-40 object-cover rounded-lg mb-2"
                />
              )}
              <button
                type="button"
                onClick={() => handleImageUpload("imageUrl")}
                className="w-full px-4 py-3 border-2 border-dashed border-gray-300 rounded-lg hover:border-purple-500 transition-colors flex items-center justify-center gap-2"
              >
                <Upload className="size-5" />
                {editedBlock.imageUrl ? "Change Image" : "Upload Image"}
              </button>
            </div>
          )}

          {/* NFT Fields */}
          {editedBlock.type === "nft" && (
            <>
              <div>
                <label className="block text-sm mb-2">NFT Image</label>
                {editedBlock.nftImage && (
                  <img
                    src={editedBlock.nftImage}
                    alt="NFT"
                    className="w-full h-40 object-cover rounded-lg mb-2"
                  />
                )}
                <button
                  type="button"
                  onClick={() => handleImageUpload("nftImage")}
                  className="w-full px-4 py-3 border-2 border-dashed border-gray-300 rounded-lg hover:border-purple-500 transition-colors flex items-center justify-center gap-2"
                >
                  <Upload className="size-5" />
                  {editedBlock.nftImage ? "Change NFT Image" : "Upload NFT Image"}
                </button>
              </div>
              <div>
                <label className="block text-sm mb-2">NFT Name</label>
                <input
                  type="text"
                  value={editedBlock.nftName || ""}
                  onChange={(e) => setEditedBlock({ ...editedBlock, nftName: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="Bored Ape #1234"
                />
              </div>
              <div>
                <label className="block text-sm mb-2">Collection Name</label>
                <input
                  type="text"
                  value={editedBlock.nftCollection || ""}
                  onChange={(e) => setEditedBlock({ ...editedBlock, nftCollection: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="Bored Ape Yacht Club"
                />
              </div>
              <div>
                <label className="block text-sm mb-2">Token ID (optional)</label>
                <input
                  type="text"
                  value={editedBlock.nftTokenId || ""}
                  onChange={(e) => setEditedBlock({ ...editedBlock, nftTokenId: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="1234"
                />
              </div>
            </>
          )}

          {/* Token/Chart Fields */}
          {(editedBlock.type === "token" || editedBlock.type === "chart") && (
            <>
              <div>
                <label className="block text-sm mb-2">Token Contract Address</label>
                <input
                  type="text"
                  value={editedBlock.tokenAddress || ""}
                  onChange={(e) => setEditedBlock({ ...editedBlock, tokenAddress: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 font-mono text-sm"
                  placeholder="0x..."
                />
              </div>
              <div>
                <label className="block text-sm mb-2">Blockchain</label>
                <select
                  value={editedBlock.chainId || "1"}
                  onChange={(e) => setEditedBlock({ ...editedBlock, chainId: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                >
                  {Object.entries(chainNames).map(([id, name]) => (
                    <option key={id} value={id}>
                      {name}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}

          {/* Background Settings */}
          <div className="pt-4 border-t border-gray-200">
            <label className="block text-sm mb-3">Background</label>
            <div className="flex gap-2 mb-4">
              <button
                type="button"
                onClick={() => setEditedBlock({ ...editedBlock, bgType: "color" })}
                className={`flex-1 px-4 py-2 rounded-lg border-2 transition-colors ${
                  editedBlock.bgType === "color"
                    ? "border-purple-500 bg-purple-50"
                    : "border-gray-200 hover:border-gray-300"
                }`}
              >
                Color
              </button>
              <button
                type="button"
                onClick={() =>
                  setEditedBlock({
                    ...editedBlock,
                    bgType: "gradient",
                    bgGradient: editedBlock.bgGradient || {
                      from: "#8b5cf6",
                      to: "#ec4899",
                      direction: "135deg",
                    },
                  })
                }
                className={`flex-1 px-4 py-2 rounded-lg border-2 transition-colors ${
                  editedBlock.bgType === "gradient"
                    ? "border-purple-500 bg-purple-50"
                    : "border-gray-200 hover:border-gray-300"
                }`}
              >
                Gradient
              </button>
              <button
                type="button"
                onClick={() => setEditedBlock({ ...editedBlock, bgType: "image" })}
                className={`flex-1 px-4 py-2 rounded-lg border-2 transition-colors ${
                  editedBlock.bgType === "image"
                    ? "border-purple-500 bg-purple-50"
                    : "border-gray-200 hover:border-gray-300"
                }`}
              >
                Image
              </button>
            </div>

            {editedBlock.bgType === "color" && (
              <div className="flex gap-2">
                <input
                  type="color"
                  value={editedBlock.bgColor || "#8b5cf6"}
                  onChange={(e) => setEditedBlock({ ...editedBlock, bgColor: e.target.value })}
                  className="size-10 rounded-lg border border-gray-200 cursor-pointer"
                />
                <input
                  type="text"
                  value={editedBlock.bgColor || "#8b5cf6"}
                  onChange={(e) => setEditedBlock({ ...editedBlock, bgColor: e.target.value })}
                  className="flex-1 px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  placeholder="#8b5cf6"
                />
              </div>
            )}

            {editedBlock.bgType === "gradient" && editedBlock.bgGradient && (
              <div className="space-y-3">
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs mb-1 text-gray-600">From</label>
                    <div className="flex gap-2">
                      <input
                        type="color"
                        value={editedBlock.bgGradient.from}
                        onChange={(e) =>
                          setEditedBlock({
                            ...editedBlock,
                            bgGradient: { ...editedBlock.bgGradient!, from: e.target.value },
                          })
                        }
                        className="size-10 rounded-lg border border-gray-200 cursor-pointer"
                      />
                      <input
                        type="text"
                        value={editedBlock.bgGradient.from}
                        onChange={(e) =>
                          setEditedBlock({
                            ...editedBlock,
                            bgGradient: { ...editedBlock.bgGradient!, from: e.target.value },
                          })
                        }
                        className="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs mb-1 text-gray-600">To</label>
                    <div className="flex gap-2">
                      <input
                        type="color"
                        value={editedBlock.bgGradient.to}
                        onChange={(e) =>
                          setEditedBlock({
                            ...editedBlock,
                            bgGradient: { ...editedBlock.bgGradient!, to: e.target.value },
                          })
                        }
                        className="size-10 rounded-lg border border-gray-200 cursor-pointer"
                      />
                      <input
                        type="text"
                        value={editedBlock.bgGradient.to}
                        onChange={(e) =>
                          setEditedBlock({
                            ...editedBlock,
                            bgGradient: { ...editedBlock.bgGradient!, to: e.target.value },
                          })
                        }
                        className="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                      />
                    </div>
                  </div>
                </div>
                <div>
                  <label className="block text-xs mb-1 text-gray-600">Direction</label>
                  <select
                    value={editedBlock.bgGradient.direction}
                    onChange={(e) =>
                      setEditedBlock({
                        ...editedBlock,
                        bgGradient: { ...editedBlock.bgGradient!, direction: e.target.value },
                      })
                    }
                    className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                  >
                    <option value="135deg">Diagonal</option>
                    <option value="90deg">Horizontal</option>
                    <option value="180deg">Vertical</option>
                    <option value="45deg">Diagonal Up</option>
                  </select>
                </div>
              </div>
            )}

            {editedBlock.bgType === "image" && (
              <div>
                {editedBlock.bgImage && (
                  <div className="mb-2 relative">
                    <img
                      src={editedBlock.bgImage}
                      alt="Background"
                      className="w-full h-32 object-cover rounded-lg"
                    />
                  </div>
                )}
                <button
                  type="button"
                  onClick={() => handleImageUpload("bgImage")}
                  className="w-full px-4 py-3 border-2 border-dashed border-gray-300 rounded-lg hover:border-purple-500 transition-colors flex items-center justify-center gap-2"
                >
                  <ImageIcon className="size-5" />
                  {editedBlock.bgImage ? "Change Background" : "Upload Background"}
                </button>
              </div>
            )}
          </div>

          <div>
            <label className="block text-sm mb-2">Text Color</label>
            <div className="flex gap-2">
              <input
                type="color"
                value={editedBlock.textColor || "#ffffff"}
                onChange={(e) => setEditedBlock({ ...editedBlock, textColor: e.target.value })}
                className="size-10 rounded-lg border border-gray-200 cursor-pointer"
              />
              <input
                type="text"
                value={editedBlock.textColor || "#ffffff"}
                onChange={(e) => setEditedBlock({ ...editedBlock, textColor: e.target.value })}
                className="flex-1 px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                placeholder="#ffffff"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm mb-2">Width (columns)</label>
              <select
                value={editedBlock.width}
                onChange={(e) => setEditedBlock({ ...editedBlock, width: Number(e.target.value) })}
                className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
              >
                <option value={1}>1 column</option>
                <option value={2}>2 columns</option>
                <option value={3}>3 columns</option>
                <option value={4}>4 columns</option>
              </select>
            </div>

            <div>
              <label className="block text-sm mb-2">Height (rows)</label>
              <select
                value={editedBlock.height}
                onChange={(e) => setEditedBlock({ ...editedBlock, height: Number(e.target.value) })}
                className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
              >
                <option value={1}>1 row</option>
                <option value={2}>2 rows</option>
                <option value={3}>3 rows</option>
              </select>
            </div>
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-6 py-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-6 py-3 bg-black text-white rounded-lg hover:bg-gray-800 transition-colors"
            >
              Save Changes
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
