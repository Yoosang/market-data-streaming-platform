package com.usang.marketdata.infra.kis;

import tools.jackson.databind.ObjectMapper;
import com.usang.marketdata.application.broadcast.StockBroadcastService;
import com.usang.marketdata.domain.stock.Trade;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import com.usang.marketdata.infra.kis.dto.KisRealtimeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KisWebsocketClient extends TextWebSocketHandler {

    private final KisApprovalKeyProvider approvalKeyProvider;
    private final StockBroadcastService stockBroadcastService;
    private final WatchlistRepository watchlistRepository;
    private final ObjectMapper objectMapper;

    private WebSocketSession kisSession;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.kisSession = session;

        // DB에 저장된 KR 종목만 구독 — US 종목은 FinnhubWebsocketClient가 담당
        List<String> symbols = watchlistRepository.findByMarket("KR").stream()
                .map(w -> w.getSymbol())
                .distinct()
                .toList();

        for (String symbol : symbols) {
            subscribe(symbol);
        }
        log.info("KIS WebSocket connected. Subscribed to: {}", symbols);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

        // KIS는 trade 메시지(pipe-delimited)와 JSON 메시지(heartbeat/응답)가 섞여 들어옴
        // '|' 포함 여부로 trade 메시지 판별
        if (!payload.contains("|")) {
            log.debug("KIS non-trade message: {}", payload);
            return;
        }

        try {
            String[] parts = payload.split("\\|");
            // [1]: tr_id — H0UNCNT0이 주식현재가 실시간 데이터
            if (parts.length < 4 || !"H0UNCNT0".equals(parts[1])) {
                return;
            }

            KisRealtimeData data = KisRealtimeData.parse(parts[3]);
            Trade trade = new Trade(data.symbol(), data.price(), data.volume(), System.currentTimeMillis());
            stockBroadcastService.broadcast(trade);

        } catch (Exception e) {
            log.error("KIS message parsing error: {}", e.getMessage());
        }
    }

    public void subscribe(String stockCode) {
        sendMessage(stockCode, "1");
    }

    public void unsubscribe(String stockCode) {
        sendMessage(stockCode, "2");
    }

    // tr_type: "1" = 구독, "2" = 구독 해제
    private void sendMessage(String stockCode, String trType) {
        if (kisSession == null || !kisSession.isOpen()) {
            log.warn("KIS session not ready. Skip {}: {}", trType.equals("1") ? "subscribe" : "unsubscribe", stockCode);
            return;
        }
        try {
            Map<String, Object> message = Map.of(
                    "header", Map.of(
                            "approval_key", approvalKeyProvider.getApprovalKey(),
                            "custtype", "P",
                            "tr_type", trType,
                            "content-type", "utf-8"
                    ),
                    "body", Map.of(
                            "input", Map.of(
                                    "tr_id", "H0UNCNT0",
                                    "tr_key", stockCode
                            )
                    )
            );
            kisSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            log.info("KIS {} to: {}", trType.equals("1") ? "subscribed" : "unsubscribed", stockCode);
        } catch (Exception e) {
            log.error("KIS sendMessage failed for {}: {}", stockCode, e.getMessage());
        }
    }
}
