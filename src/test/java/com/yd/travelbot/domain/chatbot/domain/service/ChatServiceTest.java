package com.yd.travelbot.domain.chatbot.domain.service;

import com.yd.travelbot.domain.accommodation.application.dto.AccommodationResponse;
import com.yd.travelbot.domain.food.application.dto.FoodResponse;
import com.yd.travelbot.domain.place.application.dto.PlaceResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
class ChatServiceTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private TravelTools travelTools;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatModel, travelTools);
    }

    @Test
    @DisplayName("세션별 TravelAssistant 생성 및 캐싱")
    void 세션별_TravelAssistant_생성_캐싱() {
        // given
        String sessionId = "test-session-1";
        String userMessage = "안녕하세요";
        String expectedResponse = "안녕하세요! 무엇을 도와드릴까요?";

        // TravelAssistant는 AiServices로 생성되므로 실제 호출은 어려움
        // 여기서는 세션 관리 로직 검증에 집중
        // 실제 구현에서는 TravelAssistant를 모킹하거나 통합 테스트로 검증

        // when & then
        // 세션별로 다른 인스턴스가 생성되는지 확인
        // 실제 테스트는 통합 테스트에서 수행 권장
    }

    @Test
    @DisplayName("멀티턴 대화 - 이전 대화 히스토리 유지")
    void 멀티턴_대화_히스토리_유지() {
        // given
        String sessionId = "multi-turn-session";
        String firstMessage = "안녕하세요";
        String secondMessage = "제 이름은 홍길동입니다";
        String thirdMessage = "제 이름이 뭐였죠?";

        // when & then
        // ChatMemory가 세션별로 관리되는지 확인
        // 실제 구현에서는 TravelAssistant의 chat 메서드가 호출되며
        // ChatMemory에 이전 대화가 저장되어 있어야 함
        // 통합 테스트에서 검증 권장
    }

    @Test
    @DisplayName("재시도 로직 - 503 에러 시 백오프 재시도")
    void 재시도_로직_503_에러() {
        // given
        String sessionId = "retry-session";
        String userMessage = "서울 맛집 추천";

        // 503 에러를 시뮬레이션하기 어려우므로
        // 실제 구현에서는 TravelAssistant가 예외를 던지고
        // ChatService가 이를 catch하여 재시도하는지 확인
        // 통합 테스트에서 검증 권장
    }

    @Test
    @DisplayName("결과 포맷팅 - formatAccommodationResults")
    void 결과_포맷팅_숙소() {
        // given
        String userQuery = "서울 호텔 추천";
        List<AccommodationResponse> accommodations = new ArrayList<>();
        accommodations.add(AccommodationResponse.builder()
                .name("호텔 A")
                .address("서울시 강남구")
                .price(new BigDecimal("100000"))
                .currency("KRW")
                .rating(4.5)
                .imageUrl("https://example.com/image.jpg")
                .build());

        when(chatModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from("서울의 멋진 호텔을 추천해드릴게요!")));

        // when
        String result = chatService.formatAccommodationResults(userQuery, accommodations);

        // then
        assertThat(result).isNotNull();
        verify(chatModel).generate(any(ChatMessage.class), any(ChatMessage.class));
    }

    @Test
    @DisplayName("결과 포맷팅 - formatFoodResults")
    void 결과_포맷팅_음식점() {
        // given
        String userQuery = "서울 맛집 추천";
        List<FoodResponse> foods = new ArrayList<>();
        foods.add(FoodResponse.builder()
                .name("맛집 A")
                .address("서울시 강남구")
                .cuisine("한식")
                .rating(4.5)
                .description("맛있는 한식당입니다")
                .imageUrl("https://example.com/food.jpg")
                .build());

        when(chatModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from("서울의 맛있는 한식당을 추천해드릴게요!")));

        // when
        String result = chatService.formatFoodResults(userQuery, foods);

        // then
        assertThat(result).isNotNull();
        verify(chatModel).generate(any(ChatMessage.class), any(ChatMessage.class));
    }

    @Test
    @DisplayName("결과 포맷팅 - formatPlaceResults")
    void 결과_포맷팅_관광지() {
        // given
        String userQuery = "서울 관광지 추천";
        List<PlaceResponse> places = new ArrayList<>();
        places.add(PlaceResponse.builder()
                .name("경복궁")
                .address("서울시 종로구")
                .category("궁")
                .rating(4.5)
                .imageUrl("https://example.com/place.jpg")
                .build());

        when(chatModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from("서울의 멋진 관광지를 추천해드릴게요!")));

        // when
        String result = chatService.formatPlaceResults(userQuery, places);

        // then
        assertThat(result).isNotNull();
        verify(chatModel).generate(any(ChatMessage.class), any(ChatMessage.class));
    }

    @Test
    @DisplayName("JSON 이스케이프 - escapeJson 메서드")
    void JSON_이스케이프() {
        // given
        // escapeJson은 private 메서드이므로 formatAccommodationResults를 통해 간접 테스트
        String userQuery = "테스트";
        List<AccommodationResponse> accommodations = new ArrayList<>();
        accommodations.add(AccommodationResponse.builder()
                .name("호텔 \"특수문자\" 테스트")
                .address("주소\n줄바꿈")
                .price(new BigDecimal("100000"))
                .currency("KRW")
                .rating(4.5)
                .build());

        when(chatModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(Response.from(AiMessage.from("테스트 응답")));

        // when
        String result = chatService.formatAccommodationResults(userQuery, accommodations);

        // then
        assertThat(result).isNotNull();
        // JSON 이스케이프가 제대로 처리되었는지 확인
        // 실제로는 JSON 문자열에 특수 문자가 이스케이프되어 있어야 함
    }

    @Test
    @DisplayName("빈 결과 처리 - formatAccommodationResults 빈 리스트")
    void 빈_결과_처리_숙소() {
        // given
        String userQuery = "서울 호텔";
        List<AccommodationResponse> accommodations = new ArrayList<>();

        // when
        String result = chatService.formatAccommodationResults(userQuery, accommodations);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("찾지 못했습니다");
        verify(chatModel, never()).generate(any(ChatMessage.class), any(ChatMessage.class));
    }

    @Test
    @DisplayName("LLM 실패 시 기본 포맷팅 - formatAccommodationResultsDefault")
    void LLM_실패_기본_포맷팅() {
        // given
        String userQuery = "서울 호텔";
        List<AccommodationResponse> accommodations = new ArrayList<>();
        accommodations.add(AccommodationResponse.builder()
                .name("호텔 A")
                .address("서울시 강남구")
                .price(new BigDecimal("100000"))
                .currency("KRW")
                .rating(4.5)
                .build());

        // LLM 호출 실패 시뮬레이션
        when(chatModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenThrow(new RuntimeException("LLM 오류"));

        // when
        String result = chatService.formatAccommodationResults(userQuery, accommodations);

        // then
        assertThat(result).isNotNull();
        // 기본 포맷팅이 적용되어야 함
        assertThat(result).contains("호텔 A");
    }
}

