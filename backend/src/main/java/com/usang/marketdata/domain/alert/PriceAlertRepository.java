package com.usang.marketdata.domain.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    // 아직 트리거되지 않은 전체 알림 — 10초마다 실행되는 체커가 이 목록을 순회
    List<PriceAlert> findByTriggeredFalse();

    // 특정 사용자의 알림 목록 (최신순) — REST API 응답용
    List<PriceAlert> findByUserIdOrderByCreatedAtDesc(String userId);

    // 본인 소유 알림만 삭제되도록 — userId가 일치하지 않으면 삭제되지 않음 (IDOR 방지)
    void deleteByIdAndUserId(Long id, String userId);
}
