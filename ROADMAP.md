# ROADMAP

## Ver1 — 기본 실시간 시세 ✅
- Finnhub WebSocket으로 미국 주식 실시간 trade 수신 (백엔드 단일 커넥션)
- 백엔드 → 프론트엔드 WebSocket 브로드캐스트
- 고정 종목 (AAPL, TSLA, MSFT, AMZN) 하드코딩 표시
- 모바일 WTS 스타일 레이아웃

---

## Ver2 — 관심종목 & 차트 ✅
- MySQL 도입, 사용자(임시 ID 기반, 로그인 없음)별 관심종목 저장
- 관심종목만 구독하도록 동적 구독 관리
- 틱 데이터를 1분 단위로 집계해 1분봉 캔들 생성 및 DB 저장
- TradingView Lightweight Charts 연동

---

## Ver3 — 품질 & 알림
- 프론트엔드 WebSocket 자동 재연결 (백오프 로직)
- 가격 알림: 목표가 설정 → 백엔드 감지 → 브라우저 Notification API
- 봉 타임프레임 확장: 5분봉 + 일봉 추가, 차트에서 전환 버튼 제공
- **이 단계에서 하지 않는 것**: 로그인/보안, Kafka, 한국투자증권 API

> 한국투자증권 API는 계좌/키 발급 후 별도로 추가 예정 (Ver3 이후 어느 시점에도 가능)

---

## Ver4 — 인증 ✅
- Spring Security + JWT 자체 로그인 (회원가입/로그인, 토큰 기반 인증)
- 모든 API 인증 필수, 프론트엔드 로그인/로그아웃 페이지
- KIS WebSocket 재연결 로직, @Async 브로드캐스트 안정화

---

## Ver5 — AI 브리핑 ✅
- 종목 5% 이상 급등/급락 감지 (5분 전 기준가 대비)
- Finnhub 뉴스 API로 최근 헤드라인 조회 (US 종목)
- Claude API 호출 → "왜 오르고/내리는지" 한 줄 브리핑 생성
- 브라우저에 SURGE 배너 즉시 표시 → AI 분석 결과 후속 표시

---

## Ver6 — RAG 기반 KR 종목 뉴스 브리핑 ✅
- **배경**: Ver5의 AI 브리핑은 Finnhub 뉴스 API가 KR 종목을 지원하지 않아, 국내 종목은 뉴스 없이 일반적인 시장 맥락으로만 브리핑을 생성하던 한계가 있었음
- 네이버 뉴스 검색 API로 관심종목(KR) 뉴스를 `@Scheduled` 배치로 수집 (30분 주기, 종목당 1회, URL 중복 스킵)
- OpenAI `text-embedding-3-small`로 기사 임베딩 생성 → 별도 벡터 DB 없이 MySQL TEXT 컬럼에 JSON 직렬화해 저장
- 급등/급락 감지 시 종목명 기반 쿼리("{종목명} 주가 급등/급락 이유")를 임베딩해 저장된 코퍼스와 코사인 유사도 계산 → 상위 3개 기사를 Claude 프롬프트에 주입
- 벡터 DB(pgvector/Pinecone 등)는 도입하지 않음 — 이 프로젝트 스케일(종목당 수십 건)에서는 애플리케이션 레벨 브루트포스 계산으로 충분하다고 판단, RAG 개념 자체 학습에 집중

---

## Ver7 — 페이지 리팩토링 + 세션 보안 + RAG 벡터 DB화 ✅
- **배경**: 애초 "프로덕션 하드닝"(Dockerfile, prod 설정, AWS 배포)으로 계획했으나, 착수 전
  코드베이스를 감사하다 보니 관심종목 화면 하나에 목록·차트·알림·AI브리핑이 다 뭉쳐있어 UX/코드
  구조 정리가 더 급하다고 판단해 범위를 바꿈. 프로덕션 하드닝/배포는 Ver9로 이동(원래 Ver8 예정이었으나
  Ver8에서 AI Agent + MCP를 먼저 진행하며 한 번 더 미룸).

- **즉시 버그 수정** ✅ (Ver7 착수 전 먼저 처리한 버그 3건)
  - IDOR: `PriceAlertController.deleteAlert`에 소유권 체크가 없어 누구나 남의 알림을 삭제 가능하던 문제
  - WebSocket이 `ALERT` 메시지(다른 사용자의 목표가 포함)를 인증 없이 전체 브로드캐스트하던 문제
    → `JwtHandshakeInterceptor` + `sendToUser`로 수정. 이후 Step1에서 가격 알림 자체가 텔레그램
    전용으로 바뀌며 이 경로는 통째로 제거됐다가, Step 마지막에 다른 목적(시세/급등/AI브리핑
    필터링)으로 같은 메커니즘이 다시 도입됨 — 아래 "그 외" 참고
  - `symbol` 값이 검증 없이 외부 API URL에 그대로 삽입되던 인젝션 여지 → 관심종목/알림 등록
    시점에 형식 검증 추가
  - KIS 접근토큰(`KisAccessTokenProvider`)과 approval key(`KisApprovalKeyProvider`)가
    앱을 재기동할 때마다 재발급되던 문제 → 로컬 파일에 캐싱해 재기동 간 재사용 (개발 중 반복
    재시작으로 KIS 발급 API를 과호출해 계정 제한 경고를 받은 것이 계기)

- **Step1: 페이지 분리 + 텔레그램 알림** ✅
  - `/watchlist`(목록)와 `/watchlist/register`(등록) 페이지 분리, Apple 디자인 시스템
    (`DESIGN-apple.md`) 적용
  - 알림 설정 버튼을 등록 페이지에서 목록 페이지로 이동
  - 가격 알림을 텔레그램 전용으로 전환 — 기존 브라우저 Notification/WebSocket `ALERT` 경로 완전 제거
  - KR 알림 메시지는 종목코드 대신 종목명 표시, 소수점 제거, 모든 금액 표기에 콤마 포맷 적용

- **Step2: 종목 상세 페이지** ✅
  - `/watchlist/[symbol]` 신설 — 차트 + 관련 뉴스 목록 + 온디맨드 AI 분석
  - `NewsController`(`GET /api/news/{symbol}`), `AiBriefingController`(`POST /api/ai-briefing/{symbol}`) 신설
  - Finnhub 뉴스 조회 로직을 `AiBriefingService`에서 `FinnhubNewsClient`로 분리해 `NewsController`와 공용화

- **Step3: 세션 보안 + pgvector + 차트 버그** ✅
  - JWT 세션 만료 24시간 → 30분
  - `useAuthGuard` 훅 신설 — 미인증 시 즉시 리다이렉트, JWT `exp` 디코딩해 만료 시점에 자동 로그아웃
  - RAG 뉴스 코퍼스(`news_article`)를 MySQL 브루트포스 코사인 계산에서 pgvector(Postgres)로
    마이그레이션 — 유사도 검색을 `<=>` 연산자로 DB에서 직접 수행, `NewsArticle`은 JPA 엔티티에서
    JdbcTemplate 기반 POJO로 전환
  - `CandleAggregator` 타임존 버그(서버 로컬 타임존을 UTC로 잘못 해석하던 문제) 및 flush
    레이스컨디션(forEach 중 들어온 틱 유실 가능성 → 원자적 맵 교체로 수정) 해결

- **Step4: Kafka/Redis 필요성 분석** ✅ — 실제 구현 없이 문서화만. 다중 인스턴스 배포 시 실제로
  깨지는 지점과 필요한 기술 매핑, 아래 "미래 검토(확장성)" 섹션 참고

- **그 외** ✅
  - 가격 알림 트리거 시 즉시 삭제로 단순화 (`triggered` 필드/이력 제거, 10초 폴링과 맞물려 이력을
    남길 필요가 없다고 판단)
  - WebSocket 시세/급등/AI브리핑을 관심종목 소유자에게만 전송하도록 필터링 —
    `JwtHandshakeInterceptor`를 재도입해 세션에 userId 부여, `StockWebSocketHandler.sendToWatchers`로
    관심종목 소유자에게만 전송 (기존엔 전체 세션에 브로드캐스트해 다른 사용자 관심종목 정보가
    새고 있었음 — 위 ALERT 유출 버그와 동일한 구조)
  - 미사용 코드 정리 (프론트 export/prop, 백엔드 dead code 스윕)

- **Ver9로 이동한 범위** (예정) — 애초 Ver7로 계획했던 프로덕션 하드닝/배포, Ver8에서 한 번 더 미룸
  - 배포 최소요건: Dockerfile, 프론트 API/WS URL 환경변수화, `application-prod.yaml` 분리,
    Flyway 도입, Actuator 헬스체크 인증 예외, `RestClient` 타임아웃, `@Async` 전용 스레드풀,
    `spring-boot-starter-validation` 활성화
  - 기능 감사: Finnhub/KIS/AI브리핑/RAG의 구조적 제약(비용, 사용량 제한 등)을 유지·데모모드·제거
    중 무엇으로 할지 결정, 관심종목/알림 개수 제한
  - 스케일링 대응: 실제 다중 인스턴스 배포 계획이 생기면 Kafka/Redis 도입 (필요성 분석은 Step4로 완료)
  - 실제 AWS 배포: ECS/App Runner/EC2 중 선택, RDS 전환, Secrets Manager 연동, CI/CD 파이프라인

---

## Ver8 — AI PB 대화형 Agent + MCP 서버 ✅

- **배경**: 기존 AI브리핑(`AiBriefingService`)은 급등 감지 시 Claude에 프롬프트를 1회 호출하는
  단발성 흐름이지, 여러 단계에 걸쳐 스스로 도구를 선택·호출하는 "Agent"는 아니었다. RAG(뉴스 임베딩+
  pgvector 유사도 검색)는 이미 있지만 MCP는 전혀 없었다. 이 둘을 추가해 학습 범위를 넓혔고, 이후
  계획했던 프로덕션 하드닝/배포는 Ver9로 미뤘다 (Ver7이 이미 한 번 범위를 바꾼 선례가 있음).

### Feature 1: 대화형 AI PB Agent (Tool Use) ✅
로그인 사용자가 "내 관심종목 어때?" 같은 질문을 하면, Claude가 도구(관심종목 조회, 최근 뉴스 검색,
캔들 통계)를 스스로 선택·호출하며 답을 구성하는 멀티턴 루프.
- **Step1~3** ✅: `infra/anthropic/AnthropicClient.java` (tools 포함 멀티턴 `/v1/messages` 호출,
  SDK 미사용 — 기존 `AiBriefingService`와 같은 `RestClient` 패턴), `AgentToolService.getWatchlist`,
  `AgentChatService`(루프, `MAX_ITERATIONS=5`) + `AgentChatController`(`POST /api/agent/chat`)
- **Step4~5** ✅: `get_recent_news`(US: Finnhub 헤드라인 / KR: RAG 유사도 검색), `get_candle_stats`
  (캔들 요약) 도구 추가 — 전부 기존 서비스(`NewsRetrievalService`, `SurgeDetector`,
  `LatestPriceStore`, `CandleRepository`) 재사용, 새 로직 없음
- **Step6** ✅: "AAPL 지금 사도 될까?" 질문에 Claude가 `get_candle_stats`+`get_recent_news`를 같은
  턴에 함께 호출하는 멀티스텝 동작 실증. `MAX_ITERATIONS=1`로 낮춰 반복 상한 fallback 경로도 확인
  후 5로 복원, 도구 호출 로그(`AgentChatService`) 추가
- **Step7 (여유분)** — 미구현: `get_price_alerts`. 이미 확보한 도구 3종으로 멀티스텝 tool-use 증명은
  충분해 시간 대비 임팩트가 낮다고 판단해 스코프 아웃
- **Step8** ✅: 프론트 `/agent` 페이지 — 대화형 채팅 UI, 호출된 도구를 보여주는 칩(`🔧 get_watchlist`),
  `react-markdown`+`remark-gfm`으로 답변 렌더링. 대화 이력은 DB 저장 없이 프론트가 들고 재전송
- 도구 실행 시 `userId`는 메서드 파라미터로만 전달하고 도구의 JSON 스키마에는 포함하지 않음 —
  Claude가 다른 사용자의 데이터를 요청할 구조적 방법 자체를 차단

### Feature 2: MCP 서버 ✅
Feature 1과 동일한 도구셋을 MCP로 노출해 Claude Desktop 등 외부 MCP 클라이언트에서도 호출 가능하게 함.
- **아키텍처**: 독립 Python MCP 서버(안트로픽 공식 `mcp` SDK, `FastMCP`)가 백엔드 REST API를
  호출하는 방식 채택 — Spring AI MCP 통합은 Spring Boot 4.0.5와의 호환성 리스크로 배제. 최초
  계획은 Node.js였으나, Python 공식 SDK가 TypeScript SDK와 동급이고 실제 AI/에이전트 툴링
  생태계가 Python 위주라 이 프로젝트 목적에 더 적합하다고 판단해 착수 직전 변경
- **Step9** ✅: `api/agent/AgentToolsController.java` — `AgentToolService`를 감싸는 REST 엔드포인트
  (`GET /api/agent-tools/watchlist`, `/news/{symbol}`, `/candle-stats/{symbol}`). 내부 Agent
  루프는 이 컨트롤러를 거치지 않고 같은 JVM 안에서 `AgentToolService`를 직접 호출, 외부 MCP
  서버만 HTTP로 호출 — 로직은 하나, 호출자만 둘
- **Step10** ✅: `mcp-server/` 스캐폴딩 — `auth_client.py`(데모 계정으로 로그인, JWT 메모리 캐시,
  401 시 재로그인). 별도 토큰 발급 시스템 없이 기존 로그인(JWT) 그대로 재사용
- **Step11** ✅: `tools.py`에 도구 3종을 `@mcp.tool()`로 정의(함수 시그니처+docstring에서 JSON
  Schema 자동 생성), `server.py`가 stdio transport로 기동. `mcp` SDK의 `ClientSession`으로 직접
  작성한 스크립트로 도구 목록/호출을 1차 검증
- **Step12**: Claude Desktop이 개발 환경에 설치되어 있지 않아 실제 연동은 생략. 대신 공식
  [MCP Inspector](https://github.com/modelcontextprotocol/inspector) CLI(`npx
  @modelcontextprotocol/inspector --cli`)로 도구 목록/호출을 2차 검증(안트로픽 공식 도구 기준
  검증이라는 의미가 있음). Claude Desktop 연결 방법은 README.md "실행 방법"에 문서화해둠 —
  나중에 앱을 설치하면 바로 연결 가능
- **Step13** ✅: README.md에 Ver8 변경 이력, 사용 예시, MCP 서버 실행/Claude Desktop 연동 방법 문서화

### 배운 점 / 메모
- FastMCP(Python SDK)는 함수 시그니처와 docstring만으로 도구의 JSON Schema를 자동 생성해준다 —
  백엔드 Java 쪽에서 `AgentChatService.buildToolDefinitions()`로 손수 `Map`을 만든 것과 대비됨
- 같은 도구 로직(`AgentToolService`)을 두 가지 호출 경로(내부 직접 호출 / 외부 REST)로 노출하는
  구조가 "하나의 도구 정의, 여러 클라이언트"라는 MCP의 취지를 코드 레벨에서도 잘 보여줌

---

## 미래 검토 (확장성): Kafka/Redis 필요성 분석

**결론**: 지금(단일 인스턴스)은 필요 없음. 아래 문제는 전부 "여러 인스턴스가 동시에 떠 있는"
시나리오에서만 발생한다. 실제 다중 인스턴스 배포 계획이 생기는 시점(Ver9 스케일링 단계)에 도입.

### 다중 인스턴스로 확장하면 실제로 깨지는 것들

1. **캔들 저장이 깨짐 (가장 심각)** — `CandleAggregator`
   각 인스턴스는 자신이 받은 틱만으로 캔들을 부분적으로 만든다. N개 인스턴스가 동시에
   `flushOneMin`/`flushFiveMin`/`flushDaily`를 실행하면 같은 `(symbol, interval_type, open_time)`에
   대해 서로 다른(불완전한) OHLCV를 각각 저장하려 시도 — unique 제약 위반 에러가 나거나 조용히
   틀린 데이터가 남는다.
   → **Kafka**로 틱 수집을 한 곳(컨슈머 그룹)에 모아 캔들 집계를 인스턴스 하나가 전담해야 함.
   Redis만으로는 안 풀림 — 틱 자체가 인스턴스마다 분산돼 있어 "부분 데이터" 문제가 그대로 남음.

2. **가격 알림 중복 발송** — `PriceAlertChecker`
   N개 인스턴스가 10초마다 동시에 같은 알림 row를 읽고 조건 충족을 판단한다. 삭제 커밋 전에
   서로 겹치면 텔레그램이 중복 발송될 수 있다.
   → **Redis 분산 락**(또는 DB `SELECT ... FOR UPDATE`)으로 인스턴스 하나만 처리하게 함.

3. **뉴스 수집/임베딩 비용 중복** — `NewsCollectionScheduler`
   N개 인스턴스가 30분마다 각각 네이버 뉴스 API(일 2.5만 건 쿼터를 공유 소진)와 OpenAI 임베딩
   (실제 과금)을 중복 호출한다. `existsByUrl` 체크도 저장 커밋 전 레이스가 있어 중복 임베딩
   비용이 발생할 수 있다.
   → **Redis 분산 락**으로 스케줄러를 인스턴스 하나만 실행.

4. **KIS 토큰 재발급 경합** — `KisAccessTokenProvider`
   KIS 토큰 발급 API는 분당 1회 제한이 걸려있다. N개 인스턴스가 거의 동시에 갱신을 시도하면
   제한에 걸려 계정 경고를 받을 수 있다. 로컬 파일 캐시도 인스턴스마다 따로라 공유되지 않는다.
   → **Redis**에 토큰 값 자체를 공유 저장하고 갱신 시 분산 락 사용.

5. **급등 감지 중복 발화 + AI 브리핑 중복 과금** — `SurgeDetector`
   `baselinePrices`/`cooldownUntil`이 인스턴스별 로컬 메모리라, 한 인스턴스의 쿨다운이 끝난
   시점에 다른 인스턴스가 이미 같은 종목으로 발화했어도 알 방법이 없어 또 발화한다 — 사용자에게
   중복 알림이 가고, Claude API도 중복 호출(실비용)된다.
   → **Redis**에 기준가/쿨다운을 공유 캐시로 저장(쿨다운은 `SETNX`+TTL로 구현).

6. **KIS/Finnhub 업스트림 연결 자체가 N배로 중복 — 근본 원인**
   `KisClientConfig`/`FinnhubClientConfig`가 인스턴스마다 독립적으로 업스트림에 접속해서 완전히
   동일한 시세를 N번 중복 수신한다. 1번과 5번 문제의 근본 원인이기도 하다.
   → **Kafka**: 업스트림 연결을 전담하는 별도 ingest 서비스를 하나만 두고, 나머지 인스턴스는
   Kafka에서 틱을 구독하는 구조로 바꾸면 근본적으로 해결됨.

### 예상과 달리 실제로는 문제 없는 부분

**프론트로 보내는 WebSocket 브로드캐스트 자체는 인스턴스 간 전파가 필요 없다.** 각 인스턴스가
(6번 문제 때문에) 이미 독립적으로 전체 시세를 다 받고 있어서, 자기한테 붙은 브라우저 세션에는
자기가 알아서 다 만들어 보낼 수 있다. "인스턴스 A에서 만든 메시지가 인스턴스 B에 붙은 세션까지
못 감"이라는 흔한 멀티 인스턴스 문제가 이 앱 구조에서는 실제로 발생하지 않는다 — 대신 진짜
문제는 "업스트림 연결과 계산 자체가 N배로 낭비된다"는 쪽이다. (Redis pub/sub로 WS를 팬아웃하는
건 6번을 Kafka로 먼저 풀고 나서, 그 ingest 서비스에서 각 인스턴스로 틱을 릴레이할 때나 필요해짐)

### 이미 준비된 스캐폴딩
- `build.gradle:27-28`(runtime), `:38-39`(test) — `spring-boot-starter-data-redis`/`kafka` 의존성은
  이미 있음, 미사용
- `application.yaml:46-49` — `spring.autoconfigure.exclude`로 Redis/Kafka 자동설정 비활성화 중
- `application.yaml:68-78` — `spring.kafka` 설정("Ver4에서 활성화" 표시), `:80-84` —
  `spring.data.redis` 설정("Ver3에서 활성화" 표시)이 주석으로 이미 준비돼 있음
- `docker-compose.yml:6-14`(redis), `:15-33`(kafka) — 서비스 정의는 이미 있음, 앱에서 아직 안 씀

### 결론
다중 인스턴스 배포 계획이 실제로 생기면 위 순서(Kafka로 업스트림 일원화 → 나머지는 Redis 분산
락/캐시)로 도입. 지금은 인스턴스가 1개라 위 문제들이 전혀 발생하지 않으므로, "필요하지 않은 걸
미리 만들지 않는다"는 프로젝트 원칙에 따라 구현하지 않는다.

---

## 메모 (미래 버전에서 검토)
<!-- 구현 중 떠오른 아이디어를 여기에 기록 -->

### KR 종목 목록 자동 업데이트
- 현재: KRX 사이트에서 수동으로 CSV 다운로드 → JSON 변환 스크립트 실행
- 개선 시: KRX OTP 인증 플로우를 스크립트로 자동화하거나 FinanceDataReader 등 대안 라이브러리 활용
- 목표: GitHub Actions로 매 거래일 새벽에 kr-stocks.json 자동 갱신 (신규 상장/상장폐지 반영)
- 미룬 이유: KRX API가 세션 기반 인증을 요구해 단순 HTTP 요청으로는 우회 불가. 해결책 탐색 필요.


### STOMP 프로토콜 도입
- 현재: Ver7에서 백엔드 필터링 도입(`StockWebSocketHandler.sendToWatchers`) — 하지만 브로드캐스트마다 DB에서 관심종목 소유자를 조회하고 전체 WebSocket 세션을 순회하며 매칭하는 방식이라, 틱마다 O(세션 수) 스캔 + DB 쿼리가 반복됨
- 개선 시: STOMP의 topic 기반 구독(`/topic/price/AAPL`)으로 전환하면 브로커가 구독 정보를 미리 들고 있어 발행 시점에 매번 조회/스캔할 필요가 없음. 사용자·세션이 많아질수록 효과 커짐
- Spring: `@EnableWebSocketMessageBroker` + `SimpMessagingTemplate` / 프론트: `@stomp/stompjs` 라이브러리
- 미룬 이유: 현재 사용자·세션 수에서는 매 틱 DB 쿼리+세션 스캔 비용이 무의미. STOMP는 Ver4(확장성) 단계에서 Kafka, Redis와 함께 검토.

### 비동기 브로드캐스트용 스레드 풀 도입
- 현재: `@EnableAsync`만 설정돼 있고 커스텀 `TaskExecutor` 빈이 없어, 틱마다 호출되는 `StockBroadcastService.broadcast()`와 `AiBriefingService.generateAsync()`가 기본값인 `SimpleAsyncTaskExecutor`로 실행됨 — 호출마다 스레드를 새로 생성하고 상한이 없음
- 개선 시: `ThreadPoolTaskExecutor` 빈을 정의해 스레드 수 상한과 큐잉을 둠
- 미룬 이유: 지금 틱 볼륨에서는 체감 차이 없음. 사용자·구독 종목 수가 늘어 틱 빈도가 높아지면 스레드 폭증 위험이 커져서 그때 우선순위 높여야 함

### 프론트 시세 리렌더링 배칭
- 현재: `useStockWebSocket`이 틱을 받을 때마다 `setPrices`로 새 객체를 만들어 즉시 리렌더링 — 틱 1개당 리렌더링 1번
- 개선 시: 일정 간격(예: 200ms)으로 들어온 틱을 모아서 한 번에 반영하거나 `requestAnimationFrame` 기반으로 스로틀링
- 미룬 이유: 관심종목 수가 적은 지금은 리렌더링 비용이 무시할 수준. 관심종목·틱 빈도가 늘어나면 체감되는 시점에 도입
