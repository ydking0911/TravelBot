package com.yd.travelbot.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "exchange-rates")
@Getter
@Setter
public class ExchangeRatesConfig {
    private String apiKey;
}

