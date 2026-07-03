"use client";

import { useState } from "react";
import { useAlerts } from "@/hooks/useAlerts";

interface Props {
  symbol: string;
  onMarkTriggered?: (symbol: string) => void;
}

export default function AlertForm({ symbol, onMarkTriggered }: Props) {
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
      <p className="text-xs text-gray-500 mb-2">가격 알림</p>

      {/* 활성 알림 목록 */}
      {alerts.length > 0 && (
        <div className="space-y-1 mb-3">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className="flex items-center justify-between bg-gray-800 rounded-lg px-3 py-2"
            >
              <span className="text-xs text-gray-300">
                <span
                  className={
                    alert.direction === "ABOVE" ? "text-green-400" : "text-red-400"
                  }
                >
                  {alert.direction === "ABOVE" ? "↑" : "↓"}
                </span>{" "}
                ${alert.targetPrice.toFixed(2)}{" "}
                <span className="text-gray-500">
                  {alert.direction === "ABOVE" ? "이상" : "이하"}
                </span>
              </span>
              <button
                onClick={() => removeAlert(alert.id)}
                className="text-gray-600 hover:text-red-400 text-lg leading-none"
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
        <div className="flex rounded-lg overflow-hidden border border-gray-700 text-xs shrink-0">
          <button
            onClick={() => setDirection("ABOVE")}
            className={`px-2 py-1.5 transition-colors ${
              direction === "ABOVE" ? "bg-green-700 text-white" : "text-gray-400"
            }`}
          >
            이상
          </button>
          <button
            onClick={() => setDirection("BELOW")}
            className={`px-2 py-1.5 transition-colors ${
              direction === "BELOW" ? "bg-red-700 text-white" : "text-gray-400"
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
          className="flex-1 min-w-0 bg-gray-800 text-white rounded-lg px-3 py-1.5 text-xs outline-none placeholder-gray-600"
        />
        <button
          onClick={handleAdd}
          className="bg-blue-600 hover:bg-blue-500 text-white rounded-lg px-3 py-1.5 text-xs shrink-0"
        >
          설정
        </button>
      </div>
    </div>
  );
}
