package com.masterclass.lc4jagentic;

import com.masterclass.shared.dto.AgentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agentic")
@Tag(name = "LangChain4j Agentic", description = "Sequential Research→Critic→Synthesizer workflow")
@SecurityRequirement(name = "bearerAuth")
public class AgenticController {

    private final ResearchWorkflow workflow;

    public AgenticController(ResearchWorkflow workflow) {
        this.workflow = workflow;
    }

    @PostMapping("/research")
    @Operation(summary = "Run the 3-step research workflow on a topic",
               description = "Invokes Research → Critic → Synthesizer agents sequentially. Expect 3 LLM calls.")
    public ResponseEntity<WorkflowResult> research(
            @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        WorkflowResult result = workflow.run(request.message());
        return ResponseEntity.ok(result);
    }
}
