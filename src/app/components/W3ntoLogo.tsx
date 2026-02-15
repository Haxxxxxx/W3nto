export default function W3ntoLogo({ size = 40 }: { size?: number }) {
  return (
    <div
      className="relative flex items-center justify-center rounded-2xl bg-gradient-to-br from-purple-500 via-pink-500 to-orange-400"
      style={{ width: size, height: size }}
    >
      <svg
        width={size * 0.65}
        height={size * 0.65}
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <rect x="2" y="2" width="6" height="6" rx="1" fill="white" opacity="0.95" />
        <rect x="9" y="2" width="6" height="6" rx="1" fill="white" opacity="0.85" />
        <rect x="16" y="2" width="6" height="6" rx="1" fill="white" opacity="0.95" />
        <rect x="2" y="9" width="6" height="6" rx="1" fill="white" opacity="0.75" />
        <rect x="9" y="9" width="6" height="6" rx="1" fill="white" opacity="0.95" />
        <rect x="16" y="9" width="6" height="6" rx="1" fill="white" opacity="0.75" />
        <rect x="9" y="16" width="6" height="6" rx="1" fill="white" opacity="0.85" />
      </svg>
    </div>
  );
}
