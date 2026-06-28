import { useEffect, useState } from "react";

export interface Trade {
  symbol: string;
  price: number;
  volume: number;
  timestamp: number;
}

// 종목별 최신 체결가를 Map으로 관리
// 백엔드 ws://localhost:8080/ws/stock 에 연결해 실시간 Trade를 수신한다
export function useStockWebSocket(symbols: string[]) {
  const [prices, setPrices] = useState<Record<string, Trade>>({});

  useEffect(() => {
    const ws = new WebSocket("ws://localhost:8080/ws/stock");

    ws.onopen = () => console.log("WebSocket connected");
    ws.onclose = () => console.log("WebSocket disconnected");
    ws.onerror = (e) => console.error(`WebSocket error ${e}`);

    ws.onmessage = (event) => {
      try {
        const trade: Trade = JSON.parse(event.data);
        if (!symbols.includes(trade.symbol)) return;
        setPrices((prev) => ({ ...prev, [trade.symbol]: trade }));
      } catch {
        // 파싱 실패 시 무시
      }
    };

    return () => ws.close();
  }, []);

  return prices;
}
