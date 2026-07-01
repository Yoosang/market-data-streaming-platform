package com.usang.marketdata.application.stock;

import tools.jackson.databind.ObjectMapper;
import com.usang.marketdata.api.stock.StockWebSocketHandler;
import com.usang.marketdata.application.candle.CandleAggregator;
import com.usang.marketdata.domain.stock.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockBroadcastService {

    private final StockWebSocketHandler stockWebSocketHandler;
    private final ObjectMapper objectMapper;
    private final CandleAggregator candleAggregator;

    public void broadcast(Trade trade) {
        // 틱 데이터를 캔들 집계기에 전달 (프론트 전송과 동시에 처리)
        candleAggregator.onTrade(trade);

        try {
            String json = objectMapper.writeValueAsString(trade);
            stockWebSocketHandler.sendToAll(json);
        } catch (Exception e) {
            log.error("Failed to serialize trade: {}", e.getMessage());
        }
    }
}
