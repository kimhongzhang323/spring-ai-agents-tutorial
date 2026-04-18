package com.masterclass.tools.tool;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stubbed currency conversion. In production: wire to Open Exchange Rates or Fixer.io.
 * Demonstrates a tool that composes data from two lookups (rate table lookup + arithmetic).
 */
@Component
public class CurrencyTool {

    // Stub rates relative to USD
    private static final Map<String, Double> USD_RATES = Map.of(
            "USD", 1.0,
            "EUR", 0.92,
            "GBP", 0.79,
            "JPY", 149.50,
            "CAD", 1.36,
            "AUD", 1.53,
            "CHF", 0.90,
            "CNY", 7.24
    );

    @Tool(description = """
            Convert an amount of money from one currency to another using today's approximate exchange rates.
            Use this when the user asks to convert currency, or asks "how much is X in Y currency".
            Inputs: amount (numeric), fromCurrency (ISO code, e.g. "USD"), toCurrency (ISO code, e.g. "EUR").
            Returns: converted amount, exchange rate used, and both currency codes.
            Supported currencies: USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY.
            """)
    @CircuitBreaker(name = "externalApiCircuitBreaker", fallbackMethod = "conversionFallback")
    public ConversionResult convert(double amount, String fromCurrency, String toCurrency) {
        String from = fromCurrency.toUpperCase();
        String to   = toCurrency.toUpperCase();

        if (!USD_RATES.containsKey(from)) {
            return new ConversionResult(0, 0, from, to, "Unsupported currency: " + from);
        }
        if (!USD_RATES.containsKey(to)) {
            return new ConversionResult(0, 0, from, to, "Unsupported currency: " + to);
        }

        double inUsd       = amount / USD_RATES.get(from);
        double converted   = inUsd * USD_RATES.get(to);
        double rate        = USD_RATES.get(to) / USD_RATES.get(from);

        return new ConversionResult(
                Math.round(converted * 100.0) / 100.0,
                Math.round(rate * 10000.0) / 10000.0,
                from, to, null
        );
    }

    public ConversionResult conversionFallback(double amount, String from, String to, Exception ex) {
        return new ConversionResult(0, 0, from, to, "Currency service temporarily unavailable");
    }

    public record ConversionResult(
            double convertedAmount,
            double exchangeRate,
            String fromCurrency,
            String toCurrency,
            String error
    ) {}
}
