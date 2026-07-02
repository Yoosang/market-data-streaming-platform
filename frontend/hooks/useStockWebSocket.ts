import { useCallback, useEffect, useRef, useState } from "react";

export interface Trade {
  symbol: string;
  price: number;
  volume: number;
  timestamp: number;
}

export function useStockWebSocket(symbols: string[]) {
  const [prices, setPrices] = useState<Record<string, Trade>>({});

  const symbolsRef = useRef(symbols);
  useEffect(() => {
    symbolsRef.current = symbols;
  }, [symbols]);

  // 재연결 관련 ref — 렌더링과 무관하게 유지
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectDelay = useRef(1000);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  // 언마운트 시 의도적으로 닫는 경우 재연결 시도를 막기 위한 플래그
  const isUnmounting = useRef(false);

  const connect = useCallback(() => {
    const ws = new WebSocket("ws://localhost:8080/ws/stock");
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("WebSocket connected");
      reconnectDelay.current = 1000; // 연결 성공 시 딜레이 초기화
    };

    ws.onclose = () => {
      if (isUnmounting.current) return; // 언마운트로 인한 종료면 재연결 안 함

      const delay = reconnectDelay.current;
      console.log(`WebSocket disconnected. Reconnecting in ${delay}ms...`);

      reconnectTimer.current = setTimeout(() => {
        // 다음 재연결 시도의 딜레이를 2배로 증가 (최대 30초)
        reconnectDelay.current = Math.min(delay * 2, 30000);
        connect();
      }, delay);
    };

    ws.onerror = (e) => console.error("WebSocket error", e);

    ws.onmessage = (event) => {
      try {
        const trade: Trade = JSON.parse(event.data);
        if (!symbolsRef.current.includes(trade.symbol)) return;
        setPrices((prev) => ({ ...prev, [trade.symbol]: trade }));
      } catch {
        // 파싱 실패 시 무시
      }
    };
  }, []); // connect는 ref만 참조하므로 한 번만 생성

  useEffect(() => {
    isUnmounting.current = false;
    connect();

    return () => {
      isUnmounting.current = true;
      clearTimeout(reconnectTimer.current); // 예약된 재연결 타이머 취소
      wsRef.current?.close();
    };
  }, [connect]);

  return prices;
}
