package com.yd.travelbot.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "amadeus")
@Getter
@Setter
public class AmadeusConfig {
    private String apiKey;
    private String apiSecret;
}

