package com.usang.marketdata.application.candle;

import com.usang.marketdata.domain.candle.Candle;
import com.usang.marketdata.domain.candle.CandleRepository;
import com.usang.marketdata.domain.stock.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandleAggregator {

    private final CandleRepository candleRepository;

    // 타임프레임별 집계 상태 — 틱이 들어올 때마다 세 Map이 동시에 업데이트됨
    // AtomicReference로 감싸 flush 시 새 Map으로 원자적 교체 — forEach 도중 들어온 틱이
    // clear()로 유실되던 문제 방지 (교체 이후 틱은 새 Map에, 이전 틱은 old Map에 안전하게 분리됨)
    private final AtomicReference<Map<String, InProgressCandle>> oneMinCandles  = newMapRef();
    private final AtomicReference<Map<String, InProgressCandle>> fiveMinCandles = newMapRef();
    private final AtomicReference<Map<String, InProgressCandle>> dailyCandles   = newMapRef();

    private static AtomicReference<Map<String, InProgressCandle>> newMapRef() {
        return new AtomicReference<>(new ConcurrentHashMap<>());
    }

    public void onTrade(Trade trade) {
        update(oneMinCandles,  trade);
        update(fiveMinCandles, trade);
        update(dailyCandles,   trade);
    }

    // compute()는 ConcurrentHashMap의 원자적 연산 — 동시에 여러 틱이 들어와도 안전
    private void update(AtomicReference<Map<String, InProgressCandle>> mapRef, Trade trade) {
        mapRef.get().compute(trade.symbol(), (symbol, candle) -> {
            if (candle == null) return InProgressCandle.start(trade.price(), trade.volume());
            candle.update(trade.price(), trade.volume());
            return candle;
        });
    }

    // ── 1분봉: 매 분 0초 ──────────────────────────────────────────
    @Scheduled(cron = "0 * * * * *")
    public void flushOneMin() {
        // 스케줄러가 10:05:00에 실행 → 저장할 캔들의 openTime은 10:04:00
        LocalDateTime openTime = LocalDateTime.now(ZoneOffset.UTC)
                .minusMinutes(1)
                .truncatedTo(ChronoUnit.MINUTES);
        flush(oneMinCandles, "1m", openTime);
    }

    // ── 5분봉: 매 5분 0초 (0, 5, 10, ... 분) ────────────────────
    @Scheduled(cron = "0 */5 * * * *")
    public void flushFiveMin() {
        // 크론이 5분 경계에 정확히 실행되므로, 5분 빼고 분 단위 절삭만으로 openTime 계산
        // 예: 10:05:00 실행 → openTime = 10:00:00
        LocalDateTime openTime = LocalDateTime.now(ZoneOffset.UTC)
                .minusMinutes(5)
                .truncatedTo(ChronoUnit.MINUTES);
        flush(fiveMinCandles, "5m", openTime);
    }

    // ── 일봉: 매일 자정 (UTC 기준) ────────────────────────────────
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void flushDaily() {
        // 자정에 실행 → 저장할 캔들의 openTime은 전날 자정
        LocalDateTime openTime = LocalDateTime.now(ZoneOffset.UTC)
                .minusDays(1)
                .truncatedTo(ChronoUnit.DAYS);
        flush(dailyCandles, "1d", openTime);
    }

    private void flush(AtomicReference<Map<String, InProgressCandle>> mapRef, String intervalType, LocalDateTime openTime) {
        // 새 빈 Map으로 원자적 교체 — 교체 시점 이후의 onTrade()는 새 Map에 쌓이고,
        // 이 시점까지 쌓인 틱만 담긴 old Map을 안전하게 순회·저장
        Map<String, InProgressCandle> old = mapRef.getAndSet(new ConcurrentHashMap<>());
        if (old.isEmpty()) return;

        List<Candle> toSave = new ArrayList<>();
        old.forEach((symbol, inProgress) ->
                toSave.add(Candle.of(symbol, intervalType,
                        inProgress.open, inProgress.high, inProgress.low, inProgress.close,
                        inProgress.volume, openTime))
        );

        candleRepository.saveAll(toSave);
        log.info("Candle flush [{}]: {} symbols saved for {}", intervalType, toSave.size(), openTime);
    }

    // 집계 중인 캔들의 임시 상태 — CandleAggregator 내부에서만 사용
    static class InProgressCandle {
        double open, high, low, close, volume;

        static InProgressCandle start(double price, double vol) {
            InProgressCandle c = new InProgressCandle();
            c.open = c.high = c.low = c.close = price;
            c.volume = vol;
            return c;
        }

        void update(double price, double vol) {
            if (price > high) high = price;
            if (price < low) low = price;
            close = price;
            volume += vol;
        }
    }
}
