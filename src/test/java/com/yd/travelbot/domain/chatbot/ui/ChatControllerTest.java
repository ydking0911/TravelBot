package com.yd.travelbot.domain.chatbot.ui;

import com.yd.travelbot.domain.chatbot.application.dto.request.ChatRequest;
import com.yd.travelbot.domain.chatbot.application.dto.response.ChatResponse;
import com.yd.travelbot.domain.chatbot.application.usecase.ProcessChatMessageUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@DisplayName("ChatController 테스트")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessChatMessageUseCase processChatMessageUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("정상 요청 - POST /api/chat")
    void 정상_요청_성공() throws Exception {
        // given
        ChatController.WebChatRequest request = new ChatController.WebChatRequest();
        request.setMessage("서울 맛집 추천해줘");
        request.setSessionId("test-session-123");

        ChatResponse chatResponse = ChatResponse.builder()
                .message("서울의 맛집을 추천해드릴게요!")
                .success(true)
                .sessionId("test-session-123")
                .build();

        when(processChatMessageUseCase.execute(any(ChatRequest.class), any()))
                .thenReturn(chatResponse);

        // when & then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("서울의 맛집을 추천해드릴게요!"))
            .andExpect(jsonPath("$.sessionId").value("test-session-123"));
    }

    @Test
    @DisplayName("요청 바디 검증 - message, sessionId")
    void 요청_바디_검증() throws Exception {
        // given
        ChatController.WebChatRequest request = new ChatController.WebChatRequest();
        request.setMessage("안녕하세요");
        request.setSessionId("session-456");

        ChatResponse chatResponse = ChatResponse.builder()
                .message("안녕하세요! 무엇을 도와드릴까요?")
                .success(true)
                .sessionId("session-456")
                .build();

        when(processChatMessageUseCase.execute(any(ChatRequest.class), any()))
                .thenReturn(chatResponse);

        // when & then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    @DisplayName("응답 형식 검증 - success, message, sessionId")
    void 응답_형식_검증() throws Exception {
        // given
        ChatController.WebChatRequest request = new ChatController.WebChatRequest();
        request.setMessage("테스트");

        ChatResponse chatResponse = ChatResponse.builder()
                .message("테스트 응답")
                .success(true)
                .sessionId("new-session")
                .build();

        when(processChatMessageUseCase.execute(any(ChatRequest.class), any()))
                .thenReturn(chatResponse);

        // when & then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").exists())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    @DisplayName("예외 처리 - UseCase 예외 발생 시 에러 응답")
    void 예외_처리_UseCase_예외() throws Exception {
        // given
        ChatController.WebChatRequest request = new ChatController.WebChatRequest();
        request.setMessage("에러 발생");

        when(processChatMessageUseCase.execute(any(ChatRequest.class), any()))
                .thenThrow(new RuntimeException("서비스 오류"));

        // when & then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("죄송합니다. 오류가 발생했습니다: 서비스 오류"));
    }

    @Test
    @DisplayName("빈 메시지 처리")
    void 빈_메시지_처리() throws Exception {
        // given
        ChatController.WebChatRequest request = new ChatController.WebChatRequest();
        request.setMessage("");

        ChatResponse chatResponse = ChatResponse.builder()
                .message("메시지를 입력해주세요.")
                .success(true)
                .sessionId("empty-session")
                .build();

        when(processChatMessageUseCase.execute(any(ChatRequest.class), any()))
                .thenReturn(chatResponse);

        // when & then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("세션 ID 재사용 - 기존 sessionId 전달")
    void 세션_ID_재사용() throws Exception {
        // given
        ChatController.WebChatRequest request = new ChatController.WebChatRequest();
        request.setMessage("더 추천해줘");
        request.setSessionId("existing-session-789");

        ChatResponse chatResponse = ChatResponse.builder()
                .message("추가 추천입니다.")
                .success(true)
                .sessionId("existing-session-789")
                .build();

        when(processChatMessageUseCase.execute(any(ChatRequest.class), any()))
                .thenReturn(chatResponse);

        // when & then
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("existing-session-789"));
    }
}

