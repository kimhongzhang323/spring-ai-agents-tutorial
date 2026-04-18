package com.masterclass.tools.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CurrencyToolTest {

    private final CurrencyTool currencyTool = new CurrencyTool();

    @Test
    void convertsSameCurrency() {
        var result = currencyTool.convert(100, "USD", "USD");
        assertThat(result.error()).isNull();
        assertThat(result.convertedAmount()).isCloseTo(100.0, within(0.01));
    }

    @Test
    void convertsUsdToEur() {
        var result = currencyTool.convert(100, "USD", "EUR");
        assertThat(result.error()).isNull();
        assertThat(result.convertedAmount()).isGreaterThan(0);
        assertThat(result.exchangeRate()).isLessThan(1.0); // EUR is worth more than USD
    }

    @Test
    void unsupportedCurrencyReturnsError() {
        var result = currencyTool.convert(100, "USD", "XYZ");
        assertThat(result.error()).containsIgnoringCase("Unsupported");
    }

    @Test
    void fallbackReturnsErrorMessage() {
        var result = currencyTool.conversionFallback(100, "USD", "EUR", new RuntimeException("circuit open"));
        assertThat(result.error()).containsIgnoringCase("unavailable");
    }
}
