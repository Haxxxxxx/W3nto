import { useNavigate } from "react-router";
import { Home } from "lucide-react";

export default function NotFound() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center max-w-md px-6">
        <div className="text-8xl mb-6">404</div>
        <h1 className="text-3xl mb-3">Page not found</h1>
        <p className="text-gray-600 mb-8">
          The page you're looking for doesn't exist or has been moved.
        </p>
        <button
          onClick={() => navigate("/")}
          className="px-6 py-3 bg-black text-white rounded-full hover:bg-gray-800 transition-colors inline-flex items-center gap-2"
        >
          <Home className="size-4" />
          Go Home
        </button>
      </div>
    </div>
  );
}
