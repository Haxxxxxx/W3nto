import { useState, useEffect, useRef } from "react";
import { Profile } from "../types";
import { Upload, Eye, EyeOff } from "lucide-react";
import { createImageInput } from "../utils/imageUpload";
import { isReservedUsername, isUsernameAvailable } from "../utils/storage";

interface ProfileEditorProps {
  profile: Profile;
  onUpdate: (profile: Profile) => void;
}

export default function ProfileEditor({ profile, onUpdate }: ProfileEditorProps) {
  const [usernameStatus, setUsernameStatus] = useState<"idle" | "checking" | "available" | "taken">("idle");
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();
  const originalUsernameRef = useRef(profile.username);

  useEffect(() => {
    const username = profile.username;
    if (!username || username === originalUsernameRef.current) {
      setUsernameStatus("idle");
      return;
    }
    if (isReservedUsername(username)) {
      setUsernameStatus("taken");
      return;
    }

    setUsernameStatus("checking");
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      try {
        const available = await isUsernameAvailable(username);
        setUsernameStatus(available ? "available" : "taken");
      } catch {
        setUsernameStatus("idle");
      }
    }, 500);

    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [profile.username]);
  const handleAvatarUpload = () => {
    createImageInput((dataUrl) => {
      onUpdate({ ...profile, avatar: dataUrl });
    });
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl mb-6">Profile Settings</h2>

      <div>
        <label className="block text-sm mb-2">Avatar</label>
        <div className="flex gap-4 items-start">
          {profile.avatar ? (
            <img
              src={profile.avatar}
              alt={profile.displayName}
              className="size-24 rounded-full object-cover border-2 border-gray-200"
            />
          ) : (
            <div className="size-24 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-white text-2xl border-2 border-gray-200">
              {profile.displayName.charAt(0)}
            </div>
          )}
          <div className="flex-1">
            <button
              type="button"
              onClick={handleAvatarUpload}
              className="w-full px-4 py-3 border-2 border-dashed border-gray-300 rounded-lg hover:border-purple-500 transition-colors flex items-center justify-center gap-2 mb-2"
            >
              <Upload className="size-5" />
              Upload Avatar
            </button>
            <p className="text-sm text-gray-500">Or enter an image URL below</p>
            <input
              type="url"
              value={profile.avatar}
              onChange={(e) => onUpdate({ ...profile, avatar: e.target.value })}
              className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 mt-2"
              placeholder="https://example.com/avatar.jpg"
            />
          </div>
        </div>
      </div>

      <div>
        <label className="block text-sm mb-2">Username</label>
        <div className="flex items-center gap-2">
          <span className="text-gray-500">w3nto.app/</span>
          <input
            type="text"
            value={profile.username}
            onChange={(e) => {
              const value = e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, "");
              if (!isReservedUsername(value)) {
                onUpdate({ ...profile, username: value });
              }
            }}
            className={`flex-1 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 ${
              isReservedUsername(profile.username) ? "border-red-400" : "border-gray-200"
            }`}
            placeholder="username"
            pattern="[a-z0-9_]+"
          />
        </div>
        {isReservedUsername(profile.username) ? (
          <p className="text-sm text-red-500 mt-1">This username is reserved</p>
        ) : usernameStatus === "checking" ? (
          <p className="text-sm text-gray-400 mt-1">Checking availability...</p>
        ) : usernameStatus === "taken" ? (
          <p className="text-sm text-red-500 mt-1">Username taken</p>
        ) : usernameStatus === "available" ? (
          <p className="text-sm text-green-600 mt-1">Username available</p>
        ) : (
          <p className="text-sm text-gray-500 mt-1">Only lowercase letters, numbers, and underscores</p>
        )}
      </div>

      <div>
        <label className="block text-sm mb-2">Display Name</label>
        <input
          type="text"
          value={profile.displayName}
          onChange={(e) => onUpdate({ ...profile, displayName: e.target.value })}
          className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
          placeholder="Your Name"
        />
      </div>

      <div>
        <label className="block text-sm mb-2">Bio</label>
        <textarea
          value={profile.bio}
          onChange={(e) => onUpdate({ ...profile, bio: e.target.value })}
          className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 min-h-[100px]"
          placeholder="Tell people about yourself..."
        />
      </div>

      <div className="p-4 bg-gray-50 rounded-lg">
        <div className="flex items-center justify-between mb-3">
          <div>
            <div className="text-sm mb-1">Wallet Address</div>
            <div className="font-mono text-sm text-gray-600 break-all">
              {profile.walletAddress}
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3 pt-3 border-t border-gray-200">
          <button
            onClick={() => onUpdate({ ...profile, showWallet: !profile.showWallet })}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
              profile.showWallet
                ? "bg-purple-100 text-purple-700"
                : "bg-gray-200 text-gray-600"
            }`}
          >
            {profile.showWallet ? <Eye className="size-4" /> : <EyeOff className="size-4" />}
            {profile.showWallet ? "Wallet Visible" : "Wallet Hidden"}
          </button>
          <p className="text-xs text-gray-500">
            {profile.showWallet
              ? "Wallet address will be shown on your public profile"
              : "Wallet address will be hidden from your public profile"}
          </p>
        </div>
      </div>
    </div>
  );
}
