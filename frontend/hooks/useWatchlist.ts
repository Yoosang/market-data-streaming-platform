import { useEffect, useState } from "react";
import { authHeaders, getToken } from "@/lib/auth";

const API_BASE = "http://localhost:8080";

export type Market = "US" | "KR";
type WatchlistItem = { symbol: string; market: Market; name?: string };

export function useWatchlist() {
  const [watchlist, setWatchlist] = useState<WatchlistItem[]>([]);

  useEffect(() => {
    if (!getToken()) return;
    fetch(`${API_BASE}/api/watchlist/symbols`, { headers: authHeaders() })
      .then((res) => (res.ok ? res.json() : []))
      .then(setWatchlist)
      .catch(console.error);
  }, []);

  const addSymbol = async (symbol: string, market: Market, name?: string) => {
    const normalized = market === "KR" ? symbol.trim() : symbol.toUpperCase().trim();
    if (!normalized || watchlist.some((w) => w.symbol === normalized)) return;

    await fetch(`${API_BASE}/api/watchlist/symbols`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify({ symbol: normalized, market, name }),
    });
    setWatchlist((prev) => [...prev, { symbol: normalized, market, name }]);
  };

  const removeSymbol = async (symbol: string) => {
    await fetch(`${API_BASE}/api/watchlist/symbols/${symbol}`, {
      method: "DELETE",
      headers: authHeaders(),
    });
    setWatchlist((prev) => prev.filter((w) => w.symbol !== symbol));
  };

  return { watchlist, addSymbol, removeSymbol };
}
