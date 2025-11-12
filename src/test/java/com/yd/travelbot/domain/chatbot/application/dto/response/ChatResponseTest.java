package com.yd.travelbot.domain.chatbot.application.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatResponse 테스트")
class ChatResponseTest {

    @Test
    @DisplayName("빌더 패턴 - 모든 필드 포함하여 객체 생성")
    void 빌더_패턴_모든_필드() {
        // when
        ChatResponse response = ChatResponse.builder()
                .message("안녕하세요! 무엇을 도와드릴까요?")
                .success(true)
                .sessionId("session-123")
                .build();

        // then
        assertThat(response.getMessage()).isEqualTo("안녕하세요! 무엇을 도와드릴까요?");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSessionId()).isEqualTo("session-123");
    }

    @Test
    @DisplayName("빌더 패턴 - 일부 필드만 포함하여 객체 생성")
    void 빌더_패턴_일부_필드() {
        // when
        ChatResponse response = ChatResponse.builder()
                .message("에러가 발생했습니다")
                .success(false)
                .build();

        // then
        assertThat(response.getMessage()).isEqualTo("에러가 발생했습니다");
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getSessionId()).isNull();
    }

    @Test
    @DisplayName("기본 생성자 - 빈 객체 생성 가능")
    void 기본_생성자_빈_객체() {
        // when
        ChatResponse response = new ChatResponse();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.isSuccess()).isFalse(); // boolean 기본값
        assertThat(response.getSessionId()).isNull();
    }

    @Test
    @DisplayName("AllArgsConstructor - 모든 필드로 객체 생성")
    void AllArgsConstructor_모든_필드() {
        // when
        ChatResponse response = new ChatResponse("테스트 메시지", true, "session-456");

        // then
        assertThat(response.getMessage()).isEqualTo("테스트 메시지");
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSessionId()).isEqualTo("session-456");
    }
}

