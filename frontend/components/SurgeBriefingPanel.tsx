"use client";

import { useEffect, useState } from "react";
import { SurgeMessage, AiBriefingMessage } from "@/hooks/useStockWebSocket";
import { formatPrice } from "@/lib/format";

interface SurgeEvent {
  surge: SurgeMessage;
  briefing?: string;
  loading: boolean;
  market: "US" | "KR";
  displayName: string;
}

interface Props {
  surgeEvents: Record<string, SurgeEvent>;
}

export default function SurgeBriefingPanel({ surgeEvents }: Props) {
  const entries = Object.values(surgeEvents);
  if (entries.length === 0) return null;

  return (
    <div className="w-full max-w-sm space-y-2 mb-4">
      {entries.map(({ surge, briefing, loading, market, displayName }) => {
        const isUp = surge.direction === "UP";
        const sign = isUp ? "+" : "";
        const borderColor = isUp ? "border-red-500" : "border-blue-500";
        const badgeColor = isUp
          ? "bg-red-500/20 text-red-400"
          : "bg-blue-500/20 text-blue-400";
        const arrow = isUp ? "▲" : "▼";

        return (
          <div
            key={surge.symbol}
            className={`rounded-lg border ${borderColor} bg-surface-tile-1 px-4 py-3`}
          >
            {/* 급등/급락 헤더 */}
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <span className="text-sm font-bold text-body-on-dark">{displayName}</span>
                <span className={`text-xs font-semibold px-2 py-0.5 rounded-pill ${badgeColor}`}>
                  {arrow} {sign}{surge.changePercent.toFixed(2)}%
                </span>
              </div>
              <span className="text-xs text-body-muted">
                {formatPrice(surge.currentPrice, market)}
              </span>
            </div>

            {/* AI 브리핑 */}
            <div className="text-xs text-body-muted leading-relaxed">
              {loading ? (
                <span className="flex items-center gap-1 text-ink-muted-48">
                  <span className="animate-pulse">●</span> AI 분석 중...
                </span>
              ) : (
                <span>{briefing}</span>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
