package com.usang.marketdata.api.stock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class StockWebSocketHandler extends TextWebSocketHandler {

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

    public void sendToAll(String message) {
        sessions.removeIf(session -> !session.isOpen()); // 닫힌 세션 먼저 정리
        //log.debug("sendToAll: {} session(s), message={}", sessions.size(), message);

        for (WebSocketSession session : sessions) {
            send(session, message);
        }
    }

    // ALERT처럼 본인에게만 보내야 하는 메시지용 — 핸드셰이크 시 토큰이 없었던 세션에는 전송되지 않음
    public void sendToUser(String userId, String message) {
        sessions.removeIf(session -> !session.isOpen());

        for (WebSocketSession session : sessions) {
            if (userId.equals(session.getAttributes().get("userId"))) {
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
