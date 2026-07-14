package com.usang.marketdata.application.surge;

import com.usang.marketdata.api.stock.StockWebSocketHandler;
import com.usang.marketdata.domain.stock.Trade;
import com.usang.marketdata.infra.kis.KisAccessTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurgeDetectorTest {

    @Mock
    private StockWebSocketHandler stockWebSocketHandler;

    @Mock
    private AiBriefingService aiBriefingService;

    // 전일 종가 조회용 HTTP 클라이언트 — mock이므로 get() → null → NPE → catch → fallback(첫 틱 가격)
    @Mock
    private RestClient restClient;

    @Mock
    private KisAccessTokenProvider kisAccessTokenProvider;

    private SurgeDetector surgeDetector;

    // 실제 ObjectMapper 사용 — JSON 직렬화가 테스트 대상이므로 mock으로 대체하지 않음
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        surgeDetector = new SurgeDetector(stockWebSocketHandler, aiBriefingService, restClient, objectMapper, kisAccessTokenProvider);
        // @Value 필드는 Spring 컨텍스트 없이는 주입되지 않아 ReflectionTestUtils로 설정
        ReflectionTestUtils.setField(surgeDetector, "finnhubToken", "test-token");
        ReflectionTestUtils.setField(surgeDetector, "kisAppKey", "test-key");
        ReflectionTestUtils.setField(surgeDetector, "kisAppSecret", "test-secret");
        ReflectionTestUtils.setField(surgeDetector, "kisRestUri", "https://test-kis");
        // restClient.get()이 null을 반환 → fetchPreviousClose에서 NPE → catch → 0 반환
        // → 첫 틱 가격을 기준가로 사용 (테스트에서 원하는 동작)
    }

    @Test
    @DisplayName("첫 번째 틱은 기준가를 초기화하고 SURGE를 발화하지 않는다")
    void 첫_틱은_기준가만_설정하고_SURGE_미발화() {
        surgeDetector.onTrade(new Trade("AAPL", 100.0, 100, System.currentTimeMillis()));

        verifyNoInteractions(stockWebSocketHandler);
        verifyNoInteractions(aiBriefingService);
    }

    @Test
    @DisplayName("전일 종가 대비 5% 이상 급등하면 방향 UP으로 SURGE 메시지가 전송된다")
    void 급등_5퍼센트_이상이면_SURGE_UP_발화() {
        // given: 첫 틱 → 기준가 100으로 초기화
        surgeDetector.onTrade(new Trade("AAPL", 100.0, 100, System.currentTimeMillis()));

        // when: 6% 급등 (5% 초과)
        surgeDetector.onTrade(new Trade("AAPL", 106.0, 100, System.currentTimeMillis()));

        // then: SURGE 메시지 전송
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(stockWebSocketHandler).sendToWatchers(eq("AAPL"), captor.capture());

        String message = captor.getValue();
        assertThat(message).contains("\"type\":\"SURGE\"");
        assertThat(message).contains("\"symbol\":\"AAPL\"");
        assertThat(message).contains("\"direction\":\"UP\"");
        assertThat(message).contains("\"currentPrice\":106.0");

        // AI 브리핑도 비동기 트리거됨
        verify(aiBriefingService).generateAsync(eq("AAPL"), anyDouble(), eq("UP"));
    }

    @Test
    @DisplayName("전일 종가 대비 5% 이상 급락하면 방향 DOWN으로 SURGE 메시지가 전송된다")
    void 급락_5퍼센트_이상이면_SURGE_DOWN_발화() {
        surgeDetector.onTrade(new Trade("TSLA", 100.0, 100, System.currentTimeMillis()));

        // 6% 급락
        surgeDetector.onTrade(new Trade("TSLA", 94.0, 100, System.currentTimeMillis()));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(stockWebSocketHandler).sendToWatchers(eq("TSLA"), captor.capture());

        assertThat(captor.getValue()).contains("\"direction\":\"DOWN\"");
        verify(aiBriefingService).generateAsync(eq("TSLA"), anyDouble(), eq("DOWN"));
    }

    @Test
    @DisplayName("변동이 5% 미만이면 SURGE가 발화되지 않는다")
    void 변동_5퍼센트_미만이면_SURGE_미발화() {
        surgeDetector.onTrade(new Trade("AAPL", 100.0, 100, System.currentTimeMillis()));

        // 4.9% 상승 — 임계값 5% 미달
        surgeDetector.onTrade(new Trade("AAPL", 104.9, 100, System.currentTimeMillis()));

        verifyNoInteractions(stockWebSocketHandler);
        verifyNoInteractions(aiBriefingService);
    }

    @Test
    @DisplayName("정확히 5%일 때 SURGE가 발화된다 (경계값)")
    void 정확히_5퍼센트_경계값에서_SURGE_발화() {
        surgeDetector.onTrade(new Trade("AAPL", 100.0, 100, System.currentTimeMillis()));

        surgeDetector.onTrade(new Trade("AAPL", 105.0, 100, System.currentTimeMillis()));

        verify(stockWebSocketHandler).sendToWatchers(eq("AAPL"), anyString());
    }

    @Test
    @DisplayName("SURGE 발화 후 10분 쿨다운 동안 같은 종목의 재발화가 차단된다")
    void 쿨다운_10분내_재발화_방지() {
        surgeDetector.onTrade(new Trade("AAPL", 100.0, 100, System.currentTimeMillis()));

        // 첫 번째 SURGE 발화 (6% 급등)
        surgeDetector.onTrade(new Trade("AAPL", 106.0, 100, System.currentTimeMillis()));

        // 쿨다운 중 추가 급등
        surgeDetector.onTrade(new Trade("AAPL", 115.0, 100, System.currentTimeMillis()));

        // sendToWatchers는 딱 1번만 호출됨 (쿨다운이 두 번째를 차단)
        verify(stockWebSocketHandler, times(1)).sendToWatchers(eq("AAPL"), anyString());
    }

    @Test
    @DisplayName("서로 다른 종목은 독립적으로 감지되어 각각 SURGE가 발화된다")
    void 다른_종목은_독립적으로_감지() {
        surgeDetector.onTrade(new Trade("AAPL", 100.0, 100, System.currentTimeMillis()));
        surgeDetector.onTrade(new Trade("TSLA", 200.0, 100, System.currentTimeMillis()));

        surgeDetector.onTrade(new Trade("AAPL", 106.0, 100, System.currentTimeMillis()));
        surgeDetector.onTrade(new Trade("TSLA", 211.0, 100, System.currentTimeMillis()));

        // 각 종목마다 독립적으로 SURGE 발화
        verify(stockWebSocketHandler, times(2)).sendToWatchers(anyString(), anyString());
    }

    @Test
    @DisplayName("전일 종가 조회 실패 시 첫 틱 가격을 기준가로 사용하고 이후 5% 변동을 감지한다")
    void 전일종가_조회_실패시_첫_틱이_기준가() {
        // restClient mock이 null 반환 → fetchPreviousClose 내부 NPE → catch → 0 반환 → 첫 틱 가격 사용
        surgeDetector.onTrade(new Trade("AAPL", 100.0, 100, System.currentTimeMillis()));

        // 기준가(첫 틱 100) 대비 5.1% 상승
        surgeDetector.onTrade(new Trade("AAPL", 105.1, 100, System.currentTimeMillis()));

        verify(stockWebSocketHandler).sendToWatchers(eq("AAPL"), anyString());
    }

    @Test
    @DisplayName("KR 종목은 Finnhub 대신 KIS REST API로 전일 종가를 조회한다")
    void KR_종목은_KIS_REST_전일종가_조회() {
        // kisAccessTokenProvider.getAccessToken()이 호출되면 KIS 경로로 진입했음을 확인할 수 있음
        // restClient.get()이 null 반환 → fetchKisPreviousClose 내부 NPE → catch → 첫 틱 가격으로 대체
        surgeDetector.onTrade(new Trade("005930", 70000.0, 10, System.currentTimeMillis()));

        verify(kisAccessTokenProvider).getAccessToken();
    }
}
