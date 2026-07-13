"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useWatchlist } from "@/hooks/useWatchlist";
import { useStockWebSocket, AiBriefingMessage } from "@/hooks/useStockWebSocket";
import { authHeaders, isLoggedIn } from "@/lib/auth";
import CandleChart from "@/components/CandleChart";

const API_BASE = "http://localhost:8080";

interface NewsItem {
  title: string;
  description: string | null;
  url: string | null;
  source: string | null;
}

export default function WatchlistDetailPage() {
  const router = useRouter();
  const params = useParams<{ symbol: string }>();
  const symbol = params.symbol;
  const { watchlist } = useWatchlist();

  const [news, setNews] = useState<NewsItem[]>([]);
  const [newsLoading, setNewsLoading] = useState(false);
  const [briefing, setBriefing] = useState<string | null>(null);
  const [briefingLoading, setBriefingLoading] = useState(false);
  const [briefingError, setBriefingError] = useState<string | null>(null);

  // 미로그인 시 로그인 페이지로 이동
  useEffect(() => {
    if (!isLoggedIn()) router.push("/login");
  }, [router]);

  const item = watchlist.find((w) => w.symbol === symbol);
  const displayName = item?.market === "KR" && item?.name ? item.name : symbol;

  const fetchNews = useCallback((refresh: boolean) => {
    setNewsLoading(true);
    fetch(`${API_BASE}/api/news/${symbol}?refresh=${refresh}`, { headers: authHeaders() })
      .then((res) => (res.ok ? res.json() : []))
      .then((data: NewsItem[]) => setNews(data))
      .catch(console.error)
      .finally(() => setNewsLoading(false));
  }, [symbol]);

  // 상세 페이지 진입 시 뉴스 자동 로드 (저장된 게 없으면 백엔드가 즉시 수집)
  useEffect(() => {
    if (symbol) fetchNews(false);
  }, [symbol, fetchNews]);

  // AI_BRIEFING: 이 페이지의 symbol에 해당하는 결과만 반영
  const handleAiBriefing = useCallback((msg: AiBriefingMessage) => {
    if (msg.symbol !== symbol) return;
    setBriefing(msg.briefing);
    setBriefingLoading(false);
  }, [symbol]);

  useStockWebSocket(undefined, handleAiBriefing);

  const handleAnalyze = async () => {
    setBriefingLoading(true);
    setBriefingError(null);
    setBriefing(null);

    const res = await fetch(`${API_BASE}/api/ai-briefing/${symbol}`, {
      method: "POST",
      headers: authHeaders(),
    });

    // 성공(202)이면 결과는 WebSocket AI_BRIEFING 메시지로 옴 — 여기선 로딩만 유지
    if (!res.ok) {
      setBriefingLoading(false);
      setBriefingError("실시간 시세가 없어 분석할 수 없습니다");
    }
  };

  return (
    <main className="min-h-screen bg-surface-black text-body-on-dark flex flex-col items-center py-8 px-4">
      <div className="w-full max-w-sm mb-6">
        <Link href="/watchlist" className="text-xs text-ink-muted-48 hover:text-body-muted transition-colors">
          ← 목록으로
        </Link>
        <h1 className="text-[21px] font-semibold text-body-on-dark mt-2">{displayName}</h1>
      </div>

      <CandleChart symbol={symbol} />

      {/* AI 분석 */}
      <div className="w-full max-w-sm mt-6">
        <button
          onClick={handleAnalyze}
          disabled={briefingLoading}
          className="w-full bg-primary hover:bg-primary-focus disabled:opacity-50 text-body-on-dark rounded-pill py-3 text-[17px] transition-colors"
        >
          {briefingLoading ? "분석 중..." : "AI 분석"}
        </button>
        {briefingError && (
          <p className="text-red-400 text-xs mt-2 px-1">{briefingError}</p>
        )}
        {briefing && (
          <p className="text-sm text-body-muted mt-3 px-1 leading-relaxed">{briefing}</p>
        )}
      </div>

      {/* 뉴스 */}
      <div className="w-full max-w-sm mt-6">
        <div className="flex items-center justify-between mb-2 px-1">
          <p className="text-xs text-body-muted">관련 뉴스</p>
          <button
            onClick={() => fetchNews(true)}
            disabled={newsLoading}
            className="text-xs text-primary-on-dark hover:opacity-80 disabled:opacity-50 transition-opacity"
          >
            뉴스 새로고침
          </button>
        </div>

        {newsLoading && news.length === 0 && (
          <p className="text-center text-ink-muted-48 text-sm py-6">불러오는 중...</p>
        )}
        {!newsLoading && news.length === 0 && (
          <p className="text-center text-ink-muted-48 text-sm py-6">관련 뉴스가 없습니다</p>
        )}

        <div className="space-y-2">
          {news.map((n, i) =>
            n.url ? (
              <a
                key={i}
                href={n.url}
                target="_blank"
                rel="noopener noreferrer"
                className="block rounded-lg px-4 py-3 bg-surface-tile-1 hover:bg-surface-tile-2 transition-colors"
              >
                {n.source && (
                  <p className="text-xs text-primary-on-dark mb-1">{n.source}</p>
                )}
                <p className="text-sm text-body-on-dark">{n.title}</p>
                {n.description && (
                  <p className="text-xs text-body-muted mt-1">{n.description}</p>
                )}
              </a>
            ) : (
              <div key={i} className="rounded-lg px-4 py-3 bg-surface-tile-1">
                {n.source && (
                  <p className="text-xs text-primary-on-dark mb-1">{n.source}</p>
                )}
                <p className="text-sm text-body-on-dark">{n.title}</p>
              </div>
            )
          )}
        </div>
      </div>
    </main>
  );
}
