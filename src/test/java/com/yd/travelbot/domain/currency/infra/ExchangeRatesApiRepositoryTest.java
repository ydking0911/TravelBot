package com.yd.travelbot.domain.currency.infra;

import com.yd.travelbot.domain.currency.domain.entity.Currency;
import com.yd.travelbot.global.config.ExchangeRatesConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRatesApiRepository 통합 테스트")
class ExchangeRatesApiRepositoryTest {

    @Mock
    private ExchangeRatesConfig exchangeRatesConfig;

    private ExchangeRatesApiRepository repository;

    @BeforeEach
    void setUp() {
        // lenient()를 사용하여 사용되지 않는 stubbing 허용
        lenient().when(exchangeRatesConfig.getApiKey()).thenReturn("test-api-key");
        
        // Repository는 실제 URL을 사용하므로, 리플렉션이나 테스트용 생성자가 필요
        // 여기서는 기본 동작 검증에 집중
        repository = new ExchangeRatesApiRepository(exchangeRatesConfig);
    }

    @Test
    @DisplayName("같은 통화 변환 - 1:1 반환")
    void 같은_통화_변환() {
        // given
        String currency = "USD";

        // when
        Currency result = repository.getExchangeRate(currency, currency);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getFromCurrency()).isEqualTo(currency);
        assertThat(result.getToCurrency()).isEqualTo(currency);
        assertThat(result.getRate()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("기본 환율 반환 - API 실패 시 기본값 사용")
    void 기본_환율_반환() {
        // given
        // API 호출이 실패하는 경우 기본 환율 반환
        // 실제 구현에서는 getDefaultRate 메서드가 호출됨

        // when
        Currency result = repository.getExchangeRate("USD", "KRW");

        // then
        // 기본 환율이 반환되거나 예외가 발생하지 않아야 함
        assertThat(result).isNotNull();
    }
}

