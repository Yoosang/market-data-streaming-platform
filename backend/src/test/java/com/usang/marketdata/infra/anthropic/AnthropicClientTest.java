package com.usang.marketdata.infra.anthropic;

import com.usang.marketdata.infra.anthropic.dto.AnthropicResponse;
import com.usang.marketdata.infra.anthropic.dto.ContentBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicClientTest {

    private final AnthropicClient client =
            new AnthropicClient(RestClient.create(), new ObjectMapper());

    @Test
    @DisplayName("text 블록만 있는 응답을 파싱한다")
    void text_블록만_있는_응답_파싱() {
        String responseStr = """
                {
                  "content": [{"type": "text", "text": "안녕하세요"}],
                  "stop_reason": "end_turn"
                }
                """;

        AnthropicResponse response = client.parseResponse(responseStr);

        assertThat(response.stopReason()).isEqualTo("end_turn");
        assertThat(response.content()).containsExactly(new ContentBlock.Text("안녕하세요"));
    }

    @Test
    @DisplayName("tool_use 블록이 있는 응답에서 도구명과 입력을 파싱한다")
    void tool_use_블록_파싱() {
        String responseStr = """
                {
                  "content": [
                    {"type": "tool_use", "id": "toolu_123", "name": "get_watchlist", "input": {"foo": "bar"}}
                  ],
                  "stop_reason": "tool_use"
                }
                """;

        AnthropicResponse response = client.parseResponse(responseStr);

        assertThat(response.stopReason()).isEqualTo("tool_use");
        assertThat(response.content()).containsExactly(
                new ContentBlock.ToolUse("toolu_123", "get_watchlist", java.util.Map.of("foo", "bar")));
    }

    @Test
    @DisplayName("text와 tool_use가 섞인 응답을 순서대로 파싱한다")
    void text와_tool_use_혼합_응답_파싱() {
        String responseStr = """
                {
                  "content": [
                    {"type": "text", "text": "확인해볼게요"},
                    {"type": "tool_use", "id": "toolu_456", "name": "get_recent_news", "input": {"symbol": "AAPL"}}
                  ],
                  "stop_reason": "tool_use"
                }
                """;

        AnthropicResponse response = client.parseResponse(responseStr);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0)).isEqualTo(new ContentBlock.Text("확인해볼게요"));
        assertThat(response.content().get(1)).isEqualTo(
                new ContentBlock.ToolUse("toolu_456", "get_recent_news", java.util.Map.of("symbol", "AAPL")));
    }
}
