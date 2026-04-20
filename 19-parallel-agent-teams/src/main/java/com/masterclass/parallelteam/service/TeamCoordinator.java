package com.masterclass.parallelteam.service;

import com.masterclass.parallelteam.agent.AnalysisAgent;
import com.masterclass.parallelteam.agent.CitationAgent;
import com.masterclass.parallelteam.agent.ResearchAgent;
import com.masterclass.parallelteam.agent.SynthesisAgent;
import com.masterclass.parallelteam.event.AgentEvent;
import com.masterclass.parallelteam.event.AgentEventBus;
import com.masterclass.parallelteam.model.TeamJob;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Orchestrates the parallel agent team.
 *
 * Fan-out: Research, Analysis, Citation agents all launch concurrently on virtual threads.
 * Fan-in:  CompletableFuture.allOf() collects results, then SynthesisAgent runs.
 *
 * If any parallel agent throws, the StructuredTaskScope approach would cancel siblings;
 * here we use exceptionally() to record failures and still attempt synthesis with partial data.
 */
@Service
public class TeamCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TeamCoordinator.class);

    private final ResearchAgent researchAgent;
    private final AnalysisAgent analysisAgent;
    private final CitationAgent citationAgent;
    private final SynthesisAgent synthesisAgent;
    private final AgentEventBus eventBus;
    private final JobStore jobStore;
    private final MeterRegistry meterRegistry;

    // Virtual-thread executor — Java 21 feature; each LLM call gets its own virtual thread
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public TeamCoordinator(ResearchAgent researchAgent, AnalysisAgent analysisAgent,
                           CitationAgent citationAgent, SynthesisAgent synthesisAgent,
                           AgentEventBus eventBus, JobStore jobStore, MeterRegistry meterRegistry) {
        this.researchAgent = researchAgent;
        this.analysisAgent = analysisAgent;
        this.citationAgent = citationAgent;
        this.synthesisAgent = synthesisAgent;
        this.eventBus = eventBus;
        this.jobStore = jobStore;
        this.meterRegistry = meterRegistry;
    }

    public TeamJob startJob(String topic, String userId) {
        String jobId = UUID.randomUUID().toString();
        TeamJob job = TeamJob.create(jobId, topic, userId);
        jobStore.save(job);
        eventBus.createJob(jobId);

        // Launch the parallel team asynchronously so the HTTP response returns immediately
        CompletableFuture.runAsync(() -> runTeam(jobId, topic), virtualThreadExecutor);

        return job;
    }

    private void runTeam(String jobId, String topic) {
        Timer.Sample timerSample = Timer.start(meterRegistry);
        log.info("[job={}] Launching parallel team for topic: {}", jobId, topic);

        // Fan-out: three agents run simultaneously on virtual threads
        CompletableFuture<String> researchFuture = CompletableFuture
                .supplyAsync(() -> researchAgent.research(jobId, topic), virtualThreadExecutor)
                .exceptionally(ex -> {
                    log.error("[job={}] ResearchAgent failed: {}", jobId, ex.getMessage());
                    return "[Research unavailable due to error]";
                });

        CompletableFuture<String> analysisFuture = CompletableFuture
                .supplyAsync(() -> analysisAgent.analyze(jobId, topic), virtualThreadExecutor)
                .exceptionally(ex -> {
                    log.error("[job={}] AnalysisAgent failed: {}", jobId, ex.getMessage());
                    return "[Analysis unavailable due to error]";
                });

        CompletableFuture<String> citationFuture = CompletableFuture
                .supplyAsync(() -> citationAgent.findCitations(jobId, topic), virtualThreadExecutor)
                .exceptionally(ex -> {
                    log.error("[job={}] CitationAgent failed: {}", jobId, ex.getMessage());
                    return "[Citations unavailable due to error]";
                });

        // Fan-in: wait for all three, then synthesize
        CompletableFuture.allOf(researchFuture, analysisFuture, citationFuture)
                .thenRunAsync(() -> {
                    String facts = researchFuture.join();
                    String trends = analysisFuture.join();
                    String citations = citationFuture.join();

                    log.info("[job={}] All parallel agents done; starting synthesis", jobId);
                    String report = synthesisAgent.synthesize(jobId, topic, facts, trends, citations);

                    TeamJob completed = jobStore.find(jobId)
                            .map(j -> j.withCompleted(report))
                            .orElseThrow();
                    jobStore.update(completed);

                    timerSample.stop(meterRegistry.timer("parallel.team.duration", "status", "success"));
                    log.info("[job={}] Team completed successfully", jobId);
                }, virtualThreadExecutor)
                .exceptionally(ex -> {
                    log.error("[job={}] Team failed during synthesis: {}", jobId, ex.getMessage());
                    jobStore.find(jobId).map(TeamJob::withFailed).ifPresent(jobStore::update);
                    eventBus.publish(new AgentEvent.AgentFailed(jobId, "TeamCoordinator", ex.getMessage(), Instant.now()));
                    timerSample.stop(meterRegistry.timer("parallel.team.duration", "status", "failed"));
                    return null;
                });
    }
}
