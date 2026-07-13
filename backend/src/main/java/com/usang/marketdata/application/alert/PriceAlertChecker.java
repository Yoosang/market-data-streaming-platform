package com.usang.marketdata.application.alert;

import com.usang.marketdata.domain.alert.PriceAlert;
import com.usang.marketdata.domain.alert.PriceAlertRepository;
import com.usang.marketdata.domain.watchlist.Watchlist;
import com.usang.marketdata.domain.watchlist.WatchlistRepository;
import com.usang.marketdata.infra.telegram.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceAlertChecker {

    private final PriceAlertRepository priceAlertRepository;
    private final LatestPriceStore latestPriceStore;
    private final WatchlistRepository watchlistRepository;
    private final TelegramNotifier telegramNotifier;

    // 10초마다 전체 알림을 순회하며 조건 충족 여부 확인 — 트리거된 알림은 이력을 남길 필요가 없어 바로 삭제
    @Scheduled(fixedRate = 10000)
    public void check() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findAll();

        for (PriceAlert alert : activeAlerts) {
            Optional<Double> price = latestPriceStore.getPrice(alert.getSymbol());
            // 종목별 상세 내역은 디버깅용 — 평소에는 완료 로그 한 줄만 남도록 debug로 낮춤
            log.debug("AlertChecker: symbol={} target={} {} latestPrice={}",
                    alert.getSymbol(), alert.getTargetPrice(), alert.getDirection(),
                    price.map(String::valueOf).orElse("N/A (no price in store)"));

            price.ifPresent(p -> {
                if (isConditionMet(alert, p)) {
                    priceAlertRepository.delete(alert);
                    notifyTelegram(alert, p);
                }
            });
        }

        log.info("AlertChecker: {} active alert(s) checked", activeAlerts.size());
    }

    private boolean isConditionMet(PriceAlert alert, double currentPrice) {
        return switch (alert.getDirection()) {
            case ABOVE -> currentPrice >= alert.getTargetPrice();
            case BELOW -> currentPrice <= alert.getTargetPrice();
        };
    }

    private void notifyTelegram(PriceAlert alert, double currentPrice) {
        String direction = alert.getDirection().name().equals("ABOVE") ? "이상" : "이하";
        boolean isKr = alert.getSymbol().matches("\\d+");
        String displayName = isKr ? krCompanyName(alert) : alert.getSymbol();

        telegramNotifier.notify("%s 현재가 %s — 목표가 %s %s 도달".formatted(
                displayName,
                formatPrice(currentPrice, isKr),
                formatPrice(alert.getTargetPrice(), isKr),
                direction));

        log.info("Alert triggered: {} {} {} (current: {})",
                alert.getSymbol(), alert.getDirection(), alert.getTargetPrice(), currentPrice);
    }

    // 관심종목에 등록된 이름을 사용 — 못 찾으면 종목코드로 대체
    private String krCompanyName(PriceAlert alert) {
        return watchlistRepository.findByUserIdAndSymbol(alert.getUserId(), alert.getSymbol())
                .map(Watchlist::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(alert.getSymbol());
    }

    // KR: 콤마 구분 + 소수점 없음 / US: 콤마 구분 + 소수점 둘째 자리
    private String formatPrice(double price, boolean isKr) {
        return isKr ? "%,.0f원".formatted(price) : "$%,.2f".formatted(price);
    }
}
