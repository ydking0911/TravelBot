package com.yd.travelbot.domain.chatbot.application.usecase;

import com.yd.travelbot.domain.chatbot.application.dto.request.ChatRequest;
import com.yd.travelbot.domain.chatbot.application.dto.response.ChatResponse;
import com.yd.travelbot.domain.chatbot.domain.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessChatMessageUseCase 테스트")
class ProcessChatMessageUseCaseTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ProcessChatMessageUseCase useCase;

    @Test
    @DisplayName("정상 케이스 - 메시지 처리 성공")
    void 정상_케이스_성공() {
        // given
        ChatRequest request = ChatRequest.builder()
                .message("서울 맛집 추천해줘")
                .build();
        String expectedResponse = "서울의 맛집을 추천해드릴게요!";
        String sessionId = "test-session-123";

        when(chatService.chatWithHistory("서울 맛집 추천해줘", "", sessionId))
                .thenReturn(expectedResponse);

        // when
        ChatResponse result = useCase.execute(request, sessionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo(expectedResponse);
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        verify(chatService).chatWithHistory("서울 맛집 추천해줘", "", sessionId);
    }

    @Test
    @DisplayName("세션 ID 생성 - sessionId가 null일 때 자동 생성")
    void 세션_ID_생성_자동() {
        // given
        ChatRequest request = ChatRequest.builder()
                .message("안녕하세요")
                .build();
        String expectedResponse = "안녕하세요! 무엇을 도와드릴까요?";

        when(chatService.chatWithHistory(anyString(), anyString(), anyString()))
                .thenReturn(expectedResponse);

        // when
        ChatResponse result = useCase.execute(request, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSessionId()).isNotNull();
        assertThat(result.getSessionId()).isNotEmpty();
        verify(chatService).chatWithHistory(eq("안녕하세요"), eq(""), anyString());
    }

    @Test
    @DisplayName("세션 ID 생성 - sessionId가 빈 문자열일 때 자동 생성")
    void 세션_ID_생성_빈_문자열() {
        // given
        ChatRequest request = ChatRequest.builder()
                .message("안녕하세요")
                .build();
        String expectedResponse = "안녕하세요! 무엇을 도와드릴까요?";

        when(chatService.chatWithHistory(anyString(), anyString(), anyString()))
                .thenReturn(expectedResponse);

        // when
        ChatResponse result = useCase.execute(request, "");

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSessionId()).isNotNull();
        assertThat(result.getSessionId()).isNotEmpty();
    }

    @Test
    @DisplayName("세션 ID 재사용 - 기존 sessionId로 멀티턴 대화 유지")
    void 세션_ID_재사용() {
        // given
        ChatRequest request = ChatRequest.builder()
                .message("더 추천해줘")
                .build();
        String sessionId = "existing-session-456";
        String expectedResponse = "추가로 추천해드릴게요!";

        when(chatService.chatWithHistory("더 추천해줘", "", sessionId))
                .thenReturn(expectedResponse);

        // when
        ChatResponse result = useCase.execute(request, sessionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        verify(chatService).chatWithHistory("더 추천해줘", "", sessionId);
    }

    @Test
    @DisplayName("예외 처리 - ChatService 예외 발생 시 에러 응답 반환")
    void 예외_처리_ChatService_예외() {
        // given
        ChatRequest request = ChatRequest.builder()
                .message("서울 맛집 추천해줘")
                .build();
        String sessionId = "test-session-789";
        String errorMessage = "서비스 오류 발생";

        when(chatService.chatWithHistory(anyString(), anyString(), eq(sessionId)))
                .thenThrow(new RuntimeException(errorMessage));

        // when
        ChatResponse result = useCase.execute(request, sessionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("오류가 발생했습니다");
        assertThat(result.getMessage()).contains(errorMessage);
        assertThat(result.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("빈 메시지 처리 - 빈 문자열 처리")
    void 빈_메시지_처리_빈_문자열() {
        // given
        ChatRequest request = ChatRequest.builder()
                .message("")
                .build();
        String sessionId = "test-session-empty";
        String expectedResponse = "메시지를 입력해주세요.";

        when(chatService.chatWithHistory("", "", sessionId))
                .thenReturn(expectedResponse);

        // when
        ChatResponse result = useCase.execute(request, sessionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        verify(chatService).chatWithHistory("", "", sessionId);
    }

    @Test
    @DisplayName("execute 오버로드 - sessionId 없이 호출")
    void execute_오버로드_sessionId_없음() {
        // given
        ChatRequest request = ChatRequest.builder()
                .message("안녕하세요")
                .build();
        String expectedResponse = "안녕하세요!";

        when(chatService.chatWithHistory(anyString(), anyString(), anyString()))
                .thenReturn(expectedResponse);

        // when
        ChatResponse result = useCase.execute(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSessionId()).isNotNull();
        verify(chatService).chatWithHistory(eq("안녕하세요"), eq(""), anyString());
    }
}

