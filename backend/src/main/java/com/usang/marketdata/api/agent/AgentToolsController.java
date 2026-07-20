package com.usang.marketdata.api.agent;

import com.usang.marketdata.application.agent.AgentToolService;
import com.usang.marketdata.application.agent.dto.CandleStats;
import com.usang.marketdata.application.agent.dto.NewsItem;
import com.usang.marketdata.application.agent.dto.WatchlistStat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// AgentToolService를 그대로 감싸는 REST 엔드포인트 — 외부 MCP 서버(mcp-server/)가 이 API를 호출해
// 백엔드 내부 Agent(AgentChatService)와 동일한 도구를 사용한다.
// AgentChatService는 이 컨트롤러를 거치지 않고 AgentToolService를 같은 JVM 내에서 직접 호출한다.
@RestController
@RequestMapping("/api/agent-tools")
@RequiredArgsConstructor
public class AgentToolsController {

    private final AgentToolService agentToolService;

    @GetMapping("/watchlist")
    public List<WatchlistStat> getWatchlist(@AuthenticationPrincipal String userId) {
        return agentToolService.getWatchlist(userId);
    }

    @GetMapping("/news/{symbol}")
    public List<NewsItem> getRecentNews(@PathVariable String symbol, @RequestParam String query) {
        return agentToolService.getRecentNews(symbol, query);
    }

    @GetMapping("/candle-stats/{symbol}")
    public CandleStats getCandleStats(@PathVariable String symbol,
                                       @RequestParam(defaultValue = "1d") String interval,
                                       @RequestParam(defaultValue = "5") int count) {
        return agentToolService.getCandleStats(symbol, interval, count);
    }
}
