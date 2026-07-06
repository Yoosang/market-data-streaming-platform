import { useEffect, useState } from "react";
import { authHeaders, getToken } from "@/lib/auth";

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

  useEffect(() => {
    if (!symbol || !getToken()) return;

    fetch(`${API_BASE}/api/alerts`, { headers: authHeaders() })
      .then((res) => (res.ok ? res.json() : []))
      .then((data: Alert[]) =>
        setAlerts(data.filter((a) => a.symbol === symbol && !a.triggered))
      )
      .catch(console.error);
  }, [symbol]);

  const addAlert = async (targetPrice: number, direction: "ABOVE" | "BELOW") => {
    const res = await fetch(`${API_BASE}/api/alerts`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify({ symbol, targetPrice, direction }),
    });
    const newAlert: Alert = await res.json();
    setAlerts((prev) => [newAlert, ...prev]);
  };

  const removeAlert = async (id: number) => {
    await fetch(`${API_BASE}/api/alerts/${id}`, {
      method: "DELETE",
      headers: authHeaders(),
    });
    setAlerts((prev) => prev.filter((a) => a.id !== id));
  };

  const markTriggered = (alertSymbol: string) => {
    setAlerts((prev) => prev.filter((a) => a.symbol !== alertSymbol));
  };

  return { alerts, addAlert, removeAlert, markTriggered };
}
