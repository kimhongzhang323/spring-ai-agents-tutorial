package com.masterclass.antihallucination;

import com.masterclass.antihallucination.config.AntiHallucinationConfig;
import com.masterclass.antihallucination.guard.GroundingScore;
import com.masterclass.antihallucination.guard.HallucinationGuard;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HallucinationGuardTest {

    private ChatModel mockModel;
    private HallucinationGuard guard;

    @BeforeEach
    void setUp() {
        mockModel = mock(ChatModel.class);
        AntiHallucinationConfig config = new AntiHallucinationConfig(
                new AntiHallucinationConfig.GuardConfig(0.70, 0.65, "gpt-4o-mini", 2),
                new AntiHallucinationConfig.SelfConsistencyConfig(1, 0.7),
                new AntiHallucinationConfig.WorkflowConfig(300, 3600)
        );
        guard = new HallucinationGuard(mockModel, config, new SimpleMeterRegistry());
    }

    @Test
    void shouldPassWhenResponseIsWellGrounded() {
        stubJudge("""
                {"faithfulness": 0.92, "confidence": 0.88, "explanation": "All claims present in docs."}
                """);

        GroundingScore score = guard.verify(
                "What is the capital of France?",
                "The capital of France is Paris.",
                List.of("France is a country in Western Europe. Its capital city is Paris.")
        );

        assertThat(score.faithfulness()).isGreaterThanOrEqualTo(0.70);
        assertThat(score.confidence()).isGreaterThanOrEqualTo(0.65);
        assertThat(score.passes(0.70, 0.65)).isTrue();
    }

    @Test
    void shouldFailWhenResponseContainsHallucination() {
        stubJudge("""
                {"faithfulness": 0.20, "confidence": 0.90, "explanation": "Claim about Lyon not in docs."}
                """);

        GroundingScore score = guard.verify(
                "What is the capital of France?",
                "The capital of France is Lyon.",
                List.of("France is a country in Western Europe. Its capital city is Paris.")
        );

        assertThat(score.passes(0.70, 0.65)).isFalse();
        assertThat(score.explanation()).contains("Lyon");
    }

    @Test
    void shouldSkipCheckWhenNoGroundingDocsProvided() {
        GroundingScore score = guard.verify("any question", "any answer", List.of());

        assertThat(score.passes(0.70, 0.65)).isTrue();
        assertThat(score.explanation()).contains("skipped");
        verifyNoInteractions(mockModel);
    }

    private void stubJudge(String jsonResponse) {
        ChatResponse chatResponse = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(new AssistantMessage(jsonResponse));
        when(chatResponse.getResult()).thenReturn(generation);
        when(mockModel.call(any())).thenReturn(chatResponse);
    }
}
