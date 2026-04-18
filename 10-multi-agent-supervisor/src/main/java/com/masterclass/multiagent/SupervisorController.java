package com.masterclass.multiagent;

import com.masterclass.shared.dto.AgentRequest;
import com.masterclass.shared.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/supervisor")
@Tag(name = "Multi-Agent Supervisor", description = "Module 10 – supervisor orchestrates research → analysis → writing")
@SecurityRequirement(name = "bearerAuth")
public class SupervisorController {

    private final SupervisorAgent supervisorAgent;

    public SupervisorController(SupervisorAgent supervisorAgent) {
        this.supervisorAgent = supervisorAgent;
    }

    @PostMapping("/process")
    @Operation(summary = "Submit a task to the multi-agent supervisor",
               description = """
                       The supervisor delegates to three specialist agents in sequence:
                       1. ResearchAgent — gathers facts
                       2. AnalysisAgent — performs SWOR analysis
                       3. WriterAgent — produces the final report

                       Example: "Write a report on the impact of AI on software engineering jobs."
                       Note: multi-agent requests are slower and more expensive than single-agent calls.
                       """)
    public ResponseEntity<AgentResponse> process(@Valid @RequestBody AgentRequest request) {
        return ResponseEntity.ok(AgentResponse.of(supervisorAgent.process(request.message())));
    }
}
