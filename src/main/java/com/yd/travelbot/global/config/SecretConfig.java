package com.yd.travelbot.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "langchain")
@Getter
@Setter
public class SecretConfig {
    private String geminiApiKey;
}

