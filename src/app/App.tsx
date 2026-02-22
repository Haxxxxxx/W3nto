import { RouterProvider } from "react-router";
import { router } from "./routes";
import { WalletProvider } from "./contexts/WalletContext";
import { Toaster } from "./components/ui/sonner";

export default function App() {
  return (
    <WalletProvider>
      <RouterProvider router={router} />
      <Toaster position="bottom-right" richColors />
    </WalletProvider>
  );
}
