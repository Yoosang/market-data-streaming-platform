package com.usang.marketdata.infra.kis;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

// KIS WebSocket 연결 전에 반드시 approval key를 먼저 발급받아야 함 (Finnhub은 URL 파라미터 방식, KIS는 별도 REST 호출 방식)
@Component
@Slf4j
public class KisApprovalKeyProvider {

    @Value("${app.kis.app-key}")
    private String appKey;

    @Value("${app.kis.app-secret}")
    private String appSecret;

    @Value("${app.kis.rest-uri}")
    private String restUri;

    private String approvalKey;

    @PostConstruct
    public void init() {
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
            log.info("KIS approval key obtained successfully");

        } catch (Exception e) {
            log.error("Failed to obtain KIS approval key: {}", e.getMessage());
            throw new RuntimeException("KIS approval key initialization failed", e);
        }
    }

    public String getApprovalKey() {
        return approvalKey;
    }

    record ApprovalResponse(String approval_key) {}
}
