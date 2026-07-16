package com.usang.marketdata.application.agent;

import com.usang.marketdata.application.agent.dto.AgentChatResult;
import com.usang.marketdata.application.agent.dto.ChatTurn;
import com.usang.marketdata.infra.anthropic.AnthropicClient;
import com.usang.marketdata.infra.anthropic.dto.AnthropicResponse;
import com.usang.marketdata.infra.anthropic.dto.ContentBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 사용자 질문에 답하기 위해 Claude가 도구를 스스로 선택·호출하는 멀티턴 루프.
// 프론트에는 항상 정제된 user/assistant 텍스트 턴만 노출하고, tool_use/tool_result 왕복은
// 이 메서드 호출(HTTP 요청 하나) 안에서만 일어나고 끝난다 — 대화 이력은 DB에 저장하지 않는다.
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentChatService {

    private static final int MAX_ITERATIONS = 5;
    private static final String FALLBACK = "죄송합니다, 답변을 완성하지 못했습니다.";
    private static final String SYSTEM_PROMPT = """
            당신은 사용자의 주식 관심종목을 분석해주는 AI PB(Private Banker) 어시스턴트입니다.
            필요한 정보는 반드시 도구를 호출해 직접 조회하고, 추측하지 마세요.
            확실하지 않은 내용은 "~것으로 보입니다" 형태로 표현하고, 반드시 한국어로 답하세요.
            """;

    private final AnthropicClient anthropicClient;
    private final AgentToolService agentToolService;
    private final ObjectMapper objectMapper;

    public AgentChatResult chat(String userId, String message, List<ChatTurn> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatTurn turn : history) {
            messages.add(Map.of("role", turn.role(), "content", turn.content()));
        }
        messages.add(Map.of("role", "user", "content", message));

        List<String> toolCalls = new ArrayList<>();
        List<Map<String, Object>> tools = buildToolDefinitions();

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            AnthropicResponse response = anthropicClient.createMessage(SYSTEM_PROMPT, messages, tools);
            messages.add(Map.of("role", "assistant", "content", toApiContent(response.content())));

            List<ContentBlock.ToolUse> toolUses = response.content().stream()
                    .filter(ContentBlock.ToolUse.class::isInstance)
                    .map(ContentBlock.ToolUse.class::cast)
                    .toList();

            if (toolUses.isEmpty()) {
                String reply = extractText(response.content());
                return new AgentChatResult(reply, appendTurn(history, message, reply), toolCalls);
            }

            List<Map<String, Object>> toolResults = new ArrayList<>();
            for (ContentBlock.ToolUse toolUse : toolUses) {
                toolCalls.add(toolUse.name());
                Object result = executeTool(userId, toolUse);
                toolResults.add(Map.of(
                        "type", "tool_result",
                        "tool_use_id", toolUse.id(),
                        "content", objectMapper.writeValueAsString(result)
                ));
            }
            messages.add(Map.of("role", "user", "content", toolResults));
        }

        log.warn("Agent chat exceeded max iterations ({}) for user {}", MAX_ITERATIONS, userId);
        return new AgentChatResult(FALLBACK, appendTurn(history, message, FALLBACK), toolCalls);
    }

    private List<ChatTurn> appendTurn(List<ChatTurn> history, String userMessage, String reply) {
        List<ChatTurn> updated = new ArrayList<>(history);
        updated.add(new ChatTurn("user", userMessage));
        updated.add(new ChatTurn("assistant", reply));
        return updated;
    }

    // 도구 이름은 여기서만 분기 — 새 도구가 늘어나도 이 switch 한 곳만 늘어남
    private Object executeTool(String userId, ContentBlock.ToolUse toolUse) {
        Map<String, Object> input = toolUse.input();
        return switch (toolUse.name()) {
            case "get_watchlist" -> agentToolService.getWatchlist(userId);
            case "get_recent_news" -> agentToolService.getRecentNews(
                    (String) input.get("symbol"), (String) input.get("query"));
            case "get_candle_stats" -> agentToolService.getCandleStats(
                    (String) input.get("symbol"),
                    input.get("interval") != null ? (String) input.get("interval") : "1d",
                    input.get("count") != null ? ((Number) input.get("count")).intValue() : 5);
            default -> Map.of("error", "unknown tool: " + toolUse.name());
        };
    }

    // Claude 응답의 content(파싱된 ContentBlock)를 다음 요청에 그대로 되돌려보낼 원본 형태로 변환
    private List<Map<String, Object>> toApiContent(List<ContentBlock> blocks) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (ContentBlock block : blocks) {
            if (block instanceof ContentBlock.Text t) {
                content.add(Map.of("type", "text", "text", t.text()));
            } else if (block instanceof ContentBlock.ToolUse tu) {
                content.add(Map.of("type", "tool_use", "id", tu.id(), "name", tu.name(), "input", tu.input()));
            }
        }
        return content;
    }

    private String extractText(List<ContentBlock> blocks) {
        return blocks.stream()
                .filter(ContentBlock.Text.class::isInstance)
                .map(b -> ((ContentBlock.Text) b).text())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    }

    private List<Map<String, Object>> buildToolDefinitions() {
        return List.of(
                Map.of(
                        "name", "get_watchlist",
                        "description", "사용자의 관심종목 목록과 각 종목의 현재가, 전일종가, 등락률을 조회합니다.",
                        "input_schema", Map.of("type", "object", "properties", Map.of())
                ),
                Map.of(
                        "name", "get_recent_news",
                        "description", "특정 종목의 최근 뉴스를 조회합니다. US 종목(알파벳 티커)은 최신 헤드라인을, "
                                + "KR 종목(숫자 코드)은 query와 의미상 가장 관련 있는 기사를 검색해 반환합니다.",
                        "input_schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "symbol", Map.of("type", "string", "description", "종목 심볼 (예: AAPL, 005930)"),
                                        "query", Map.of("type", "string",
                                                "description", "찾고자 하는 뉴스 주제 (예: '주가 급등 이유', '최근 실적'). KR 종목 검색에만 사용됨")
                                ),
                                "required", List.of("symbol", "query")
                        )
                ),
                Map.of(
                        "name", "get_candle_stats",
                        "description", "특정 종목의 최근 캔들(OHLCV) 데이터를 요약해 최고가, 최저가, 최근 종가, "
                                + "기간 등락률을 계산합니다.",
                        "input_schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "symbol", Map.of("type", "string", "description", "종목 심볼 (예: AAPL, 005930)"),
                                        "interval", Map.of("type", "string",
                                                "description", "캔들 단위: 1m, 5m, 1d 중 하나 (기본값 1d)"),
                                        "count", Map.of("type", "integer",
                                                "description", "조회할 캔들 개수 (기본값 5)")
                                ),
                                "required", List.of("symbol")
                        )
                )
        );
    }
}
