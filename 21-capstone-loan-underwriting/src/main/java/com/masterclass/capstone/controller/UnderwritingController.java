package com.masterclass.capstone.controller;

import com.masterclass.capstone.domain.LoanApplication;
import com.masterclass.capstone.domain.UnderwritingJob;
import com.masterclass.capstone.event.UnderwritingEvent;
import com.masterclass.capstone.event.UnderwritingEventBus;
import com.masterclass.capstone.repository.DecisionRepository;
import com.masterclass.capstone.service.UnderwritingOrchestrator;
import com.masterclass.shared.guardrails.InputValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/underwrite")
@Tag(name = "Loan Underwriting", description = "Submit and track AI-driven underwriting decisions")
@SecurityRequirement(name = "bearerAuth")
public class UnderwritingController {

    private final UnderwritingOrchestrator orchestrator;
    private final DecisionRepository decisions;
    private final UnderwritingEventBus eventBus;
    private final InputValidator inputValidator;

    public UnderwritingController(UnderwritingOrchestrator orchestrator,
                                  DecisionRepository decisions,
                                  UnderwritingEventBus eventBus,
                                  InputValidator inputValidator) {
        this.orchestrator = orchestrator;
        this.decisions = decisions;
        this.eventBus = eventBus;
        this.inputValidator = inputValidator;
    }

    @PostMapping
    @Operation(summary = "Submit a loan application for underwriting. Returns a job ID immediately; poll or stream for results.")
    public ResponseEntity<?> submit(
            @Valid @RequestBody LoanApplication app,
            @AuthenticationPrincipal UserDetails user) {

        // Input guardrail: validate the applicant ID string before it reaches any agent
        var v = inputValidator.validate(app.applicantId());
        if (!v.valid()) {
            return ResponseEntity.badRequest().body(new ErrorResponse(v.reason()));
        }

        UnderwritingJob job = orchestrator.submit(user.getUsername(), app);
        return ResponseEntity.accepted().body(toSummary(job));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Poll for job status and the final decision when complete")
    public ResponseEntity<JobSummary> status(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails user) {

        return decisions.find(jobId)
                .map(j -> ResponseEntity.ok(toSummary(j)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream live SSE progress events for a running underwriting job")
    public SseEmitter stream(@PathVariable String jobId,
                             @AuthenticationPrincipal UserDetails user) {

        SseEmitter emitter = new SseEmitter(300_000L);

        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            try {
                // Replay history for late-joining clients (race-free)
                for (UnderwritingEvent event : eventBus.getHistory(jobId)) {
                    sendEvent(emitter, event);
                }
                if (!eventBus.isOpen(jobId)) {
                    emitter.complete();
                    return;
                }
                eventBus.subscribe(jobId, event -> {
                    sendEvent(emitter, event);
                    if (event instanceof UnderwritingEvent.DecisionMade
                            || event instanceof UnderwritingEvent.DecisionRejected
                            || event instanceof UnderwritingEvent.AgentFailed) {
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, UnderwritingEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event.getClass().getSimpleName())
                    .data(event.toString()));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private JobSummary toSummary(UnderwritingJob job) {
        String outcome = job.decision() != null ? job.decision().outcome().name() : null;
        return new JobSummary(job.jobId(), job.applicantId(), job.status().name(), outcome,
                job.failureReason(), "/api/v1/underwrite/" + job.jobId() + "/stream");
    }

    public record JobSummary(String jobId, String applicantId, String status,
                             String outcome, String failureReason, String streamUrl) {}
    public record ErrorResponse(String error) {}
}
