package com.usang.marketdata.api.alert;

import com.usang.marketdata.domain.alert.AlertDirection;
import com.usang.marketdata.domain.alert.PriceAlert;
import com.usang.marketdata.domain.alert.PriceAlertRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertRepository priceAlertRepository;

    @GetMapping
    public List<AlertResponse> getAlerts(@AuthenticationPrincipal String userId) {
        return priceAlertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AlertResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertResponse addAlert(@AuthenticationPrincipal String userId,
                                  @Valid @RequestBody AlertRequest request) {
        PriceAlert alert = priceAlertRepository.save(
                PriceAlert.of(userId, request.symbol(), request.targetPrice(), request.direction()));
        return AlertResponse.from(alert);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlert(@AuthenticationPrincipal String userId, @PathVariable Long id) {
        priceAlertRepository.deleteByIdAndUserId(id, userId);
    }

    // symbol: US 티커(알파벳) 또는 KR 종목코드(숫자) — 외부 API URL에 그대로 삽입되므로 형식을 여기서 제한
    public record AlertRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{1,10}$") String symbol,
            @Positive double targetPrice,
            @NotNull AlertDirection direction) {}

    public record AlertResponse(Long id, String symbol, double targetPrice,
                         AlertDirection direction, boolean triggered, LocalDateTime createdAt) {
        static AlertResponse from(PriceAlert alert) {
            return new AlertResponse(
                    alert.getId(), alert.getSymbol(), alert.getTargetPrice(),
                    alert.getDirection(), alert.isTriggered(), alert.getCreatedAt());
        }
    }
}
