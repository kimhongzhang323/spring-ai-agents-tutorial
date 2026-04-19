package com.masterclass.llmbasics.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Thin wrapper around java.net.http.HttpClient.
 * No framework magic — just a POST with JSON body and bearer auth.
 */
public class HttpHelper {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static String post(String url, Map<String, Object> body, String bearerToken) throws Exception {
        String json = MAPPER.writeValueAsString(body);

        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (bearerToken != null && !bearerToken.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }

        HttpResponse<String> response = CLIENT.send(requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP %d: %s".formatted(response.statusCode(), response.body()));
        }
        return response.body();
    }

    /** POST with an extra custom header (e.g. x-goog-api-key for Gemini). */
    public static String postWithHeader(String url, Map<String, Object> body,
                                        String headerName, String headerValue) throws Exception {
        String json = MAPPER.writeValueAsString(body);

        HttpResponse<String> response = CLIENT.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .header(headerName, headerValue)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP %d: %s".formatted(response.statusCode(), response.body()));
        }
        return response.body();
    }
}
