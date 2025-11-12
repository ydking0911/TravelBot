package com.yd.travelbot.domain.currency.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConvertRequest {
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal amount;
}

