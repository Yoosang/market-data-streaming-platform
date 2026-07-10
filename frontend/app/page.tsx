"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useWatchlist, Market } from "@/hooks/useWatchlist";
import { useStockWebSocket, AlertMessage, SurgeMessage, AiBriefingMessage } from "@/hooks/useStockWebSocket";
import { formatPrice } from "@/lib/format";
import { isLoggedIn, removeToken } from "@/lib/auth";
import CandleChart from "@/components/CandleChart";
import AlertForm from "@/components/AlertForm";
import KrStockSearchInput from "@/components/KrStockSearchInput";
import SurgeBriefingPanel from "@/components/SurgeBriefingPanel";

export default function Home() {
  const router = useRouter();
  const { watchlist, addSymbol, removeSymbol } = useWatchlist();
  const [input, setInput] = useState("");
  const [market, setMarket] = useState<Market>("US");
  const [selectedSymbol, setSelectedSymbol] = useState<string | null>(null);

  // 급등/급락 이벤트 상태 — symbol별로 surge 정보와 AI 브리핑을 함께 관리
  const [surgeEvents, setSurgeEvents] = useState<Record<string, {
    surge: SurgeMessage;
    briefing?: string;
    loading: boolean;
    market: "US" | "KR";
  }>>({});

  // 미로그인 시 로그인 페이지로 이동
  useEffect(() => {
    if (!isLoggedIn()) router.push("/login");
  }, [router]);

  // 브라우저 Notification 권한을 최초 1회 요청
  useEffect(() => {
    if (typeof Notification !== "undefined" && Notification.permission === "default") {
      Notification.requestPermission();
    }
  }, []);

  // 알림 트리거 시 브라우저 알림 표시
  const handleAlert = useCallback(
    (alert: AlertMessage) => {
      const item = watchlist.find((w) => w.symbol === alert.symbol);
      const m = item?.market ?? "US";
      const direction = alert.direction === "ABOVE" ? "이상" : "이하";
      const body = `${alert.symbol} 현재가 ${formatPrice(alert.price, m)} — 목표가 ${formatPrice(alert.targetPrice, m)} ${direction} 도달`;

      if (Notification.permission === "granted") {
        new Notification(`${alert.symbol} 가격 알림`, { body });
      } else {
        console.log("[ALERT]", body);
      }
    },
    [watchlist]
  );

  // SURGE: 즉시 카드 표시 (loading=true), 30초 후 자동 제거
  const handleSurge = useCallback((surge: SurgeMessage) => {
    const item = watchlist.find((w) => w.symbol === surge.symbol);
    const m = (item?.market ?? "US") as "US" | "KR";
    setSurgeEvents((prev) => ({
      ...prev,
      [surge.symbol]: { surge, loading: true, market: m },
    }));
    setTimeout(() => {
      setSurgeEvents((prev) => {
        const next = { ...prev };
        delete next[surge.symbol];
        return next;
      });
    }, 30000);
  }, [watchlist]);

  // AI_BRIEFING: 같은 symbol의 카드에 브리핑 텍스트 업데이트
  const handleAiBriefing = useCallback((msg: AiBriefingMessage) => {
    setSurgeEvents((prev) => {
      if (!prev[msg.symbol]) return prev;
      return { ...prev, [msg.symbol]: { ...prev[msg.symbol], briefing: msg.briefing, loading: false } };
    });
  }, []);

  const prices = useStockWebSocket(handleAlert, handleSurge, handleAiBriefing);

  const handleAdd = () => {
    const trimmed = input.trim();
    if (!trimmed) return;
    addSymbol(trimmed, market);
    setInput("");
  };

  const handleKrSelect = (symbol: string, name: string) => {
    addSymbol(symbol, "KR", name);
  };

  const handleMarketSwitch = (m: Market) => {
    setMarket(m);
    setInput("");
  };

  const handleRowClick = (symbol: string) => {
    setSelectedSymbol((prev) => (prev === symbol ? null : symbol));
  };

  return (
    <main className="min-h-screen bg-gray-950 text-white flex flex-col items-center py-8 px-4">
      {/* 헤더 */}
      <div className="w-full max-w-sm flex items-center justify-between mb-6">
        <h1 className="text-lg font-semibold text-gray-300 tracking-widest uppercase">
          Stock Market · Live
        </h1>
        <button
          onClick={() => { removeToken(); router.push("/login"); }}
          className="text-xs text-gray-600 hover:text-gray-400"
        >
          로그아웃
        </button>
      </div>

      {/* US / KR 시장 선택 */}
      <div className="flex gap-1 bg-gray-800 rounded-xl p-1 w-full max-w-sm mb-3">
        {(["US", "KR"] as Market[]).map((m) => (
          <button
            key={m}
            onClick={() => handleMarketSwitch(m)}
            className={`flex-1 py-2 text-sm font-semibold rounded-lg transition-colors ${
              market === m ? "bg-blue-600 text-white" : "text-gray-400 hover:text-white"
            }`}
          >
            {m === "US" ? "미국 (US)" : "국내 (KR)"}
          </button>
        ))}
      </div>

      {/* 종목 추가 */}
      <div className="flex gap-2 w-full max-w-sm mb-6">
        {market === "KR" ? (
          // KR: 종목명 자동완성 검색
          <KrStockSearchInput onSelect={handleKrSelect} />
        ) : (
          // US: 티커 직접 입력
          <>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value.toUpperCase())}
              onKeyDown={(e) => e.key === "Enter" && handleAdd()}
              placeholder="종목 입력 (예: AAPL)"
              className="flex-1 bg-gray-800 text-white rounded-xl px-4 py-3 text-sm outline-none placeholder-gray-600"
            />
            <button
              onClick={handleAdd}
              className="bg-blue-600 hover:bg-blue-500 text-white rounded-xl px-5 py-3 text-sm font-semibold"
            >
              추가
            </button>
          </>
        )}
      </div>

      {/* 급등/급락 AI 브리핑 패널 */}
      <SurgeBriefingPanel surgeEvents={surgeEvents} />

      {/* 관심종목 목록 */}
      <div className="w-full max-w-sm space-y-2">
        {watchlist.length === 0 && (
          <p className="text-center text-gray-600 text-sm py-6">
            관심종목을 추가해보세요
          </p>
        )}
        {watchlist.map(({ symbol, market: itemMarket, name }) => {
          const trade = prices[symbol];
          const isSelected = selectedSymbol === symbol;
          return (
            <div key={symbol}>
              <div
                onClick={() => handleRowClick(symbol)}
                className={`flex items-center justify-between rounded-xl px-5 py-4 cursor-pointer transition-colors ${
                  isSelected ? "bg-gray-700" : "bg-gray-900 hover:bg-gray-800"
                }`}
              >
                <div className="flex flex-col">
                  <span className="text-base font-bold text-white">
                    {itemMarket === "KR" && name ? name : symbol}
                  </span>
                  <span className="text-xs text-gray-500">
                    {itemMarket === "KR" && name ? symbol : itemMarket}
                  </span>
                </div>

                <div className="flex items-center gap-4">
                  {trade ? (
                    <span className="text-base font-mono font-semibold text-green-400">
                      {formatPrice(trade.price, itemMarket)}
                    </span>
                  ) : (
                    <span className="text-sm text-gray-600">대기 중...</span>
                  )}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      if (selectedSymbol === symbol) setSelectedSymbol(null);
                      removeSymbol(symbol);
                    }}
                    className="text-gray-600 hover:text-red-400 text-lg leading-none"
                    aria-label={`${symbol} 삭제`}
                  >
                    ×
                  </button>
                </div>
              </div>

              {isSelected && (
                <>
                  <CandleChart symbol={symbol} />
                  <AlertForm symbol={symbol} market={itemMarket} />
                </>
              )}
            </div>
          );
        })}
      </div>

      <p className="mt-8 text-xs text-gray-700">
        미국 주식: 한국 기준 밤 10시~새벽 5시 · 국내 주식: 오전 9시~오후 3시 30분
      </p>
    </main>
  );
}
