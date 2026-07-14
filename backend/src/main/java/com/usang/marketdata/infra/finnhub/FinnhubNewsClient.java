package com.usang.marketdata.infra.finnhub;

import com.usang.marketdata.infra.finnhub.dto.FinnhubNewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Finnhub REST API로 US 종목 최근 뉴스를 조회 (US는 저장하지 않으므로 매 호출이 곧 최신 조회)
@Component
@RequiredArgsConstructor
@Slf4j
public class FinnhubNewsClient {

    private static final String FINNHUB_NEWS_URL = "https://finnhub.io/api/v1/company-news";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.finnhub.token}")
    private String finnhubToken;

    public List<FinnhubNewsItem> fetchNews(String symbol) {
        try {
            String today = LocalDate.now().toString();
            String yesterday = LocalDate.now().minusDays(1).toString();
            String url = "%s?symbol=%s&from=%s&to=%s&token=%s"
                    .formatted(FINNHUB_NEWS_URL, symbol, yesterday, today, finnhubToken);

            String response = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode articles = objectMapper.readTree(response);

            List<FinnhubNewsItem> items = new ArrayList<>();
            for (int i = 0; i < Math.min(3, articles.size()); i++) {
                JsonNode article = articles.get(i);
                String headline = article.path("headline").asText();
                if (headline.isBlank()) continue;
                items.add(new FinnhubNewsItem(headline, article.path("summary").asText(null),
                        article.path("url").asText(null), article.path("source").asText(null)));
            }
            return items;
        } catch (Exception e) {
            log.warn("Failed to fetch news for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }
}
