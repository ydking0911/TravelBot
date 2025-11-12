package com.yd.travelbot.domain.currency.application.usecase;

import com.yd.travelbot.domain.currency.application.dto.CurrencyConvertRequest;
import com.yd.travelbot.domain.currency.application.dto.CurrencyResponse;
import com.yd.travelbot.domain.currency.domain.entity.Currency;
import com.yd.travelbot.domain.currency.domain.repository.CurrencyRepository;
import com.yd.travelbot.domain.currency.domain.service.CurrencyDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConvertCurrencyUseCase 테스트")
class ConvertCurrencyUseCaseTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CurrencyDomainService domainService;

    @InjectMocks
    private ConvertCurrencyUseCase useCase;

    private Currency currency;

    @BeforeEach
    void setUp() {
        currency = Currency.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .rate(new BigDecimal("1300.50"))
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("정상 변환 - fromCurrency, toCurrency, amount로 환율 변환")
    void 정상_변환_성공() {
        // given
        CurrencyConvertRequest request = CurrencyConvertRequest.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .amount(new BigDecimal("100"))
                .build();

        BigDecimal convertedAmount = new BigDecimal("130050.00");

        when(domainService.isValidCurrencyCode("USD")).thenReturn(true);
        when(domainService.isValidCurrencyCode("KRW")).thenReturn(true);
        when(currencyRepository.getExchangeRate("USD", "KRW"))
                .thenReturn(currency);
        when(domainService.convert(currency, new BigDecimal("100")))
                .thenReturn(convertedAmount);

        // when
        CurrencyResponse result = useCase.execute(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getFromCurrency()).isEqualTo("USD");
        assertThat(result.getToCurrency()).isEqualTo("KRW");
        assertThat(result.getRate()).isEqualByComparingTo(new BigDecimal("1300.50"));
        assertThat(result.getConvertedAmount()).isEqualByComparingTo(convertedAmount);
        verify(currencyRepository).getExchangeRate("USD", "KRW");
        verify(domainService).convert(currency, new BigDecimal("100"));
    }

    @Test
    @DisplayName("통화 코드 검증 - fromCurrency가 유효하지 않은 경우 예외 발생")
    void 통화_코드_검증_fromCurrency_실패() {
        // given
        CurrencyConvertRequest request = CurrencyConvertRequest.builder()
                .fromCurrency("INVALID")
                .toCurrency("KRW")
                .amount(new BigDecimal("100"))
                .build();

        when(domainService.isValidCurrencyCode("INVALID")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 통화 코드");
        verify(currencyRepository, never()).getExchangeRate(anyString(), anyString());
    }

    @Test
    @DisplayName("통화 코드 검증 - toCurrency가 유효하지 않은 경우 예외 발생")
    void 통화_코드_검증_toCurrency_실패() {
        // given
        CurrencyConvertRequest request = CurrencyConvertRequest.builder()
                .fromCurrency("USD")
                .toCurrency("INVALID")
                .amount(new BigDecimal("100"))
                .build();

        when(domainService.isValidCurrencyCode("USD")).thenReturn(true);
        when(domainService.isValidCurrencyCode("INVALID")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 통화 코드");
        verify(currencyRepository, never()).getExchangeRate(anyString(), anyString());
    }

    @Test
    @DisplayName("환율 정보 없음 - 환율 정보를 가져올 수 없을 때 예외 발생")
    void 환율_정보_없음_예외() {
        // given
        CurrencyConvertRequest request = CurrencyConvertRequest.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .amount(new BigDecimal("100"))
                .build();

        when(domainService.isValidCurrencyCode("USD")).thenReturn(true);
        when(domainService.isValidCurrencyCode("KRW")).thenReturn(true);
        when(currencyRepository.getExchangeRate("USD", "KRW"))
                .thenReturn(null);

        // when & then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("환율 정보를 가져올 수 없습니다");
        verify(domainService, never()).convert(any(), any());
    }

    @Test
    @DisplayName("null 처리 - amount가 null인 경우")
    void amount_null_처리() {
        // given
        CurrencyConvertRequest request = CurrencyConvertRequest.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .amount(null)
                .build();

        when(domainService.isValidCurrencyCode("USD")).thenReturn(true);
        when(domainService.isValidCurrencyCode("KRW")).thenReturn(true);
        when(currencyRepository.getExchangeRate("USD", "KRW"))
                .thenReturn(currency);
        when(domainService.convert(currency, null))
                .thenThrow(new IllegalArgumentException("금액이 필요합니다"));

        // when & then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("다양한 통화 쌍 변환")
    void 다양한_통화_쌍_변환() {
        // given
        Currency eurToKrw = Currency.builder()
                .fromCurrency("EUR")
                .toCurrency("KRW")
                .rate(new BigDecimal("1400.00"))
                .lastUpdated(LocalDateTime.now())
                .build();

        CurrencyConvertRequest request = CurrencyConvertRequest.builder()
                .fromCurrency("EUR")
                .toCurrency("KRW")
                .amount(new BigDecimal("50"))
                .build();

        BigDecimal convertedAmount = new BigDecimal("70000.00");

        when(domainService.isValidCurrencyCode("EUR")).thenReturn(true);
        when(domainService.isValidCurrencyCode("KRW")).thenReturn(true);
        when(currencyRepository.getExchangeRate("EUR", "KRW"))
                .thenReturn(eurToKrw);
        when(domainService.convert(eurToKrw, new BigDecimal("50")))
                .thenReturn(convertedAmount);

        // when
        CurrencyResponse result = useCase.execute(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getFromCurrency()).isEqualTo("EUR");
        assertThat(result.getToCurrency()).isEqualTo("KRW");
        assertThat(result.getConvertedAmount()).isEqualByComparingTo(convertedAmount);
    }
}

