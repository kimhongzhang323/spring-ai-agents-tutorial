package com.masterclass.llmbasics;

import com.masterclass.llmbasics.anthropic.AnthropicClient;
import com.masterclass.llmbasics.common.LlmResponse;
import com.masterclass.llmbasics.gemini.GeminiClient;
import com.masterclass.llmbasics.ollama.OllamaClient;
import com.masterclass.llmbasics.openai.OpenAiClient;
import com.masterclass.llmbasics.qwen.QwenClient;

/**
 * Entry point — run each provider demo in sequence.
 *
 * Run with:
 *   mvn exec:java -Dexec.mainClass=com.masterclass.llmbasics.Main
 *
 * Set environment variables before running:
 *   export OPENAI_API_KEY=sk-...
 *   export ANTHROPIC_API_KEY=sk-ant-...
 *   export DASHSCOPE_API_KEY=sk-...   (Qwen / Alibaba DashScope)
 *   export GEMINI_API_KEY=AIza...
 *   # Ollama needs no key — just: ollama pull llama3.2
 */
public class Main {

    private static final String QUESTION = "In one sentence, what is a large language model?";

    public static void main(String[] args) {
        System.out.println("=== LLM API Basics — Raw HTTP, No Framework ===");
        System.out.println("Question: " + QUESTION);
        System.out.println();

        runOpenAi();
        runAnthropic();
        runQwen();
        runGemini();
        runOllama();
    }

    private static void runOpenAi() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) { skip("OpenAI", "OPENAI_API_KEY"); return; }
        try {
            LlmResponse r = new OpenAiClient(key, "gpt-4o-mini").chat(QUESTION);
            System.out.println(r);
        } catch (Exception e) {
            System.err.println("[OpenAI ERROR] " + e.getMessage());
        }
    }

    private static void runAnthropic() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) { skip("Anthropic", "ANTHROPIC_API_KEY"); return; }
        try {
            LlmResponse r = new AnthropicClient(key, "claude-haiku-4-5-20251001").chat(QUESTION);
            System.out.println(r);
        } catch (Exception e) {
            System.err.println("[Anthropic ERROR] " + e.getMessage());
        }
    }

    private static void runQwen() {
        String key = System.getenv("DASHSCOPE_API_KEY");
        if (key == null || key.isBlank()) { skip("Qwen", "DASHSCOPE_API_KEY"); return; }
        try {
            LlmResponse r = new QwenClient(key, "qwen-turbo").chat(QUESTION);
            System.out.println(r);
        } catch (Exception e) {
            System.err.println("[Qwen ERROR] " + e.getMessage());
        }
    }

    private static void runGemini() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank()) { skip("Gemini", "GEMINI_API_KEY"); return; }
        try {
            LlmResponse r = new GeminiClient(key, "gemini-2.0-flash").chat(QUESTION);
            System.out.println(r);
        } catch (Exception e) {
            System.err.println("[Gemini ERROR] " + e.getMessage());
        }
    }

    private static void runOllama() {
        System.out.println("── Ollama (local — no key needed) ──");
        System.out.println("Tip: run `ollama pull llama3.2` first, then start `ollama serve`");
        try {
            LlmResponse r = new OllamaClient("llama3.2").chat(QUESTION);
            System.out.println(r);
        } catch (Exception e) {
            System.err.println("[Ollama] Not running or model missing: " + e.getMessage());
        }
    }

    private static void skip(String provider, String envVar) {
        System.out.printf("[SKIP] %s — set %s to enable%n%n", provider, envVar);
    }
}
