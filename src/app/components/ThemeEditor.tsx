import { Profile } from "../types";

interface ThemeEditorProps {
  profile: Profile;
  onUpdate: (profile: Profile) => void;
}

const fonts = [
  { name: "Inter", value: "Inter" },
  { name: "Roboto", value: "Roboto" },
  { name: "Open Sans", value: "Open Sans" },
  { name: "Montserrat", value: "Montserrat" },
  { name: "Poppins", value: "Poppins" },
  { name: "Playfair Display", value: "Playfair Display" },
  { name: "Raleway", value: "Raleway" },
  { name: "Space Grotesk", value: "Space Grotesk" },
];

export default function ThemeEditor({ profile, onUpdate }: ThemeEditorProps) {
  const updateTheme = (key: keyof typeof profile.theme, value: any) => {
    onUpdate({
      ...profile,
      theme: {
        ...profile.theme,
        [key]: value,
      },
    });
  };

  const theme = {
    backgroundColor: profile.theme.backgroundColor || "#ffffff",
    cardBackground: profile.theme.cardBackground || "#f9fafb",
    primaryColor: profile.theme.primaryColor || "#8b5cf6",
    secondaryColor: profile.theme.secondaryColor || "#ec4899",
    textColor: profile.theme.textColor || "#111827",
    borderRadius: profile.theme.borderRadius || "16px",
    fontFamily: profile.theme.fontFamily || "Inter",
    buttonStyle: profile.theme.buttonStyle || "rounded",
    buttonBackground: profile.theme.buttonBackground || "#8b5cf6",
    buttonText: profile.theme.buttonText || "#ffffff",
  };

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl mb-2">Theme Customization</h2>
        <p className="text-sm text-gray-600">Customize the look and feel of your profile</p>
      </div>

      <div>
        <label className="block text-sm mb-2">Font Family</label>
        <select
          value={theme.fontFamily}
          onChange={(e) => updateTheme("fontFamily", e.target.value)}
          className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
          style={{ fontFamily: theme.fontFamily }}
        >
          {fonts.map((font) => (
            <option key={font.value} value={font.value} style={{ fontFamily: font.value }}>
              {font.name}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-sm mb-3">Button Style</label>
        <div className="grid grid-cols-2 gap-3">
          <button
            onClick={() => updateTheme("buttonStyle", "rounded")}
            className={`p-4 border-2 rounded-lg transition-all ${
              theme.buttonStyle === "rounded"
                ? "border-purple-500 bg-purple-50"
                : "border-gray-200 hover:border-gray-300"
            }`}
          >
            <div className="w-full h-12 bg-gray-900 rounded-full flex items-center justify-center text-white text-sm mb-2">
              Rounded
            </div>
            <div className="text-xs text-gray-600">Fully rounded edges</div>
          </button>
          <button
            onClick={() => updateTheme("buttonStyle", "squared")}
            className={`p-4 border-2 rounded-lg transition-all ${
              theme.buttonStyle === "squared"
                ? "border-purple-500 bg-purple-50"
                : "border-gray-200 hover:border-gray-300"
            }`}
          >
            <div className="w-full h-12 bg-gray-900 rounded-lg flex items-center justify-center text-white text-sm mb-2">
              Squared
            </div>
            <div className="text-xs text-gray-600">Subtle rounded corners</div>
          </button>
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-sm">Colors</h3>
        <div className="grid grid-cols-2 gap-4">
          {[
            { label: "Background", key: "backgroundColor" as const, val: theme.backgroundColor },
            { label: "Card Background", key: "cardBackground" as const, val: theme.cardBackground },
            { label: "Primary Color", key: "primaryColor" as const, val: theme.primaryColor },
            { label: "Secondary Color", key: "secondaryColor" as const, val: theme.secondaryColor },
            { label: "Text Color", key: "textColor" as const, val: theme.textColor },
            { label: "Button Background", key: "buttonBackground" as const, val: theme.buttonBackground },
            { label: "Button Text", key: "buttonText" as const, val: theme.buttonText },
          ].map(({ label, key, val }) => (
            <div key={key}>
              <label className="block text-xs mb-2 text-gray-600">{label}</label>
              <div className="flex gap-2">
                <input
                  type="color"
                  value={val}
                  onChange={(e) => updateTheme(key, e.target.value)}
                  className="size-10 rounded-lg border border-gray-200 cursor-pointer"
                />
                <input
                  type="text"
                  value={val}
                  onChange={(e) => updateTheme(key, e.target.value)}
                  className="flex-1 px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      <div>
        <label className="block text-sm mb-3">Border Radius</label>
        <input
          type="range"
          min="0"
          max="32"
          value={parseInt(theme.borderRadius)}
          onChange={(e) => updateTheme("borderRadius", `${e.target.value}px`)}
          className="w-full"
        />
        <div className="flex justify-between text-xs text-gray-600 mt-1">
          <span>0px (Sharp)</span>
          <span className="font-medium">{theme.borderRadius}</span>
          <span>32px (Round)</span>
        </div>
      </div>

      <div>
        <label className="block text-sm mb-3">Preview</label>
        <div
          className="p-6 rounded-xl border-2 border-gray-200"
          style={{
            backgroundColor: theme.backgroundColor,
            fontFamily: theme.fontFamily,
          }}
        >
          <div className="max-w-sm mx-auto space-y-4">
            <div
              className="p-6 shadow-lg"
              style={{
                backgroundColor: theme.cardBackground,
                borderRadius: theme.borderRadius,
              }}
            >
              <h3
                className="text-xl mb-2"
                style={{ color: theme.textColor }}
              >
                Card Title
              </h3>
              <p
                className="text-sm opacity-75"
                style={{ color: theme.textColor }}
              >
                This is how your content will look with the current theme.
              </p>
            </div>
            <button
              className="w-full py-3 px-6 transition-opacity hover:opacity-90"
              style={{
                backgroundColor: theme.buttonBackground,
                color: theme.buttonText,
                borderRadius: theme.buttonStyle === "rounded" ? "9999px" : theme.borderRadius,
              }}
            >
              Button Example
            </button>
          </div>
        </div>
      </div>

      <div>
        <label className="block text-sm mb-3">Quick Presets</label>
        <div className="grid grid-cols-3 gap-3">
          <button
            onClick={() =>
              onUpdate({
                ...profile,
                theme: {
                  ...profile.theme,
                  backgroundColor: "#ffffff",
                  cardBackground: "#f9fafb",
                  primaryColor: "#8b5cf6",
                  textColor: "#111827",
                  buttonBackground: "#8b5cf6",
                  buttonText: "#ffffff",
                },
              })
            }
            className="p-3 border-2 border-gray-200 rounded-lg hover:border-purple-500 transition-colors"
          >
            <div className="text-sm">Light</div>
          </button>
          <button
            onClick={() =>
              onUpdate({
                ...profile,
                theme: {
                  ...profile.theme,
                  backgroundColor: "#0f172a",
                  cardBackground: "#1e293b",
                  primaryColor: "#8b5cf6",
                  textColor: "#f1f5f9",
                  buttonBackground: "#8b5cf6",
                  buttonText: "#ffffff",
                },
              })
            }
            className="p-3 border-2 border-gray-200 rounded-lg hover:border-purple-500 transition-colors"
          >
            <div className="text-sm">Dark</div>
          </button>
          <button
            onClick={() =>
              onUpdate({
                ...profile,
                theme: {
                  ...profile.theme,
                  backgroundColor: "#fdf2f8",
                  cardBackground: "#ffffff",
                  primaryColor: "#ec4899",
                  textColor: "#831843",
                  buttonBackground: "#ec4899",
                  buttonText: "#ffffff",
                },
              })
            }
            className="p-3 border-2 border-gray-200 rounded-lg hover:border-purple-500 transition-colors"
          >
            <div className="text-sm">Pink</div>
          </button>
        </div>
      </div>
    </div>
  );
}
