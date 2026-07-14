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

## Ver7 — 프로덕션 하드닝 (진행 중)
- **배경**: Ver1~6까지 개발 속도 위주로 기능을 쌓아와서 품질 점검이 부족했다고 판단, 실제 AWS에
  배포해 쓸 수 있는 수준으로 다듬기로 함. 백엔드 품질/테스트, 보안/설정, 배포 준비, 프론트+기능
  인벤토리 4개 관점으로 전체 코드베이스를 감사한 뒤 아래 단계로 나눠 진행

- **즉시 버그 수정** ✅ (Ver7 태그와 별개로 먼저 처리한 실제 버그)
  - IDOR: `PriceAlertController.deleteAlert`에 소유권 체크가 없어 누구나 남의 알림을 삭제 가능하던 문제
  - WebSocket이 `ALERT` 메시지(다른 사용자의 목표가 포함)를 인증 없이 전체 브로드캐스트하던 문제
    → `JwtHandshakeInterceptor`로 연결 시 토큰을 검증해 세션에 userId 부여,
      `StockWebSocketHandler.sendToUser`로 본인에게만 전송 (공개 데이터는 기존 브로드캐스트 유지)
  - `symbol` 값이 검증 없이 외부 API URL에 그대로 삽입되던 인젝션 여지 → 관심종목/알림 등록
    시점에 형식 검증 추가
  - KIS 접근토큰(`KisAccessTokenProvider`)과 approval key(`KisApprovalKeyProvider`)가
    앱을 재기동할 때마다 재발급되던 문제 → 로컬 파일에 캐싱해 재기동 간 재사용 (개발 중 반복
    재시작으로 KIS 발급 API를 과호출해 계정 제한 경고를 받은 것이 계기)

- **Ver7-1: 배포 최소요건** (진행 중) — "AWS에 실제로 띄울 수 있는 최소 상태" 목표, 스케일링
  대응은 제외
  - Dockerfile (backend/frontend)
  - 프론트 하드코딩된 `localhost:8080` API/WS URL을 환경변수로 전환
  - 백엔드 `application-prod.yaml` 분리, Flyway 도입, CORS/WS origin 환경변수화
  - Actuator 헬스체크가 인증 없이 동작하도록 `/actuator/health` 예외 처리
  - `RestClient` 타임아웃 설정, `@Async` 전용 스레드풀 적용
  - `spring-boot-starter-validation` 실제 활성화 + 전역 예외 처리

- **Ver7-2: 기능 감사** (예정) — Finnhub(미장 시간대 제한)/KIS(개인 계정 키)/AI 브리핑(비용,
  관심종목당 무제한 트리거 가능)/RAG(Ver6, 스스로 "학습용 브루트포스"라 명시) 등 구조적 제약이
  있는 기능을 유지·데모모드·제거 중 무엇으로 할지 제품 관점에서 결정. 관심종목/알림 개수
  제한(비용 리스크 방지)도 함께 처리

- **Ver7-3: 스케일링 대응** (예정) — 현재 모든 상태(최신가 캐시, 급등 감지 기준가/쿨다운, 캔들
  집계, WebSocket 세션)가 JVM 로컬 메모리라 다중 인스턴스로 확장 불가. Redis 캐시/pub-sub 도입,
  `@Scheduled` 작업 분산 락 등 — 실제 다중 인스턴스 배포 계획이 생기면 진행

- **Ver7-4: 실제 AWS 배포** (예정) — ECS/App Runner/EC2 중 선택, RDS 전환, Secrets Manager
  연동, CI/CD 파이프라인

---

## 미래 검토 (확장성)
- Kafka 도입: 여러 Spring Boot 인스턴스가 시세 공유 (docker-compose 멀티 인스턴스 검증)
- Redis 도입: 최근 시세 및 캔들 캐싱으로 DB 부하 감소

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
