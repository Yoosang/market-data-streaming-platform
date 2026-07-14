package com.usang.marketdata.infra.kis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// KIS REST API 호출에 필요한 OAuth2 access token을 관리
// WebSocket용 approval key(KisApprovalKeyProvider)와는 별개 — REST API는 Bearer 토큰 방식 사용
@Component
@Slf4j
public class KisAccessTokenProvider {

    // 토큰 발급 API가 분당 1회로 제한되고 과호출 시 계정이 제한될 수 있어, 재기동 간에도 재사용하도록 파일에 캐싱
    private static final Path TOKEN_CACHE_PATH =
            Path.of(System.getProperty("user.home"), ".marketdata", "kis-access-token.txt");
    // 만료 10분 전부터는 갱신 대상으로 간주
    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofMinutes(10);

    @Value("${app.kis.app-key}")
    private String appKey;

    @Value("${app.kis.app-secret}")
    private String appSecret;

    @Value("${app.kis.rest-uri}")
    private String restUri;

    private String accessToken;
    private Instant expiresAt;

    // access token 유효기간: 24시간 — 캐시된 토큰이 아직 유효하면 재사용, 아니면 새로 발급
    @PostConstruct
    public void init() {
        if (!loadFromCache()) {
            issueNewToken();
        }
    }

    // 장시간 구동 시 24시간 후 토큰이 만료되어 KIS 호출이 조용히 실패하는 것을 막기 위해 만료 전 자동 갱신
    @Scheduled(fixedRate = 60 * 60 * 1000) // 1시간마다 만료 임박 여부 확인
    public void refreshIfNeeded() {
        if (expiresAt != null && Instant.now().isBefore(expiresAt.minus(RENEW_BEFORE_EXPIRY))) return;
        log.info("KIS access token nearing expiry, refreshing...");
        issueNewToken();
    }

    private boolean loadFromCache() {
        try {
            if (!Files.exists(TOKEN_CACHE_PATH)) return false;
            List<String> lines = Files.readAllLines(TOKEN_CACHE_PATH);
            if (lines.size() < 2) return false;

            Instant cachedExpiresAt = Instant.ofEpochMilli(Long.parseLong(lines.get(1)));
            if (!cachedExpiresAt.isAfter(Instant.now().plus(RENEW_BEFORE_EXPIRY))) return false;

            this.accessToken = lines.get(0);
            this.expiresAt = cachedExpiresAt;
            log.info("Reusing cached KIS access token (expires at {})", expiresAt);
            return true;
        } catch (Exception e) {
            log.warn("Failed to load cached KIS token, will issue a new one: {}", e.getMessage());
            return false;
        }
    }

    private void issueNewToken() {
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
            this.expiresAt = Instant.now().plusSeconds(response.expires_in());
            saveToCache();
            log.info("Issued new KIS access token (expires at {})", expiresAt);

        } catch (Exception e) {
            log.error("Failed to obtain KIS access token: {}", e.getMessage());
            throw new RuntimeException("KIS access token initialization failed", e);
        }
    }

    private void saveToCache() {
        try {
            Files.createDirectories(TOKEN_CACHE_PATH.getParent());
            Files.writeString(TOKEN_CACHE_PATH, accessToken + "\n" + expiresAt.toEpochMilli());
        } catch (IOException e) {
            log.warn("Failed to cache KIS token to disk: {}", e.getMessage());
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    record TokenResponse(String access_token, String token_type, Long expires_in) {}
}
