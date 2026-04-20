package com.masterclass.antihallucination;

import com.masterclass.antihallucination.config.AntiHallucinationConfig;
import com.masterclass.antihallucination.domain.*;
import com.masterclass.antihallucination.guard.GroundingScore;
import com.masterclass.antihallucination.guard.HallucinationGuard;
import com.masterclass.antihallucination.guard.SelfConsistencyChecker;
import com.masterclass.antihallucination.workflow.AgentStep;
import com.masterclass.antihallucination.workflow.WorkflowEngine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkflowEngineTest {

    private WorkflowEngine engine;
    private ChatModel mockModel;
    private HallucinationGuard mockGuard;

    @BeforeEach
    void setUp() {
        mockModel = mock(ChatModel.class);
        mockGuard = mock(HallucinationGuard.class);
        AntiHallucinationConfig config = new AntiHallucinationConfig(
                new AntiHallucinationConfig.GuardConfig(0.70, 0.65, "gpt-4o-mini", 2),
                new AntiHallucinationConfig.SelfConsistencyConfig(1, 0.7),
                new AntiHallucinationConfig.WorkflowConfig(300, 3600)
        );
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SelfConsistencyChecker checker = new SelfConsistencyChecker(mockModel, registry);
        AgentStep agentStep = new AgentStep(mockModel, mockGuard, checker, config);
        engine = new WorkflowEngine(agentStep, mockGuard, config, registry);
    }

    @Test
    void shouldCompleteAllStepsWhenAllPass() {
        stubLlm("Paris is the capital of France.");
        when(mockGuard.verify(any(), any(), any()))
                .thenReturn(new GroundingScore(0.95, 0.90, "Well grounded."));

        WorkflowDef def = new WorkflowDef("test-workflow", List.of(
                new StepDef("step1", StepType.AGENT,
                        "What is the capital of France?",
                        List.of("France capital is Paris."),
                        1, true, null)
        ), true);

        List<Object> events = engine.execute(def).collectList().block();

        assertThat(events).hasSize(2); // 1 StepResult + 1 WorkflowResult
        assertThat(events.get(0)).isInstanceOf(StepResult.class);
        assertThat(((StepResult) events.get(0)).status()).isEqualTo(StepStatus.PASSED);
        assertThat(events.get(1)).isInstanceOf(WorkflowResult.class);
        assertThat(((WorkflowResult) events.get(1)).overallStatus()).isEqualTo(StepStatus.PASSED);
    }

    @Test
    void shouldAbortAfterFirstFailedStepWhenConfigured() {
        stubLlm("Lyon is the capital of France.");
        when(mockGuard.verify(any(), any(), any()))
                .thenReturn(new GroundingScore(0.15, 0.90, "Hallucination detected."));

        WorkflowDef def = new WorkflowDef("abort-test", List.of(
                new StepDef("step1", StepType.AGENT,
                        "What is the capital of France?",
                        List.of("France capital is Paris."),
                        1, true, null),
                new StepDef("step2", StepType.AGENT,
                        "Follow-up question?",
                        List.of(), 1, false, null)
        ), true);

        List<Object> events = engine.execute(def).collectList().block();

        // Should abort after step1 fails; step2 never runs
        assertThat(events).hasSize(2); // step1 result + final WorkflowResult
        assertThat(((StepResult) events.get(0)).status()).isEqualTo(StepStatus.FAILED);
        assertThat(((WorkflowResult) events.get(1)).overallStatus()).isEqualTo(StepStatus.FAILED);
    }

    @Test
    void shouldEmitHumanPauseEventForHumanPauseStep() {
        WorkflowDef def = new WorkflowDef("pause-test", List.of(
                new StepDef("review", StepType.HUMAN_PAUSE,
                        null, List.of(), 1, false, null)
        ), false);

        List<Object> events = engine.execute(def).collectList().block();

        assertThat(events).hasSize(2);
        StepResult pauseResult = (StepResult) events.get(0);
        assertThat(pauseResult.status()).isEqualTo(StepStatus.AWAITING_HUMAN);
    }

    private void stubLlm(String response) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(new AssistantMessage(response));
        when(chatResponse.getResult()).thenReturn(generation);
        when(mockModel.call(any())).thenReturn(chatResponse);
    }
}
