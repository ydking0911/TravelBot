package com.yd.travelbot.domain.currency.domain.service;

import com.yd.travelbot.domain.currency.domain.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CurrencyDomainService 테스트")
class CurrencyDomainServiceTest {

    private CurrencyDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new CurrencyDomainService();
    }

    @Test
    @DisplayName("통화 코드 검증 - 정상 케이스")
    void 통화_코드_검증_정상() {
        // given
        String[] validCodes = {"USD", "KRW", "EUR", "JPY", "CNY", "GBP"};

        // when & then
        for (String code : validCodes) {
            boolean result = domainService.isValidCurrencyCode(code);
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("통화 코드 검증 - 소문자 코드는 유효하지 않음")
    void 통화_코드_검증_소문자_실패() {
        // given
        String code = "usd";

        // when
        boolean result = domainService.isValidCurrencyCode(code);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("통화 코드 검증 - 길이가 3이 아닌 경우")
    void 통화_코드_검증_길이_실패() {
        // given
        String[] invalidCodes = {"US", "USDD", "U", "USDKRW"};

        // when & then
        for (String code : invalidCodes) {
            boolean result = domainService.isValidCurrencyCode(code);
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("통화 코드 검증 - 숫자가 포함된 경우")
    void 통화_코드_검증_숫자_포함_실패() {
        // given
        String[] invalidCodes = {"US1", "12D", "A1B"};

        // when & then
        for (String code : invalidCodes) {
            boolean result = domainService.isValidCurrencyCode(code);
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("통화 코드 검증 - null인 경우")
    void 통화_코드_검증_null() {
        // given
        String code = null;

        // when
        boolean result = domainService.isValidCurrencyCode(code);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("환율 변환 - 정상 케이스")
    void 환율_변환_성공() {
        // given
        Currency currency = Currency.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .rate(new BigDecimal("1300.50"))
                .lastUpdated(LocalDateTime.now())
                .build();
        BigDecimal amount = new BigDecimal("100");

        // when
        BigDecimal result = domainService.convert(currency, amount);

        // then
        assertThat(result).isEqualByComparingTo(new BigDecimal("130050.00"));
    }

    @Test
    @DisplayName("환율 변환 - 소수점 반올림")
    void 환율_변환_소수점_반올림() {
        // given
        Currency currency = Currency.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .rate(new BigDecimal("1300.555"))
                .lastUpdated(LocalDateTime.now())
                .build();
        BigDecimal amount = new BigDecimal("100");

        // when
        BigDecimal result = domainService.convert(currency, amount);

        // then
        assertThat(result).isEqualByComparingTo(new BigDecimal("130055.50"));
        assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("환율 변환 - currency가 null인 경우 예외 발생")
    void 환율_변환_currency_null_예외() {
        // given
        Currency currency = null;
        BigDecimal amount = new BigDecimal("100");

        // when & then
        assertThatThrownBy(() -> domainService.convert(currency, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("환율 정보가 없습니다");
    }

    @Test
    @DisplayName("환율 변환 - rate가 null인 경우 예외 발생")
    void 환율_변환_rate_null_예외() {
        // given
        Currency currency = Currency.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .rate(null)
                .lastUpdated(LocalDateTime.now())
                .build();
        BigDecimal amount = new BigDecimal("100");

        // when & then
        assertThatThrownBy(() -> domainService.convert(currency, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("환율 정보가 없습니다");
    }

    @Test
    @DisplayName("환율 변환 - amount가 0인 경우")
    void 환율_변환_amount_0() {
        // given
        Currency currency = Currency.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .rate(new BigDecimal("1300.50"))
                .lastUpdated(LocalDateTime.now())
                .build();
        BigDecimal amount = BigDecimal.ZERO;

        // when
        BigDecimal result = domainService.convert(currency, amount);

        // then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

