package com.yd.travelbot.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain.gemini.api-key:}")
    private String apiKey;

    @Value("${langchain.gemini.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${langchain.gemini.temperature:0.8}")
    private Double temperature;

    @Bean
    public ChatLanguageModel chatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }
}

