package com.usang.marketdata.application.surge;

import com.usang.marketdata.api.stock.StockWebSocketHandler;
import com.usang.marketdata.domain.stock.Trade;
import com.usang.marketdata.infra.kis.KisAccessTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 틱마다 전일 종가 대비 5% 이상 급등/급락을 감지하여 SURGE 이벤트를 발행
@Component
@RequiredArgsConstructor
@Slf4j
public class SurgeDetector {

    private static final double SURGE_THRESHOLD_PERCENT = 5.0;
    private static final long COOLDOWN_MS = 10 * 60 * 1000L; // 10분: 동일 종목 연속 발화 방지

    private final StockWebSocketHandler stockWebSocketHandler;
    private final AiBriefingService aiBriefingService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final KisAccessTokenProvider kisAccessTokenProvider;

    // 전일 종가(기준가) — 종목당 장 시작 시 한 번만 조회하여 고정
    private final Map<String, Double> baselinePrices = new ConcurrentHashMap<>();
    // 쿨다운: 같은 종목이 10분 내에 재발화되지 않도록 제어
    private final Map<String, Long> cooldownUntil = new ConcurrentHashMap<>();

    @Value("${app.finnhub.token}")
    private String finnhubToken;

    @Value("${app.kis.app-key}")
    private String kisAppKey;

    @Value("${app.kis.app-secret}")
    private String kisAppSecret;

    @Value("${app.kis.rest-uri}")
    private String kisRestUri;

    public void onTrade(Trade trade) {
        String symbol = trade.symbol();
        double currentPrice = trade.price();

        // 첫 틱: 전일 종가를 조회하여 기준가로 사용. 조회 실패 시 첫 틱 가격으로 대체
        baselinePrices.computeIfAbsent(symbol, k -> {
            double pc = fetchPreviousClose(k);
            return pc > 0 ? pc : currentPrice;
        });

        double baseline = baselinePrices.get(symbol);
        if (baseline == 0) return;

        double changePercent = (currentPrice - baseline) / baseline * 100.0;

        if (Math.abs(changePercent) >= SURGE_THRESHOLD_PERCENT && !isOnCooldown(symbol)) {
            setCooldown(symbol);
            String direction = changePercent > 0 ? "UP" : "DOWN";
            log.info("Surge detected: {} {}{}% (baseline={}, current={})",
                    symbol, direction.equals("UP") ? "+" : "",
                    String.format("%.2f", changePercent), baseline, currentPrice);
            sendSurgeMessage(symbol, currentPrice, baseline, changePercent, direction);
            // AI 브리핑은 @Async로 실행 — SURGE 전송을 블로킹하지 않음
            aiBriefingService.generateAsync(symbol, changePercent, direction);
        }
    }

    // 온디맨드 AI 분석(AiBriefingController)에서도 재사용 — 캐시에 있으면 그대로, 없으면 새로 조회(캐시에 반영하지 않음)
    public double getPreviousClose(String symbol) {
        Double cached = baselinePrices.get(symbol);
        return cached != null ? cached : fetchPreviousClose(symbol);
    }

    // US 종목: Finnhub /quote의 pc(previous close) 필드
    // KR 종목: KIS REST API의 stck_prdy_clpr(전일 종가) 필드
    // 조회 실패 시 0 반환 → onTrade에서 첫 틱 가격으로 대체
    private double fetchPreviousClose(String symbol) {
        try {
            if (symbol.matches("\\d+")) {
                return fetchKisPreviousClose(symbol);
            }
            return fetchFinnhubPreviousClose(symbol);
        } catch (Exception e) {
            log.warn("Failed to fetch previous close for {}: {}", symbol, e.getMessage());
            return 0;
        }
    }

    private double fetchFinnhubPreviousClose(String symbol) throws Exception {
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + finnhubToken;
        String response = restClient.get().uri(url).retrieve().body(String.class);
        JsonNode root = objectMapper.readTree(response);
        double pc = root.path("pc").asDouble(0);
        log.info("Finnhub previous close for {}: {}", symbol, pc);
        return pc;
    }

    private double fetchKisPreviousClose(String symbol) throws Exception {
        String token = kisAccessTokenProvider.getAccessToken(); // RestClient 체인 전에 미리 조회
        String url = kisRestUri + "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + symbol;
        String response = restClient.get()
                .uri(url)
                .header("authorization", "Bearer " + token)
                .header("appkey", kisAppKey)
                .header("appsecret", kisAppSecret)
                .header("tr_id", "FHKST01010100")
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(response);
        String priceStr = root.path("output").path("stck_prdy_clpr").asText("0");
        double pc = Double.parseDouble(priceStr.replace(",", ""));
        log.info("KIS previous close for {}: {}", symbol, pc);
        return pc;
    }

    private boolean isOnCooldown(String symbol) {
        Long until = cooldownUntil.get(symbol);
        return until != null && System.currentTimeMillis() < until;
    }

    private void setCooldown(String symbol) {
        cooldownUntil.put(symbol, System.currentTimeMillis() + COOLDOWN_MS);
    }

    private void sendSurgeMessage(String symbol, double currentPrice, double baselinePrice,
                                   double changePercent, String direction) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "SURGE",
                    "symbol", symbol,
                    "currentPrice", currentPrice,
                    "baselinePrice", baselinePrice,
                    "changePercent", Math.round(changePercent * 100.0) / 100.0,
                    "direction", direction
            );
            stockWebSocketHandler.sendToAll(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("Failed to send SURGE message for {}: {}", symbol, e.getMessage());
        }
    }
}
