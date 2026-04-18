package com.masterclass.providers;

import com.masterclass.providers.benchmark.BenchmarkReport;
import com.masterclass.providers.benchmark.ProviderBenchmarkService;
import com.masterclass.providers.fallback.FallbackChainService;
import com.masterclass.providers.fallback.FallbackResult;
import com.masterclass.providers.router.ProviderRouter;
import com.masterclass.providers.router.RoutingStrategy;
import com.masterclass.providers.streaming.StreamingService;
import com.masterclass.shared.guardrails.InputValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Multi-Provider API", description = "Route requests to OpenAI, Anthropic, Gemini, Groq, Mistral, and more")
public class ProviderController {

    private final ProviderRouter router;
    private final FallbackChainService fallbackChain;
    private final ProviderBenchmarkService benchmarkService;
    private final StreamingService streamingService;
    private final InputValidator inputValidator;

    public ProviderController(ProviderRouter router,
                              FallbackChainService fallbackChain,
                              ProviderBenchmarkService benchmarkService,
                              StreamingService streamingService,
                              InputValidator inputValidator) {
        this.router = router;
        this.fallbackChain = fallbackChain;
        this.benchmarkService = benchmarkService;
        this.streamingService = streamingService;
        this.inputValidator = inputValidator;
    }

    @GetMapping("/available")
    @Operation(summary = "List all configured providers", description = "Returns provider names whose API keys are set")
    public ResponseEntity<Set<String>> availableProviders() {
        return ResponseEntity.ok(router.availableProviders());
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with a selected LLM provider",
               description = "Routes to a provider based on strategy (COST, QUALITY, BALANCED, LOCAL, EXPLICIT)")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails user) {

        var validation = inputValidator.validate(request.message());
        if (!validation.valid()) {
            return ResponseEntity.badRequest().body(new ChatResponse(null, null, validation.reason(), null));
        }

        RoutingStrategy strategy = request.strategy() != null
                ? RoutingStrategy.valueOf(request.strategy().toUpperCase())
                : RoutingStrategy.BALANCED;

        String providerUsed = router.selectProviderName(strategy, request.provider());
        String response = router.select(strategy, request.provider())
                .prompt()
                .user(request.message())
                .call()
                .content();

        return ResponseEntity.ok(new ChatResponse(response, providerUsed, null, strategy.name()));
    }

    @PostMapping("/chat/fallback")
    @Operation(summary = "Chat with automatic fallback",
               description = "Tries providers in priority order; returns first successful response")
    public ResponseEntity<FallbackResponse> chatWithFallback(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails user) {

        var validation = inputValidator.validate(request.message());
        if (!validation.valid()) {
            return ResponseEntity.badRequest().build();
        }

        RoutingStrategy strategy = request.strategy() != null
                ? RoutingStrategy.valueOf(request.strategy().toUpperCase())
                : RoutingStrategy.BALANCED;

        FallbackResult result = fallbackChain.execute(request.message(), strategy);
        return ResponseEntity.ok(new FallbackResponse(
                result.response(),
                result.providerUsed(),
                result.attemptsNeeded(),
                result.succeeded()
        ));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream a response via SSE",
               description = "Returns Server-Sent Events — suitable for progressive rendering in the browser")
    public Flux<String> stream(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserDetails user) {

        var validation = inputValidator.validate(request.message());
        if (!validation.valid()) {
            return Flux.just("Error: " + validation.reason());
        }

        RoutingStrategy strategy = request.strategy() != null
                ? RoutingStrategy.valueOf(request.strategy().toUpperCase())
                : RoutingStrategy.BALANCED;

        return streamingService.stream(request.message(), strategy, request.provider());
    }

    @PostMapping("/benchmark")
    @Operation(summary = "Benchmark multiple providers",
               description = "Sends the same prompt to all specified providers in parallel and compares latency and responses")
    public ResponseEntity<BenchmarkReport> benchmark(
            @Valid @RequestBody BenchmarkRequest request,
            @AuthenticationPrincipal UserDetails user) {

        var validation = inputValidator.validate(request.prompt());
        if (!validation.valid()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                benchmarkService.benchmark(request.prompt(),
                        request.providers() != null ? request.providers() : List.of()));
    }

    // ── Request / Response records ───────────────────────────────────────────

    public record ChatRequest(
            @NotBlank @Size(max = 4000) String message,
            String strategy,
            String provider
    ) {}

    public record ChatResponse(
            String response,
            String providerUsed,
            String error,
            String strategyApplied
    ) {}

    public record FallbackResponse(
            String response,
            String providerUsed,
            int attemptsNeeded,
            boolean succeeded
    ) {}

    public record BenchmarkRequest(
            @NotBlank @Size(max = 1000) String prompt,
            List<String> providers
    ) {}
}
