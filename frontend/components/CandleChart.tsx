"use client";

import { useEffect, useRef } from "react";
import {
  createChart,
  CandlestickSeries,
  IChartApi,
  ISeriesApi,
  CandlestickSeriesOptions,
  Time,
} from "lightweight-charts";

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

export default function CandleChart({ symbol }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  // chart와 series를 ref로 보관 — 심볼 변경 시 차트를 새로 만들지 않고 데이터만 교체
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);

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

  // 심볼이 바뀔 때마다 데이터만 새로 불러와서 교체
  useEffect(() => {
    if (!seriesRef.current || !symbol) return;

    fetch(`${API_BASE}/api/candles/${symbol}?interval=1m&limit=60`)
      .then((res) => res.json())
      .then((data: CandleData[]) => {
        seriesRef.current?.setData(data);
        chartRef.current?.timeScale().fitContent();
      })
      .catch(console.error);
  }, [symbol]);

  return (
    <div className="w-full max-w-sm mt-4">
      <p className="text-xs text-gray-500 mb-2 px-1">{symbol} · 1분봉</p>
      <div ref={containerRef} className="w-full h-64 rounded-xl overflow-hidden" />
    </div>
  );
}
