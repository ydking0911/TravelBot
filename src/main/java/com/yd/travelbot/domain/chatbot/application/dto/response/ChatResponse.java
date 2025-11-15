package com.yd.travelbot.domain.chatbot.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private boolean success;
    private String sessionId;
    
    public String getMessage() {
        return message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getSessionId() {
        return sessionId;
    }
}

