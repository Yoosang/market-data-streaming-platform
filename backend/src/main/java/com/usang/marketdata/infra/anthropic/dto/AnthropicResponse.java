package com.usang.marketdata.infra.anthropic.dto;

import java.util.List;

public record AnthropicResponse(List<ContentBlock> content, String stopReason) {}
