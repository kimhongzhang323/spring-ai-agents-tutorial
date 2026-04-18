package com.masterclass.tools.tool;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool rules (enforced by .claude/rules/coding-standards.md):
 * 1. @Tool description must be written for the LLM — explain WHEN to call it, not HOW it works.
 * 2. Tool methods call services — they never contain business logic themselves.
 * 3. Every external call is protected by a circuit breaker and bulkhead.
 * 4. Return types must be simple strings or records — the LLM cannot handle complex Java types.
 */
@Component
public class WeatherTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    @Tool(description = """
            Get the current weather conditions for a specific city.
            Use this tool when the user asks about the weather, temperature, conditions, or forecast for a location.
            Returns current temperature in Celsius, weather description, humidity percentage, and wind speed in km/h.
            Input: city name as a string (e.g. "London", "Tokyo", "New York").
            """)
    @CircuitBreaker(name = "externalApiCircuitBreaker", fallbackMethod = "weatherFallback")
    @Bulkhead(name = "externalApiBulkhead", type = Bulkhead.Type.SEMAPHORE)
    public WeatherResult getCurrentWeather(String city) {
        log.debug("Fetching weather for city: {}", city);
        // In a real implementation this calls a weather API (OpenWeatherMap, WeatherAPI, etc.)
        // Stubbed here to keep the module runnable without external API keys
        return switch (city.toLowerCase()) {
            case "london"   -> new WeatherResult(city, 12, "Overcast", 78, 18);
            case "tokyo"    -> new WeatherResult(city, 24, "Partly cloudy", 65, 12);
            case "new york" -> new WeatherResult(city, 18, "Sunny", 55, 22);
            case "sydney"   -> new WeatherResult(city, 28, "Clear sky", 60, 15);
            default         -> new WeatherResult(city, 20, "Data unavailable for this city — try a major city", 50, 10);
        };
    }

    public WeatherResult weatherFallback(String city, Exception ex) {
        log.warn("Weather API circuit open for city {}: {}", city, ex.getMessage());
        return new WeatherResult(city, 0, "Weather service temporarily unavailable", 0, 0);
    }

    public record WeatherResult(
            String city,
            int temperatureCelsius,
            String description,
            int humidityPercent,
            int windSpeedKmh
    ) {}
}
