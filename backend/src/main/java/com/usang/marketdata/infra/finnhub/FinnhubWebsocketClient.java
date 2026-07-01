package com.usang.marketdata.infra.finnhub;

import tools.jackson.databind.ObjectMapper;
import com.usang.marketdata.application.stock.StockBroadcastService;
import com.usang.marketdata.domain.stock.Trade;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
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
    private final WatchlistRepository watchlistRepository;

    // 현재 Finnhub과 연결된 세션. subscribe/unsubscribe 메시지 전송에 사용
    private WebSocketSession finnhubSession;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.finnhubSession = session;

        // DB에 저장된 관심종목 전체를 구독 (앱 재시작 시에도 기존 구독 복원)
        List<String> symbols = watchlistRepository.findAll().stream()
                .map(w -> w.getSymbol())
                .distinct()
                .toList();

        for (String symbol : symbols) {
            sendSubscribeMessage(session, symbol);
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

    // WatchlistService → SubscriptionManager → 여기로 호출됨
    public void subscribe(String symbol) {
        if (finnhubSession == null || !finnhubSession.isOpen()) {
            log.warn("Finnhub session not ready. Skip subscribe: {}", symbol);
            return;
        }
        try {
            sendSubscribeMessage(finnhubSession, symbol);
        } catch (Exception e) {
            log.error("Failed to subscribe {}: {}", symbol, e.getMessage());
        }
    }

    public void unsubscribe(String symbol) {
        if (finnhubSession == null || !finnhubSession.isOpen()) {
            log.warn("Finnhub session not ready. Skip unsubscribe: {}", symbol);
            return;
        }
        try {
            String msg = String.format("{\"type\":\"unsubscribe\",\"symbol\":\"%s\"}", symbol);
            finnhubSession.sendMessage(new TextMessage(msg));
        } catch (Exception e) {
            log.error("Failed to unsubscribe {}: {}", symbol, e.getMessage());
        }
    }

    private void sendSubscribeMessage(WebSocketSession session, String symbol) throws Exception {
        String msg = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", symbol);
        session.sendMessage(new TextMessage(msg));
    }
}
