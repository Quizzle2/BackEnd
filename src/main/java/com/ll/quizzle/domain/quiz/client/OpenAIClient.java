package com.ll.quizzle.domain.quiz.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.quizzle.global.config.OpenAIProperties;
import com.ll.quizzle.global.exceptions.ErrorCode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class OpenAIClient {

    private final OpenAIProperties openAIProperties;
    private final String apiUrl;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAIClient(OpenAIProperties openAIProperties) {
        this.openAIProperties = openAIProperties;
        this.apiUrl = openAIProperties.getApiUrl();
        this.model = openAIProperties.getModel();
        this.objectMapper = new ObjectMapper();
    }

    public String sendRequest(String systemPrompt, String userMessage) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.7
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            ErrorCode.INTERNAL_SERVER_ERROR.throwServiceException(e);
            return null; // Unreachable
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openAIProperties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                ErrorCode.INTERNAL_SERVER_ERROR.throwServiceException(
                        new RuntimeException("OpenAI API returned status " + response.statusCode()));
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            ErrorCode.INTERNAL_SERVER_ERROR.throwServiceException(e);
            return null; // Unreachable
        }
    }
}
