package com.masterclass.providers.router;

/**
 * Routing strategies for selecting an LLM provider.
 *
 * COST     — cheapest provider available (Groq > DeepSeek > Together > Mistral Small)
 * QUALITY  — most capable provider (GPT-4o > Claude 3.7 Sonnet > Gemini 2.0 Pro)
 * BALANCED — good quality at reasonable cost (Claude Haiku > GPT-4o-mini > Mistral Small)
 * LOCAL    — local Ollama only; no external API calls (private data)
 * RESEARCH — web-grounded answers (Perplexity > OpenAI > Anthropic)
 * EXPLICIT — caller specifies the provider name directly
 */
public enum RoutingStrategy {
    COST,
    QUALITY,
    BALANCED,
    LOCAL,
    RESEARCH,
    EXPLICIT
}
