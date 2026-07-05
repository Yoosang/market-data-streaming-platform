package com.usang.marketdata.global.config;

import com.usang.marketdata.infra.kis.KisWebsocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
@RequiredArgsConstructor
public class KisClientConfig {

    private final KisWebsocketClient kisWebsocketClient;

    @Value("${app.kis.ws-uri}")
    private String kisWsUri;

    // kisApprovalKeyProvider가 먼저 초기화되어야 WebSocket 연결 시 구독 메시지를 보낼 수 있음
    @Bean
    @DependsOn("kisApprovalKeyProvider")
    public WebSocketConnectionManager kisConnectionManager() {
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                new StandardWebSocketClient(),
                kisWebsocketClient,
                kisWsUri
        );
        manager.setAutoStartup(true);
        return manager;
    }
}
