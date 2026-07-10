package com.usang.marketdata.infra.kis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

// KIS REST API 호출에 필요한 OAuth2 access token을 관리
// WebSocket용 approval key(KisApprovalKeyProvider)와는 별개 — REST API는 Bearer 토큰 방식 사용
@Component
@Slf4j
public class KisAccessTokenProvider {

    @Value("${app.kis.app-key}")
    private String appKey;

    @Value("${app.kis.app-secret}")
    private String appSecret;

    @Value("${app.kis.rest-uri}")
    private String restUri;

    private String accessToken;

    // access token 유효기간: 24시간 — 서버 기동 시 1회 발급
    @PostConstruct
    public void init() {
        try {
            TokenResponse response = RestClient.create()
                    .post()
                    .uri(restUri + "/oauth2/tokenP")
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "grant_type", "client_credentials",
                            "appkey", appKey,
                            "appsecret", appSecret
                    ))
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.access_token() == null) {
                throw new IllegalStateException("KIS access token response is empty");
            }

            this.accessToken = response.access_token();
            log.info("KIS access token obtained successfully");

        } catch (Exception e) {
            log.error("Failed to obtain KIS access token: {}", e.getMessage());
            throw new RuntimeException("KIS access token initialization failed", e);
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    record TokenResponse(String access_token, String token_type, Long expires_in) {}
}
