import { useCallback, useEffect, useRef, useState } from "react";

export interface Trade {
  symbol: string;
  price: number;
  volume: number;
  timestamp: number;
}

export interface AlertMessage {
  type: "ALERT";
  symbol: string;
  price: number;
  targetPrice: number;
  direction: "ABOVE" | "BELOW";
}

export function useStockWebSocket(
  symbols: string[],
  onAlert?: (alert: AlertMessage) => void
) {
  const [prices, setPrices] = useState<Record<string, Trade>>({});

  const symbolsRef = useRef(symbols);
  useEffect(() => {
    symbolsRef.current = symbols;
  }, [symbols]);

  // onAlert도 ref로 관리 — 콜백이 바뀌어도 WebSocket을 재연결하지 않음
  const onAlertRef = useRef(onAlert);
  useEffect(() => {
    onAlertRef.current = onAlert;
  }, [onAlert]);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectDelay = useRef(1000);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const isUnmounting = useRef(false);

  const connect = useCallback(() => {
    const ws = new WebSocket("ws://localhost:8080/ws/stock");
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("WebSocket connected");
      reconnectDelay.current = 1000;
    };

    ws.onclose = () => {
      if (isUnmounting.current) return;

      const delay = reconnectDelay.current;
      console.log(`WebSocket disconnected. Reconnecting in ${delay}ms...`);

      reconnectTimer.current = setTimeout(() => {
        reconnectDelay.current = Math.min(delay * 2, 30000);
        connect();
      }, delay);
    };

    ws.onerror = (e) => console.error("WebSocket error", e);

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);

        // type 필드가 있으면 알림 메시지 — trade 메시지와 분기
        if (msg.type === "ALERT") {
          onAlertRef.current?.(msg as AlertMessage);
          return;
        }

        const trade = msg as Trade;
        if (!symbolsRef.current.includes(trade.symbol)) return;
        setPrices((prev) => ({ ...prev, [trade.symbol]: trade }));
      } catch {
        // 파싱 실패 시 무시
      }
    };
  }, []);

  useEffect(() => {
    isUnmounting.current = false;
    connect();

    return () => {
      isUnmounting.current = true;
      clearTimeout(reconnectTimer.current);
      wsRef.current?.close();
    };
  }, [connect]);

  return prices;
}
