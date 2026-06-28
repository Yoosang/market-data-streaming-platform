package com.usang.marketdata.application.stock;

import tools.jackson.databind.ObjectMapper;
import com.usang.marketdata.api.stock.StockWebSocketHandler;
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

    public void broadcast(Trade trade) {
        try {
            String json = objectMapper.writeValueAsString(trade);
            stockWebSocketHandler.sendToAll(json);
        } catch (Exception e) {
            log.error("Failed to serialize trade: {}", e.getMessage());
        }
    }
}
