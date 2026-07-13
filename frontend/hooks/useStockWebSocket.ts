import { useEffect, useRef, useState } from "react";

export interface Trade {
  symbol: string;
  price: number;
  volume: number;
  timestamp: number;
}

export interface SurgeMessage {
  type: "SURGE";
  symbol: string;
  currentPrice: number;
  baselinePrice: number;
  changePercent: number;
  direction: "UP" | "DOWN";
}

export interface AiBriefingMessage {
  type: "AI_BRIEFING";
  symbol: string;
  briefing: string;
  newsCount: number;
}

export function useStockWebSocket(
  onSurge?: (surge: SurgeMessage) => void,
  onAiBriefing?: (briefing: AiBriefingMessage) => void,
) {
  const [prices, setPrices] = useState<Record<string, Trade>>({});

  // 콜백이 바뀌어도 WebSocket을 재연결하지 않도록 ref로 관리
  const onSurgeRef = useRef(onSurge);
  useEffect(() => { onSurgeRef.current = onSurge; }, [onSurge]);

  const onAiBriefingRef = useRef(onAiBriefing);
  useEffect(() => { onAiBriefingRef.current = onAiBriefing; }, [onAiBriefing]);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => {
    // closed는 이 effect 실행마다 독립적인 변수 — 공유 ref 사용 시 타이밍 문제 방지
    // (cleanup → 새 마운트 → 이전 소켓 onerror 순서로 실행될 때 공유 ref가 리셋되는 문제)
    let closed = false;
    let delay = 1000;

    function connect() {
      if (closed) return;

      const ws = new WebSocket("ws://localhost:8080/ws/stock");
      wsRef.current = ws;

      ws.onopen = () => {
        console.log("WebSocket connected");
        delay = 1000;
      };

      ws.onclose = () => {
        if (closed) return;
        console.log(`WebSocket disconnected. Reconnecting in ${delay}ms...`);
        reconnectTimer.current = setTimeout(() => {
          delay = Math.min(delay * 2, 30000);
          connect();
        }, delay);
      };

      // closed가 true이면 의도적으로 닫은 것이므로 에러 로그 출력하지 않음
      ws.onerror = (e) => {
        if (!closed) console.error("WebSocket error", e);
      };

      ws.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data);

          // type 필드로 메시지 종류 분기
          if (msg.type === "SURGE") {
            onSurgeRef.current?.(msg as SurgeMessage);
            return;
          }
          if (msg.type === "AI_BRIEFING") {
            onAiBriefingRef.current?.(msg as AiBriefingMessage);
            return;
          }

          // 심볼 필터링을 하지 않음 — 새로고침 시 watchlist 로드 전에 메시지가 도착하면
          // symbolsRef가 비어있어 전부 필터링되는 타이밍 문제가 생기기 때문
          // 표시할 심볼 결정은 page.tsx의 watchlist.map()이 담당
          setPrices((prev) => ({ ...prev, [(msg as Trade).symbol]: msg as Trade }));
        } catch {
          // 파싱 실패 시 무시
        }
      };
    }

    connect();

    return () => {
      closed = true;
      clearTimeout(reconnectTimer.current);
      wsRef.current?.close();
    };
  }, []);

  return prices;
}
