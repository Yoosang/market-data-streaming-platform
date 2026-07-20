package com.usang.marketdata.application.agent.dto;

// 프론트가 들고 있다가 다음 요청에 재전송하는 대화 턴 — DB에 저장하지 않음(stateless)
public record ChatTurn(String role, String content) {}
