package com.usang.marketdata.infra.anthropic;

import com.usang.marketdata.infra.anthropic.dto.AnthropicResponse;
import com.usang.marketdata.infra.anthropic.dto.ContentBlock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Claude API에 tools[]를 포함해 호출하고, 멀티턴 tool_use 루프(AgentChatService)가 쓸 수 있는
// 형태로 응답을 파싱. AiBriefingService의 단발성 호출과는 요청/응답 형태가 달라 별도 클라이언트로 분리
@Component
public class AnthropicClient {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final int MAX_TOKENS = 1024;

    @Value("${app.anthropic.api-key}")
    private String apiKey;

    @Value("${app.anthropic.model}")
    private String model;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AnthropicClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public AnthropicResponse createMessage(String system, List<Map<String, Object>> messages,
                                            List<Map<String, Object>> tools) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", MAX_TOKENS,
                "system", system,
                "messages", messages,
                "tools", tools
        );

        String responseStr = restClient.post()
                .uri(ANTHROPIC_API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

        return parseResponse(responseStr);
    }

    // 패키지 내부 테스트에서 직접 검증하기 위해 package-private
    AnthropicResponse parseResponse(String responseStr) {
        JsonNode root = objectMapper.readTree(responseStr);
        String stopReason = root.path("stop_reason").asText(null);

        List<ContentBlock> blocks = new ArrayList<>();
        for (JsonNode block : root.path("content")) {
            String type = block.path("type").asText();
            if (type.equals("text")) {
                blocks.add(new ContentBlock.Text(block.path("text").asText()));
            } else if (type.equals("tool_use")) {
                blocks.add(new ContentBlock.ToolUse(
                        block.path("id").asText(),
                        block.path("name").asText(),
                        objectMapper.readValue(block.path("input").toString(), new TypeReference<Map<String, Object>>() {})
                ));
            }
        }
        return new AnthropicResponse(blocks, stopReason);
    }
}
