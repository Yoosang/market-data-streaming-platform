import { useEffect, useMemo, useState } from "react";
import { getUserId } from "@/lib/userId";

const API_BASE = "http://localhost:8080";

export function useWatchlist() {
  const userId = useMemo(() => getUserId(), []);
  const [symbols, setSymbols] = useState<string[]>([]);

  // 마운트 시 서버에서 관심종목 목록 가져오기
  useEffect(() => {
    if (!userId) return;
    fetch(`${API_BASE}/api/watchlist/${userId}/symbols`)
      .then((res) => res.json())
      .then(setSymbols)
      .catch(console.error);
  }, [userId]);

  const addSymbol = async (symbol: string) => {
    const upper = symbol.toUpperCase().trim();
    if (!upper || symbols.includes(upper)) return;

    await fetch(`${API_BASE}/api/watchlist/${userId}/symbols`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ symbol: upper }),
    });
    setSymbols((prev) => [...prev, upper]);
  };

  const removeSymbol = async (symbol: string) => {
    await fetch(`${API_BASE}/api/watchlist/${userId}/symbols/${symbol}`, {
      method: "DELETE",
    });
    setSymbols((prev) => prev.filter((s) => s !== symbol));
  };

  return { symbols, addSymbol, removeSymbol };
}
