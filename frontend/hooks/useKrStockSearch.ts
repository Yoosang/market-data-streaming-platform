import { useEffect, useMemo, useState } from "react";

type KrStock = { symbol: string; name: string };

// 파일을 한 번만 fetch하도록 모듈 레벨에서 캐시
let cache: KrStock[] | null = null;
let pending: Promise<KrStock[]> | null = null;

function loadStocks(): Promise<KrStock[]> {
  if (cache) return Promise.resolve(cache);
  if (!pending) {
    pending = fetch("/kr-stocks.json")
      .then((r) => r.json())
      .then((data) => { cache = data; return data; });
  }
  return pending;
}

export function useKrStockSearch(query: string) {
  const [allStocks, setAllStocks] = useState<KrStock[]>(cache ?? []);

  useEffect(() => {
    if (cache) return;
    loadStocks().then(setAllStocks).catch(console.error);
  }, []);

  const results = useMemo(() => {
    const q = query.trim();
    if (q.length < 1) return [];
    // 종목명 또는 종목코드로 검색 (앞에서부터 일치하는 항목 우선)
    return allStocks
      .filter((s) => s.name.includes(q) || s.symbol.startsWith(q))
      .slice(0, 8);
  }, [query, allStocks]);

  return results;
}
