package com.usang.marketdata.application.agent;

import com.usang.marketdata.application.agent.dto.AgentChatResult;
import com.usang.marketdata.application.agent.dto.CandleStats;
import com.usang.marketdata.application.agent.dto.ChatTurn;
import com.usang.marketdata.application.agent.dto.WatchlistStat;
import com.usang.marketdata.infra.anthropic.AnthropicClient;
import com.usang.marketdata.infra.anthropic.dto.AnthropicResponse;
import com.usang.marketdata.infra.anthropic.dto.ContentBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentChatServiceTest {

    @Mock
    private AnthropicClient anthropicClient;

    @Mock
    private AgentToolService agentToolService;

    private AgentChatService agentChatService;

    @BeforeEach
    void setUp() {
        agentChatService = new AgentChatService(anthropicClient, agentToolService, new ObjectMapper());
    }

    @Test
    @DisplayName("도구 호출 없이 텍스트만 응답하면 그대로 reply로 반환한다")
    void 도구_호출_없으면_텍스트_그대로_반환() {
        when(anthropicClient.createMessage(anyString(), anyList(), anyList()))
                .thenReturn(new AnthropicResponse(
                        List.of(new ContentBlock.Text("안녕하세요, 무엇을 도와드릴까요?")), "end_turn"));

        AgentChatResult result = agentChatService.chat("user1", "안녕", List.of());

        assertThat(result.reply()).isEqualTo("안녕하세요, 무엇을 도와드릴까요?");
        assertThat(result.toolCalls()).isEmpty();
        assertThat(result.history()).containsExactly(
                new ChatTurn("user", "안녕"),
                new ChatTurn("assistant", "안녕하세요, 무엇을 도와드릴까요?"));
        verify(anthropicClient, times(1)).createMessage(anyString(), anyList(), anyList());
    }

    @Test
    @DisplayName("get_watchlist 도구를 호출한 뒤 최종 텍스트로 응답한다")
    void get_watchlist_호출_후_최종_응답() {
        AnthropicResponse toolUseResponse = new AnthropicResponse(
                List.of(new ContentBlock.ToolUse("toolu_1", "get_watchlist", Map.of())), "tool_use");
        AnthropicResponse finalResponse = new AnthropicResponse(
                List.of(new ContentBlock.Text("관심종목은 AAPL 1개이며 +5% 상승 중입니다.")), "end_turn");

        when(anthropicClient.createMessage(anyString(), anyList(), anyList()))
                .thenReturn(toolUseResponse)
                .thenReturn(finalResponse);
        when(agentToolService.getWatchlist("user1"))
                .thenReturn(List.of(new WatchlistStat("AAPL", "US", null, 110.0, 100.0, 10.0)));

        AgentChatResult result = agentChatService.chat("user1", "내 관심종목 어때?", List.of());

        assertThat(result.reply()).isEqualTo("관심종목은 AAPL 1개이며 +5% 상승 중입니다.");
        assertThat(result.toolCalls()).containsExactly("get_watchlist");
        verify(agentToolService).getWatchlist("user1");
        verify(anthropicClient, times(2)).createMessage(anyString(), anyList(), anyList());
    }

    @Test
    @DisplayName("get_recent_news 호출 시 input의 symbol/query를 그대로 전달한다")
    void get_recent_news_호출시_symbol_query_전달() {
        AnthropicResponse toolUseResponse = new AnthropicResponse(
                List.of(new ContentBlock.ToolUse("toolu_2", "get_recent_news",
                        Map.of("symbol", "AAPL", "query", "주가 급등 이유"))), "tool_use");
        AnthropicResponse finalResponse = new AnthropicResponse(
                List.of(new ContentBlock.Text("최근 실적 발표 영향으로 보입니다.")), "end_turn");

        when(anthropicClient.createMessage(anyString(), anyList(), anyList()))
                .thenReturn(toolUseResponse)
                .thenReturn(finalResponse);
        when(agentToolService.getRecentNews("AAPL", "주가 급등 이유")).thenReturn(List.of());

        AgentChatResult result = agentChatService.chat("user1", "AAPL 왜 올랐어?", List.of());

        assertThat(result.toolCalls()).containsExactly("get_recent_news");
        verify(agentToolService).getRecentNews("AAPL", "주가 급등 이유");
        assertThat(result.reply()).isEqualTo("최근 실적 발표 영향으로 보입니다.");
    }

    @Test
    @DisplayName("get_candle_stats 호출 시 interval/count가 없으면 기본값(1d, 5)을 사용한다")
    void get_candle_stats_기본값_사용() {
        AnthropicResponse toolUseResponse = new AnthropicResponse(
                List.of(new ContentBlock.ToolUse("toolu_3", "get_candle_stats", Map.of("symbol", "AAPL"))), "tool_use");
        AnthropicResponse finalResponse = new AnthropicResponse(
                List.of(new ContentBlock.Text("최근 5일간 10% 상승했습니다.")), "end_turn");

        when(anthropicClient.createMessage(anyString(), anyList(), anyList()))
                .thenReturn(toolUseResponse)
                .thenReturn(finalResponse);
        when(agentToolService.getCandleStats("AAPL", "1d", 5))
                .thenReturn(new CandleStats("AAPL", "1d", 5, 112.0, 98.0, 110.0, 10.0));

        AgentChatResult result = agentChatService.chat("user1", "AAPL 최근 흐름 어때?", List.of());

        assertThat(result.toolCalls()).containsExactly("get_candle_stats");
        verify(agentToolService).getCandleStats("AAPL", "1d", 5);
        assertThat(result.reply()).isEqualTo("최근 5일간 10% 상승했습니다.");
    }

    @Test
    @DisplayName("get_candle_stats 호출 시 interval/count가 명시되면 그 값을 사용한다")
    void get_candle_stats_명시값_사용() {
        AnthropicResponse toolUseResponse = new AnthropicResponse(
                List.of(new ContentBlock.ToolUse("toolu_4", "get_candle_stats",
                        Map.of("symbol", "AAPL", "interval", "1m", "count", 30))), "tool_use");
        AnthropicResponse finalResponse = new AnthropicResponse(
                List.of(new ContentBlock.Text("최근 30분간 변동은 미미합니다.")), "end_turn");

        when(anthropicClient.createMessage(anyString(), anyList(), anyList()))
                .thenReturn(toolUseResponse)
                .thenReturn(finalResponse);
        when(agentToolService.getCandleStats("AAPL", "1m", 30))
                .thenReturn(new CandleStats("AAPL", "1m", 30, 101.0, 99.0, 100.0, 0.5));

        agentChatService.chat("user1", "AAPL 최근 30분 흐름 어때?", List.of());

        verify(agentToolService).getCandleStats("AAPL", "1m", 30);
    }

    @Test
    @DisplayName("반복 상한을 넘기면 fallback 메시지를 반환한다")
    void 반복_상한_초과시_fallback_반환() {
        AnthropicResponse alwaysToolUse = new AnthropicResponse(
                List.of(new ContentBlock.ToolUse("toolu_x", "get_watchlist", Map.of())), "tool_use");
        when(anthropicClient.createMessage(anyString(), anyList(), anyList())).thenReturn(alwaysToolUse);
        when(agentToolService.getWatchlist("user1")).thenReturn(List.of());

        AgentChatResult result = agentChatService.chat("user1", "계속 물어볼게", List.of());

        assertThat(result.reply()).isEqualTo("죄송합니다, 답변을 완성하지 못했습니다.");
        verify(anthropicClient, times(5)).createMessage(anyString(), anyList(), anyList());
    }
}
