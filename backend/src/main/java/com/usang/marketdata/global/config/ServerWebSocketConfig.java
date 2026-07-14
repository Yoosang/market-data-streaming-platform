package com.usang.marketdata.global.config;

import com.usang.marketdata.api.stock.StockWebSocketHandler;
import com.usang.marketdata.infra.security.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableAsync
@RequiredArgsConstructor
public class ServerWebSocketConfig implements WebSocketConfigurer {

    private final StockWebSocketHandler stockWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 프론트엔드가 ws://localhost:8080/ws/stock?token=... 으로 연결
        // setAllowedOrigins("*"): 로컬 개발 환경에서 Next.js(3000포트)의 CORS 허용
        registry.addHandler(stockWebSocketHandler, "/ws/stock")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
