package com.yd.travelbot.domain.currency.application.dto;

import com.yd.travelbot.domain.currency.domain.entity.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyResponse {
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private BigDecimal convertedAmount;
    private LocalDateTime lastUpdated;

    public static CurrencyResponse from(Currency currency, BigDecimal convertedAmount) {
        return CurrencyResponse.builder()
                .fromCurrency(currency.getFromCurrency())
                .toCurrency(currency.getToCurrency())
                .rate(currency.getRate())
                .convertedAmount(convertedAmount)
                .lastUpdated(currency.getLastUpdated())
                .build();
    }
}

