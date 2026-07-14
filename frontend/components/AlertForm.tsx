"use client";

import { useState } from "react";
import { useAlerts } from "@/hooks/useAlerts";
import { Market } from "@/hooks/useWatchlist";
import { formatPrice } from "@/lib/format";

interface Props {
  symbol: string;
  market: Market;
}

export default function AlertForm({ symbol, market }: Props) {
  const { alerts, addAlert, removeAlert } = useAlerts(symbol);
  const [targetPrice, setTargetPrice] = useState("");
  const [direction, setDirection] = useState<"ABOVE" | "BELOW">("ABOVE");

  const handleAdd = async () => {
    const price = parseFloat(targetPrice);
    if (isNaN(price) || price <= 0) return;
    await addAlert(price, direction);
    setTargetPrice("");
  };

  return (
    <div className="w-full max-w-sm mt-3 px-1">
      <p className="text-xs text-body-muted mb-2">가격 알림</p>

      {/* 활성 알림 목록 */}
      {alerts.length > 0 && (
        <div className="space-y-1 mb-3">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className="flex items-center justify-between bg-surface-tile-2 rounded-md px-3 py-2"
            >
              <span className="text-xs text-body-muted">
                <span
                  className={
                    alert.direction === "ABOVE" ? "text-green-400" : "text-red-400"
                  }
                >
                  {alert.direction === "ABOVE" ? "↑" : "↓"}
                </span>{" "}
                {formatPrice(alert.targetPrice, market)}{" "}
                <span className="text-ink-muted-48">
                  {alert.direction === "ABOVE" ? "이상" : "이하"}
                </span>
              </span>
              <button
                onClick={() => removeAlert(alert.id)}
                className="text-ink-muted-48 hover:text-red-400 text-lg leading-none transition-colors"
                aria-label="알림 삭제"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      {/* 알림 추가 폼 */}
      <div className="flex gap-2">
        <div className="flex rounded-pill overflow-hidden border border-hairline-on-dark text-xs shrink-0">
          <button
            onClick={() => setDirection("ABOVE")}
            className={`px-3 py-1.5 transition-colors ${
              direction === "ABOVE" ? "bg-green-700 text-body-on-dark" : "text-body-muted"
            }`}
          >
            이상
          </button>
          <button
            onClick={() => setDirection("BELOW")}
            className={`px-3 py-1.5 transition-colors ${
              direction === "BELOW" ? "bg-red-700 text-body-on-dark" : "text-body-muted"
            }`}
          >
            이하
          </button>
        </div>
        <input
          type="number"
          value={targetPrice}
          onChange={(e) => setTargetPrice(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleAdd()}
          placeholder="목표가"
          className="flex-1 min-w-0 bg-surface-tile-2 text-body-on-dark rounded-pill px-3 py-1.5 text-xs outline-none placeholder-ink-muted-48"
        />
        <button
          onClick={handleAdd}
          className="bg-primary hover:bg-primary-focus text-body-on-dark rounded-pill px-3 py-1.5 text-xs shrink-0 transition-colors"
        >
          설정
        </button>
      </div>
    </div>
  );
}
