import { useState } from "react";
import { QRCodeCanvas } from "qrcode.react";
import { X, Copy, Check } from "lucide-react";

interface ShareModalProps {
  url: string;
  onClose: () => void;
}

export default function ShareModal({ url, onClose }: ShareModalProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback for non-HTTPS or denied permissions
      const textArea = document.createElement("textarea");
      textArea.value = url;
      textArea.style.position = "fixed";
      textArea.style.opacity = "0";
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand("copy");
      document.body.removeChild(textArea);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-2xl shadow-xl max-w-sm w-full mx-4 p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold">Share Page</h3>
          <button
            onClick={onClose}
            className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="size-5 text-gray-500" />
          </button>
        </div>

        <div className="flex justify-center mb-6">
          <div className="p-4 bg-white border-2 border-gray-100 rounded-xl">
            <QRCodeCanvas
              value={url}
              size={240}
              level="M"
              includeMargin={false}
            />
          </div>
        </div>

        <div className="flex items-center gap-2 p-3 bg-gray-50 rounded-lg">
          <span className="text-sm text-gray-600 truncate flex-1 select-all font-mono">
            {url}
          </span>
          <button
            onClick={handleCopy}
            className="shrink-0 p-2 hover:bg-gray-200 rounded-lg transition-colors"
          >
            {copied ? (
              <Check className="size-4 text-green-600" />
            ) : (
              <Copy className="size-4 text-gray-500" />
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
