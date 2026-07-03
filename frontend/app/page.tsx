"use client";

import { useCallback, useEffect, useState } from "react";
import { useWatchlist } from "@/hooks/useWatchlist";
import { useStockWebSocket, AlertMessage } from "@/hooks/useStockWebSocket";
import CandleChart from "@/components/CandleChart";
import AlertForm from "@/components/AlertForm";

export default function Home() {
  const { symbols, addSymbol, removeSymbol } = useWatchlist();
  const [input, setInput] = useState("");
  const [selectedSymbol, setSelectedSymbol] = useState<string | null>(null);

  // 브라우저 Notification 권한을 최초 1회 요청
  useEffect(() => {
    if (typeof Notification !== "undefined" && Notification.permission === "default") {
      Notification.requestPermission();
    }
  }, []);

  // 알림 트리거 시 브라우저 알림 표시
  const handleAlert = useCallback((alert: AlertMessage) => {
    const direction = alert.direction === "ABOVE" ? "이상" : "이하";
    const body = `${alert.symbol} 현재가 $${alert.price.toFixed(2)} — 목표가 $${alert.targetPrice.toFixed(2)} ${direction} 도달`;

    if (Notification.permission === "granted") {
      new Notification(`${alert.symbol} 가격 알림`, { body });
    } else {
      // 권한이 없으면 콘솔로라도 확인
      console.log("[ALERT]", body);
    }
  }, []);

  const prices = useStockWebSocket(symbols, handleAlert);

  const handleAdd = () => {
    if (!input.trim()) return;
    addSymbol(input);
    setInput("");
  };

  const handleRowClick = (symbol: string) => {
    setSelectedSymbol((prev) => (prev === symbol ? null : symbol));
  };

  return (
    <main className="min-h-screen bg-gray-950 text-white flex flex-col items-center py-8 px-4">
      <h1 className="text-lg font-semibold text-gray-300 mb-6 tracking-widest uppercase">
        US Stocks · Live
      </h1>

      {/* 종목 추가 */}
      <div className="flex gap-2 w-full max-w-sm mb-6">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value.toUpperCase())}
          onKeyDown={(e) => e.key === "Enter" && handleAdd()}
          placeholder="종목 입력 (예: AAPL)"
          className="flex-1 bg-gray-800 text-white rounded-xl px-4 py-3 text-sm outline-none placeholder-gray-600"
        />
        <button
          onClick={handleAdd}
          className="bg-blue-600 hover:bg-blue-500 text-white rounded-xl px-5 py-3 text-sm font-semibold"
        >
          추가
        </button>
      </div>

      {/* 관심종목 목록 */}
      <div className="w-full max-w-sm space-y-2">
        {symbols.length === 0 && (
          <p className="text-center text-gray-600 text-sm py-6">
            관심종목을 추가해보세요
          </p>
        )}
        {symbols.map((symbol) => {
          const trade = prices[symbol];
          const isSelected = selectedSymbol === symbol;
          return (
            <div key={symbol}>
              <div
                onClick={() => handleRowClick(symbol)}
                className={`flex items-center justify-between rounded-xl px-5 py-4 cursor-pointer transition-colors ${
                  isSelected ? "bg-gray-700" : "bg-gray-900 hover:bg-gray-800"
                }`}
              >
                <span className="text-base font-bold text-white">{symbol}</span>

                <div className="flex items-center gap-4">
                  {trade ? (
                    <span className="text-base font-mono font-semibold text-green-400">
                      ${trade.price.toFixed(2)}
                    </span>
                  ) : (
                    <span className="text-sm text-gray-600">대기 중...</span>
                  )}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      if (selectedSymbol === symbol) setSelectedSymbol(null);
                      removeSymbol(symbol);
                    }}
                    className="text-gray-600 hover:text-red-400 text-lg leading-none"
                    aria-label={`${symbol} 삭제`}
                  >
                    ×
                  </button>
                </div>
              </div>

              {isSelected && (
                <>
                  <CandleChart symbol={symbol} />
                  <AlertForm symbol={symbol} />
                </>
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
