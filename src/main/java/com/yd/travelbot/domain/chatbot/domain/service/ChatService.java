package com.yd.travelbot.domain.chatbot.domain.service;

import com.yd.travelbot.domain.accommodation.application.dto.AccommodationResponse;
import com.yd.travelbot.domain.food.application.dto.FoodResponse;
import com.yd.travelbot.domain.place.application.dto.PlaceResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatLanguageModel chatModel;
    private final TravelTools travelTools;
    
    // 세션별 TravelAssistant 인스턴스 (ChatMemory 포함)
    private final Map<String, TravelAssistant> assistantCache = new ConcurrentHashMap<>();

    /**
     * 기본 챗 (세션 없이)
     */
    public String chat(String userMessage) {
        return chatWithHistory(userMessage, "", null);
    }
    
    /**
     * 대화 히스토리를 포함한 챗 (멀티홉 추론 지원 - LangChain4j Tools 사용)
     */
    public String chatWithHistory(String userMessage, String conversationHistory, String sessionId) {
            // 세션 ID가 없으면 새로 생성
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = "default-" + System.currentTimeMillis();
            }
            
            // 세션별 TravelAssistant 가져오기 또는 생성 (ChatMemory 포함)
            TravelAssistant assistant = assistantCache.computeIfAbsent(sessionId, id -> {
                ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);
                return AiServices.builder(TravelAssistant.class)
                        .chatLanguageModel(chatModel)
                        .tools(travelTools)
                        .chatMemory(chatMemory)
                        .build();
            });
            
        // 재시도(백오프) 로직: 모델 과부하(503/UNAVAILABLE/overloaded) 시 최대 3회 재시도
        int maxRetries = 3;
        long[] backoffsMs = new long[]{400L, 800L, 1500L};

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
            return assistant.chat(userMessage);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean overload =
                        msg.contains("503")
                                || msg.toLowerCase().contains("unavailable")
                                || msg.toLowerCase().contains("overloaded");
                log.warn("LLM 호출 실패 (attempt {}/{}): {}", attempt + 1, maxRetries, msg);
                if (overload && attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(backoffsMs[Math.min(attempt, backoffsMs.length - 1)]);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                // 재시도 불가/최종 실패
                log.error("챗봇 응답 생성 실패: {}", msg, e);
                if (overload) {
                return "지금은 모델 사용량이 많아 응답을 생성하지 못했어요. 잠시 후 다시 시도해 주세요 🙏";
            }
            return "응답 생성 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.";
        }
        }
        // 이 지점에는 도달하지 않음
        return "응답 생성 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.";
    }

    /**
     * 숙소 검색 결과를 자연스러운 대화 형식으로 포맷팅합니다.
     */
    public String formatAccommodationResults(String userQuery, List<AccommodationResponse> accommodations) {
        if (accommodations.isEmpty()) {
            return "죄송합니다. 요청하신 조건에 맞는 숙소를 찾지 못했습니다. 다른 조건으로 검색해보시겠어요?";
        }

        String dataJson = accommodations.stream()
                .limit(10)
                .map(acc -> String.format("""
                    {
                        "name": "%s",
                        "address": "%s",
                        "price": "%s",
                        "currency": "%s",
                        "rating": %s,
                        "imageUrl": "%s"
                    }""", 
                    escapeJson(acc.getName()),
                    escapeJson(acc.getAddress() != null ? acc.getAddress() : ""),
                    acc.getPrice() != null ? acc.getPrice().toString() : "",
                    acc.getCurrency() != null ? acc.getCurrency() : "",
                    acc.getRating() != null ? acc.getRating() : "",
                    escapeJson(acc.getImageUrl() != null ? acc.getImageUrl() : ""))
                )
                .collect(Collectors.joining(",\n"));

        String prompt = String.format("""
            사용자가 "%s"라고 질문했습니다.
            
            다음은 검색된 숙소 정보입니다 (총 %d개, 최대 10개 표시):
            [
            %s
            ]
            
            이 정보를 바탕으로 친근하고 자연스러운 대화 형식으로 답변해주세요. 
            - 각 숙소의 이름, 주소, 가격, 평점을 포함하되 자연스럽게 소개해주세요.
            - 너무 딱딱한 리스트 형식이 아닌, 친구에게 추천하는 것처럼 말해주세요.
            - 이모지를 적절히 사용해주세요.
            - 최대 10개의 숙소를 모두 포함해주세요.
            - 가능한 경우 최소 5개 이상을 소개해주세요.
            - imageUrl 정보가 있는 경우, 각 항목 옆에 마크다운 이미지 형식(![이미지](URL))으로 포함해주세요.
            """, userQuery, accommodations.size(), dataJson);

        String llmResponse = generateFormattedResponse(prompt);
        if (llmResponse != null) {
            return llmResponse;
        }
        
        // LLM 실패 시 기본 포맷팅
        return formatAccommodationResultsDefault(accommodations);
    }
    
    private String formatAccommodationResultsDefault(List<AccommodationResponse> accommodations) {
        // 기본 포맷팅도 LLM을 통해 처리
        String dataJson = accommodations.stream()
                .limit(10)
                .map(acc -> String.format("""
                    {
                        "name": "%s",
                        "address": "%s",
                        "price": "%s",
                        "currency": "%s",
                        "rating": %s,
                        "imageUrl": "%s"
                    }""", 
                    escapeJson(acc.getName()),
                    escapeJson(acc.getAddress() != null ? acc.getAddress() : ""),
                    acc.getPrice() != null ? acc.getPrice().toString() : "",
                    acc.getCurrency() != null ? acc.getCurrency() : "",
                    acc.getRating() != null ? acc.getRating() : "",
                    escapeJson(acc.getImageUrl() != null ? acc.getImageUrl() : ""))
                )
                .collect(Collectors.joining(",\n"));
        
        String simplePrompt = String.format("""
            다음 숙소 정보를 친근하고 자연스러운 대화 형식으로 정리해주세요:
            [
            %s
            ]
            
            - 각 숙소의 이름, 주소, 가격, 평점을 포함하되 자연스럽게 소개해주세요.
            - 너무 딱딱한 리스트 형식이 아닌, 친구에게 추천하는 것처럼 말해주세요.
            - 이모지를 적절히 사용해주세요.
            - 가능한 경우 최소 5개 이상을 소개해주세요.
            - imageUrl 정보가 있는 경우, 각 항목 옆에 마크다운 이미지 형식(![이미지](URL))으로 포함해주세요.
            """, dataJson);
        
        String llmResponse = generateFormattedResponse(simplePrompt);
        if (llmResponse != null) {
            return llmResponse;
        }
        
        // 최종 fallback: 기본 포맷팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("🏨 숙소 검색 결과 (%d개):\n\n", accommodations.size()));
        
        int count = Math.min(accommodations.size(), 10);
        for (int i = 0; i < count; i++) {
            AccommodationResponse acc = accommodations.get(i);
            response.append(String.format("%d. %s\n", i + 1, acc.getName()));
            if (acc.getAddress() != null && !acc.getAddress().isEmpty()) {
                response.append(String.format("   📍 주소: %s\n", acc.getAddress()));
            }
            if (acc.getPrice() != null && acc.getCurrency() != null) {
                response.append(String.format("   💰 가격: %s %s/박\n", acc.getPrice(), acc.getCurrency()));
            }
            if (acc.getRating() != null) {
                response.append(String.format("   ⭐ 평점: %.1f/5.0\n", acc.getRating()));
            }
            if (acc.getImageUrl() != null && !acc.getImageUrl().isEmpty()) {
                response.append(String.format("   ![이미지](%s)\n", acc.getImageUrl()));
            }
            response.append("\n");
        }
        
        return response.toString();
    }

    /**
     * 음식점 검색 결과를 자연스러운 대화 형식으로 포맷팅합니다.
     */
    public String formatFoodResults(String userQuery, List<FoodResponse> foods) {
        if (foods.isEmpty()) {
            return "죄송합니다. 요청하신 조건에 맞는 음식점을 찾지 못했습니다. 다른 조건으로 검색해보시겠어요?";
        }

        String dataJson = foods.stream()
                .limit(10)
                .map(food -> String.format("""
                    {
                        "name": "%s",
                        "address": "%s",
                        "cuisine": "%s",
                        "rating": %s,
                        "description": "%s",
                        "imageUrl": "%s"
                    }""",
                    escapeJson(food.getName()),
                    escapeJson(food.getAddress() != null ? food.getAddress() : ""),
                    escapeJson(food.getCuisine() != null ? food.getCuisine() : ""),
                    food.getRating() != null ? food.getRating() : "",
                    escapeJson(food.getDescription() != null ? food.getDescription() : ""),
                    escapeJson(food.getImageUrl() != null ? food.getImageUrl() : ""))
                )
                .collect(Collectors.joining(",\n"));

        String prompt = String.format("""
            사용자가 "%s"라고 질문했습니다.
            
            다음은 검색된 음식점 정보입니다 (총 %d개, 최대 10개 표시):
            [
            %s
            ]
            
            이 정보를 바탕으로 친근하고 자연스러운 대화 형식으로 답변해주세요.
            - 각 음식점의 이름, 주소, 음식 종류, 평점을 포함하되 자연스럽게 소개해주세요.
            - description(설명) 정보가 있는 경우, 각 음식점의 특징이나 추천 이유를 포함하여 소개해주세요.
            - 너무 딱딱한 리스트 형식이 아닌, 친구에게 추천하는 것처럼 말해주세요.
            - 이모지를 적절히 사용해주세요.
            - 최대 10개의 음식점을 모두 포함해주세요.
            - 가능한 경우 최소 5개 이상을 소개해주세요.
            - imageUrl 정보가 있는 경우, 반드시 각 항목 옆에 마크다운 이미지 형식(![이미지](URL))으로 포함해주세요. 이미지가 있으면 반드시 표시해야 합니다.
            - 각 음식점마다 이름, 설명(있는 경우), 주소, 평점, 이미지(있는 경우)를 모두 포함하여 소개해주세요.
            """, userQuery, foods.size(), dataJson);

        String llmResponse = generateFormattedResponse(prompt);
        if (llmResponse != null) {
            return llmResponse;
        }
        
        // LLM 실패 시 기본 포맷팅
        return formatFoodResultsDefault(foods);
    }
    
    private String formatFoodResultsDefault(List<FoodResponse> foods) {
        // 기본 포맷팅도 LLM을 통해 처리
        String dataJson = foods.stream()
                .limit(10)
                .map(food -> String.format("""
                    {
                        "name": "%s",
                        "address": "%s",
                        "cuisine": "%s",
                        "rating": %s,
                        "description": "%s",
                        "imageUrl": "%s"
                    }""",
                    escapeJson(food.getName()),
                    escapeJson(food.getAddress() != null ? food.getAddress() : ""),
                    escapeJson(food.getCuisine() != null ? food.getCuisine() : ""),
                    food.getRating() != null ? food.getRating() : "",
                    escapeJson(food.getDescription() != null ? food.getDescription() : ""),
                    escapeJson(food.getImageUrl() != null ? food.getImageUrl() : ""))
                )
                .collect(Collectors.joining(",\n"));
        
        String simplePrompt = String.format("""
            다음 음식점 정보를 친근하고 자연스러운 대화 형식으로 정리해주세요:
            [
            %s
            ]
            
            - 각 음식점의 이름, 주소, 음식 종류, 평점을 포함하되 자연스럽게 소개해주세요.
            - description(설명) 정보가 있는 경우, 각 음식점의 특징이나 추천 이유를 포함하여 소개해주세요.
            - 너무 딱딱한 리스트 형식이 아닌, 친구에게 추천하는 것처럼 말해주세요.
            - 이모지를 적절히 사용해주세요.
            - 가능한 경우 최소 5개 이상을 소개해주세요.
            - imageUrl 정보가 있는 경우, 반드시 각 항목 옆에 마크다운 이미지 형식(![이미지](URL))으로 포함해주세요. 이미지가 있으면 반드시 표시해야 합니다.
            - 각 음식점마다 이름, 설명(있는 경우), 주소, 평점, 이미지(있는 경우)를 모두 포함하여 소개해주세요.
            """, dataJson);
        
        String llmResponse = generateFormattedResponse(simplePrompt);
        if (llmResponse != null) {
            return llmResponse;
        }
        
        // 최종 fallback: 기본 포맷팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("🍽️ 음식점 검색 결과 (%d개):\n\n", foods.size()));
        
        int count = Math.min(foods.size(), 10);
        for (int i = 0; i < count; i++) {
            FoodResponse food = foods.get(i);
            response.append(String.format("%d. %s\n", i + 1, food.getName()));
            if (food.getAddress() != null && !food.getAddress().isEmpty()) {
                response.append(String.format("   📍 주소: %s\n", food.getAddress()));
            }
            if (food.getCuisine() != null && !food.getCuisine().isEmpty()) {
                response.append(String.format("   🍜 음식 종류: %s\n", food.getCuisine()));
            }
            if (food.getRating() != null) {
                response.append(String.format("   ⭐ 평점: %.1f/5.0\n", food.getRating()));
            }
            if (food.getImageUrl() != null && !food.getImageUrl().isEmpty()) {
                response.append(String.format("   ![이미지](%s)\n", food.getImageUrl()));
            }
            response.append("\n");
        }
        
        return response.toString();
    }

    /**
     * 관광지 검색 결과를 자연스러운 대화 형식으로 포맷팅합니다.
     */
    public String formatPlaceResults(String userQuery, List<PlaceResponse> places) {
        if (places.isEmpty()) {
            return "죄송합니다. 요청하신 조건에 맞는 관광지를 찾지 못했습니다. 다른 조건으로 검색해보시겠어요?";
        }

        String dataJson = places.stream()
                .limit(10)
                .map(place -> String.format("""
                    {
                        "name": "%s",
                        "address": "%s",
                        "category": "%s",
                        "rating": %s,
                        "imageUrl": "%s"
                    }""",
                    escapeJson(place.getName()),
                    escapeJson(place.getAddress() != null ? place.getAddress() : ""),
                    escapeJson(place.getCategory() != null ? place.getCategory() : ""),
                    place.getRating() != null ? place.getRating() : "",
                    escapeJson(place.getImageUrl() != null ? place.getImageUrl() : ""))
                )
                .collect(Collectors.joining(",\n"));

        String prompt = String.format("""
            사용자가 "%s"라고 질문했습니다.
            
            다음은 검색된 관광지 정보입니다 (총 %d개, 최대 10개 표시):
            [
            %s
            ]
            
            이 정보를 바탕으로 친근하고 자연스러운 대화 형식으로 답변해주세요.
            - 각 관광지의 이름, 주소, 카테고리, 평점을 포함하되 자연스럽게 소개해주세요.
            - 너무 딱딱한 리스트 형식이 아닌, 친구에게 추천하는 것처럼 말해주세요.
            - 이모지를 적절히 사용해주세요.
            - 최대 10개의 관광지를 모두 포함해주세요.
            - 가능한 경우 최소 5개 이상을 소개해주세요.
            - imageUrl 정보가 있는 경우, 각 항목 옆에 마크다운 이미지 형식(![이미지](URL))으로 포함해주세요.
            """, userQuery, places.size(), dataJson);

        String llmResponse = generateFormattedResponse(prompt);
        if (llmResponse != null) {
            return llmResponse;
        }
        
        // LLM 실패 시 기본 포맷팅
        return formatPlaceResultsDefault(places);
    }
    
    private String formatPlaceResultsDefault(List<PlaceResponse> places) {
        // 기본 포맷팅도 LLM을 통해 처리
        String dataJson = places.stream()
                .limit(10)
                .map(place -> String.format("""
                    {
                        "name": "%s",
                        "address": "%s",
                        "category": "%s",
                        "rating": %s,
                        "imageUrl": "%s"
                    }""",
                    escapeJson(place.getName()),
                    escapeJson(place.getAddress() != null ? place.getAddress() : ""),
                    escapeJson(place.getCategory() != null ? place.getCategory() : ""),
                    place.getRating() != null ? place.getRating() : "",
                    escapeJson(place.getImageUrl() != null ? place.getImageUrl() : ""))
                )
                .collect(Collectors.joining(",\n"));
        
        String simplePrompt = String.format("""
            다음 관광지 정보를 친근하고 자연스러운 대화 형식으로 정리해주세요:
            [
            %s
            ]
            
            - 각 관광지의 이름, 주소, 카테고리, 평점을 포함하되 자연스럽게 소개해주세요.
            - 너무 딱딱한 리스트 형식이 아닌, 친구에게 추천하는 것처럼 말해주세요.
            - 이모지를 적절히 사용해주세요.
            - 가능한 경우 최소 5개 이상을 소개해주세요.
            - imageUrl 정보가 있는 경우, 각 항목 옆에 마크다운 이미지 형식(![이미지](URL))으로 포함해주세요.
            """, dataJson);
        
        String llmResponse = generateFormattedResponse(simplePrompt);
        if (llmResponse != null) {
            return llmResponse;
        }
        
        // 최종 fallback: 기본 포맷팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("🗺️ 관광지 검색 결과 (%d개):\n\n", places.size()));
        
        int count = Math.min(places.size(), 10);
        for (int i = 0; i < count; i++) {
            PlaceResponse place = places.get(i);
            response.append(String.format("%d. %s\n", i + 1, place.getName()));
            if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                response.append(String.format("   📍 주소: %s\n", place.getAddress()));
            }
            if (place.getCategory() != null && !place.getCategory().isEmpty()) {
                response.append(String.format("   🏷️ 카테고리: %s\n", place.getCategory()));
            }
            if (place.getRating() != null) {
                response.append(String.format("   ⭐ 평점: %.1f/5.0\n", place.getRating()));
            }
            if (place.getImageUrl() != null && !place.getImageUrl().isEmpty()) {
                response.append(String.format("   ![이미지](%s)\n", place.getImageUrl()));
            }
            response.append("\n");
        }
        
        return response.toString();
    }

    /**
     * LLM을 사용하여 포맷팅된 응답을 생성합니다.
     * timeout이 발생해도 재시도하여 최종적으로 LLM 응답을 받아옵니다.
     */
    private String generateFormattedResponse(String prompt) {
        int maxRetries = 3;
        int retryCount = 0;
        
        String systemPrompt = "당신은 친절하고 도움이 되는 여행 챗봇입니다. 모든 답변은 반드시 한국어로 작성해주세요.";
        
        while (retryCount < maxRetries) {
            try {
                SystemMessage systemMessage = SystemMessage.from(systemPrompt);
                UserMessage userMsg = UserMessage.from(prompt);
                
                Response<AiMessage> response = chatModel.generate(systemMessage, userMsg);
                return response.content().text();
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("LLM 포맷팅 실패 ({}회 재시도 후): {}", maxRetries, e.getMessage());
                    // 최종 실패 시에도 간단한 프롬프트로 재시도
                    try {
                        String simplePrompt = "다음 정보를 친근하고 자연스러운 대화 형식으로 정리해주세요:\n\n" + 
                                            prompt.substring(prompt.indexOf("다음은") > 0 ? prompt.indexOf("다음은") : 0);
                        SystemMessage systemMessage = SystemMessage.from(systemPrompt);
                        UserMessage userMsg = UserMessage.from(simplePrompt);
                        Response<AiMessage> response = chatModel.generate(systemMessage, userMsg);
                        return response.content().text();
                    } catch (Exception finalException) {
                        log.error("최종 LLM 포맷팅 실패: {}", finalException.getMessage());
                        return null;
                    }
                } else {
                    log.warn("LLM 포맷팅 실패 (재시도 {}/{}): {}", retryCount, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(1000 * retryCount); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * JSON 문자열에서 특수 문자를 이스케이프합니다.
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

