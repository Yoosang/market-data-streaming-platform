package com.usang.marketdata.application.candle;

import com.usang.marketdata.domain.candle.Candle;
import com.usang.marketdata.domain.candle.CandleRepository;
import com.usang.marketdata.domain.stock.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandleAggregator {

    private final CandleRepository candleRepository;

    // symbol → 현재 집계 중인 1분봉 상태
    private final Map<String, InProgressCandle> currentCandles = new ConcurrentHashMap<>();

    public void onTrade(Trade trade) {
        // compute()는 ConcurrentHashMap의 원자적 연산 — 동시에 여러 틱이 들어와도 안전
        currentCandles.compute(trade.symbol(), (symbol, candle) -> {
            if (candle == null) {
                return InProgressCandle.start(trade.price(), trade.volume());
            }
            candle.update(trade.price(), trade.volume());
            return candle;
        });
    }

    // 매 분 0초에 실행 — 직전 1분 동안 쌓인 캔들을 DB에 저장하고 메모리를 비움
    @Scheduled(cron = "0 * * * * *")
    public void flush() {
        if (currentCandles.isEmpty()) {
            return;
        }

        // 스케줄러가 10:05:00에 실행되면 → 저장할 캔들의 openTime은 10:04:00
        LocalDateTime openTime = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES);

        List<Candle> toSave = new ArrayList<>();
        currentCandles.forEach((symbol, inProgress) ->
                toSave.add(Candle.of(symbol, "1m",
                        inProgress.open, inProgress.high, inProgress.low, inProgress.close,
                        inProgress.volume, openTime))
        );
        currentCandles.clear();

        candleRepository.saveAll(toSave);
        log.info("Candle flush: {} symbols saved for minute {}", toSave.size(), openTime);
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
            close = price;  // 마지막 틱이 종가
            volume += vol;
        }
    }
}
