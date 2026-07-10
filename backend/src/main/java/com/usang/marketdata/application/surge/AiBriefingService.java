package com.usang.marketdata.application.surge;

import com.usang.marketdata.api.stock.StockWebSocketHandler;
import com.usang.marketdata.application.news.NewsRetrievalService;
import com.usang.marketdata.domain.news.NewsArticle;
import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 급등/급락 감지 후 Claude API를 호출해 한 줄 브리핑을 생성하고 WebSocket으로 전송
@Service
@Slf4j
public class AiBriefingService {

    private static final String FALLBACK = "현재 AI 분석을 일시적으로 제공할 수 없습니다.";
    private static final int MAX_BRIEFING_LENGTH = 150;
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String FINNHUB_NEWS_URL = "https://finnhub.io/api/v1/company-news";

    @Value("${app.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${app.anthropic.model}")
    private String model;

    @Value("${app.finnhub.token}")
    private String finnhubToken;

    private final StockWebSocketHandler stockWebSocketHandler;
    private final ObjectMapper objectMapper;
    // RestClient를 생성자로 주입 — 테스트에서 mock으로 교체 가능하게 설계
    private final RestClient restClient;
    private final NewsRetrievalService newsRetrievalService;
    private final WatchlistRepository watchlistRepository;

    public AiBriefingService(StockWebSocketHandler stockWebSocketHandler,
                              ObjectMapper objectMapper,
                              RestClient restClient,
                              NewsRetrievalService newsRetrievalService,
                              WatchlistRepository watchlistRepository) {
        this.stockWebSocketHandler = stockWebSocketHandler;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
        this.newsRetrievalService = newsRetrievalService;
        this.watchlistRepository = watchlistRepository;
    }

    // @Async: SURGE 전송 후 별도 스레드에서 실행 — AI 응답 지연이 시세 흐름을 막지 않음
    @Async
    public void generateAsync(String symbol, double changePercent, String direction) {
        try {
            List<String> headlines = fetchNewsHeadlines(symbol, direction);
            String briefing = callClaudeApi(symbol, changePercent, direction, headlines);
            briefing = validateAndFallback(briefing);
            sendBriefingMessage(symbol, briefing, headlines.size());
        } catch (Exception e) {
            log.error("AI briefing failed for {}: {}", symbol, e.getMessage());
            sendBriefingMessage(symbol, FALLBACK, 0);
        }
    }

    // US 종목: Finnhub 뉴스 헤드라인. KR 종목(숫자 코드): RAG로 수집된 뉴스 코퍼스에서 유사도 검색
    private List<String> fetchNewsHeadlines(String symbol, String direction) {
        if (symbol.matches("\\d+")) return fetchKrNewsHeadlines(symbol, direction);

        try {
            String today = LocalDate.now().toString();
            String yesterday = LocalDate.now().minusDays(1).toString();
            String url = "%s?symbol=%s&from=%s&to=%s&token=%s"
                    .formatted(FINNHUB_NEWS_URL, symbol, yesterday, today, finnhubToken);

            String response = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode articles = objectMapper.readTree(response);

            List<String> headlines = new ArrayList<>();
            for (int i = 0; i < Math.min(3, articles.size()); i++) {
                String headline = articles.get(i).path("headline").asText();
                if (!headline.isBlank()) headlines.add(headline);
            }
            return headlines;
        } catch (Exception e) {
            log.warn("Failed to fetch news for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    // 미리 수집·임베딩된 KR 뉴스 코퍼스에서 이 종목의 급등/급락과 가장 관련 있는 기사를 검색 (RAG)
    private List<String> fetchKrNewsHeadlines(String symbol, String direction) {
        try {
            String companyName = watchlistRepository.findFirstBySymbol(symbol)
                    .map(Watchlist::getName)
                    .orElse(symbol);
            String movement = direction.equals("UP") ? "급등" : "급락";
            String queryText = "%s 주가 %s 이유".formatted(companyName, movement);

            return newsRetrievalService.findRelevant(symbol, queryText, 3).stream()
                    .map(NewsArticle::getTitle)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to retrieve KR news for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    private String callClaudeApi(String symbol, double changePercent, String direction,
                                  List<String> headlines) {
        String systemPrompt = """
                당신은 금융 분석 AI입니다. 주식 가격 변동의 이유를 한 문장으로 간결하게 설명합니다.
                확실하지 않은 내용은 "~가능성이 있습니다", "~것으로 보입니다" 형태로 표현하세요.
                반드시 한국어로, 한 문장으로만 답하세요.
                """;

        String sign = changePercent > 0 ? "+" : "";
        String movement = direction.equals("UP") ? "급등" : "급락";
        String userPrompt;

        if (headlines.isEmpty()) {
            // KR 종목이거나 뉴스를 가져오지 못한 경우
            userPrompt = """
                    종목 %s이(가) 최근 5분간 %s%.2f%% %s했습니다.
                    뉴스 데이터 없이, 일반적인 시장 맥락에서 가능한 이유를 한 문장으로 설명해주세요.
                    """.formatted(symbol, sign, Math.abs(changePercent), movement);
        } else {
            String headlineList = headlines.stream()
                    .map(h -> "- " + h)
                    .reduce("", (a, b) -> a + "\n" + b);
            userPrompt = """
                    종목 %s이(가) 최근 5분간 %s%.2f%% %s했습니다.
                    최근 뉴스 헤드라인:%s
                    이 가격 변동의 가능한 이유를 한 문장으로 설명해주세요.
                    """.formatted(symbol, sign, Math.abs(changePercent), movement, headlineList);
        }

        // Claude API 요청 바디 구성
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 150,
                "temperature", 0.3,  // 낮을수록 사실적, 창의성 억제 → 할루시네이션 감소
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        String responseStr = callWithRetry(body);
        // .path()는 missing node를 반환하므로 NPE 없이 안전하게 탐색
        JsonNode root = objectMapper.readTree(responseStr);
        return root.path("content").path(0).path("text").asText().strip();
    }

    // 네트워크 순간 장애 대비 1회 재시도
    private String callWithRetry(Map<String, Object> body) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return restClient.post()
                        .uri(ANTHROPIC_API_URL)
                        .header("x-api-key", anthropicApiKey)
                        .header("anthropic-version", "2023-06-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(body))
                        .retrieve()
                        .body(String.class);
            } catch (Exception e) {
                if (attempt == 2) throw new RuntimeException("Claude API call failed after retry", e);
                log.warn("Claude API attempt {} failed, retrying...", attempt);
            }
        }
        throw new RuntimeException("Unreachable");
    }

    // 응답 유효성 검증: 비어있거나 너무 길면 fallback 반환
    private String validateAndFallback(String briefing) {
        if (briefing == null || briefing.isBlank() || briefing.length() > MAX_BRIEFING_LENGTH) {
            log.warn("AI briefing invalid (length={}), using fallback",
                    briefing == null ? 0 : briefing.length());
            return FALLBACK;
        }
        return briefing;
    }

    private void sendBriefingMessage(String symbol, String briefing, int newsCount) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "AI_BRIEFING",
                    "symbol", symbol,
                    "briefing", briefing,
                    "newsCount", newsCount
            );
            stockWebSocketHandler.sendToAll(objectMapper.writeValueAsString(message));
            log.info("AI briefing sent for {}: {}", symbol, briefing);
        } catch (Exception e) {
            log.error("Failed to send AI_BRIEFING for {}: {}", symbol, e.getMessage());
        }
    }
}
