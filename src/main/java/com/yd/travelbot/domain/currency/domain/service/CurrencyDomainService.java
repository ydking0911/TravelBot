package com.yd.travelbot.domain.currency.domain.service;

import com.yd.travelbot.domain.currency.domain.entity.Currency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyDomainService {

    public BigDecimal convert(Currency currency, BigDecimal amount) {
        if (currency == null || currency.getRate() == null) {
            throw new IllegalArgumentException("환율 정보가 없습니다.");
        }
        return amount.multiply(currency.getRate()).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isValidCurrencyCode(String code) {
        return code != null && code.length() == 3 && code.matches("[A-Z]{3}");
    }
}

