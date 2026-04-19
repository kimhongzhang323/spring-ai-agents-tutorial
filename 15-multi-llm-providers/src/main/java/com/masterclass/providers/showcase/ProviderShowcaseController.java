package com.masterclass.providers.showcase;

import com.masterclass.providers.tuning.ResponseConfig;
import com.masterclass.providers.tuning.ResponseTuningService;
import com.masterclass.providers.tuning.TemperatureComparisonResult;
import com.masterclass.providers.tuning.TuningResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Showcase endpoints that demonstrate:
 *   1. How to connect to each trending LLM provider (OpenAI, Anthropic, Gemini,
 *      Groq, DeepSeek, Together AI, Mistral, Perplexity, Ollama)
 *   2. How to configure AI response parameters at runtime (temperature, max tokens,
 *      top-p, top-k, stop sequences, system prompts)
 *
 * These endpoints are intentionally verbose and educational — production code
 * would collapse them into the existing /api/v1/providers/chat endpoint.
 */
@RestController
@RequestMapping("/api/v1/providers/showcase")
@Tag(name = "Provider Showcase", description = "Educational demos: connect providers, tune responses")
public class ProviderShowcaseController {

    private final ResponseTuningService tuningService;

    public ProviderShowcaseController(ResponseTuningService tuningService) {
        this.tuningService = tuningService;
    }

    // ── 1. Provider Connection Demos ─────────────────────────────────────────

    @PostMapping("/openai")
    @Operation(
        summary = "Demo: OpenAI GPT",
        description = """
            Connects to OpenAI using the Spring AI openai starter.
            Required env var: OPENAI_API_KEY
            Default model: gpt-4o-mini (change via spring.ai.openai.chat.options.model)
            Supports: function calling, vision, structured output, streaming.
            """)
    public ResponseEntity<ProviderDemoResponse> demoOpenAi(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        var result = tuningService.chat(request.prompt(), "openai",
                ResponseConfig.defaults());
        return ResponseEntity.ok(toDemo(result, "openai",
                "gpt-4o-mini", "spring-ai-openai-spring-boot-starter",
                "OPENAI_API_KEY"));
    }

    @PostMapping("/anthropic")
    @Operation(
        summary = "Demo: Anthropic Claude",
        description = """
            Connects to Anthropic Claude using the Spring AI anthropic starter.
            Required env var: ANTHROPIC_API_KEY
            Default model: claude-3-5-haiku-20241022
            Supports: extended thinking, vision, streaming, large context windows.
            """)
    public ResponseEntity<ProviderDemoResponse> demoAnthropic(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        var result = tuningService.chat(request.prompt(), "anthropic",
                ResponseConfig.defaults());
        return ResponseEntity.ok(toDemo(result, "anthropic",
                "claude-3-5-haiku-20241022", "spring-ai-anthropic-spring-boot-starter",
                "ANTHROPIC_API_KEY"));
    }

    @PostMapping("/groq")
    @Operation(
        summary = "Demo: Groq (ultra-fast inference)",
        description = """
            Groq runs open models (Llama, Mixtral) on custom LPU hardware — typically
            10-100x faster than GPU inference at a fraction of the cost.
            Uses the OpenAI-compatible API via a manually constructed OpenAiChatModel bean.
            Required env var: GROQ_API_KEY
            Default model: llama-3.1-70b-versatile
            No Spring AI starter needed — just spring-ai-openai.
            """)
    public ResponseEntity<ProviderDemoResponse> demoGroq(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        var result = tuningService.chat(request.prompt(), "groq",
                ResponseConfig.defaults());
        return ResponseEntity.ok(toDemo(result, "groq",
                "llama-3.1-70b-versatile", "spring-ai-openai (custom baseUrl)",
                "GROQ_API_KEY"));
    }

    @PostMapping("/deepseek")
    @Operation(
        summary = "Demo: DeepSeek (powerful reasoning at low cost)",
        description = """
            DeepSeek-V3/R1 offers frontier reasoning performance at a fraction of GPT-4o cost.
            Also uses the OpenAI-compatible endpoint — no extra dependency needed.
            Required env var: DEEPSEEK_API_KEY
            Default model: deepseek-chat (maps to DeepSeek-V3)
            Use deepseek-reasoner for chain-of-thought reasoning tasks.
            """)
    public ResponseEntity<ProviderDemoResponse> demoDeepSeek(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        var result = tuningService.chat(request.prompt(), "deepseek",
                ResponseConfig.defaults());
        return ResponseEntity.ok(toDemo(result, "deepseek",
                "deepseek-chat", "spring-ai-openai (custom baseUrl: api.deepseek.com)",
                "DEEPSEEK_API_KEY"));
    }

    @PostMapping("/together")
    @Operation(
        summary = "Demo: Together AI (100+ open models in the cloud)",
        description = """
            Together AI hosts Llama, Mistral, Qwen, DBRX, and 100+ open models.
            Useful when you want open-model flexibility without running Ollama locally.
            Required env var: TOGETHER_API_KEY
            Default model: meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo
            Uses the OpenAI-compatible endpoint.
            """)
    public ResponseEntity<ProviderDemoResponse> demoTogether(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        var result = tuningService.chat(request.prompt(), "together",
                ResponseConfig.defaults());
        return ResponseEntity.ok(toDemo(result, "together",
                "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo",
                "spring-ai-openai (custom baseUrl: api.together.xyz/v1)",
                "TOGETHER_API_KEY"));
    }

    @PostMapping("/perplexity")
    @Operation(
        summary = "Demo: Perplexity AI (web-grounded responses)",
        description = """
            Perplexity's sonar models search the web before answering, returning
            cited factual responses. Ideal for current-events queries.
            Required env var: PERPLEXITY_API_KEY
            Default model: sonar-pro
            Uses the OpenAI-compatible endpoint.
            """)
    public ResponseEntity<ProviderDemoResponse> demoPerplexity(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        var result = tuningService.chat(request.prompt(), "perplexity",
                ResponseConfig.defaults());
        return ResponseEntity.ok(toDemo(result, "perplexity",
                "sonar-pro", "spring-ai-openai (custom baseUrl: api.perplexity.ai)",
                "PERPLEXITY_API_KEY"));
    }

    @PostMapping("/ollama")
    @Operation(
        summary = "Demo: Ollama (local, private, no API key)",
        description = """
            Ollama runs open models locally on CPU or GPU.
            No API key, no data leaves your machine — ideal for private data.
            Required: Ollama running at http://localhost:11434 with a model pulled.
            Pull a model: ollama pull llama3.1
            """)
    public ResponseEntity<ProviderDemoResponse> demoOllama(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        var result = tuningService.chat(request.prompt(), "ollama",
                ResponseConfig.defaults());
        return ResponseEntity.ok(toDemo(result, "ollama",
                "llama3.1", "spring-ai-ollama-spring-boot-starter", "none required"));
    }

    // ── 2. Response Configuration Demos ──────────────────────────────────────

    @PostMapping("/tune")
    @Operation(
        summary = "Full response config: temperature, maxTokens, topP, topK, stop sequences",
        description = """
            Send any prompt with explicit control over every response parameter.
            Use this to understand how each knob affects output quality, diversity,
            and cost. All parameters are optional — omit to use provider defaults.

            temperature: 0.0 = deterministic, 0.7 = balanced, 1.0+ = creative
            maxTokens:   caps response length (1 token ≈ 0.75 English words)
            topP:        nucleus sampling (0.9 default); mutually exclusive with temperature
            topK:        limit to K most likely tokens per step (provider-dependent)
            stopAt:      stop generating when this string is output
            systemPrompt: override the default system prompt for this request only
            """)
    public ResponseEntity<TuningResult> tune(
            @Valid @RequestBody TuningRequest request,
            @AuthenticationPrincipal UserDetails user) {

        var config = new ResponseConfig(
                request.temperature(),
                request.maxTokens(),
                request.topP(),
                request.topK(),
                request.stopSequences(),
                request.systemPrompt()
        );
        return ResponseEntity.ok(tuningService.chat(request.prompt(), request.provider(), config));
    }

    @PostMapping("/compare-temperatures")
    @Operation(
        summary = "Compare same prompt at temperature 0.0, 0.5, and 1.0",
        description = """
            Runs the same prompt three times at different temperatures and returns
            all three responses side by side. This is the most direct way to
            understand the creativity vs. determinism trade-off.
            """)
    public ResponseEntity<TemperatureComparisonResult> compareTemperatures(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                tuningService.compareTemperatures(request.prompt(), request.provider()));
    }

    @PostMapping("/preset/factual")
    @Operation(
        summary = "Preset: factual (low temperature, short output)",
        description = "temperature=0.1, maxTokens=512 — good for classification, Q&A, extraction.")
    public ResponseEntity<TuningResult> factualPreset(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                tuningService.chat(request.prompt(), request.provider(), ResponseConfig.factual()));
    }

    @PostMapping("/preset/creative")
    @Operation(
        summary = "Preset: creative (high temperature, long output)",
        description = "temperature=1.0, maxTokens=2048 — good for stories, brainstorming, marketing copy.")
    public ResponseEntity<TuningResult> creativePreset(
            @Valid @RequestBody SimplePromptRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                tuningService.chat(request.prompt(), request.provider(), ResponseConfig.creative()));
    }

    @PostMapping("/stop-sequence")
    @Operation(
        summary = "Demo stop sequences: halt generation at a specific string",
        description = """
            Ask the model to produce a list or structured output, and stop at a
            known delimiter. Useful for extracting the first sentence, stopping
            before a section break, or cutting off after a JSON object closes.
            """)
    public ResponseEntity<TuningResult> stopSequenceDemo(
            @Valid @RequestBody StopSequenceRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                tuningService.chatWithStop(request.prompt(), request.stopAt(), request.provider()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ProviderDemoResponse toDemo(TuningResult result, String provider,
                                        String defaultModel, String springAiDependency,
                                        String requiredEnvVar) {
        return new ProviderDemoResponse(
                result.response(),
                provider,
                defaultModel,
                springAiDependency,
                requiredEnvVar,
                result.latencyMs()
        );
    }

    // ── Request / Response records ────────────────────────────────────────────

    public record SimplePromptRequest(
            @NotBlank @Size(max = 4000) String prompt,
            String provider
    ) {}

    public record TuningRequest(
            @NotBlank @Size(max = 4000) String prompt,
            String provider,
            @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
            @Min(1) @Max(8192) Integer maxTokens,
            @DecimalMin("0.0") @DecimalMax("1.0") Double topP,
            @Min(1) @Max(200) Integer topK,
            List<String> stopSequences,
            @Size(max = 1000) String systemPrompt
    ) {}

    public record StopSequenceRequest(
            @NotBlank @Size(max = 4000) String prompt,
            @NotBlank @Size(max = 50) String stopAt,
            String provider
    ) {}

    public record ProviderDemoResponse(
            String response,
            String provider,
            String defaultModel,
            String springAiDependency,
            String requiredEnvVar,
            long latencyMs
    ) {}
}
