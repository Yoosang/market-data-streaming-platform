package com.usang.marketdata.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

// WebSocket 연결 시 쿼리 파라미터의 JWT를 검증해 세션에 userId를 부여 — 관심종목 소유자에게만 시세/급등/AI 브리핑을 전송하기 위함
// 토큰이 없거나 유효하지 않아도 연결 자체는 허용하되, userId가 없는 세션은 아무 메시지도 받지 못함
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (token != null && jwtTokenProvider.isValid(token)) {
            attributes.put("userId", jwtTokenProvider.getUsername(token));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }
}
