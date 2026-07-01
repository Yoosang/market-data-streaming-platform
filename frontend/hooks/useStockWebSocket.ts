import { useEffect, useRef, useState } from "react";

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

  // ref로 최신 symbols를 참조 — WebSocket을 재연결하지 않고도 필터링 기준을 동적으로 갱신
  const symbolsRef = useRef(symbols);
  useEffect(() => {
    symbolsRef.current = symbols;
  }, [symbols]);

  useEffect(() => {
    const ws = new WebSocket("ws://localhost:8080/ws/stock");

    ws.onopen = () => console.log("WebSocket connected");
    ws.onclose = () => console.log("WebSocket disconnected");
    ws.onerror = (e) => console.error(`WebSocket error ${e}`);

    ws.onmessage = (event) => {
      try {
        const trade: Trade = JSON.parse(event.data);
        if (!symbolsRef.current.includes(trade.symbol)) return;
        setPrices((prev) => ({ ...prev, [trade.symbol]: trade }));
      } catch {
        // 파싱 실패 시 무시
      }
    };

    return () => ws.close();
  }, []); // WebSocket은 마운트 시 한 번만 연결

  return prices;
}
