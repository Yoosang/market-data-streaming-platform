package com.usang.marketdata.application.candle;

import com.usang.marketdata.domain.candle.Candle;
import com.usang.marketdata.domain.candle.CandleRepository;
import com.usang.marketdata.domain.stock.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandleAggregatorTest {

    @Mock
    private CandleRepository candleRepository;

    private CandleAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new CandleAggregator(candleRepository);
    }

    @Test
    @DisplayName("여러 틱이 들어오면 OHLCV가 올바르게 집계된다")
    void ohlcv_is_correct() {
        // given: AAPL 틱 4개 (순서대로 100, 105, 98, 102)
        aggregator.onTrade(new Trade("AAPL", 100.0, 10, 1000L));
        aggregator.onTrade(new Trade("AAPL", 105.0, 20, 1001L));
        aggregator.onTrade(new Trade("AAPL", 98.0,  15, 1002L));
        aggregator.onTrade(new Trade("AAPL", 102.0, 5,  1003L));

        // when
        aggregator.flushOneMin();

        // then
        ArgumentCaptor<List<Candle>> captor = ArgumentCaptor.forClass(List.class);
        verify(candleRepository).saveAll(captor.capture());

        Candle candle = captor.getValue().get(0);
        assertThat(candle.getSymbol()).isEqualTo("AAPL");
        assertThat(candle.getIntervalType()).isEqualTo("1m");
        assertThat(candle.getOpen()).isEqualTo(100.0);   // 첫 번째 틱
        assertThat(candle.getHigh()).isEqualTo(105.0);   // 최고가
        assertThat(candle.getLow()).isEqualTo(98.0);     // 최저가
        assertThat(candle.getClose()).isEqualTo(102.0);  // 마지막 틱
        assertThat(candle.getVolume()).isEqualTo(50.0);  // 10+20+15+5
    }

    @Test
    @DisplayName("여러 종목의 틱이 섞여 들어와도 종목별로 분리 집계된다")
    void multiple_symbols_are_aggregated_separately() {
        // given
        aggregator.onTrade(new Trade("AAPL", 100.0, 10, 1000L));
        aggregator.onTrade(new Trade("TSLA", 200.0, 5,  1001L));
        aggregator.onTrade(new Trade("AAPL", 110.0, 20, 1002L));
        aggregator.onTrade(new Trade("TSLA", 190.0, 8,  1003L));

        // when
        aggregator.flushOneMin();

        // then
        ArgumentCaptor<List<Candle>> captor = ArgumentCaptor.forClass(List.class);
        verify(candleRepository).saveAll(captor.capture());

        List<Candle> candles = captor.getValue();
        assertThat(candles).hasSize(2);

        Candle aapl = candles.stream().filter(c -> c.getSymbol().equals("AAPL")).findFirst().orElseThrow();
        assertThat(aapl.getOpen()).isEqualTo(100.0);
        assertThat(aapl.getClose()).isEqualTo(110.0);
        assertThat(aapl.getVolume()).isEqualTo(30.0);

        Candle tsla = candles.stream().filter(c -> c.getSymbol().equals("TSLA")).findFirst().orElseThrow();
        assertThat(tsla.getHigh()).isEqualTo(200.0);
        assertThat(tsla.getLow()).isEqualTo(190.0);
    }

    @Test
    @DisplayName("틱이 없는 분은 flush 시 DB 저장이 발생하지 않는다")
    void flush_does_nothing_when_no_trades() {
        // when
        aggregator.flushOneMin();

        // then
        verify(candleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("flush 후 메모리가 초기화되어 다음 분 집계가 새로 시작된다")
    void memory_is_cleared_after_flush() {
        // given
        aggregator.onTrade(new Trade("AAPL", 100.0, 10, 1000L));
        aggregator.flushOneMin();

        // when: 두 번째 분에 새 틱
        aggregator.onTrade(new Trade("AAPL", 200.0, 5, 2000L));
        aggregator.flushOneMin();

        // then: saveAll이 2번 호출되어야 함
        ArgumentCaptor<List<Candle>> captor = ArgumentCaptor.forClass(List.class);
        verify(candleRepository, times(2)).saveAll(captor.capture());

        // 두 번째 캔들의 open은 200 (이전 분의 100이 남아있으면 안 됨)
        List<Candle> secondFlush = captor.getAllValues().get(1);
        assertThat(secondFlush.get(0).getOpen()).isEqualTo(200.0);
    }

    @Test
    @DisplayName("5분봉 flush 시 intervalType이 '5m'으로 저장된다")
    void five_min_candle_has_correct_interval_type() {
        // given
        aggregator.onTrade(new Trade("AAPL", 100.0, 10, 1000L));
        aggregator.onTrade(new Trade("AAPL", 110.0, 20, 1001L));

        // when: 1분봉은 flush하지 않고 5분봉만 flush
        aggregator.flushFiveMin();

        // then
        ArgumentCaptor<List<Candle>> captor = ArgumentCaptor.forClass(List.class);
        verify(candleRepository).saveAll(captor.capture());

        Candle candle = captor.getValue().get(0);
        assertThat(candle.getIntervalType()).isEqualTo("5m");
        assertThat(candle.getOpen()).isEqualTo(100.0);
        assertThat(candle.getClose()).isEqualTo(110.0);
    }

    @Test
    @DisplayName("1분봉과 5분봉은 각자 독립적으로 집계된다")
    void one_min_and_five_min_are_independent() {
        // given: 틱 2개
        aggregator.onTrade(new Trade("AAPL", 100.0, 10, 1000L));
        aggregator.onTrade(new Trade("AAPL", 120.0, 5, 1001L));

        // when: 1분봉만 flush
        aggregator.flushOneMin();

        // then: 1분봉 flush → 5분봉 맵은 아직 남아있음
        // 5분봉을 flush하면 saveAll이 한 번 더 호출되어야 함
        aggregator.flushFiveMin();

        ArgumentCaptor<List<Candle>> captor = ArgumentCaptor.forClass(List.class);
        verify(candleRepository, times(2)).saveAll(captor.capture());

        // 1분봉 캔들
        Candle oneMin = captor.getAllValues().get(0).get(0);
        assertThat(oneMin.getIntervalType()).isEqualTo("1m");

        // 5분봉 캔들 — 1분봉 flush 후에도 5분봉 맵이 살아있으므로 동일한 틱 데이터를 가짐
        Candle fiveMin = captor.getAllValues().get(1).get(0);
        assertThat(fiveMin.getIntervalType()).isEqualTo("5m");
        assertThat(fiveMin.getOpen()).isEqualTo(100.0);
        assertThat(fiveMin.getClose()).isEqualTo(120.0);
    }
}
