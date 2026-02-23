import { Component, ErrorInfo, ReactNode } from "react";
import { RouterProvider } from "react-router";
import { router } from "./routes";
import { WalletProvider } from "./contexts/WalletContext";
import { Toaster } from "./components/ui/sonner";

class ErrorBoundary extends Component<
  { children: ReactNode },
  { hasError: boolean; error: Error | null }
> {
  constructor(props: { children: ReactNode }) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }
  componentDidCatch(_error: Error, _info: ErrorInfo) {
    // Error captured by boundary — extend with error reporting service if needed
  }
  render() {
    if (this.state.hasError) {
      return (
        <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: "system-ui" }}>
          <div style={{ textAlign: "center", maxWidth: 400 }}>
            <h1 style={{ fontSize: 24, marginBottom: 8 }}>Something went wrong</h1>
            <p style={{ color: "#666", marginBottom: 16 }}>{this.state.error?.message}</p>
            <button onClick={() => window.location.reload()} style={{ padding: "8px 24px", background: "#8b5cf6", color: "white", border: "none", borderRadius: 8, cursor: "pointer" }}>
              Reload
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}

export default function App() {
  return (
    <ErrorBoundary>
      <WalletProvider>
        <RouterProvider router={router} />
        <Toaster position="bottom-right" richColors />
      </WalletProvider>
    </ErrorBoundary>
  );
}
