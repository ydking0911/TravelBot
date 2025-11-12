package com.yd.travelbot.domain.chatbot.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiIntentDetector 테스트")
class AiIntentDetectorTest {

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @InjectMocks
    private AiIntentDetector aiIntentDetector;

    @Test
    @DisplayName("의도 감지 성공 - ACCOMMODATION 의도")
    void 의도_감지_성공_숙소() {
        // given
        String userInput = "서울 호텔 예약하고 싶어요";
        String jsonResponse = """
                {
                  "intent": "ACCOMMODATION",
                  "confidence": 0.95,
                  "city": "서울",
                  "country": "한국",
                  "checkIn": "2024-12-25",
                  "checkOut": "2024-12-27",
                  "guests": 2,
                  "cuisine": "",
                  "category": "",
                  "amount": 0,
                  "fromCurrency": "",
                  "toCurrency": ""
                }
                """;

        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(response);

        // when
        AiIntentDetector.Result result = aiIntentDetector.detect(userInput);

        // then
        assertThat(result.intent).isEqualTo(IntentAnalyzer.Intent.ACCOMMODATION);
        assertThat(result.confidence).isEqualTo(0.95f);
        assertThat(result.city).isEqualTo("서울");
        assertThat(result.country).isEqualTo("한국");
        assertThat(result.checkIn).isEqualTo("2024-12-25");
        assertThat(result.checkOut).isEqualTo("2024-12-27");
        assertThat(result.guests).isEqualTo(2);
    }

    @Test
    @DisplayName("의도 감지 성공 - FOOD 의도")
    void 의도_감지_성공_음식() {
        // given
        String userInput = "부산 맛집 추천해줘";
        String jsonResponse = """
                {
                  "intent": "FOOD",
                  "confidence": 0.9,
                  "city": "부산",
                  "country": "한국",
                  "checkIn": "",
                  "checkOut": "",
                  "guests": 0,
                  "cuisine": "한식",
                  "category": "",
                  "amount": 0,
                  "fromCurrency": "",
                  "toCurrency": ""
                }
                """;

        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(response);

        // when
        AiIntentDetector.Result result = aiIntentDetector.detect(userInput);

        // then
        assertThat(result.intent).isEqualTo(IntentAnalyzer.Intent.FOOD);
        assertThat(result.confidence).isEqualTo(0.9f);
        assertThat(result.city).isEqualTo("부산");
        assertThat(result.cuisine).isEqualTo("한식");
    }

    @Test
    @DisplayName("의도 감지 성공 - CURRENCY 의도")
    void 의도_감지_성공_환율() {
        // given
        String userInput = "100 USD를 KRW로 변환해줘";
        String jsonResponse = """
                {
                  "intent": "CURRENCY",
                  "confidence": 0.98,
                  "city": "",
                  "country": "",
                  "checkIn": "",
                  "checkOut": "",
                  "guests": 0,
                  "cuisine": "",
                  "category": "",
                  "amount": 100,
                  "fromCurrency": "USD",
                  "toCurrency": "KRW"
                }
                """;

        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(response);

        // when
        AiIntentDetector.Result result = aiIntentDetector.detect(userInput);

        // then
        assertThat(result.intent).isEqualTo(IntentAnalyzer.Intent.CURRENCY);
        assertThat(result.confidence).isEqualTo(0.98f);
        assertThat(result.amount).isEqualTo(100.0);
        assertThat(result.fromCurrency).isEqualTo("USD");
        assertThat(result.toCurrency).isEqualTo("KRW");
    }

    @Test
    @DisplayName("JSON 정리 - 백틱과 코드펜스 제거")
    void JSON_정리_백틱_제거() {
        // given
        String userInput = "테스트";
        String jsonWithBackticks = "```json\n{\"intent\": \"GENERAL\", \"confidence\": 0.5}\n```";
        String jsonWithCodeFence = "```\n{\"intent\": \"GENERAL\", \"confidence\": 0.5}\n```";

        AiMessage aiMessage1 = AiMessage.from(jsonWithBackticks);
        Response<AiMessage> response1 = Response.from(aiMessage1);

        AiMessage aiMessage2 = AiMessage.from(jsonWithCodeFence);
        Response<AiMessage> response2 = Response.from(aiMessage2);

        when(chatLanguageModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(response1)
                .thenReturn(response2);

        // when
        AiIntentDetector.Result result1 = aiIntentDetector.detect(userInput);
        AiIntentDetector.Result result2 = aiIntentDetector.detect(userInput);

        // then
        assertThat(result1.intent).isEqualTo(IntentAnalyzer.Intent.GENERAL);
        assertThat(result1.confidence).isEqualTo(0.5f);
        assertThat(result2.intent).isEqualTo(IntentAnalyzer.Intent.GENERAL);
        assertThat(result2.confidence).isEqualTo(0.5f);
    }

    @Test
    @DisplayName("예외 처리 - LLM 호출 실패 시 기본값 반환")
    void 예외_처리_LLM_호출_실패() {
        // given
        String userInput = "테스트";
        when(chatLanguageModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenThrow(new RuntimeException("LLM 호출 실패"));

        // when
        AiIntentDetector.Result result = aiIntentDetector.detect(userInput);

        // then
        assertThat(result.intent).isEqualTo(IntentAnalyzer.Intent.GENERAL);
        assertThat(result.confidence).isEqualTo(0.0f);
        assertThat(result.city).isNull();
    }

    @Test
    @DisplayName("의도 매핑 - 알 수 없는 의도는 GENERAL로 매핑")
    void 의도_매핑_알수없는_의도() {
        // given
        String userInput = "테스트";
        String jsonResponse = """
                {
                  "intent": "UNKNOWN_INTENT",
                  "confidence": 0.5,
                  "city": "",
                  "country": "",
                  "checkIn": "",
                  "checkOut": "",
                  "guests": 0,
                  "cuisine": "",
                  "category": "",
                  "amount": 0,
                  "fromCurrency": "",
                  "toCurrency": ""
                }
                """;

        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(response);

        // when
        AiIntentDetector.Result result = aiIntentDetector.detect(userInput);

        // then
        assertThat(result.intent).isEqualTo(IntentAnalyzer.Intent.GENERAL);
    }

    @Test
    @DisplayName("JSON 파싱 - 빈 JSON 객체 처리")
    void JSON_파싱_빈_객체() {
        // given
        String userInput = "테스트";
        String jsonResponse = "{}";

        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);

        when(chatLanguageModel.generate(any(SystemMessage.class), any(UserMessage.class)))
                .thenReturn(response);

        // when
        AiIntentDetector.Result result = aiIntentDetector.detect(userInput);

        // then
        assertThat(result.intent).isEqualTo(IntentAnalyzer.Intent.GENERAL);
        assertThat(result.confidence).isEqualTo(0.0f);
        assertThat(result.city).isEmpty();
    }
}

