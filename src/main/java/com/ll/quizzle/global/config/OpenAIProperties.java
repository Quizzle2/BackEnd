package com.ll.quizzle.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "openai")
public class OpenAIProperties {
    private final String apiUrl;
    private final String model;
    private final String apiKey;

    @ConstructorBinding
    public OpenAIProperties(String apiUrl, String model, String apiKey) {
        this.apiUrl = apiUrl;
        this.model = model;
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getModel() {
        return model;
    }

    public String getApiKey() {
        return apiKey;
    }
}
