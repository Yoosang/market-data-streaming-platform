import { useEffect, useState } from "react";
import { getUserId } from "@/lib/userId";

const API_BASE = "http://localhost:8080";

export interface Alert {
  id: number;
  symbol: string;
  targetPrice: number;
  direction: "ABOVE" | "BELOW";
  triggered: boolean;
}

export function useAlerts(symbol: string) {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const userId = getUserId();

  useEffect(() => {
    if (!symbol || !userId) return;

    fetch(`${API_BASE}/api/alerts/${userId}`)
      .then((res) => res.json())
      .then((data: Alert[]) =>
        // 해당 종목의 미트리거 알림만 표시
        setAlerts(data.filter((a) => a.symbol === symbol && !a.triggered))
      )
      .catch(console.error);
  }, [symbol, userId]);

  const addAlert = async (targetPrice: number, direction: "ABOVE" | "BELOW") => {
    const res = await fetch(`${API_BASE}/api/alerts/${userId}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ symbol, targetPrice, direction }),
    });
    const newAlert: Alert = await res.json();
    setAlerts((prev) => [newAlert, ...prev]);
  };

  const removeAlert = async (id: number) => {
    await fetch(`${API_BASE}/api/alerts/${userId}/${id}`, { method: "DELETE" });
    setAlerts((prev) => prev.filter((a) => a.id !== id));
  };

  // 알림이 트리거되면 목록에서 제거 (WebSocket push 수신 후 page.tsx에서 호출)
  const markTriggered = (alertSymbol: string) => {
    setAlerts((prev) => prev.filter((a) => a.symbol !== alertSymbol));
  };

  return { alerts, addAlert, removeAlert, markTriggered };
}
