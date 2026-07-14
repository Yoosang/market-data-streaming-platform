"use client";

import { useState } from "react";
import Link from "next/link";
import { useWatchlist, Market } from "@/hooks/useWatchlist";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import KrStockSearchInput from "@/components/KrStockSearchInput";

export default function WatchlistRegisterPage() {
  const authed = useAuthGuard();
  const { watchlist, addSymbol, removeSymbol } = useWatchlist();
  const [input, setInput] = useState("");
  const [market, setMarket] = useState<Market>("US");

  const handleAdd = () => {
    const trimmed = input.trim();
    if (!trimmed) return;
    addSymbol(trimmed, market);
    setInput("");
  };

  const handleKrSelect = (symbol: string, name: string) => {
    addSymbol(symbol, "KR", name);
  };

  const handleMarketSwitch = (m: Market) => {
    setMarket(m);
    setInput("");
  };

  if (!authed) return null;

  return (
    <main className="min-h-screen bg-surface-black text-body-on-dark flex flex-col items-center py-8 px-4">
      {/* 헤더 */}
      <div className="w-full max-w-sm flex items-center justify-between mb-6">
        <h1 className="text-[21px] font-semibold text-body-muted tracking-widest uppercase">
          관심종목 관리
        </h1>
        <Link href="/watchlist" className="text-xs text-ink-muted-48 hover:text-body-muted transition-colors">
          목록으로
        </Link>
      </div>

      {/* US / KR 시장 선택 */}
      <div className="flex gap-1 bg-surface-tile-2 rounded-pill p-1 w-full max-w-sm mb-3">
        {(["US", "KR"] as Market[]).map((m) => (
          <button
            key={m}
            onClick={() => handleMarketSwitch(m)}
            className={`flex-1 py-2 text-sm font-semibold rounded-pill transition-colors ${
              market === m ? "bg-primary text-body-on-dark" : "text-body-muted hover:text-body-on-dark"
            }`}
          >
            {m === "US" ? "미국 (US)" : "국내 (KR)"}
          </button>
        ))}
      </div>

      {/* 종목 추가 */}
      <div className="flex gap-2 w-full max-w-sm mb-6">
        {market === "KR" ? (
          <KrStockSearchInput onSelect={handleKrSelect} />
        ) : (
          <>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value.toUpperCase())}
              onKeyDown={(e) => e.key === "Enter" && handleAdd()}
              placeholder="종목 입력 (예: AAPL)"
              className="flex-1 bg-surface-tile-2 text-body-on-dark rounded-pill px-5 py-3 text-[17px] outline-none placeholder-ink-muted-48"
            />
            <button
              onClick={handleAdd}
              className="bg-primary hover:bg-primary-focus text-body-on-dark rounded-pill px-5 py-3 text-[17px] transition-colors"
            >
              추가
            </button>
          </>
        )}
      </div>

      {/* 관심종목 목록 — 삭제 */}
      <div className="w-full max-w-sm space-y-2">
        {watchlist.length === 0 && (
          <p className="text-center text-ink-muted-48 text-sm py-6">
            관심종목을 추가해보세요
          </p>
        )}
        {watchlist.map(({ symbol, market: itemMarket, name }) => (
          <div
            key={symbol}
            className="flex items-center justify-between rounded-lg px-5 py-4 bg-surface-tile-1"
          >
            <div className="flex flex-col">
              <span className="text-[17px] font-semibold text-body-on-dark">
                {itemMarket === "KR" && name ? name : symbol}
              </span>
              <span className="text-xs text-body-muted">
                {itemMarket === "KR" && name ? symbol : itemMarket}
              </span>
            </div>

            <button
              onClick={() => removeSymbol(symbol)}
              className="text-ink-muted-48 hover:text-red-400 text-lg leading-none transition-colors"
              aria-label={`${symbol} 삭제`}
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </main>
  );
}
