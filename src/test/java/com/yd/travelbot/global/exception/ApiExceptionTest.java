package com.yd.travelbot.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApiException 테스트")
class ApiExceptionTest {

    @Test
    @DisplayName("메시지만 있는 예외 생성")
    void 메시지만_있는_예외_생성() {
        // given
        String message = "API 호출 실패";

        // when
        ApiException exception = new ApiException(message);

        // then
        assertThat(exception.getMessage()).isEqualTo("API 호출 실패");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("메시지와 원인을 포함한 예외 생성")
    void 메시지와_원인_포함_예외_생성() {
        // given
        String message = "API 호출 실패";
        Throwable cause = new RuntimeException("네트워크 오류");

        // when
        ApiException exception = new ApiException(message, cause);

        // then
        assertThat(exception.getMessage()).isEqualTo("API 호출 실패");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("네트워크 오류");
    }

    @Test
    @DisplayName("예외 체이닝 - 원인 예외의 메시지 확인")
    void 예외_체이닝_원인_메시지() {
        // given
        String message = "외부 API 오류";
        IllegalArgumentException cause = new IllegalArgumentException("잘못된 파라미터");

        // when
        ApiException exception = new ApiException(message, cause);

        // then
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getCause().getMessage()).isEqualTo("잘못된 파라미터");
    }
}

