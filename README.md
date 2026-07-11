# Market Data Streaming Platform

미국·국내 주식 실시간 시세를 모바일 WTS 스타일로 보여주는 학습용 프로젝트입니다.

> **학습 목적으로 단계별 기능을 추가하며 발전시킨 프로젝트입니다.**  
> Ver1 → Ver2 → Ver3 → Ver4 → Ver5 → Ver6 → Ver7 순서로 기능을 확장하고, 각 버전은 git tag로 구분합니다.

---

## 현재 버전: Ver7 (진행 중)

Ver1~6까지 개발 속도 위주로 기능을 쌓아온 뒤, 실제 AWS에 배포해 쓸 수 있는 수준으로 다듬는
프로덕션 하드닝 단계입니다. 자세한 진행 상황은 [ROADMAP.md](./ROADMAP.md)의 Ver7 항목을 참고하세요.

### 버전별 학습 목표 요약

| 버전 | 핵심 학습 | 주요 기능 |
|------|-----------|-----------|
| Ver1 | WebSocket 기초 | Finnhub 실시간 시세, 브로드캐스트 |
| Ver2 | DB 설계, 캔들 집계 | 관심종목 저장, 분봉/일봉 차트 |
| Ver3 | 다중 데이터소스, 알림 | 한국투자증권 API, 가격 알림 |
| Ver4 | 인증 (JWT) | Spring Security + JWT 로그인 |
| Ver5 | LLM API 연동 | 급등/급락 감지 + Claude AI 브리핑 |
| Ver6 | RAG (검색 증강 생성) | 뉴스 임베딩 + 벡터 유사도 검색으로 KR 브리핑 개선 |
| Ver7 | 프로덕션 하드닝 | 보안 버그 수정 + AWS 배포 최소 요건 (진행 중) |

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
| 가격 알림 | 브라우저 Notification API | Ver3 |
| 인증 | Spring Security + JWT (jjwt) | Ver4 |
| AI 브리핑 | Claude API (claude-haiku) | Ver5 |
| KR 뉴스 수집 | 네이버 뉴스 검색 API | Ver6 |
| 임베딩 / RAG | OpenAI Embeddings (text-embedding-3-small) | Ver6 |

---

## 실행 방법

### 사전 요구사항

- Java 17
- Node.js 18+
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
│   │   ├── application/          # 비즈니스 로직 (캔들 집계, 알림 체커, 브로드캐스트, 뉴스 수집/검색)
│   │   ├── infra/                # 외부 연동 (Finnhub, KIS WebSocket, JWT, 네이버 뉴스, OpenAI 임베딩)
│   │   └── api/                  # REST Controller, WebSocket Handler
│   └── build.gradle
├── frontend/
│   ├── app/                      # Next.js 페이지 (메인, 로그인)
│   ├── components/               # CandleChart, AlertForm, KrStockSearchInput
│   ├── hooks/                    # useStockWebSocket, useWatchlist, useAlerts
│   └── lib/                      # auth (JWT), format
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
                                      ├─→ StockWebSocketHandler ──→ 브라우저
                                      └─→ SurgeDetector
                                               ├─→ SURGE 메시지 즉시 전송
                                               └─→ AiBriefingService (@Async)
                                                       ├─→ US: Finnhub 뉴스 조회
                                                       ├─→ KR: NewsRetrievalService 유사도 검색 (RAG)
                                                       ├─→ Claude API 호출
                                                       └─→ AI_BRIEFING 메시지 전송

[Ver6 RAG 배치 파이프라인] (관심종목 KR 뉴스, 30분 주기)
NewsCollectionScheduler (@Scheduled)
        └─→ NaverNewsClient (뉴스 검색) ──→ OpenAiEmbeddingClient (임베딩)
                                                    └─→ NewsArticle 저장 (MySQL, 벡터는 JSON 직렬화)
                                                              ↑
                                     NewsRetrievalService가 코사인 유사도로 조회 (급등 감지 시)
```

- 가격 알림: `PriceAlertChecker` (@Scheduled, 10초마다) → `LatestPriceStore` 조회 → 조건 충족 시 브라우저 알림 전송
- 인증: JWT 토큰을 `Authorization: Bearer` 헤더로 전달, `JwtAuthenticationFilter`에서 검증
- AI 브리핑: 전일 종가 대비 5% 변동 감지 → SURGE 즉시 전송 → Claude API 비동기 호출 → AI_BRIEFING 후속 전송
- RAG: 관심종목(KR) 뉴스를 미리 수집·임베딩해 MySQL에 저장해두고, 급등 감지 시 종목명 기반 쿼리로 코사인 유사도가 가장 높은 기사를 검색해 Claude 프롬프트에 주입 (벡터 DB 없이 애플리케이션 레벨 브루트포스 계산)
- WebSocket 인증(Ver7): 연결 시 쿼리파라미터로 받은 JWT를 `JwtHandshakeInterceptor`가 검증해 세션에 userId 부여 (토큰 없어도 연결은 허용). 개인 알림(`ALERT`)은 `sendToUser`로 본인 세션에만 전송, 시세/SURGE/AI_BRIEFING 등 공개 데이터는 기존처럼 전체 브로드캐스트

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

### Ver7 — 프로덕션 하드닝 (진행 중)
- 전체 코드베이스 감사(백엔드 품질/테스트, 보안/설정, 배포 준비, 프론트+기능) 후 발견한 문제를
  우선순위화해 단계별로 진행 — 자세한 계획은 [ROADMAP.md](./ROADMAP.md) 참고
- 즉시 수정한 버그:
  - IDOR — 알림 삭제에 소유권 체크 없이 누구나 남의 알림 삭제 가능하던 문제
  - WebSocket이 `ALERT`(다른 사용자 목표가 포함)를 인증 없이 전체 브로드캐스트하던 문제 →
    핸드셰이크 시 JWT 검증 후 본인에게만 전송하도록 수정
  - `symbol`이 검증 없이 외부 API URL에 삽입되던 인젝션 여지 → 입력 경계에 형식 검증 추가
  - KIS 접근토큰/approval key가 앱 재기동마다 재발급되어 KIS 측 계정 제한 경고를 받은 문제 →
    파일 캐싱으로 재기동 간 재사용
- Ver7-1(배포 최소요건: Dockerfile, 환경변수화, prod 설정 분리, 헬스체크 등) 진행 중

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
