package com.usang.marketdata.api.alert;

import com.usang.marketdata.domain.alert.AlertDirection;
import com.usang.marketdata.domain.alert.PriceAlert;
import com.usang.marketdata.domain.alert.PriceAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts/{userId}")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertRepository priceAlertRepository;

    @GetMapping
    public List<AlertResponse> getAlerts(@PathVariable String userId) {
        return priceAlertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AlertResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertResponse addAlert(@PathVariable String userId, @RequestBody AlertRequest request) {
        PriceAlert alert = priceAlertRepository.save(
                PriceAlert.of(userId, request.symbol(), request.targetPrice(), request.direction()));
        return AlertResponse.from(alert);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlert(@PathVariable String userId, @PathVariable Long id) {
        priceAlertRepository.deleteById(id);
    }

    record AlertRequest(String symbol, double targetPrice, AlertDirection direction) {}

    record AlertResponse(Long id, String symbol, double targetPrice,
                         AlertDirection direction, boolean triggered, LocalDateTime createdAt) {
        static AlertResponse from(PriceAlert alert) {
            return new AlertResponse(
                    alert.getId(), alert.getSymbol(), alert.getTargetPrice(),
                    alert.getDirection(), alert.isTriggered(), alert.getCreatedAt());
        }
    }
}
