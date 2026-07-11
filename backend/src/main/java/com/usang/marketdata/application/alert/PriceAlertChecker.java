package com.usang.marketdata.application.alert;

import tools.jackson.databind.ObjectMapper;
import com.usang.marketdata.api.stock.StockWebSocketHandler;
import com.usang.marketdata.domain.alert.PriceAlert;
import com.usang.marketdata.domain.alert.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAlertChecker {

    private final PriceAlertRepository priceAlertRepository;
    private final LatestPriceStore latestPriceStore;
    private final StockWebSocketHandler stockWebSocketHandler;
    private final ObjectMapper objectMapper;

    // 10초마다 미트리거 알림 전체를 순회하며 조건 충족 여부 확인
    @Scheduled(fixedRate = 10000)
    public void check() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findByTriggeredFalse();
        log.debug("AlertChecker: {} active alert(s)", activeAlerts.size());

        for (PriceAlert alert : activeAlerts) {
            Optional<Double> price = latestPriceStore.getPrice(alert.getSymbol());
            log.debug("AlertChecker: symbol={} target={} {} latestPrice={}",
                    alert.getSymbol(), alert.getTargetPrice(), alert.getDirection(),
                    price.map(String::valueOf).orElse("N/A (no price in store)"));

            price.ifPresent(p -> {
                if (isConditionMet(alert, p)) {
                    alert.trigger();
                    priceAlertRepository.save(alert);
                    sendAlertToFrontend(alert, p);
                }
            });
        }
    }

    private boolean isConditionMet(PriceAlert alert, double currentPrice) {
        return switch (alert.getDirection()) {
            case ABOVE -> currentPrice >= alert.getTargetPrice();
            case BELOW -> currentPrice <= alert.getTargetPrice();
        };
    }

    private void sendAlertToFrontend(PriceAlert alert, double currentPrice) {
        try {
            // 기존 trade 메시지와 구분하기 위해 type 필드를 포함한 별도 포맷 사용
            Map<String, Object> message = Map.of(
                    "type", "ALERT",
                    "symbol", alert.getSymbol(),
                    "price", currentPrice,
                    "targetPrice", alert.getTargetPrice(),
                    "direction", alert.getDirection().name()
            );
            // 본인 알림이므로 전체 브로드캐스트가 아닌 소유자 세션에만 전송 (다른 사용자의 목표가 노출 방지)
            stockWebSocketHandler.sendToUser(alert.getUserId(), objectMapper.writeValueAsString(message));
            log.info("Alert triggered: {} {} {} (current: {})",
                    alert.getSymbol(), alert.getDirection(), alert.getTargetPrice(), currentPrice);
        } catch (Exception e) {
            log.error("Failed to send alert notification: {}", e.getMessage());
        }
    }
}
