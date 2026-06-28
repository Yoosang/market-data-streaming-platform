
# Market Data Streaming Platform

미국 주식 실시간 시세를 모바일 WTS(Wealth Trading System) 스타일로 보여주는 학습용 프로젝트입니다.

> **학습 목적으로 단계별로 기능을 추가하며 발전시키는 프로젝트입니다.**
> Ver1 → Ver2 → Ver3 → Ver4 순서로 기능을 확장하고, 각 버전은 git tag로 구분합니다.

---

## 현재 버전: Ver1

### 학습 목표
- Finnhub WebSocket으로 미국 주식 실시간 시세 수신
- Spring Boot에서 단일 WebSocket 커넥션 유지 후 프론트엔드로 브로드캐스트
- React(Next.js) 화면에서 실시간 데이터 렌더링

### Ver1 범위
- Finnhub WebSocket 연결 (AAPL, TSLA, MSFT, AMZN 하드코딩)
- 백엔드 → 프론트엔드 WebSocket 브로드캐스트
- 모바일 WTS 스타일 레이아웃

---

## 프로젝트 구조

```
marketdata/
├── backend/           # Spring Boot 백엔드
│   ├── src/
│   │   └── main/java/com/usang/marketdata/
│   │       ├── global/config/        # WebSocket 설정
│   │       └── infra/finnhub/        # Finnhub WebSocket 클라이언트
│   ├── build.gradle
│   └── docker-compose.yml
└── frontend/          # Next.js 프론트엔드 (Ver1 진행 중)
```

---

## 기술 스택

| 레이어 | 기술 | 도입 버전 |
|---|---|---|
| Backend | Java 17, Spring Boot 4.x, WebSocket | Ver1 |
| Frontend | Next.js (App Router), React | Ver1 |
| 실시간 시세 | Finnhub WebSocket | Ver1 |
| DB | MySQL | Ver2 |
| Chart | TradingView Lightweight Charts | Ver2 |
| 알림 | 브라우저 알림 / 서버 로그 | Ver3 |
| 메시징 | Apache Kafka | Ver4 |
| 인증 | Spring Security + JWT | Ver4 |

---

## 버전 로드맵

### Ver1 — 기본 실시간 시세 ← 현재
- Finnhub WebSocket 실시간 체결가 수신
- 백엔드 → 프론트엔드 WebSocket 브로드캐스트
- 고정 종목(AAPL, TSLA, MSFT, AMZN) 실시간 화면

### Ver2 — 관심종목 & 차트
- MySQL 도입, 사용자별 관심종목 저장 (로그인 없이 임시 ID 기반)
- 동적 구독 관리
- 틱 데이터 집계 → 분봉/일봉 캔들 직접 적재
- TradingView Lightweight Charts 연동

### Ver3 — 알림 & 다중 데이터소스
- 가격 도달 알림 (브라우저 알림)
- 한국투자증권 API 추가 연동

### Ver4 — 인증 & 확장성
- Spring Security + JWT 로그인
- Kafka 도입으로 멀티 인스턴스 시세 공유

---

## 실행 방법

### 사전 요구사항
- Java 17
- Node.js 18+
- Finnhub API 키 ([무료 가입](https://finnhub.io/))

### 백엔드

```bash
# 환경변수 설정
cp backend/.env.example backend/.env
# .env에 FINNHUB_API_KEY 값 입력 후 실행

cd backend
FINNHUB_API_KEY=your_key_here ./gradlew bootRun
```

> IntelliJ를 사용하는 경우: Run Configuration → Environment variables에 `FINNHUB_API_KEY=값` 입력

### 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

---

## 환경변수

`backend/.env.example`을 복사해 `.env`를 만들고 아래 값을 채워주세요.

```
FINNHUB_API_KEY=your_finnhub_api_key_here
```

> Finnhub 무료 티어는 미국 거래소 종목만 실시간 지원합니다.
> 한국 시간 기준 **야간(밤 10시 ~ 새벽 5시)** 에만 실제 데이터가 흐릅니다.

---

## 패키지 구조 (Backend)

```
com.usang.marketdata
├── global/
│   └── config/          # WebSocket, 공통 설정
├── domain/              # 도메인 모델 (Ver2부터)
├── application/         # 비즈니스 로직 (Ver2부터)
├── infra/
│   └── finnhub/         # Finnhub WebSocket 클라이언트
└── api/                 # REST Controller (Ver2부터)
```

---

## 커밋 컨벤션

```
[Ver1] 기능 설명
[Ver2] 기능 설명
```

---

## 라이선스

MIT
