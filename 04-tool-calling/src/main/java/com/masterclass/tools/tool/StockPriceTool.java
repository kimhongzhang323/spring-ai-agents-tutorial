package com.masterclass.tools.tool;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stubbed stock price tool. In production: wire to Yahoo Finance, Alpha Vantage, or Polygon.io.
 * Demonstrates circuit breaker protecting an external financial data API.
 */
@Component
public class StockPriceTool {

    private static final Logger log = LoggerFactory.getLogger(StockPriceTool.class);

    private static final Map<String, StockQuote> STUB_PRICES = Map.of(
            "AAPL",  new StockQuote("AAPL",  "Apple Inc.",        182.50, 1.23,  "USD"),
            "GOOGL", new StockQuote("GOOGL", "Alphabet Inc.",    140.25, -0.45, "USD"),
            "MSFT",  new StockQuote("MSFT",  "Microsoft Corp.",  415.80, 2.10,  "USD"),
            "AMZN",  new StockQuote("AMZN",  "Amazon.com Inc.",  185.90, 0.75,  "USD"),
            "NVDA",  new StockQuote("NVDA",  "NVIDIA Corp.",     875.30, 15.40, "USD")
    );

    @Tool(description = """
            Look up the current stock price and daily change for a publicly traded company.
            Use this when the user asks about a stock price, market value, or share price.
            Input: ticker symbol as an uppercase string (e.g. "AAPL" for Apple, "MSFT" for Microsoft, "GOOGL" for Google).
            Returns: company name, current price, daily change, and currency.
            Only works for major US stocks. If the symbol is not found, returns an error message.
            """)
    @CircuitBreaker(name = "externalApiCircuitBreaker", fallbackMethod = "stockFallback")
    @Bulkhead(name = "externalApiBulkhead", type = Bulkhead.Type.SEMAPHORE)
    public StockQuote getStockPrice(String ticker) {
        log.debug("Looking up stock price for ticker: {}", ticker);
        String symbol = ticker.toUpperCase().trim();
        return STUB_PRICES.getOrDefault(symbol,
                new StockQuote(symbol, "Unknown — ticker not found in stub data", 0, 0, "N/A"));
    }

    public StockQuote stockFallback(String ticker, Exception ex) {
        log.warn("Stock API circuit open for {}: {}", ticker, ex.getMessage());
        return new StockQuote(ticker, "Stock data service temporarily unavailable", 0, 0, "N/A");
    }

    public record StockQuote(
            String ticker,
            String companyName,
            double price,
            double dailyChange,
            String currency
    ) {}
}
