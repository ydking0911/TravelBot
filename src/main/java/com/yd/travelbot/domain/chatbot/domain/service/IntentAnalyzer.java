package com.yd.travelbot.domain.chatbot.domain.service;

import org.springframework.stereotype.Component;

@Component
public class IntentAnalyzer {

    public Intent analyze(String userInput) {
        String lowerInput = userInput.toLowerCase();

        // 숙소 검색
        if (containsKeywords(lowerInput, "숙소", "호텔", "예약", "accommodation", "hotel", "booking")) {
            return Intent.ACCOMMODATION;
        }

        // 음식점 검색
        if (containsKeywords(lowerInput, "음식", "맛집", "식당", "레스토랑", "food", "restaurant", "cuisine")) {
            return Intent.FOOD;
        }

        // 관광지 검색
        if (containsKeywords(lowerInput, "관광지", "명소", "여행지", "place", "attraction", "tourist")) {
            return Intent.PLACE;
        }

        // 환율 변환
        if (containsKeywords(lowerInput, "환율", "변환", "exchange", "currency", "convert")) {
            return Intent.CURRENCY;
        }

        // 일반 대화
        return Intent.GENERAL;
    }

    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public enum Intent {
        ACCOMMODATION,
        FOOD,
        PLACE,
        CURRENCY,
        GENERAL
    }
}

