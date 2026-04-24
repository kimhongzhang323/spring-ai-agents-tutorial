package com.masterclass.capstone.service;

import com.masterclass.capstone.agent.*;
import com.masterclass.capstone.domain.Finding;
import com.masterclass.capstone.domain.LoanApplication;
import com.masterclass.capstone.domain.UnderwritingJob;
import com.masterclass.capstone.event.UnderwritingEvent;
import com.masterclass.capstone.event.UnderwritingEventBus;
import com.masterclass.capstone.guardrails.CitationValidator;
import com.masterclass.capstone.repository.ApplicantRepository;
import com.masterclass.capstone.repository.DecisionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Central orchestration service.
 *
 * Fan-out: Credit, Fraud, Income agents run in parallel on virtual threads.
 * Compliance runs after fan-in (needs the three prior results).
 * Supervisor adjudicates last; CitationValidator gates the decision.
 */
@Service
public class UnderwritingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingOrchestrator.class);

    private final CreditAnalysisAgent creditAgent;
    private final FraudDetectionAgent fraudAgent;
    private final IncomeVerificationAgent incomeAgent;
    private final ComplianceAgent complianceAgent;
    private final UnderwritingSupervisor supervisor;
    private final CitationValidator citationValidator;
    private final ApplicantRepository applicants;
    private final DecisionRepository decisions;
    private final UnderwritingEventBus eventBus;
    private final MeterRegistry meterRegistry;

    private final Executor vtExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public UnderwritingOrchestrator(CreditAnalysisAgent creditAgent,
                                    FraudDetectionAgent fraudAgent,
                                    IncomeVerificationAgent incomeAgent,
                                    ComplianceAgent complianceAgent,
                                    UnderwritingSupervisor supervisor,
                                    CitationValidator citationValidator,
                                    ApplicantRepository applicants,
                                    DecisionRepository decisions,
                                    UnderwritingEventBus eventBus,
                                    MeterRegistry meterRegistry) {
        this.creditAgent = creditAgent;
        this.fraudAgent = fraudAgent;
        this.incomeAgent = incomeAgent;
        this.complianceAgent = complianceAgent;
        this.supervisor = supervisor;
        this.citationValidator = citationValidator;
        this.applicants = applicants;
        this.decisions = decisions;
        this.eventBus = eventBus;
        this.meterRegistry = meterRegistry;
    }

    public UnderwritingJob submit(String officerId, LoanApplication app) {
        applicants.findById(app.applicantId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown applicant: " + app.applicantId()));

        String jobId = UUID.randomUUID().toString();
        UnderwritingJob job = UnderwritingJob.create(jobId, officerId, app);
        decisions.save(job);
        eventBus.createJob(jobId);

        CompletableFuture.runAsync(() -> runPipeline(jobId, app), vtExecutor);
        return job;
    }

    private void runPipeline(String jobId, LoanApplication app) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String applicantId = app.applicantId();
        log.info("[job={}] Pipeline started for applicant {}", jobId, applicantId);

        // ── Phase 1: parallel specialist agents ───────────────────────────────
        CompletableFuture<List<Finding>> creditFuture = CompletableFuture
                .supplyAsync(() -> creditAgent.analyze(jobId, applicantId), vtExecutor)
                .whenComplete((findings, ex) -> {
                    if (ex == null) eventBus.publish(new UnderwritingEvent.CreditCompleted(jobId, findings, Instant.now()));
                    else eventBus.publish(new UnderwritingEvent.AgentFailed(jobId, "CreditAnalysisAgent", ex.getMessage(), Instant.now()));
                })
                .exceptionally(ex -> List.of(fallbackFinding("CR-ERR", "CREDIT", ex)));

        CompletableFuture<List<Finding>> fraudFuture = CompletableFuture
                .supplyAsync(() -> fraudAgent.screen(jobId, applicantId), vtExecutor)
                .whenComplete((findings, ex) -> {
                    if (ex == null) eventBus.publish(new UnderwritingEvent.FraudCompleted(jobId, findings, Instant.now()));
                    else eventBus.publish(new UnderwritingEvent.AgentFailed(jobId, "FraudDetectionAgent", ex.getMessage(), Instant.now()));
                })
                .exceptionally(ex -> List.of(fallbackFinding("FR-ERR", "FRAUD", ex)));

        CompletableFuture<List<Finding>> incomeFuture = CompletableFuture
                .supplyAsync(() -> incomeAgent.verify(jobId, applicantId), vtExecutor)
                .whenComplete((findings, ex) -> {
                    if (ex == null) eventBus.publish(new UnderwritingEvent.IncomeCompleted(jobId, findings, Instant.now()));
                    else eventBus.publish(new UnderwritingEvent.AgentFailed(jobId, "IncomeVerificationAgent", ex.getMessage(), Instant.now()));
                })
                .exceptionally(ex -> List.of(fallbackFinding("IN-ERR", "INCOME", ex)));

        // ── Phase 2: compliance after fan-in ─────────────────────────────────
        CompletableFuture.allOf(creditFuture, fraudFuture, incomeFuture)
                .thenRunAsync(() -> {
                    List<Finding> allFindings = new ArrayList<>();
                    allFindings.addAll(creditFuture.join());
                    allFindings.addAll(fraudFuture.join());
                    allFindings.addAll(incomeFuture.join());

                    List<Finding> complianceFindings = complianceAgent.review(
                            jobId, applicantId, allFindings, app.purpose(), app.loanAmount());
                    eventBus.publish(new UnderwritingEvent.ComplianceCompleted(jobId, complianceFindings, Instant.now()));
                    allFindings.addAll(complianceFindings);

                    // ── Phase 3: supervisor adjudication ─────────────────────
                    var decision = supervisor.adjudicate(jobId, app, allFindings);

                    // ── Phase 4: citation guardrail ───────────────────────────
                    var validation = citationValidator.validate(decision, allFindings);
                    if (!validation.valid()) {
                        log.warn("[job={}] Citation validation failed: {}", jobId, validation.reason());
                        eventBus.publish(new UnderwritingEvent.DecisionRejected(jobId, validation.reason(), Instant.now()));
                        decisions.save(decisions.find(jobId).map(j -> j.rejectedByGuardrail(validation.reason())).orElseThrow());
                        eventBus.closeJob(jobId);
                        timer.stop(meterRegistry.timer("underwriting.pipeline.duration", "status", "rejected"));
                        return;
                    }

                    eventBus.publish(new UnderwritingEvent.DecisionMade(jobId, decision, Instant.now()));
                    decisions.save(decisions.find(jobId).map(j -> j.completed(decision)).orElseThrow());
                    eventBus.closeJob(jobId);
                    timer.stop(meterRegistry.timer("underwriting.pipeline.duration",
                            "status", "completed", "outcome", decision.outcome().name()));
                    log.info("[job={}] Pipeline completed: {}", jobId, decision.outcome());

                }, vtExecutor)
                .exceptionally(ex -> {
                    log.error("[job={}] Pipeline failed: {}", jobId, ex.getMessage(), ex);
                    decisions.save(decisions.find(jobId).map(j -> j.failed(ex.getMessage())).orElseThrow());
                    eventBus.publish(new UnderwritingEvent.AgentFailed(jobId, "Orchestrator", ex.getMessage(), Instant.now()));
                    eventBus.closeJob(jobId);
                    timer.stop(meterRegistry.timer("underwriting.pipeline.duration", "status", "failed"));
                    return null;
                });
    }

    private Finding fallbackFinding(String id, String source, Throwable ex) {
        return new Finding(id, source,
                "Agent unavailable: " + ex.getMessage(), Finding.Severity.WARNING);
    }
}
