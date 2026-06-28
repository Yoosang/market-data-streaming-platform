package com.usang.marketdata.infra.finnhub;

import tools.jackson.databind.ObjectMapper;
import com.usang.marketdata.application.stock.StockBroadcastService;
import com.usang.marketdata.domain.stock.Trade;
import com.usang.marketdata.infra.finnhub.dto.FinnhubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinnhubWebsocketClient extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final StockBroadcastService stockBroadcastService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 연결 직후 관심 종목 구독 메시지를 Finnhub 서버로 전송
        List<String> symbols = List.of("AAPL", "TSLA", "MSFT", "AMZN");
        for (String symbol : symbols) {
            String msg = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", symbol);
            session.sendMessage(new TextMessage(msg));
        }
        log.info("Finnhub WebSocket connected. Subscribed to: {}", symbols);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            FinnhubMessage finnhubMessage = objectMapper.readValue(message.getPayload(), FinnhubMessage.class);

            // ping은 heartbeat 용도이므로 무시
            if (!"trade".equals(finnhubMessage.type()) || finnhubMessage.data() == null) {
                return;
            }

            finnhubMessage.data().stream()
                    .map(t -> new Trade(t.symbol(), t.price(), t.volume(), t.timestamp()))
                    .forEach(stockBroadcastService::broadcast);

        } catch (Exception e) {
            log.error("Finnhub message parsing error: {}", e.getMessage());
        }
    }
}
