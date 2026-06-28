"use client";

import { useStockWebSocket } from "@/hooks/useStockWebSocket";

const SYMBOLS = ["AAPL", "TSLA", "MSFT", "AMZN"];

export default function Home() {
  const prices = useStockWebSocket(SYMBOLS);

  return (
    <main className="min-h-screen bg-gray-950 text-white flex flex-col items-center py-8 px-4">
      <h1 className="text-lg font-semibold text-gray-300 mb-6 tracking-widest uppercase">
        US Stocks · Live
      </h1>

      <div className="w-full max-w-sm space-y-2">
        {SYMBOLS.map((symbol) => {
          const trade = prices[symbol];
          return (
            <div
              key={symbol}
              className="flex items-center justify-between bg-gray-900 rounded-xl px-5 py-4"
            >
              <span className="text-base font-bold text-white">{symbol}</span>

              {trade ? (
                <span className="text-base font-mono font-semibold text-green-400">
                  ${trade.price.toFixed(2)}
                </span>
              ) : (
                <span className="text-sm text-gray-600">대기 중...</span>
              )}
            </div>
          );
        })}
      </div>

      <p className="mt-8 text-xs text-gray-700">
        미국 장 시간(한국 기준 밤 10시~새벽 5시)에만 실시간 데이터가 수신됩니다.
      </p>
    </main>
  );
}
