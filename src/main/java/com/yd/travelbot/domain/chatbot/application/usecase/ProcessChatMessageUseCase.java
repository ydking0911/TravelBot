package com.yd.travelbot.domain.chatbot.application.usecase;

import com.yd.travelbot.domain.chatbot.application.dto.request.ChatRequest;
import com.yd.travelbot.domain.chatbot.application.dto.response.ChatResponse;
import com.yd.travelbot.domain.chatbot.domain.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * LangChain4j Agents 파이프라인을 사용한 챗봇 메시지 처리
 * 
 * 모든 요청은 TravelAssistant (LangChain4j AiServices)로 라우팅되며,
 * TravelTools의 @Tool 메서드들이 자동으로 호출됩니다:
 * - searchAccommodation: 숙소 검색
 * - searchFood: 음식점 검색  
 * - searchPlace: 관광지 검색
 * - convertCurrency: 환율 변환
 * 
 * 하드코딩된 키워드 매칭이나 파싱 로직은 제거되었습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessChatMessageUseCase {

    private final ChatService chatService;

    public ChatResponse execute(ChatRequest request) {
        return execute(request, null);
    }
    
    /**
     * 모든 요청을 LangChain4j TravelAssistant로 라우팅
     * TravelTools의 @Tool 메서드들이 자동으로 호출되어 API를 실행합니다.
     */
    public ChatResponse execute(ChatRequest request, String sessionId) {
        try {
            String userInput = request.getMessage();
            
            // 세션 ID가 없으면 새로 생성
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
            
            // LangChain4j TravelAssistant로 모든 요청 처리
            // - TravelTools의 @Tool 메서드들이 자동으로 호출됨
            // - 멀티홉 추론 지원 (예: "제주도 관광지와 맛집 추천" → searchPlace + searchFood 자동 호출)
            // - ChatMemory가 세션별로 자동 관리됨
            String response = chatService.chatWithHistory(userInput, "", sessionId);

            return ChatResponse.builder()
                    .message(response)
                    .success(true)
                    .sessionId(sessionId)
                    .build();
        } catch (Exception e) {
            log.error("챗봇 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .message("죄송합니다. 오류가 발생했습니다: " + e.getMessage())
                    .success(false)
                    .sessionId(sessionId)
                    .build();
        }
    }
}

