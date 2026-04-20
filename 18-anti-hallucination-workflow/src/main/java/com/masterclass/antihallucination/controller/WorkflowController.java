package com.masterclass.antihallucination.controller;

import com.masterclass.antihallucination.domain.WorkflowDef;
import com.masterclass.antihallucination.domain.WorkflowResult;
import com.masterclass.antihallucination.domain.StepResult;
import com.masterclass.antihallucination.workflow.WorkflowEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Accepts a workflow definition and streams step-by-step results as Server-Sent Events.
 *
 * Authentication: JWT bearer (JwtAuthFilter from shared/)
 * Rate limiting:  Bucket4j per-user (Bucket4jConfig from shared/)
 * Observability:  OpenTelemetry auto-instrumented; metrics via Micrometer
 */
@RestController
@RequestMapping("/api/v1/workflow")
@Tag(name = "Workflow", description = "Anti-hallucination workflow execution with SSE streaming")
@SecurityRequirement(name = "bearerAuth")
public class WorkflowController {

    private final WorkflowEngine workflowEngine;

    public WorkflowController(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    /**
     * Submits a workflow definition and streams each step result plus a final summary.
     *
     * Response format: text/event-stream
     *   - event: step  — one per completed step ({@link StepResult})
     *   - event: done  — final {@link WorkflowResult}
     */
    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Run an anti-hallucination workflow",
               description = "Executes steps sequentially with inline hallucination guards. " +
                             "Streams Server-Sent Events — one per step, plus a final summary.")
    public Flux<ServerSentEvent<?>> runWorkflow(
            @Valid @RequestBody WorkflowDef workflowDef,
            @AuthenticationPrincipal UserDetails user) {

        return workflowEngine.execute(workflowDef)
                .map(event -> {
                    if (event instanceof StepResult stepResult) {
                        return ServerSentEvent.builder(stepResult)
                                .event("step")
                                .build();
                    } else if (event instanceof WorkflowResult workflowResult) {
                        return ServerSentEvent.builder(workflowResult)
                                .event("done")
                                .build();
                    }
                    return ServerSentEvent.builder(event.toString())
                            .event("info")
                            .build();
                });
    }
}
