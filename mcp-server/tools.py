import httpx
from mcp.server.fastmcp import FastMCP

from auth_client import BACKEND_URL, get_token, refresh_token

mcp = FastMCP("marketdata-agent-tools")


def _authed_get(path: str, params: dict | None = None) -> dict | list:
    """AgentToolsController(백엔드)에 JWT를 붙여 GET 요청 — 401이면 재로그인 후 1회 재시도"""
    token = get_token()
    response = httpx.get(
        f"{BACKEND_URL}{path}",
        headers={"Authorization": f"Bearer {token}"},
        params=params,
        timeout=10.0,
    )
    if response.status_code == 401:
        token = refresh_token()
        response = httpx.get(
            f"{BACKEND_URL}{path}",
            headers={"Authorization": f"Bearer {token}"},
            params=params,
            timeout=10.0,
        )
    response.raise_for_status()
    return response.json()


@mcp.tool()
def get_watchlist() -> list[dict]:
    """사용자의 관심종목 목록과 각 종목의 현재가, 전일종가, 등락률을 조회합니다."""
    return _authed_get("/api/agent-tools/watchlist")


@mcp.tool()
def get_recent_news(symbol: str, query: str) -> list[dict]:
    """특정 종목의 최근 뉴스를 조회합니다. US 종목(알파벳 티커)은 최신 헤드라인을,
    KR 종목(숫자 코드)은 query와 의미상 가장 관련 있는 기사를 검색해 반환합니다.

    Args:
        symbol: 종목 심볼 (예: AAPL, 005930)
        query: 찾고자 하는 뉴스 주제 (예: '주가 급등 이유', '최근 실적'). KR 종목 검색에만 사용됨
    """
    return _authed_get(f"/api/agent-tools/news/{symbol}", params={"query": query})


@mcp.tool()
def get_candle_stats(symbol: str, interval: str = "1d", count: int = 5) -> dict:
    """특정 종목의 최근 캔들(OHLCV) 데이터를 요약해 최고가, 최저가, 최근 종가,
    기간 등락률을 계산합니다.

    Args:
        symbol: 종목 심볼 (예: AAPL, 005930)
        interval: 캔들 단위 - 1m, 5m, 1d 중 하나 (기본값 1d)
        count: 조회할 캔들 개수 (기본값 5)
    """
    return _authed_get(
        f"/api/agent-tools/candle-stats/{symbol}",
        params={"interval": interval, "count": count},
    )
