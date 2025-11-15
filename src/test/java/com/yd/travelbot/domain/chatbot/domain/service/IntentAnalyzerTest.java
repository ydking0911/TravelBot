package com.yd.travelbot.domain.chatbot.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yd.travelbot.domain.chatbot.domain.service.IntentAnalyzer.Intent;

@DisplayName("IntentAnalyzer 테스트")
class IntentAnalyzerTest {

    private IntentAnalyzer intentAnalyzer;

    @BeforeEach
    void setUp() {
        intentAnalyzer = new IntentAnalyzer();
    }

    @Test
    @DisplayName("숙소 의도 인식 - 한국어 키워드")
    void 숙소_의도_인식_한국어() {
        // given
        String[] inputs = {"숙소 추천해줘", "호텔 예약하고 싶어", "서울에 예약 가능한 곳"};

        // when & then
        for (String input : inputs) {
            Intent result = intentAnalyzer.analyze(input);
            assertThat(result).isEqualTo(Intent.ACCOMMODATION);
        }
    }

    @Test
    @DisplayName("숙소 의도 인식 - 영어 키워드")
    void 숙소_의도_인식_영어() {
        // given & when & then
        // 실제 구현에서는 "accommodation", "hotel", "booking" 키워드만 인식
        assertThat(intentAnalyzer.analyze("accommodation")).isEqualTo(Intent.ACCOMMODATION);
        assertThat(intentAnalyzer.analyze("hotel booking")).isEqualTo(Intent.ACCOMMODATION);
        assertThat(intentAnalyzer.analyze("book a room")).isEqualTo(Intent.GENERAL); // "book"은 키워드가 아님
    }

    @Test
    @DisplayName("음식 의도 인식 - 한국어 키워드")
    void 음식_의도_인식_한국어() {
        // given
        String[] inputs = {"맛집 추천", "음식점 찾아줘", "식당 어디 있나요", "레스토랑"};

        // when & then
        for (String input : inputs) {
            Intent result = intentAnalyzer.analyze(input);
            assertThat(result).isEqualTo(Intent.FOOD);
        }
    }

    @Test
    @DisplayName("음식 의도 인식 - 영어 키워드")
    void 음식_의도_인식_영어() {
        // given
        String[] inputs = {"food", "restaurant", "cuisine recommendation"};

        // when & then
        for (String input : inputs) {
            Intent result = intentAnalyzer.analyze(input);
            assertThat(result).isEqualTo(Intent.FOOD);
        }
    }

    @Test
    @DisplayName("관광지 의도 인식 - 한국어 키워드")
    void 관광지_의도_인식_한국어() {
        // given
        String[] inputs = {"관광지 추천", "명소 알려줘", "여행지 어디가 좋아"};

        // when & then
        for (String input : inputs) {
            Intent result = intentAnalyzer.analyze(input);
            assertThat(result).isEqualTo(Intent.PLACE);
        }
    }

    @Test
    @DisplayName("관광지 의도 인식 - 영어 키워드")
    void 관광지_의도_인식_영어() {
        // given
        String[] inputs = {"place", "attraction", "tourist spot"};

        // when & then
        for (String input : inputs) {
            Intent result = intentAnalyzer.analyze(input);
            assertThat(result).isEqualTo(Intent.PLACE);
        }
    }

    @Test
    @DisplayName("환율 의도 인식 - 한국어 키워드")
    void 환율_의도_인식_한국어() {
        // given
        String[] inputs = {"환율 알려줘", "통화 변환", "달러 환율"};

        // when & then
        for (String input : inputs) {
            Intent result = intentAnalyzer.analyze(input);
            assertThat(result).isEqualTo(Intent.CURRENCY);
        }
    }

    @Test
    @DisplayName("환율 의도 인식 - 영어 키워드")
    void 환율_의도_인식_영어() {
        // given
        String[] inputs = {"exchange rate", "currency convert", "convert USD"};

        // when & then
        for (String input : inputs) {
            Intent result = intentAnalyzer.analyze(input);
            assertThat(result).isEqualTo(Intent.CURRENCY);
        }
    }

    @Test
    @DisplayName("일반 대화 - 의도가 없는 경우")
    void 일반_대화_의도_없음() {
        // given
        String[] inputs = {"안녕하세요", "날씨가 좋네요", "고마워요"};

        // when & then
        for (String input : inputs) {
            Intent result = intentAnalyzer.analyze(input);
            assertThat(result).isEqualTo(Intent.GENERAL);
        }
    }

    @Test
    @DisplayName("대소문자 무시 - 대문자 키워드")
    void 대소문자_무시_대문자() {
        // given
        String input = "HOTEL BOOKING";

        // when
        Intent result = intentAnalyzer.analyze(input);

        // then
        assertThat(result).isEqualTo(Intent.ACCOMMODATION);
    }

    @Test
    @DisplayName("대소문자 무시 - 소문자 키워드")
    void 대소문자_무시_소문자() {
        // given
        String input = "restaurant recommendation";

        // when
        Intent result = intentAnalyzer.analyze(input);

        // then
        assertThat(result).isEqualTo(Intent.FOOD);
    }

    @Test
    @DisplayName("복합 키워드 - 여러 의도가 포함된 경우 첫 번째 매칭")
    void 복합_키워드_첫번째_매칭() {
        // given
        String input = "호텔과 맛집 추천해줘";

        // when
        Intent result = intentAnalyzer.analyze(input);

        // then
        // "호텔"이 먼저 매칭되므로 ACCOMMODATION 반환
        assertThat(result).isEqualTo(Intent.ACCOMMODATION);
    }
}

