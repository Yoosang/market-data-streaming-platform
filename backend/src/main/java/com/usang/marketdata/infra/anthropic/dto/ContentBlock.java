package com.usang.marketdata.infra.anthropic.dto;

import java.util.Map;

// Claude 응답의 content[] 각 항목 — type 필드("text"/"tool_use")에 따라 분기해서 파싱됨
public sealed interface ContentBlock {
    record Text(String text) implements ContentBlock {}
    record ToolUse(String id, String name, Map<String, Object> input) implements ContentBlock {}
}
