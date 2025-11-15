package com.yd.travelbot.domain.chatbot.ui;

import com.yd.travelbot.domain.chatbot.application.dto.request.ChatRequest;
import com.yd.travelbot.domain.chatbot.application.dto.response.ChatResponse;
import com.yd.travelbot.domain.chatbot.application.usecase.ProcessChatMessageUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ProcessChatMessageUseCase processChatMessageUseCase;

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody WebChatRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ChatRequest domainRequest = ChatRequest.builder()
                    .message(request.getMessage())
                    .build();

            // 세션 ID가 있으면 사용, 없으면 새로 생성
            String sessionId = request.getSessionId();
            ChatResponse chatResponse = processChatMessageUseCase.execute(domainRequest, sessionId);

            response.put("success", chatResponse.isSuccess());
            response.put("message", chatResponse.getMessage());
            response.put("sessionId", chatResponse.getSessionId());
        } catch (Exception e) {
            log.error("챗봇 처리 중 오류 발생: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "죄송합니다. 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    public static class WebChatRequest {
        private String message;
        private String sessionId;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}

