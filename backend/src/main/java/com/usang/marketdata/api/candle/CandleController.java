package com.usang.marketdata.api.candle;

import com.usang.marketdata.domain.candle.Candle;
import com.usang.marketdata.domain.candle.CandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/candles")
@RequiredArgsConstructor
public class CandleController {

    private final CandleRepository candleRepository;

    @GetMapping("/{symbol}")
    public List<CandleResponse> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "60") int limit
    ) {
        // DESC로 조회한 뒤 뒤집어서 오름차순(시간 순)으로 반환 — 차트가 과거→현재 순서를 요구
        List<Candle> candles = new ArrayList<>(candleRepository
                .findBySymbolAndIntervalTypeOrderByOpenTimeDesc(symbol, interval, PageRequest.of(0, limit)));
        Collections.reverse(candles);

        return candles.stream()
                .map(CandleResponse::from)
                .toList();
    }

    // TradingView Lightweight Charts가 기대하는 캔들 포맷
    record CandleResponse(
            long time,   // Unix timestamp (초 단위)
            double open,
            double high,
            double low,
            double close,
            double volume
    ) {
        static CandleResponse from(Candle candle) {
            return new CandleResponse(
                    candle.getOpenTime().toEpochSecond(ZoneOffset.UTC),
                    candle.getOpen(),
                    candle.getHigh(),
                    candle.getLow(),
                    candle.getClose(),
                    candle.getVolume()
            );
        }
    }
}
