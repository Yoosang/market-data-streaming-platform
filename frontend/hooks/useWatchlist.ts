import { useEffect, useMemo, useState } from "react";
import { getUserId } from "@/lib/userId";

const API_BASE = "http://localhost:8080";

export type Market = "US" | "KR";
export type WatchlistItem = { symbol: string; market: Market; name?: string };

export function useWatchlist() {
  const userId = useMemo(() => getUserId(), []);
  const [watchlist, setWatchlist] = useState<WatchlistItem[]>([]);

  useEffect(() => {
    if (!userId) return;
    fetch(`${API_BASE}/api/watchlist/${userId}/symbols`)
      .then((res) => res.json())
      .then(setWatchlist)
      .catch(console.error);
  }, [userId]);

  const addSymbol = async (symbol: string, market: Market, name?: string) => {
    // KR 종목코드는 숫자라 대소문자 변환 불필요, US는 대문자로 정규화
    const normalized = market === "KR" ? symbol.trim() : symbol.toUpperCase().trim();
    if (!normalized || watchlist.some((w) => w.symbol === normalized)) return;

    await fetch(`${API_BASE}/api/watchlist/${userId}/symbols`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ symbol: normalized, market, name }),
    });
    setWatchlist((prev) => [...prev, { symbol: normalized, market, name }]);
  };

  const removeSymbol = async (symbol: string) => {
    await fetch(`${API_BASE}/api/watchlist/${userId}/symbols/${symbol}`, {
      method: "DELETE",
    });
    setWatchlist((prev) => prev.filter((w) => w.symbol !== symbol));
  };

  return { watchlist, addSymbol, removeSymbol };
}
