"use client";

import { useEffect, useRef, useState } from "react";
import { authHeaders } from "@/lib/auth";
import {
  createChart,
  CandlestickSeries,
  IChartApi,
  ISeriesApi,
  CandlestickSeriesOptions,
  Time,
} from "lightweight-charts";

type Interval = "1m" | "5m" | "1d";

interface CandleData {
  time: Time;
  open: number;
  high: number;
  low: number;
  close: number;
}

interface Props {
  symbol: string;
}

const API_BASE = "http://localhost:8080";

const INTERVAL_LABELS: Record<Interval, string> = {
  "1m": "1분",
  "5m": "5분",
  "1d": "일봉",
};

export default function CandleChart({ symbol }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);

  const [interval, setInterval] = useState<Interval>("1m");

  // 차트는 마운트 시 한 번만 생성
  useEffect(() => {
    if (!containerRef.current) return;

    const chart = createChart(containerRef.current, {
      autoSize: true,
      layout: {
        background: { color: "#111827" },
        textColor: "#9ca3af",
      },
      grid: {
        vertLines: { color: "#1f2937" },
        horzLines: { color: "#1f2937" },
      },
      timeScale: {
        timeVisible: true,
        secondsVisible: false,
      },
    });

    const series = chart.addSeries(CandlestickSeries, {
      upColor: "#22c55e",
      downColor: "#ef4444",
      borderVisible: false,
      wickUpColor: "#22c55e",
      wickDownColor: "#ef4444",
    } as Partial<CandlestickSeriesOptions>);

    chartRef.current = chart;
    seriesRef.current = series;

    return () => chart.remove();
  }, []);

  // 심볼이나 interval이 바뀔 때마다 데이터만 새로 불러와서 교체
  useEffect(() => {
    if (!seriesRef.current || !symbol) return;

    fetch(`${API_BASE}/api/candles/${symbol}?interval=${interval}&limit=60`, { headers: authHeaders() })
      .then((res) => (res.ok ? res.json() : []))
      .then((data: CandleData[]) => {
        seriesRef.current?.setData(data);
        chartRef.current?.timeScale().fitContent();
      })
      .catch(console.error);
  }, [symbol, interval]);

  return (
    <div className="w-full max-w-sm mt-4">
      <div className="flex items-center justify-between px-1 mb-2">
        <p className="text-xs text-gray-500">
          {symbol} · {INTERVAL_LABELS[interval]}
        </p>
        <div className="flex gap-1">
          {(["1m", "5m", "1d"] as Interval[]).map((iv) => (
            <button
              key={iv}
              onClick={() => setInterval(iv)}
              className={`text-xs px-2 py-0.5 rounded ${
                interval === iv
                  ? "bg-blue-600 text-white"
                  : "text-gray-400 hover:text-white"
              }`}
            >
              {INTERVAL_LABELS[iv]}
            </button>
          ))}
        </div>
      </div>
      <div ref={containerRef} className="w-full h-64 rounded-xl overflow-hidden" />
    </div>
  );
}
