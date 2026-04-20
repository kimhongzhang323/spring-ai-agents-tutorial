package com.masterclass.antihallucination.workflow;

import com.masterclass.antihallucination.config.AntiHallucinationConfig;
import com.masterclass.antihallucination.domain.*;
import com.masterclass.antihallucination.guard.GroundingScore;
import com.masterclass.antihallucination.guard.HallucinationGuard;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates a {@link WorkflowDef} — iterates steps in order, propagates context,
 * and emits {@link StepResult} events to a reactive stream consumed by the SSE controller.
 */
@Service
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final AgentStep agentStep;
    private final HallucinationGuard hallucinationGuard;
    private final AntiHallucinationConfig config;
    private final MeterRegistry meterRegistry;

    public WorkflowEngine(AgentStep agentStep,
                          HallucinationGuard hallucinationGuard,
                          AntiHallucinationConfig config,
                          MeterRegistry meterRegistry) {
        this.agentStep = agentStep;
        this.hallucinationGuard = hallucinationGuard;
        this.config = config;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Executes the workflow and returns a cold Flux that emits one {@link StepResult}
     * per step, followed by a terminal {@link WorkflowResult} wrapped as the last event.
     *
     * Subscribers receive events on the thread that calls {@code subscribe()}.
     * The controller maps this Flux to SSE.
     */
    public Flux<Object> execute(WorkflowDef workflowDef) {
        return Flux.create(sink -> runWorkflow(workflowDef, sink));
    }

    private void runWorkflow(WorkflowDef def, FluxSink<Object> sink) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String workflowId = UUID.randomUUID().toString();
        WorkflowContext ctx = new WorkflowContext(workflowId);
        List<StepResult> results = new ArrayList<>();
        long workflowStart = System.currentTimeMillis();

        log.info("Starting workflow '{}' id={} steps={}", def.name(), workflowId, def.steps().size());

        try {
            for (StepDef stepDef : def.steps()) {
                StepResult result = executeStep(stepDef, ctx, def.abortOnFirstFailure(), results);
                results.add(result);
                sink.next(result);

                if (result.status() == StepStatus.FAILED && def.abortOnFirstFailure()) {
                    log.warn("Workflow '{}' aborted after failed step '{}'", def.name(), stepDef.name());
                    break;
                }

                if (result.status() == StepStatus.AWAITING_HUMAN) {
                    log.info("Workflow '{}' paused at HUMAN_PAUSE step '{}'", def.name(), stepDef.name());
                    break;
                }
            }

            WorkflowResult finalResult = WorkflowResult.from(
                    def.name(), results, System.currentTimeMillis() - workflowStart);
            sink.next(finalResult);
            sink.complete();

            timer.stop(meterRegistry.timer("workflow.execution.duration",
                    "workflow", def.name(),
                    "status", finalResult.overallStatus().name()));

            log.info("Workflow '{}' completed — passed={} failed={} duration={}ms",
                    def.name(), finalResult.passedSteps(), finalResult.failedSteps(),
                    finalResult.totalDurationMs());

        } catch (Exception ex) {
            log.error("Workflow '{}' threw unexpected exception", def.name(), ex);
            sink.error(ex);
        }
    }

    private StepResult executeStep(StepDef step, WorkflowContext ctx,
                                   boolean abortOnFirstFailure, List<StepResult> priorResults) {
        return switch (step.type()) {
            case AGENT -> agentStep.execute(step, ctx);

            case GUARD -> {
                // Verify a prior step's output against grounding docs
                String target = ctx.get(step.targetContextKey())
                        .orElse("");
                GroundingScore score = hallucinationGuard.verify(
                        step.targetContextKey(), target, step.groundingDocs());
                boolean passed = score.passes(
                        config.guard().faithfulnessThreshold(),
                        config.guard().confidenceThreshold());
                yield new StepResult(step.name(),
                        passed ? StepStatus.PASSED : StepStatus.FAILED,
                        null, score.faithfulness(), score.confidence(),
                        score.explanation(), 0L);
            }

            case TRANSFORM -> {
                // Pure context manipulation — no LLM call
                String value = ctx.get(step.targetContextKey()).orElse("");
                String transformed = value.trim();
                ctx.put(step.name(), transformed);
                yield new StepResult(step.name(), StepStatus.PASSED, transformed,
                        null, null, null, 0L);
            }

            case HUMAN_PAUSE -> new StepResult(step.name(), StepStatus.AWAITING_HUMAN,
                    "Workflow paused — awaiting human approval", null, null, null, 0L);
        };
    }
}
