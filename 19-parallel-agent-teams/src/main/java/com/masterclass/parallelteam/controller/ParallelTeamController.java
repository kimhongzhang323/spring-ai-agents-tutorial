package com.masterclass.parallelteam.controller;

import com.masterclass.parallelteam.event.AgentEvent;
import com.masterclass.parallelteam.event.AgentEventBus;
import com.masterclass.parallelteam.model.TeamJob;
import com.masterclass.parallelteam.model.TeamRequest;
import com.masterclass.parallelteam.model.TeamResponse;
import com.masterclass.parallelteam.service.JobStore;
import com.masterclass.parallelteam.service.TeamCoordinator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/parallel-team")
@Tag(name = "Parallel Agent Team", description = "Run a team of specialized agents concurrently")
public class ParallelTeamController {

    private final TeamCoordinator coordinator;
    private final JobStore jobStore;
    private final AgentEventBus eventBus;

    public ParallelTeamController(TeamCoordinator coordinator, JobStore jobStore, AgentEventBus eventBus) {
        this.coordinator = coordinator;
        this.jobStore = jobStore;
        this.eventBus = eventBus;
    }

    @PostMapping("/run")
    @Operation(summary = "Launch a parallel agent team job and return the job ID immediately")
    public ResponseEntity<TeamResponse> run(
            @Valid @RequestBody TeamRequest request,
            @AuthenticationPrincipal UserDetails user) {

        TeamJob job = coordinator.startJob(request.topic(), user.getUsername());
        return ResponseEntity.accepted().body(toResponse(job));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Poll job status and retrieve the final report when complete")
    public ResponseEntity<TeamResponse> status(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails user) {

        return jobStore.find(jobId)
                .map(job -> ResponseEntity.ok(toResponse(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live SSE progress events for a running job")
    public SseEmitter stream(
            @PathVariable String jobId,
            @AuthenticationPrincipal UserDetails user) {

        SseEmitter emitter = new SseEmitter(300_000L); // 5-minute timeout

        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            try {
                eventBus.subscribe(jobId, event -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(event.getClass().getSimpleName())
                                .data(event.toString()));

                        if (event instanceof AgentEvent.SynthesisCompleted
                                || event instanceof AgentEvent.AgentFailed) {
                            emitter.complete();
                        }
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private TeamResponse toResponse(TeamJob job) {
        return new TeamResponse(
                job.jobId(),
                job.topic(),
                job.status(),
                job.finalReport(),
                job.status() == TeamJob.JobStatus.RUNNING ? null : Instant.now(),
                "/api/v1/parallel-team/" + job.jobId() + "/stream"
        );
    }
}
