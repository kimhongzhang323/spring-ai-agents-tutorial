package com.masterclass.providers.router;

/**
 * Routing strategies for selecting an LLM provider.
 *
 * COST     — cheapest provider available (Groq > Mistral Small > GPT-4o-mini)
 * QUALITY  — most capable provider (GPT-4o > Claude 3.5 Sonnet > Gemini 1.5 Pro)
 * BALANCED — good quality at reasonable cost (Claude Haiku > GPT-4o-mini > Mistral Small)
 * LOCAL    — local Ollama only; no external API calls (private data)
 * EXPLICIT — caller specifies the provider name directly
 */
public enum RoutingStrategy {
    COST,
    QUALITY,
    BALANCED,
    LOCAL,
    EXPLICIT
}
