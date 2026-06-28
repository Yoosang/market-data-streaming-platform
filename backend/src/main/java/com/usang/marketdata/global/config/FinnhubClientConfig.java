package com.usang.marketdata.global.config;

import com.usang.marketdata.infra.finnhub.FinnhubWebsocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
@RequiredArgsConstructor
public class FinnhubClientConfig {

    private final FinnhubWebsocketClient finnhubWebsocketClient;

    @Value("${app.finnhub.uri}")
    private String finnhubUri;

    @Value("${app.finnhub.token}")
    private String finnhubToken;

    // Finnhub WebSocket 서버에 클라이언트로 연결 (애플리케이션 시작 시 자동 연결)
    @Bean
    public WebSocketConnectionManager finnhubConnectionManager() {
        String uri = finnhubUri + "?token=" + finnhubToken;

        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                new StandardWebSocketClient(),
                finnhubWebsocketClient,
                uri
        );
        manager.setAutoStartup(true);
        return manager;
    }
}
