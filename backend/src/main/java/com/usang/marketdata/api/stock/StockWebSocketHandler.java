package com.usang.marketdata.api.stock;

import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockWebSocketHandler extends TextWebSocketHandler {

    private final WatchlistRepository watchlistRepository;

    // CopyOnWriteArrayList: 다수의 Finnhub 메시지가 동시에 세션 목록을 읽을 때 스레드 안전 보장
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Frontend connected: {} (total: {})", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Frontend disconnected: {} (total: {})", session.getId(), sessions.size());
    }

    // symbol을 관심종목으로 등록한 사용자에게만 전송 — 시세/급등/AI 브리핑 모두 공용으로 사용
    // (핸드셰이크 시 토큰이 없거나 유효하지 않았던 세션은 userId가 없어 아무 메시지도 받지 못함)
    public void sendToWatchers(String symbol, String message) {
        Set<String> userIds = watchlistRepository.findBySymbol(symbol).stream()
                .map(Watchlist::getUserId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;

        sessions.removeIf(session -> !session.isOpen()); // 닫힌 세션 먼저 정리

        for (WebSocketSession session : sessions) {
            Object userId = session.getAttributes().get("userId");
            if (userId != null && userIds.contains(userId)) {
                send(session, message);
            }
        }
    }

    private void send(WebSocketSession session, String message) {
        synchronized (session) { // 동일 세션에 동시 sendMessage 방지 (@Async 환경)
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.warn("Session {} send failed, removing: {}", session.getId(), e.getMessage());
                sessions.remove(session);
            }
        }
    }
}
