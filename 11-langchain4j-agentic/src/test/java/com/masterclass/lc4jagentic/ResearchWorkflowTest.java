package com.masterclass.lc4jagentic;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResearchWorkflowTest {

    private final ResearchAgent researcher = mock(ResearchAgent.class);
    private final CriticAgent critic       = mock(CriticAgent.class);
    private final SynthesizerAgent synth   = mock(SynthesizerAgent.class);
    private final ResearchWorkflow workflow =
            new ResearchWorkflow(researcher, critic, synth, new SimpleMeterRegistry());

    @Test
    void happyPath_allAgentsCalledInOrder() {
        var researchResult = new ResearchResult("summary", "findings", "limits");
        var critiqueResult = new CritiqueResult("looks good", "APPROVED");

        when(researcher.research("AI")).thenReturn(researchResult);
        when(critic.critique(anyString(), anyString())).thenReturn(critiqueResult);
        when(synth.synthesize(anyString(), anyString(), anyString())).thenReturn("Final report");

        WorkflowResult result = workflow.run("AI");

        assertThat(result.finalReport()).isEqualTo("Final report");
        assertThat(result.critique().approved()).isTrue();

        // Verify strict ordering via call count (each called exactly once)
        verify(researcher, times(1)).research("AI");
        verify(critic,     times(1)).critique(anyString(), anyString());
        verify(synth,      times(1)).synthesize(anyString(), anyString(), anyString());
    }

    @Test
    void needsRevisionVerdictDoesNotThrow() {
        when(researcher.research(any()))
                .thenReturn(new ResearchResult("s", "f", "l"));
        when(critic.critique(any(), any()))
                .thenReturn(new CritiqueResult("Needs work", "NEEDS_REVISION"));
        when(synth.synthesize(any(), any(), any())).thenReturn("Revised report");

        WorkflowResult result = workflow.run("quantum computing");

        assertThat(result.critique().approved()).isFalse();
        assertThat(result.finalReport()).isNotBlank();
    }

    @Test
    void critiqueApprovedHelperReflectsVerdict() {
        assertThat(new CritiqueResult("ok", "APPROVED").approved()).isTrue();
        assertThat(new CritiqueResult("bad", "NEEDS_REVISION").approved()).isFalse();
        assertThat(new CritiqueResult("ok", "approved").approved()).isTrue(); // case-insensitive
    }
}
