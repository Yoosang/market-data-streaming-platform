package com.usang.marketdata.application.broadcast;

import tools.jackson.databind.ObjectMapper;
import com.usang.marketdata.api.stock.StockWebSocketHandler;
import com.usang.marketdata.application.alert.LatestPriceStore;
import com.usang.marketdata.application.candle.CandleAggregator;
import com.usang.marketdata.domain.stock.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockBroadcastService {

    private final StockWebSocketHandler stockWebSocketHandler;
    private final ObjectMapper objectMapper;
    private final CandleAggregator candleAggregator;
    private final LatestPriceStore latestPriceStore;

    // KIS/Finnhub IO 스레드에서 분리 — IO 스레드가 직접 sendMessage()를 호출하면
    // Tomcat WebSocket 내부 상태와 충돌할 수 있어 별도 스레드에서 브로드캐스트
    @Async
    public void broadcast(Trade trade) {
        candleAggregator.onTrade(trade);
        latestPriceStore.update(trade.symbol(), trade.price()); // 알림 체커가 참조하는 최신가 갱신

        try {
            String json = objectMapper.writeValueAsString(trade);
            stockWebSocketHandler.sendToAll(json);
        } catch (Exception e) {
            log.error("Failed to serialize trade: {}", e.getMessage());
        }
    }
}
