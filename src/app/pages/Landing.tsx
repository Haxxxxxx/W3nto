import { useNavigate, Link } from "react-router";
import { useWallet } from "../contexts/WalletContext";
import { Sparkles, Grid3x3, Palette, Link as LinkIcon, Wallet, ArrowRight, Github, Twitter, Instagram, Youtube } from "lucide-react";
import W3ntoLogo from "../components/W3ntoLogo";

export default function Landing() {
  const navigate = useNavigate();
  const { isConnected, connect } = useWallet();

  const handleGetStarted = async () => {
    if (!isConnected) {
      await connect();
    }
    navigate("/dashboard");
  };

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-lg border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
            <W3ntoLogo size={40} />
            <span className="text-xl">
              <span className="font-semibold">W3nto</span>
            </span>
          </Link>
          <button
            onClick={handleGetStarted}
            className="px-6 py-2.5 bg-black text-white rounded-full hover:bg-gray-800 transition-colors flex items-center gap-2"
          >
            <Wallet className="size-4" />
            {isConnected ? "Dashboard" : "Connect Wallet"}
          </button>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-purple-50 via-pink-50 to-orange-50 opacity-50"></div>
        <div className="relative max-w-7xl mx-auto px-6 py-24 text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-white rounded-full border border-gray-200 mb-8">
            <Sparkles className="size-4 text-purple-500" />
            <span className="text-sm">Bento for Web3 Creators</span>
          </div>
          <h1 className="text-6xl mb-6 max-w-4xl mx-auto">
            Your Web3 Identity,
            <br />
            <span className="bg-gradient-to-r from-purple-600 via-pink-600 to-orange-600 bg-clip-text text-transparent">
              Beautifully Organized
            </span>
          </h1>
          <p className="text-xl text-gray-600 mb-12 max-w-2xl mx-auto">
            Connect your wallet, customize your Bento grid, and share your links, NFTs, and content in a stunning portfolio.
          </p>
          <button
            onClick={handleGetStarted}
            className="px-8 py-4 bg-black text-white rounded-full hover:bg-gray-800 transition-colors inline-flex items-center gap-2 text-lg"
          >
            Get Started Free
            <ArrowRight className="size-5" />
          </button>

          {/* Example Bento Preview */}
          <div className="mt-20 max-w-5xl mx-auto">
            <div className="bg-gradient-to-br from-blue-500 via-purple-500 to-pink-500 p-8 rounded-3xl shadow-2xl">
              <div className="bg-white rounded-2xl p-8">
                <div className="flex items-start gap-8 mb-8">
                  <div className="size-20 rounded-full bg-gradient-to-br from-blue-400 to-purple-600 flex items-center justify-center text-white text-2xl">
                    🚀
                  </div>
                  <div className="flex-1">
                    <h3 className="text-2xl mb-2">Alex.eth</h3>
                    <p className="text-gray-600">Web3 Creator & Builder | NFT Enthusiast</p>
                    <div className="flex items-center gap-2 mt-3">
                      <span className="px-3 py-1 bg-gray-100 rounded-full text-sm">0x742d...35Aa</span>
                    </div>
                  </div>
                </div>

                {/* Sample Bento Grid */}
                <div className="grid grid-cols-4 gap-4">
                  <div className="col-span-1 aspect-square bg-gradient-to-br from-blue-500 to-blue-600 rounded-2xl flex items-center justify-center">
                    <Twitter className="size-8 text-white" />
                  </div>
                  <div className="col-span-1 aspect-square bg-gradient-to-br from-pink-500 to-rose-600 rounded-2xl flex items-center justify-center">
                    <Instagram className="size-8 text-white" />
                  </div>
                  <div className="col-span-2 aspect-[2/1] bg-gradient-to-br from-purple-500 to-indigo-600 rounded-2xl flex items-center justify-center">
                    <Youtube className="size-10 text-white" />
                  </div>
                  <div className="col-span-2 row-span-2 bg-gray-100 rounded-2xl overflow-hidden">
                    <div className="size-full bg-gradient-to-br from-gray-200 to-gray-300 flex items-center justify-center text-gray-500">
                      Featured Content
                    </div>
                  </div>
                  <div className="col-span-1 aspect-square bg-gradient-to-br from-gray-800 to-gray-900 rounded-2xl flex items-center justify-center">
                    <Github className="size-8 text-white" />
                  </div>
                  <div className="col-span-1 aspect-square bg-gradient-to-br from-green-500 to-emerald-600 rounded-2xl flex items-center justify-center text-white text-2xl">
                    🎨
                  </div>
                  <div className="col-span-2 aspect-square bg-gradient-to-br from-amber-400 to-orange-500 rounded-2xl p-6 flex flex-col justify-center">
                    <p className="text-white text-lg">Check out my latest NFT collection 🔥</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="max-w-7xl mx-auto px-6 py-24">
        <h2 className="text-4xl text-center mb-16">
          Everything you need to showcase your Web3 presence
        </h2>
        <div className="grid md:grid-cols-3 gap-8">
          <div className="p-8 bg-gray-50 rounded-2xl">
            <div className="size-12 bg-blue-100 rounded-xl flex items-center justify-center mb-4">
              <Wallet className="size-6 text-blue-600" />
            </div>
            <h3 className="text-xl mb-3">Wallet Connection</h3>
            <p className="text-gray-600">
              Connect with MetaMask, WalletConnect, and more. Your Web3 identity is your profile.
            </p>
          </div>
          <div className="p-8 bg-gray-50 rounded-2xl">
            <div className="size-12 bg-purple-100 rounded-xl flex items-center justify-center mb-4">
              <Grid3x3 className="size-6 text-purple-600" />
            </div>
            <h3 className="text-xl mb-3">Bento Grid Layout</h3>
            <p className="text-gray-600">
              Drag and drop blocks to create your perfect layout. Resize and arrange freely.
            </p>
          </div>
          <div className="p-8 bg-gray-50 rounded-2xl">
            <div className="size-12 bg-pink-100 rounded-xl flex items-center justify-center mb-4">
              <Palette className="size-6 text-pink-600" />
            </div>
            <h3 className="text-xl mb-3">Custom Themes</h3>
            <p className="text-gray-600">
              Choose from pre-made themes or create your own with custom colors and styles.
            </p>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="max-w-7xl mx-auto px-6 py-24">
        <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-3xl p-16 text-center text-white">
          <h2 className="text-4xl mb-6">Ready to build your Web3 presence?</h2>
          <p className="text-xl text-gray-300 mb-8 max-w-2xl mx-auto">
            Join thousands of creators, developers, and Web3 enthusiasts sharing their links.
          </p>
          <button
            onClick={handleGetStarted}
            className="px-8 py-4 bg-white text-black rounded-full hover:bg-gray-100 transition-colors inline-flex items-center gap-2 text-lg"
          >
            Connect Your Wallet
            <ArrowRight className="size-5" />
          </button>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-8 flex items-center justify-between text-sm text-gray-600">
          <div>© 2026 W3nto. Built for the decentralized web.</div>
          <div className="flex items-center gap-6">
            <a href="#" className="hover:text-black transition-colors">Twitter</a>
            <a href="#" className="hover:text-black transition-colors">Discord</a>
            <a href="#" className="hover:text-black transition-colors">GitHub</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
