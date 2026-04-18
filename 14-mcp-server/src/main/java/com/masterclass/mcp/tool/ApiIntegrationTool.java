package com.masterclass.mcp.tool;

import com.masterclass.mcp.config.McpProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * MCP Tool: generic HTTP API integration.
 *
 * Protected by a Resilience4j circuit breaker so a flaky upstream API
 * cannot cascade into MCP server instability.
 *
 * Only allows GET and POST; never passes secrets via this tool.
 */
@Component
public class ApiIntegrationTool {

    private static final Logger log = LoggerFactory.getLogger(ApiIntegrationTool.class);

    private final WebClient webClient;
    private final Duration timeout;

    public ApiIntegrationTool(McpProperties props) {
        this.timeout = Duration.ofSeconds(props.api().timeoutSeconds());
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
    }

    @Tool(description = """
            Make an HTTP GET request to any public URL and return the response body.

            Use this tool to:
              - Fetch current data from public REST APIs (weather, exchange rates, news)
              - Check the status of a public endpoint
              - Download small text/JSON payloads

            Do NOT use for:
              - Endpoints that require authentication (use the database or file tools instead)
              - Large downloads (response is capped at 512 KB)
              - Non-HTTP protocols (ftp://, file://, etc.)

            Returns: the response body as a string, or an error message.
            """)
    @CircuitBreaker(name = "apiTool", fallbackMethod = "fallbackGet")
    public String httpGet(
            @ToolParam(description = "The full URL to GET, e.g. https://api.open-meteo.com/v1/forecast?...") String url) {
        log.debug("MCP API GET: {}", url);
        validateUrl(url);
        return webClient.method(HttpMethod.GET)
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .block();
    }

    @Tool(description = """
            Make an HTTP POST request to a public URL with a JSON body and return the response.

            Use this when the API requires a POST method (e.g. GraphQL endpoints, search APIs).
            The requestBody parameter must be valid JSON.

            Returns: the response body as a string, or an error message.
            """)
    @CircuitBreaker(name = "apiTool", fallbackMethod = "fallbackPost")
    public String httpPost(
            @ToolParam(description = "The full URL to POST to") String url,
            @ToolParam(description = "JSON request body as a string") String requestBody) {
        log.debug("MCP API POST: {}", url);
        validateUrl(url);
        return webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .block();
    }

    private void validateUrl(String url) {
        if (url == null || (!url.startsWith("https://") && !url.startsWith("http://"))) {
            throw new IllegalArgumentException("Only http:// and https:// URLs are allowed.");
        }
        // Block internal/private network access in production
        if (url.contains("localhost") || url.contains("127.0.0.1") || url.contains("169.254")) {
            throw new IllegalArgumentException("Access to internal addresses is not permitted.");
        }
    }

    public String fallbackGet(String url, Throwable t) {
        log.warn("ApiIntegrationTool GET circuit open for {}: {}", url, t.getMessage());
        return "{\"error\": \"The external API is temporarily unavailable. Please try again later.\"}";
    }

    public String fallbackPost(String url, String body, Throwable t) {
        log.warn("ApiIntegrationTool POST circuit open for {}: {}", url, t.getMessage());
        return "{\"error\": \"The external API is temporarily unavailable. Please try again later.\"}";
    }
}
