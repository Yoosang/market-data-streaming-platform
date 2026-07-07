# Market Data Streaming Platform

미국·국내 주식 실시간 시세를 모바일 WTS 스타일로 보여주는 학습용 프로젝트입니다.

> **학습 목적으로 단계별 기능을 추가하며 발전시킨 프로젝트입니다.**  
> Ver1 → Ver2 → Ver3 → Ver4 순서로 기능을 확장하고, 각 버전은 git tag로 구분합니다.

---

## 현재 버전: Ver4 (완료)

### 버전별 학습 목표 요약

| 버전 | 핵심 학습 | 주요 기능 |
|------|-----------|-----------|
| Ver1 | WebSocket 기초 | Finnhub 실시간 시세, 브로드캐스트 |
| Ver2 | DB 설계, 캔들 집계 | 관심종목 저장, 분봉/일봉 차트 |
| Ver3 | 다중 데이터소스, 알림 | 한국투자증권 API, 가격 알림 |
| Ver4 | 인증 (JWT) | Spring Security + JWT 로그인 |
| Ver5 | LLM API 연동 | 급등/급락 감지 + Claude AI 브리핑 |

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

---

## 실행 방법

### 사전 요구사항

- Java 17
- Node.js 18+
- MySQL 8.x
- Finnhub API 키 ([무료 가입](https://finnhub.io/))
- 한국투자증권 API 키 (KR 종목 시세, 선택)

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
│   │   ├── domain/               # 엔티티 (User, Watchlist, PriceAlert, Candle)
│   │   ├── application/          # 비즈니스 로직 (캔들 집계, 알림 체커, 브로드캐스트)
│   │   ├── infra/                # 외부 연동 (Finnhub, KIS WebSocket, JWT)
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
                                      └─→ StockWebSocketHandler
                                                  │
                                       브라우저 WebSocket 클라이언트
```

- 가격 알림: `PriceAlertChecker` (@Scheduled, 10초마다) → `LatestPriceStore` 조회 → 조건 충족 시 브라우저 알림 전송
- 인증: JWT 토큰을 `Authorization: Bearer` 헤더로 전달, `JwtAuthenticationFilter`에서 검증

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
- 5분 전 기준가 대비 5% 이상 급등/급락 감지 (`SurgeDetector`)
- Finnhub 뉴스 API로 최근 헤드라인 조회 (US 종목)
- Claude API (claude-haiku) 호출 → 한 줄 브리핑 생성
- 브라우저에 SURGE 배너 즉시 + AI 분석 결과 후속 표시

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
