package com.masterclass.hello;

import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.shared.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/hello")
@Tag(name = "Hello Agent", description = "Module 01 — ChatClient basics: blocking and streaming responses")
@SecurityRequirement(name = "bearerAuth")
public class HelloAgentController {

    private final HelloAgentService helloAgentService;

    public HelloAgentController(HelloAgentService helloAgentService) {
        this.helloAgentService = helloAgentService;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Chat with the hello agent (blocking)",
            description = """
                    Sends a message and waits for the full response.
                    Rate-limited per user (see X-RateLimit-Remaining header).
                    Requires JWT bearer token.
                    """
    )
    public ResponseEntity<AgentResponse> chat(@Valid @RequestBody AgentRequest request) {
        String reply = helloAgentService.chat(request.message());
        return ResponseEntity.ok(AgentResponse.of(reply));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Chat with the hello agent (streaming SSE)",
            description = """
                    Returns the LLM response as a Server-Sent Events stream.
                    The browser can start rendering immediately as tokens arrive.
                    Use this pattern for long responses to avoid request timeouts.

                    Example with curl:
                      curl -N -X POST http://localhost:8080/api/v1/hello/stream \\
                        -H 'Authorization: Bearer <jwt>' \\
                        -H 'Content-Type: application/json' \\
                        -d '{"message":"Tell me a story"}'
                    """
    )
    public Flux<String> stream(@Valid @RequestBody AgentRequest request) {
        return helloAgentService.stream(request.message());
    }

    @GetMapping("/health")
    @Operation(summary = "LLM connectivity check", description = "Sends a minimal probe prompt to verify the LLM is reachable.")
    public ResponseEntity<AgentResponse> health() {
        String reply = helloAgentService.chat("Reply with exactly the word: OK");
        return ResponseEntity.ok(AgentResponse.of(reply));
    }
}
