package com.yd.travelbot.domain.currency.application.usecase;

import com.yd.travelbot.domain.currency.application.dto.CurrencyConvertRequest;
import com.yd.travelbot.domain.currency.application.dto.CurrencyResponse;
import com.yd.travelbot.domain.currency.domain.entity.Currency;
import com.yd.travelbot.domain.currency.domain.repository.CurrencyRepository;
import com.yd.travelbot.domain.currency.domain.service.CurrencyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvertCurrencyUseCase {

    private final CurrencyRepository currencyRepository;
    private final CurrencyDomainService domainService;

    public CurrencyResponse execute(CurrencyConvertRequest request) {
        if (!domainService.isValidCurrencyCode(request.getFromCurrency()) ||
            !domainService.isValidCurrencyCode(request.getToCurrency())) {
            throw new IllegalArgumentException("유효하지 않은 통화 코드입니다.");
        }

        Currency currency = currencyRepository.getExchangeRate(
                request.getFromCurrency(),
                request.getToCurrency()
        );

        if (currency == null) {
            throw new IllegalArgumentException("환율 정보를 가져올 수 없습니다.");
        }

        var convertedAmount = domainService.convert(currency, request.getAmount());
        return CurrencyResponse.from(currency, convertedAmount);
    }
}

