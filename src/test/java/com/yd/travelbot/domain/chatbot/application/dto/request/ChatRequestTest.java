package com.yd.travelbot.domain.chatbot.application.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatRequest 테스트")
class ChatRequestTest {

    @Test
    @DisplayName("빌더 패턴 - 메시지 필드로 객체 생성")
    void 빌더_패턴_메시지_필드() {
        // when
        ChatRequest request = ChatRequest.builder()
                .message("서울 호텔 추천해줘")
                .build();

        // then
        assertThat(request.getMessage()).isEqualTo("서울 호텔 추천해줘");
    }

    @Test
    @DisplayName("기본 생성자 - 빈 객체 생성 가능")
    void 기본_생성자_빈_객체() {
        // when
        ChatRequest request = new ChatRequest();

        // then
        assertThat(request).isNotNull();
        assertThat(request.getMessage()).isNull();
    }

    @Test
    @DisplayName("AllArgsConstructor - 메시지로 객체 생성")
    void AllArgsConstructor_메시지() {
        // when
        ChatRequest request = new ChatRequest("부산 맛집 추천");

        // then
        assertThat(request.getMessage()).isEqualTo("부산 맛집 추천");
    }
}

