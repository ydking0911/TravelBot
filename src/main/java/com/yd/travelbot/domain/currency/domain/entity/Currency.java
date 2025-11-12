package com.yd.travelbot.domain.currency.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.yd.travelbot.global.common.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Currency extends BaseEntity {
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDateTime lastUpdated;
}

