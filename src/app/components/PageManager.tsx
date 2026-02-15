import { BentoPage } from "../types";
import { Plus, Star, Share2, Trash2, Edit, ExternalLink } from "lucide-react";
import { useState } from "react";

interface PageManagerProps {
  pages: BentoPage[];
  currentPageId: string;
  onSelectPage: (pageId: string) => void;
  onCreatePage: () => void;
  onDeletePage: (pageId: string) => void;
  onToggleFavorite: (pageId: string) => void;
  onRenamePage: (pageId: string, name: string) => void;
  onSharePage: (page: BentoPage) => void;
  username: string;
}

export default function PageManager({
  pages,
  currentPageId,
  onSelectPage,
  onCreatePage,
  onDeletePage,
  onToggleFavorite,
  onRenamePage,
  onSharePage,
  username,
}: PageManagerProps) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState("");

  const handleStartEdit = (page: BentoPage) => {
    setEditingId(page.id);
    setEditName(page.name);
  };

  const handleSaveEdit = (pageId: string) => {
    if (editName.trim()) {
      onRenamePage(pageId, editName.trim());
    }
    setEditingId(null);
  };

  const sortedPages = [...pages].sort((a, b) => {
    if (a.isFavorite && !b.isFavorite) return -1;
    if (!a.isFavorite && b.isFavorite) return 1;
    return b.updatedAt - a.updatedAt;
  });

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm">Your Pages</h3>
        <button
          onClick={onCreatePage}
          className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
          title="Create new page"
        >
          <Plus className="size-4" />
        </button>
      </div>

      <div className="space-y-2 max-h-[400px] overflow-y-auto">
        {sortedPages.map((page) => (
          <div
            key={page.id}
            className={`group p-3 rounded-lg border-2 transition-all ${
              currentPageId === page.id
                ? "border-purple-500 bg-purple-50"
                : "border-gray-200 hover:border-gray-300"
            }`}
          >
            <div className="flex items-start gap-2">
              <button
                onClick={() => onToggleFavorite(page.id)}
                className="mt-0.5 p-1 hover:bg-white rounded transition-colors"
              >
                <Star
                  className={`size-4 ${
                    page.isFavorite ? "fill-yellow-400 text-yellow-400" : "text-gray-400"
                  }`}
                />
              </button>

              <div className="flex-1 min-w-0">
                {editingId === page.id ? (
                  <input
                    type="text"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    onBlur={() => handleSaveEdit(page.id)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") handleSaveEdit(page.id);
                      if (e.key === "Escape") setEditingId(null);
                    }}
                    className="w-full px-2 py-1 border border-purple-500 rounded text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                    autoFocus
                  />
                ) : (
                  <button
                    onClick={() => onSelectPage(page.id)}
                    className="text-left w-full"
                  >
                    <div className="text-sm mb-1 truncate">{page.name}</div>
                    <div className="text-xs text-gray-500">
                      {page.blocks.length} blocks · {page.slug}
                    </div>
                  </button>
                )}
              </div>

              <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                  onClick={() => handleStartEdit(page)}
                  className="p-1.5 hover:bg-white rounded transition-colors"
                  title="Rename"
                >
                  <Edit className="size-3.5" />
                </button>
                <button
                  onClick={() => onSharePage(page)}
                  className="p-1.5 hover:bg-white rounded transition-colors"
                  title="Share"
                >
                  <Share2 className="size-3.5" />
                </button>
                <button
                  onClick={() => window.open(`/${username}/${page.slug}`, "_blank")}
                  className="p-1.5 hover:bg-white rounded transition-colors"
                  title="Preview"
                >
                  <ExternalLink className="size-3.5" />
                </button>
                {pages.length > 1 && (
                  <button
                    onClick={() => {
                      if (confirm(`Delete "${page.name}"?`)) {
                        onDeletePage(page.id);
                      }
                    }}
                    className="p-1.5 hover:bg-red-50 rounded transition-colors text-red-600"
                    title="Delete"
                  >
                    <Trash2 className="size-3.5" />
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
