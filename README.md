# Market Data Streaming Platform

미국·국내 주식 실시간 시세를 모바일 WTS 스타일로 보여주는 학습용 프로젝트입니다.

> **학습 목적으로 단계별 기능을 추가하며 발전시킨 프로젝트입니다.**  
> Ver1 → Ver2 → Ver3 → Ver4 → Ver5 → Ver6 → Ver7 → Ver8 순서로 기능을 확장하고, 각 버전은 git tag로 구분합니다.

---

## 현재 버전: Ver8 ✅ (완료) — 다음 버전(Ver9) 계획 미정

기존 AI브리핑은 급등 감지 시 Claude를 1회 호출하는 단발성 흐름이라, 여러 단계에 걸쳐 스스로 도구를
선택·호출하는 "Agent"는 없었습니다. Ver8에서는 (1) 관심종목/뉴스/캔들 조회 도구를 Claude가 스스로
호출해가며 답하는 대화형 Agent와 (2) 동일한 도구를 MCP로 노출하는 독립 Python 서버를 추가했습니다.
애초 Ver8로 계획했던 프로덕션 하드닝/배포는 Ver9로 넘어갔습니다. 자세한 내용은
[ROADMAP.md](./ROADMAP.md)의 Ver8 항목을 참고하세요.

### 버전별 학습 목표 요약

| 버전 | 핵심 학습 | 주요 기능 |
|------|-----------|-----------|
| Ver1 | WebSocket 기초 | Finnhub 실시간 시세, 브로드캐스트 |
| Ver2 | DB 설계, 캔들 집계 | 관심종목 저장, 분봉/일봉 차트 |
| Ver3 | 다중 데이터소스, 알림 | 한국투자증권 API, 가격 알림 |
| Ver4 | 인증 (JWT) | Spring Security + JWT 로그인 |
| Ver5 | LLM API 연동 | 급등/급락 감지 + Claude AI 브리핑 |
| Ver6 | RAG (검색 증강 생성) | 뉴스 임베딩 + 벡터 유사도 검색으로 KR 브리핑 개선 |
| Ver7 | 벡터 DB (pgvector), 세션 보안 | 페이지 리팩토링, 텔레그램 알림, RAG를 pgvector로 마이그레이션, JWT 세션 30분 |
| Ver8 | AI Agent (Tool Use), MCP | 대화형 AI PB 챗봇 + 도구 3종, 동일 도구를 MCP 서버로 외부 노출 |

---

## 기술 스택

| 레이어 | 기술 | 도입 버전 |
|--------|------|-----------|
| Backend | Java 17, Spring Boot | Ver1 |
| Frontend | Next.js (App Router), React | Ver1 |
| 실시간 시세 (US) | Finnhub WebSocket | Ver1 |
| DB | MySQL + Spring Data JPA | Ver2 |
| Chart | TradingView Lightweight Charts | Ver2 |
| 실시간 시세 (KR) | 한국투자증권 WebSocket API | Ver3 |
| 가격 알림 | 텔레그램 봇 API (Ver3에서 브라우저 Notification으로 시작, Ver7에서 텔레그램 전용으로 전환) | Ver3 |
| 인증 | Spring Security + JWT (jjwt) | Ver4 |
| AI 브리핑 | Claude API (claude-haiku) | Ver5 |
| KR 뉴스 수집 | 네이버 뉴스 검색 API | Ver6 |
| 임베딩 / RAG | OpenAI Embeddings (text-embedding-3-small) | Ver6 |
| 벡터 DB | pgvector (Postgres) — RAG 뉴스 코퍼스 전용, 나머지는 MySQL 유지 | Ver7 |
| AI Agent (Tool Use) | Claude API (claude-haiku, multi-turn tool_use 루프) | Ver8 |
| MCP 서버 | Python + `mcp` SDK (stdio transport) | Ver8 |

---

## 실행 방법

### 사전 요구사항

- Java 17
- Node.js 18+
- Python 3.10+ (MCP 서버, 선택)
- MySQL 8.x
- Finnhub API 키 ([무료 가입](https://finnhub.io/))
- 한국투자증권 API 키 (KR 종목 시세, 선택)
- 네이버 개발자센터 API 키 (KR 뉴스 수집, [애플리케이션 등록](https://developers.naver.com/apps/#/register))
- OpenAI API 키 (뉴스 임베딩, [발급](https://platform.openai.com/api-keys))

### 1. 데이터베이스

```bash
docker-compose up mysql -d
```

또는 로컬 MySQL에서 `dev_db` 스키마를 생성합니다.

### 2. 백엔드

```bash
cp backend/.env.example backend/.env
# .env에 환경변수 값 입력
cd backend
./gradlew bootRun
```

### 3. 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:3000` 접속 → 회원가입 → 로그인

### 4. MCP 서버 (선택)

백엔드에 회원가입한 계정 하나를 "MCP용 데모 계정"으로 정해두고 진행합니다.

```bash
cd mcp-server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

cp .env.example .env
# .env에 BACKEND_URL, MCP_USERNAME, MCP_PASSWORD 입력

python auth_client.py   # 로그인 확인용 — "로그인 성공. JWT: ..." 출력되면 정상
```

Claude Desktop에 연결하려면 `claude_desktop_config.json`(macOS: `~/Library/Application Support/Claude/`)에
아래 항목을 추가하고 앱을 재시작합니다.

```json
{
  "mcpServers": {
    "marketdata": {
      "command": "/절대경로/mcp-server/.venv/bin/python",
      "args": ["server.py"],
      "cwd": "/절대경로/mcp-server"
    }
  }
}
```

Claude Desktop 없이 도구만 검증하려면 [MCP Inspector](https://github.com/modelcontextprotocol/inspector)를 씁니다.

```bash
npx @modelcontextprotocol/inspector --cli .venv/bin/python server.py --method tools/list
```

---

## 환경변수

`backend/.env.example`을 복사해 `backend/.env`를 만들고 아래 값을 채워주세요.

```env
# Finnhub (미국 주식 실시간 시세)
FINNHUB_API_KEY=your_finnhub_api_key_here

# 한국투자증권 (국내 주식 실시간 시세, 선택)
KIS_APP_KEY=your_kis_app_key_here
KIS_APP_SECRET=your_kis_app_secret_here

# JWT 서명 키 (32자 이상 랜덤 문자열)
# 생성: openssl rand -base64 48
JWT_SECRET=your-very-long-random-secret-key-at-least-32-chars

# Anthropic Claude API 키 (Ver5 AI 브리핑)
# https://console.anthropic.com/ 에서 발급
ANTHROPIC_API_KEY=sk-ant-your-api-key-here

# 네이버 뉴스 검색 API (Ver6 RAG — KR 종목 뉴스 수집)
# https://developers.naver.com/apps/#/register 에서 애플리케이션 등록 후 발급
NAVER_CLIENT_ID=your_naver_client_id_here
NAVER_CLIENT_SECRET=your_naver_client_secret_here

# OpenAI Embeddings API 키 (Ver6 RAG — 뉴스 임베딩)
# https://platform.openai.com/api-keys 에서 발급
OPENAI_API_KEY=sk-your-openai-api-key-here
```

> **Finnhub 무료 티어 주의사항**  
> 미국 거래소 종목만 실시간 지원합니다.  
> 한국 시간 기준 **밤 10시 ~ 새벽 5시** 에만 실제 데이터가 흐릅니다.

> **한국투자증권 주의사항**  
> 국내 장 운영시간 **오전 9시 ~ 오후 3시 30분** 에만 데이터가 흐릅니다.

---

## 프로젝트 구조

```
marketdata/
├── backend/
│   ├── src/main/java/com/usang/marketdata/
│   │   ├── global/config/        # WebSocket, Security 설정
│   │   ├── domain/               # 엔티티 (User, Watchlist, PriceAlert, Candle, NewsArticle)
│   │   ├── application/          # 비즈니스 로직 (캔들 집계, 알림 체커, 브로드캐스트, 뉴스 수집/검색, AI Agent)
│   │   ├── infra/                # 외부 연동 (Finnhub, KIS WebSocket, JWT, 네이버 뉴스, OpenAI 임베딩, Claude tool-use)
│   │   └── api/                  # REST Controller, WebSocket Handler
│   └── build.gradle
├── frontend/
│   ├── app/                      # Next.js 페이지 (로그인, /watchlist 목록·등록·상세, /agent 채팅)
│   ├── components/               # CandleChart, AlertForm, KrStockSearchInput, SurgeBriefingPanel
│   ├── hooks/                    # useStockWebSocket, useWatchlist, useAlerts, useAuthGuard, useAgentChat
│   └── lib/                      # auth (JWT), format
├── mcp-server/                   # 독립 Python MCP 서버 (agent 도구를 Claude Desktop 등 외부에 노출)
│   ├── auth_client.py            # 로그인 + JWT 캐시
│   ├── tools.py                  # MCP 도구 3종 정의
│   └── server.py                 # stdio transport 진입점
└── docker-compose.yml
```

---

## 주요 아키텍처

```
[미국 시세]  Finnhub WebSocket
                    │
[국내 시세]  KIS WebSocket ──→ StockBroadcastService (@Async)
                                      ├─→ CandleAggregator (1분/5분/일봉)
                                      ├─→ LatestPriceStore (메모리 캐시)
                                      ├─→ StockWebSocketHandler.sendToWatchers ──→ 관심종목 소유자 브라우저만
                                      └─→ SurgeDetector
                                               ├─→ SURGE 메시지 (관심종목 소유자에게만)
                                               └─→ AiBriefingService (@Async)
                                                       ├─→ US: Finnhub 뉴스 조회
                                                       ├─→ KR: NewsRetrievalService 유사도 검색 (RAG, pgvector)
                                                       ├─→ Claude API 호출
                                                       └─→ AI_BRIEFING 메시지 (관심종목 소유자에게만)

[Ver6 RAG 배치 파이프라인, Ver7에서 pgvector로 마이그레이션] (관심종목 KR 뉴스, 30분 주기)
NewsCollectionScheduler (@Scheduled)
        └─→ NaverNewsClient (뉴스 검색) ──→ OpenAiEmbeddingClient (임베딩)
                                                    └─→ NewsArticle 저장 (pgvector/Postgres, vector 타입)
                                                              ↑
                          NewsRetrievalService가 pgvector `<=>` 코사인 거리 연산자로 DB에서 직접 조회
```

- 가격 알림: `PriceAlertChecker` (@Scheduled, 10초마다) → `LatestPriceStore` 조회 → 조건 충족 시
  텔레그램 발송 후 알림 즉시 삭제(이력 보관 안 함)
- 인증: JWT 토큰을 `Authorization: Bearer` 헤더로 전달, `JwtAuthenticationFilter`에서 검증
  (세션 만료 30분, 프론트 `useAuthGuard`가 만료 시점에 자동 로그아웃)
- AI 브리핑: 전일 종가 대비 5% 변동 감지 → SURGE 전송 → Claude API 비동기 호출 → AI_BRIEFING 후속 전송.
  상세 페이지에서 온디맨드로도 요청 가능(`POST /api/ai-briefing/{symbol}`)
- RAG: 관심종목(KR) 뉴스를 미리 수집·임베딩해 pgvector(Postgres)에 저장해두고, 급등 감지 시
  종목명 기반 쿼리를 임베딩해 `<=>` 코사인 거리 연산자로 DB에서 직접 유사도 검색 (Ver6의
  애플리케이션 레벨 브루트포스 계산에서 Ver7에 실제 벡터 DB로 전환)
- WebSocket 인증/필터링(Ver7): 연결 시 쿼리파라미터로 받은 JWT를 `JwtHandshakeInterceptor`가
  검증해 세션에 userId 부여(토큰 없어도 연결은 허용, 단 아무 메시지도 받지 못함).
  `StockWebSocketHandler.sendToWatchers`가 시세/SURGE/AI_BRIEFING 모두 해당 종목을 관심종목에
  등록한 사용자에게만 전송 (가격 알림은 Ver7에서 텔레그램 전용으로 바뀌어 WS를 타지 않음)
- AI Agent(Ver8): `POST /api/agent/chat` → `AgentChatService`가 Claude에 도구 3종
  (`get_watchlist`/`get_recent_news`/`get_candle_stats`)을 알려주고, Claude가 스스로 호출을
  요청하면(`tool_use`) `AgentToolService`를 실행해 결과를 다시 넣어주는 멀티턴 루프
  (`MAX_ITERATIONS=5`). 대화 이력은 DB에 저장하지 않고 프론트가 들고 재전송
- MCP 서버(Ver8): 독립 Python 프로세스(`mcp-server/`)가 `AgentToolService`와 동일한 도구를
  `AgentToolsController`(REST) 경유로 노출 — Claude Desktop 등 외부 MCP 클라이언트가 백엔드
  데이터를 직접 조회 가능. 내부 Agent 루프는 REST를 거치지 않고 `AgentToolService`를 직접 호출

---

## 버전 히스토리

### Ver1 — 기본 실시간 시세 `v1`
- Finnhub WebSocket 연결 (AAPL, TSLA, MSFT, AMZN 하드코딩)
- 백엔드 → 프론트엔드 WebSocket 브로드캐스트
- 모바일 WTS 스타일 레이아웃

### Ver2 — 관심종목 & 차트 `v2`
- MySQL 도입, 사용자별 관심종목 저장 (로그인 없이 임시 ID 기반)
- 동적 구독 관리
- 틱 데이터 집계 → 1분봉 캔들 생성 및 DB 저장
- TradingView Lightweight Charts 연동

### Ver3 — 알림 & 국내 시세 `v3`
- 프론트엔드 WebSocket 자동 재연결 (지수 백오프)
- 가격 알림: 목표가 설정 → 백엔드 감지 → 브라우저 Notification
- 봉 타임프레임 확장: 5분봉 + 일봉
- 한국투자증권 WebSocket API 연동 (국내 주식 실시간 시세)
- 국내 종목 이름 검색 (KRX 종목 JSON 기반)

### Ver4 — 인증 `v4`
- Spring Security + JWT 회원가입/로그인
- 모든 API 인증 필수 (`/api/auth/**`, `/ws/**` 제외)
- `@Async` 브로드캐스트로 KIS IO 스레드 분리
- KIS WebSocket 재연결 로직 (5s/30s 백오프)

### Ver5 — AI 브리핑 `v5`
- 전일 종가 대비 5% 이상 급등/급락 감지 (`SurgeDetector`)
  - US: Finnhub REST API `pc`(previous close) 필드
  - KR: KIS REST API `stck_prdy_clpr`(전일 종가) 필드
- Finnhub 뉴스 API로 최근 헤드라인 조회 (US 종목) → Claude 프롬프트에 주입
- Claude API (claude-haiku, `@Async`) 호출 → 한 줄 브리핑 생성
  - temperature 0.3, max_tokens 150, 1회 retry, 실패 시 fallback
  - 10분 쿨다운으로 동일 종목 과호출 방지
- SURGE 메시지(즉시) + AI_BRIEFING 메시지(1~3초 후) 두 단계 WebSocket 푸시
- 브라우저: 급등(빨간)/급락(파란) 배너 → AI 분석 텍스트로 교체 → 30초 후 자동 제거

### Ver6 — RAG 기반 KR 종목 뉴스 브리핑 `v6`
- Ver5의 한계 보완: Finnhub이 KR 종목 뉴스를 지원하지 않아 국내 종목 브리핑이 일반적인 시장 맥락에만 의존하던 문제를 RAG로 해결
- `NewsCollectionScheduler` (@Scheduled, 30분마다): 관심종목(KR)의 회사명으로 네이버 뉴스 검색 → URL 중복 스킵 → 신규 기사만 저장
- `OpenAiEmbeddingClient`: OpenAI `text-embedding-3-small`로 기사 임베딩 생성, `float[]` ↔ JSON 문자열 직렬화
- `NewsArticle`: 벡터 DB 없이 MySQL TEXT 컬럼에 임베딩을 JSON으로 저장 (이 프로젝트 스케일에서는 브루트포스 코사인 유사도로 충분하다고 판단)
- `NewsRetrievalService`: 급등/급락 감지 시 "{종목명} 주가 급등/급락 이유" 쿼리를 임베딩해 저장된 코퍼스와 코사인 유사도 계산 → 상위 3개 기사를 Claude 프롬프트에 주입
- `AiBriefingService`의 KR 분기를 RAG 검색으로 교체 (US 분기의 Finnhub 뉴스 조회는 그대로 유지)

### Ver7 — 페이지 리팩토링 + 세션 보안 + RAG 벡터 DB화
- 애초 "프로덕션 하드닝(배포)"으로 계획했으나, 관심종목 화면 하나에 목록·차트·알림·AI브리핑이 다
  뭉쳐있어 UX/코드 구조 정리가 더 급하다고 판단해 범위를 바꿔 진행 — 배포 준비는 Ver8로 이동,
  자세한 내용은 [ROADMAP.md](./ROADMAP.md) 참고
- 착수 전 먼저 고친 버그: IDOR(알림 삭제 소유권 체크 누락), WebSocket `ALERT` 전체 브로드캐스트
  유출(이후 텔레그램 전환으로 이 경로 자체가 제거됨), `symbol` 입력 검증 누락, KIS 토큰 재기동마다
  재발급되던 문제(파일 캐싱)
- 관심종목 `/watchlist`(목록)·`/watchlist/register`(등록)·`/watchlist/[symbol]`(상세, 차트+뉴스+
  온디맨드 AI분석) 3개 페이지로 분리, Apple 디자인 시스템 적용
- 가격 알림을 텔레그램 전용으로 전환 (브라우저 Notification/WebSocket `ALERT` 완전 제거),
  트리거 시 이력 없이 즉시 삭제하도록 단순화
- JWT 세션 만료 24시간 → 30분, 프론트 `useAuthGuard` 훅으로 만료 시점 자동 로그아웃
- RAG 뉴스 코퍼스를 MySQL 브루트포스 코사인 계산에서 **pgvector(Postgres)**로 마이그레이션 —
  `<=>` 코사인 거리 연산자로 DB에서 직접 유사도 검색, `NewsArticle`을 JPA 엔티티에서 JdbcTemplate
  기반 POJO로 전환
- `CandleAggregator` 타임존 버그(서버 로컬 타임존 vs UTC 해석 불일치) 및 flush 레이스컨디션
  (원자적 맵 교체로 수정)
- WebSocket 시세/급등/AI브리핑을 관심종목 소유자에게만 전송하도록 `JwtHandshakeInterceptor` +
  `sendToWatchers`로 필터링 (기존엔 전체 세션에 브로드캐스트해 다른 사용자 관심종목 정보가
  새고 있었음)
- Kafka/Redis 필요성 분석 문서화 (구현 없이 [ROADMAP.md](./ROADMAP.md)에 근거 기반 분석만 정리)

### Ver8 — AI PB 대화형 Agent + MCP 서버

지원 중인 회사의 "AI Agent 및 업무 자동화 시스템 구축" 요구에 맞춰, 기존 AI브리핑(단발성 LLM 호출)과
구분되는 진짜 tool-use 기반 Agent와 MCP를 추가했다.

**Feature 1 — 대화형 AI PB Agent**
- `infra/anthropic/AnthropicClient.java` 신설 — `tools[]` 포함 멀티턴 `/v1/messages` 호출 (SDK
  미사용, 기존 `AiBriefingService`와 같은 `RestClient` 패턴). 기존 단발성 브리핑 흐름은 그대로 둠
- `AgentToolService`에 도구 3종: `get_watchlist`(관심종목+현재가+전일종가+등락률),
  `get_recent_news`(US: Finnhub 헤드라인 / KR: RAG 유사도 검색), `get_candle_stats`(캔들 요약)
  — 전부 기존 서비스(`WatchlistService`, `NewsRetrievalService`, `SurgeDetector`, `LatestPriceStore`,
  `CandleRepository`) 재사용
- `AgentChatService`가 tool_use ↔ tool_result 왕복 루프를 돌며 `MAX_ITERATIONS=5`로 상한선 설정,
  초과 시 fallback 메시지. `userId`는 도구의 JSON 스키마에 포함하지 않아 Claude가 다른 사용자
  데이터를 요청할 구조적 방법이 없음
- 프론트 `/agent` 페이지: 대화형 채팅 UI, 어떤 도구가 호출됐는지 보여주는 칩(`🔧 get_watchlist`),
  `react-markdown`+`remark-gfm`으로 답변의 표/볼드 렌더링. 대화 이력은 DB 저장 없이 프론트가 들고
  재전송(stateless)
- 실제 curl 검증에서 "AAPL 지금 사도 될까?" 같은 질문에 `get_candle_stats`+`get_recent_news`를
  Claude가 스스로 함께 호출하는 멀티스텝 동작 확인

**Feature 2 — MCP 서버**
- `AgentToolsController` 신설 — Feature 1의 `AgentToolService`를 감싸는 REST 엔드포인트
  (`GET /api/agent-tools/watchlist`, `/news/{symbol}`, `/candle-stats/{symbol}`). 내부 Agent
  루프는 이 컨트롤러를 거치지 않고 같은 JVM 안에서 `AgentToolService`를 직접 호출, 외부 MCP
  서버만 HTTP로 호출 — 로직은 하나, 호출자만 둘
- 독립 Python 프로젝트 `mcp-server/` — 안트로픽 공식 `mcp` SDK(`FastMCP`)로 동일 도구 3종을
  MCP tool로 재정의, 내부적으로 `AgentToolsController`를 JWT 인증해 호출. Node.js 대신 Python을
  선택한 이유는 안트로픽 공식 Python SDK가 TypeScript SDK와 동급이고, 실제 AI/에이전트 툴링
  생태계가 Python 위주라서
- `auth_client.py`가 데모 계정으로 로그인해 JWT를 메모리에 캐시, 401 시 재로그인
- Claude Desktop이 설치되지 않은 환경이라 실제 연동은 생략하고, 공식
  [MCP Inspector](https://github.com/modelcontextprotocol/inspector) CLI로 도구 목록/호출을
  검증 — Claude Desktop 연결 방법은 위 "실행 방법" 섹션에 문서화해둠
- *(스코프 아웃)* `get_price_alerts` 도구는 여유분으로 남겨두고 미구현 — 이미 확보한 도구 3종만으로
  멀티스텝 tool-use와 MCP 연동 모두 충분히 증명됨

**사용 예시**
```
사용자: 내 관심종목 알려줘
🔧 get_watchlist
AI: 현재 등록된 관심종목은 1개입니다: AAPL(Apple), 전일종가 $314.86 ...

사용자: AAPL 지금 사도 될까?
🔧 get_candle_stats  🔧 get_recent_news
AI: 최근 AI 관련 호재(Apple Intelligence 중국 진출 승인)로 상승 모멘텀이 있으나,
    투자 기간과 리스크 성향을 고려해 판단하시길 권장합니다 ...
```

자세한 단계별 진행 내역은 [ROADMAP.md](./ROADMAP.md)의 Ver8 항목 참고.

---

## 커밋 컨벤션

```
[Ver1] 기능 설명
[Ver2] 기능 설명
...
```

---

## 라이선스

MIT
