package com.usang.marketdata.infra.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

// 텔레그램 봇 API로 가격 알림을 발송 (단일 계정 — 사용자별 chat-id 등록 없이 .env에 고정)
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramNotifier {

    private final RestClient restClient;

    @Value("${app.telegram.bot-token}")
    private String botToken;

    @Value("${app.telegram.chat-id}")
    private String chatId;

    public void notify(String text) {
        try {
            restClient.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to send Telegram notification: {}", e.getMessage());
        }
    }
}
