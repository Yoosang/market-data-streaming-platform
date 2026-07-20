"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useWatchlist } from "@/hooks/useWatchlist";
import { useStockWebSocket, SurgeMessage, AiBriefingMessage } from "@/hooks/useStockWebSocket";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { formatPrice } from "@/lib/format";
import { removeToken } from "@/lib/auth";
import SurgeBriefingPanel from "@/components/SurgeBriefingPanel";
import AlertForm from "@/components/AlertForm";

export default function WatchlistPage() {
  const router = useRouter();
  const authed = useAuthGuard();
  const { watchlist } = useWatchlist();
  const [alertSymbol, setAlertSymbol] = useState<string | null>(null);

  // 급등/급락 이벤트 상태 — symbol별로 surge 정보와 AI 브리핑을 함께 관리
  const [surgeEvents, setSurgeEvents] = useState<Record<string, {
    surge: SurgeMessage;
    briefing?: string;
    loading: boolean;
    market: "US" | "KR";
    displayName: string;
  }>>({});

  // SURGE: 즉시 카드 표시 (loading=true), 30초 후 자동 제거
  const handleSurge = useCallback((surge: SurgeMessage) => {
    const item = watchlist.find((w) => w.symbol === surge.symbol);
    const m = (item?.market ?? "US") as "US" | "KR";
    const displayName = m === "KR" && item?.name ? item.name : surge.symbol;
    setSurgeEvents((prev) => ({
      ...prev,
      [surge.symbol]: { surge, loading: true, market: m, displayName },
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

  const prices = useStockWebSocket(handleSurge, handleAiBriefing);

  if (!authed) return null;

  return (
    <main className="min-h-screen bg-surface-black text-body-on-dark flex flex-col items-center py-8 px-4">
      {/* 헤더 */}
      <div className="w-full max-w-sm flex items-center justify-between mb-6">
        <h1 className="text-[21px] font-semibold text-body-muted tracking-widest uppercase">
          Stock Market · Live
        </h1>
        <div className="flex items-center gap-3">
          <Link href="/agent" className="text-xs text-primary-on-dark hover:opacity-80 transition-opacity">
            AI PB
          </Link>
          <button
            onClick={() => { removeToken(); router.push("/login"); }}
            className="text-xs text-ink-muted-48 hover:text-body-muted transition-colors"
          >
            로그아웃
          </button>
        </div>
      </div>

      {/* 급등/급락 AI 브리핑 패널 */}
      <SurgeBriefingPanel surgeEvents={surgeEvents} />

      {/* 관심종목 목록 */}
      <div className="w-full max-w-sm space-y-2">
        {watchlist.length === 0 && (
          <p className="text-center text-ink-muted-48 text-sm py-6">
            관심종목을 추가해보세요
          </p>
        )}
        {watchlist.map(({ symbol, market: itemMarket, name }) => {
          const trade = prices[symbol];
          const isAlertOpen = alertSymbol === symbol;
          return (
            <div key={symbol}>
              <div className="flex items-center justify-between rounded-lg px-5 py-4 bg-surface-tile-1 hover:bg-surface-tile-2 transition-colors">
                <Link href={`/watchlist/${symbol}`} className="flex flex-col flex-1">
                  <span className="text-[17px] font-semibold text-body-on-dark">
                    {itemMarket === "KR" && name ? name : symbol}
                  </span>
                  <span className="text-xs text-body-muted">
                    {itemMarket === "KR" && name ? symbol : itemMarket}
                  </span>
                </Link>

                <div className="flex items-center gap-3">
                  {trade ? (
                    <span className="text-base font-mono font-semibold text-green-400">
                      {formatPrice(trade.price, itemMarket)}
                    </span>
                  ) : (
                    <span className="text-sm text-ink-muted-48">대기 중...</span>
                  )}
                  <button
                    onClick={() => setAlertSymbol(isAlertOpen ? null : symbol)}
                    className="text-xs text-primary-on-dark hover:opacity-80 transition-opacity"
                  >
                    알림 설정
                  </button>
                </div>
              </div>

              {isAlertOpen && <AlertForm symbol={symbol} market={itemMarket} />}
            </div>
          );
        })}
      </div>

      <Link
        href="/watchlist/register"
        className="mt-6 bg-primary hover:bg-primary-focus text-body-on-dark rounded-pill px-5 py-3 text-[17px] w-full max-w-sm text-center transition-colors"
      >
        관심종목 관리
      </Link>

      <p className="mt-8 text-xs text-ink-muted-48">
        미국 주식: 한국 기준 밤 10시~새벽 5시 · 국내 주식: 오전 9시~오후 3시 30분
      </p>
    </main>
  );
}
