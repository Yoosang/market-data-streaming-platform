# ROADMAP

## Ver1 — 기본 실시간 시세 ✅
- Finnhub WebSocket으로 미국 주식 실시간 trade 수신 (백엔드 단일 커넥션)
- 백엔드 → 프론트엔드 WebSocket 브로드캐스트
- 고정 종목 (AAPL, TSLA, MSFT, AMZN) 하드코딩 표시
- 모바일 WTS 스타일 레이아웃

---

## Ver2 — 관심종목 & 차트
- MySQL 도입, 사용자(임시 ID 기반, 로그인 없음)별 관심종목 저장
- 관심종목만 구독하도록 동적 구독 관리
- 틱 데이터를 일정 간격으로 집계해 분봉/일봉 캔들 생성 (직접 적재)
- TradingView Lightweight Charts 연동

---

## Ver3 — 알림 & 다중 데이터소스
- 가격 도달 알림 (브라우저 알림 / 서버 로그)
- 한국투자증권 API 추가 연동 (계좌/키 발급 선행 필요)

---

## Ver4 — 인증 & 확장성
- Spring Security 기반 자체 로그인 (JWT)
- Kafka 도입 — 여러 Spring Boot 인스턴스가 시세 공유 (docker-compose 멀티 인스턴스 검증)

---

## 메모 (미래 버전에서 검토)
<!-- 구현 중 떠오른 아이디어를 여기에 기록 -->
