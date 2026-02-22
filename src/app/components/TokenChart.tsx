import { TrendingUp } from "lucide-react";

interface TokenChartProps {
  tokenAddress?: string;
  chainId?: string;
  textColor?: string;
}

export default function TokenChart({ tokenAddress, textColor = "#ffffff" }: TokenChartProps) {
  const shortAddress = tokenAddress
    ? `${tokenAddress.slice(0, 6)}...${tokenAddress.slice(-4)}`
    : null;

  return (
    <div className="h-full w-full flex flex-col items-center justify-center p-4 text-center">
      <TrendingUp className="size-8 mb-2 opacity-40" style={{ color: textColor }} />
      <p className="text-sm font-medium" style={{ color: textColor }}>
        Price chart coming soon
      </p>
      {shortAddress && (
        <p className="text-xs mt-1 opacity-60 font-mono" style={{ color: textColor }}>
          {shortAddress}
        </p>
      )}
    </div>
  );
}
