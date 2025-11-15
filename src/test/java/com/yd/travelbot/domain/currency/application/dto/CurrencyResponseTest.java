package com.yd.travelbot.domain.currency.application.dto;

import com.yd.travelbot.domain.currency.domain.entity.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CurrencyResponse 테스트")
class CurrencyResponseTest {

    @Test
    @DisplayName("from 메서드 - Currency 엔티티와 변환 금액으로 Response 생성")
    void from_메서드_엔티티_변환() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Currency currency = Currency.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .rate(new BigDecimal("1300.50"))
                .lastUpdated(now)
                .build();
        BigDecimal convertedAmount = new BigDecimal("130050");

        // when
        CurrencyResponse response = CurrencyResponse.from(currency, convertedAmount);

        // then
        assertThat(response.getFromCurrency()).isEqualTo("USD");
        assertThat(response.getToCurrency()).isEqualTo("KRW");
        assertThat(response.getRate()).isEqualByComparingTo(new BigDecimal("1300.50"));
        assertThat(response.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("130050"));
        assertThat(response.getLastUpdated()).isEqualTo(now);
    }

    @Test
    @DisplayName("빌더 패턴 - 모든 필드 포함하여 객체 생성")
    void 빌더_패턴_모든_필드() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        CurrencyResponse response = CurrencyResponse.builder()
                .fromCurrency("EUR")
                .toCurrency("JPY")
                .rate(new BigDecimal("150.25"))
                .convertedAmount(new BigDecimal("15025"))
                .lastUpdated(now)
                .build();

        // then
        assertThat(response.getFromCurrency()).isEqualTo("EUR");
        assertThat(response.getToCurrency()).isEqualTo("JPY");
        assertThat(response.getRate()).isEqualByComparingTo(new BigDecimal("150.25"));
        assertThat(response.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("15025"));
        assertThat(response.getLastUpdated()).isEqualTo(now);
    }

    @Test
    @DisplayName("기본 생성자 - 빈 객체 생성 가능")
    void 기본_생성자_빈_객체() {
        // when
        CurrencyResponse response = new CurrencyResponse();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFromCurrency()).isNull();
        assertThat(response.getToCurrency()).isNull();
        assertThat(response.getRate()).isNull();
    }
}

