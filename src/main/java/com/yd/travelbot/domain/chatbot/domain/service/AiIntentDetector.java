package com.yd.travelbot.domain.chatbot.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yd.travelbot.global.util.JsonUtil;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiIntentDetector {

    private final ChatLanguageModel chatLanguageModel;

    public Result detect(String userInput) {
        String system = """
            You are an intent and entity classifier for a travel assistant.
            Respond ONLY in strict JSON. No extra text. DO NOT wrap in code fences. DO NOT use backticks.
            Schema:
            {
              "intent": "ACCOMMODATION|FOOD|PLACE|CURRENCY|GENERAL",
              "confidence": 0.0-1.0,
              "city": "string or empty",
              "country": "string or empty",
              "checkIn": "YYYY-MM-DD or empty",
              "checkOut": "YYYY-MM-DD or empty",
              "guests": number or 0,
              "cuisine": "string or empty",
              "category": "string or empty",
              "amount": number or 0,
              "fromCurrency": "ISO code or empty",
              "toCurrency": "ISO code or empty"
            }
            """;
        String user = "Text: " + userInput + "\nReturn JSON only.";
        try {
            Response<dev.langchain4j.data.message.AiMessage> res =
                chatLanguageModel.generate(SystemMessage.from(system), UserMessage.from(user));
            String raw = res.content().text();
            String json = sanitizeToJson(raw);
            JsonNode node = JsonUtil.fromJson(json, JsonNode.class);
            Result r = new Result();
            r.intent = mapIntent(node.path("intent").asText("GENERAL"));
            r.confidence = (float) node.path("confidence").asDouble(0.0);
            r.city = node.path("city").asText("");
            r.country = node.path("country").asText("");
            r.checkIn = node.path("checkIn").asText("");
            r.checkOut = node.path("checkOut").asText("");
            r.guests = node.path("guests").asInt(0);
            r.cuisine = node.path("cuisine").asText("");
            r.category = node.path("category").asText("");
            r.amount = node.path("amount").asDouble(0.0);
            r.fromCurrency = node.path("fromCurrency").asText("");
            r.toCurrency = node.path("toCurrency").asText("");
            return r;
        } catch (Exception e) {
            log.warn("AI intent detection failed: {}", e.getMessage());
            Result r = new Result();
            r.intent = IntentAnalyzer.Intent.GENERAL;
            r.confidence = 0.0f;
            return r;
        }
    }

    private String sanitizeToJson(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        // 제거: 백틱, 코드펜스
        if (s.startsWith("```") && s.endsWith("```")) {
            s = s.substring(3, s.length() - 3).trim();
        }
        while (s.startsWith("`")) s = s.substring(1).trim();
        while (s.endsWith("`")) s = s.substring(0, s.length() - 1).trim();
        // JSON 블록만 추출
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

    private IntentAnalyzer.Intent mapIntent(String s) {
        try {
            return IntentAnalyzer.Intent.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return IntentAnalyzer.Intent.GENERAL;
        }
    }

    public static class Result {
        public IntentAnalyzer.Intent intent;
        public float confidence;
        public String city;
        public String country;
        public String checkIn;
        public String checkOut;
        public int guests;
        public String cuisine;
        public String category;
        public double amount;
        public String fromCurrency;
        public String toCurrency;
    }
}

