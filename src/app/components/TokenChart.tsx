import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { useEffect, useState } from "react";

interface TokenChartProps {
  tokenAddress?: string;
  chainId?: string;
  textColor?: string;
}

function generateMockData() {
  const data = [];
  let value = 100;
  for (let i = 0; i < 30; i++) {
    value = value + (Math.random() - 0.5) * 20;
    data.push({
      date: `Day ${i + 1}`,
      price: Math.max(10, value),
    });
  }
  return data;
}

export default function TokenChart({ tokenAddress, chainId, textColor = "#ffffff" }: TokenChartProps) {
  const [data, setData] = useState(generateMockData());

  useEffect(() => {
    setData(generateMockData());
  }, [tokenAddress, chainId]);

  const currentPrice = data[data.length - 1].price;
  const previousPrice = data[data.length - 2].price;
  const priceChange = ((currentPrice - previousPrice) / previousPrice) * 100;

  return (
    <div className="h-full w-full flex flex-col p-4">
      <div className="mb-2">
        <div className="text-2xl font-bold" style={{ color: textColor }}>
          ${currentPrice.toFixed(2)}
        </div>
        <div
          className="text-sm"
          style={{
            color: textColor,
            opacity: 0.8,
          }}
        >
          <span className={priceChange >= 0 ? "text-green-400" : "text-red-400"}>
            {priceChange >= 0 ? "+" : ""}
            {priceChange.toFixed(2)}%
          </span>
        </div>
      </div>
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 5, right: 5, left: 5, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" />
            <XAxis
              dataKey="date"
              hide
              stroke={textColor}
              style={{ fontSize: "10px", opacity: 0.5 }}
            />
            <YAxis
              hide
              stroke={textColor}
              style={{ fontSize: "10px", opacity: 0.5 }}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: "rgba(0,0,0,0.8)",
                border: "none",
                borderRadius: "8px",
                color: "#fff",
              }}
              formatter={(value: any) => [`$${value.toFixed(2)}`, "Price"]}
            />
            <Line
              type="monotone"
              dataKey="price"
              stroke={priceChange >= 0 ? "#10b981" : "#ef4444"}
              strokeWidth={2}
              dot={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
