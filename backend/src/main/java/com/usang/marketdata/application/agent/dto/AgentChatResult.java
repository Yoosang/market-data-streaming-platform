package com.usang.marketdata.application.agent.dto;

import java.util.List;

public record AgentChatResult(String reply, List<ChatTurn> history, List<String> toolCalls) {}
