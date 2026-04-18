package com.masterclass.observability;

import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.shared.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/observe")
@Tag(name = "Observability", description = "Module 08 – OTel tracing, Micrometer metrics, Grafana dashboards")
@SecurityRequirement(name = "bearerAuth")
public class ObservabilityController {

    private final AgentObservationService agentService;

    public ObservabilityController(AgentObservationService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with full observability",
               description = "Every request emits an OTel span visible in Jaeger and Prometheus metrics visible in Grafana.")
    public ResponseEntity<AgentResponse> chat(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(AgentResponse.of(agentService.chat(user.getUsername(), request.message())));
    }
}
