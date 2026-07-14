package com.usang.marketdata.infra.kis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

// KIS WebSocket 연결 전에 반드시 approval key를 먼저 발급받아야 함 (Finnhub은 URL 파라미터 방식, KIS는 별도 REST 호출 방식)
@Component
@Slf4j
public class KisApprovalKeyProvider {

    // approval key 응답에는 만료시각이 없고, 재발급 시 기존 키가 무효화될 수 있어 재기동 간에는 항상 캐시를 재사용
    private static final Path APPROVAL_KEY_CACHE_PATH =
            Path.of(System.getProperty("user.home"), ".marketdata", "kis-approval-key.txt");

    @Value("${app.kis.app-key}")
    private String appKey;

    @Value("${app.kis.app-secret}")
    private String appSecret;

    @Value("${app.kis.rest-uri}")
    private String restUri;

    private String approvalKey;

    @PostConstruct
    public void init() {
        if (!loadFromCache()) {
            issueNewApprovalKey();
        }
    }

    private boolean loadFromCache() {
        try {
            if (!Files.exists(APPROVAL_KEY_CACHE_PATH)) return false;
            String cached = Files.readString(APPROVAL_KEY_CACHE_PATH).strip();
            if (cached.isBlank()) return false;

            this.approvalKey = cached;
            log.info("Reusing cached KIS approval key");
            return true;
        } catch (Exception e) {
            log.warn("Failed to load cached KIS approval key, will issue a new one: {}", e.getMessage());
            return false;
        }
    }

    private void issueNewApprovalKey() {
        try {
            ApprovalResponse response = RestClient.create()
                    .post()
                    .uri(restUri + "/oauth2/Approval")
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "grant_type", "client_credentials",
                            "appkey", appKey,
                            "secretkey", appSecret
                    ))
                    .retrieve()
                    .body(ApprovalResponse.class);

            if (response == null || response.approval_key() == null) {
                throw new IllegalStateException("KIS approval key response is empty");
            }

            this.approvalKey = response.approval_key();
            saveToCache();
            log.info("KIS approval key obtained successfully");

        } catch (Exception e) {
            log.error("Failed to obtain KIS approval key: {}", e.getMessage());
            throw new RuntimeException("KIS approval key initialization failed", e);
        }
    }

    private void saveToCache() {
        try {
            Files.createDirectories(APPROVAL_KEY_CACHE_PATH.getParent());
            Files.writeString(APPROVAL_KEY_CACHE_PATH, approvalKey);
        } catch (IOException e) {
            log.warn("Failed to cache KIS approval key to disk: {}", e.getMessage());
        }
    }

    public String getApprovalKey() {
        return approvalKey;
    }

    record ApprovalResponse(String approval_key) {}
}
