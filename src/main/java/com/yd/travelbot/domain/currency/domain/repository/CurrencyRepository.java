package com.yd.travelbot.domain.currency.domain.repository;

import com.yd.travelbot.domain.currency.domain.entity.Currency;

public interface CurrencyRepository {
    Currency getExchangeRate(String fromCurrency, String toCurrency);
}

