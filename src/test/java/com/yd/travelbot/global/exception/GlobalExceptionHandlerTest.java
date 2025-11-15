package com.yd.travelbot.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("NoResourceFoundException 처리 - favicon.ico는 무시")
    void NoResourceFoundException_처리_favicon_무시() {
        // given
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/favicon.ico");

        // when
        ResponseEntity<Void> response = exceptionHandler.handleNoResourceFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("NoResourceFoundException 처리 - 일반 리소스는 경고")
    void NoResourceFoundException_처리_일반_리소스() {
        // given
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/api/unknown");

        // when
        ResponseEntity<Void> response = exceptionHandler.handleNoResourceFoundException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("IllegalArgumentException 처리 - BAD_REQUEST 반환")
    void IllegalArgumentException_처리_BAD_REQUEST() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("잘못된 인자입니다");

        // when
        ResponseEntity<String> response = exceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("잘못된 인자입니다");
    }

    @Test
    @DisplayName("RuntimeException 처리 - INTERNAL_SERVER_ERROR 반환")
    void RuntimeException_처리_INTERNAL_SERVER_ERROR() {
        // given
        RuntimeException exception = new RuntimeException("런타임 오류 발생");

        // when
        ResponseEntity<String> response = exceptionHandler.handleRuntimeException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("서버 오류가 발생했습니다");
        assertThat(response.getBody()).contains("런타임 오류 발생");
    }

    @Test
    @DisplayName("Exception 처리 - INTERNAL_SERVER_ERROR 반환")
    void Exception_처리_INTERNAL_SERVER_ERROR() {
        // given
        Exception exception = new Exception("예상치 못한 오류");

        // when
        ResponseEntity<String> response = exceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("예상치 못한 오류가 발생했습니다");
        assertThat(response.getBody()).contains("예상치 못한 오류");
    }

    @Test
    @DisplayName("예외 처리 순서 - RuntimeException이 Exception보다 먼저 처리됨")
    void 예외_처리_순서_RuntimeException_우선() {
        // given
        RuntimeException runtimeException = new RuntimeException("런타임 예외");

        // when
        ResponseEntity<String> response = exceptionHandler.handleRuntimeException(runtimeException);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("서버 오류가 발생했습니다");
    }
}

