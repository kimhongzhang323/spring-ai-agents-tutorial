package com.masterclass.hello;

import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.shared.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hello")
@Tag(name = "Hello Agent", description = "Module 01 – simplest ChatClient-backed endpoint")
@SecurityRequirement(name = "bearerAuth")
public class HelloAgentController {

    private final HelloAgentService helloAgentService;

    public HelloAgentController(HelloAgentService helloAgentService) {
        this.helloAgentService = helloAgentService;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Chat with the hello agent",
            description = "Send a message and receive a response from the configured LLM. " +
                    "Rate-limited per user. Requires JWT bearer token."
    )
    public ResponseEntity<AgentResponse> chat(@Valid @RequestBody AgentRequest request) {
        String reply = helloAgentService.chat(request.message());
        return ResponseEntity.ok(AgentResponse.of(reply));
    }
}
