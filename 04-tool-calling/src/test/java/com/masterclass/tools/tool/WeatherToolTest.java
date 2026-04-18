package com.masterclass.tools.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherToolTest {

    private final WeatherTool weatherTool = new WeatherTool();

    @Test
    void returnsWeatherForKnownCity() {
        var result = weatherTool.getCurrentWeather("London");
        assertThat(result.city()).isEqualTo("London");
        assertThat(result.temperatureCelsius()).isGreaterThan(0);
        assertThat(result.description()).isNotBlank();
    }

    @Test
    void returnsGracefulMessageForUnknownCity() {
        var result = weatherTool.getCurrentWeather("Atlantis");
        assertThat(result.description()).containsIgnoringCase("unavailable");
    }

    @Test
    void fallbackReturnsUsableResponse() {
        var result = weatherTool.weatherFallback("Tokyo", new RuntimeException("timeout"));
        assertThat(result.city()).isEqualTo("Tokyo");
        assertThat(result.description()).containsIgnoringCase("unavailable");
    }
}
