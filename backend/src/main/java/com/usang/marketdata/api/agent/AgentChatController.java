package com.usang.marketdata.api.agent;

import com.usang.marketdata.application.agent.AgentChatService;
import com.usang.marketdata.application.agent.dto.AgentChatResult;
import com.usang.marketdata.application.agent.dto.ChatTurn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentChatController {

    private final AgentChatService agentChatService;

    @PostMapping("/chat")
    public AgentChatResult chat(@AuthenticationPrincipal String userId,
                                 @Valid @RequestBody AgentChatRequest request) {
        List<ChatTurn> history = request.history() != null ? request.history() : List.of();
        return agentChatService.chat(userId, request.message(), history);
    }

    record AgentChatRequest(@NotBlank String message, List<ChatTurn> history) {}
}
