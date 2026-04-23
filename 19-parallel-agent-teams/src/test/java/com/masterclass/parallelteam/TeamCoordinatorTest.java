package com.masterclass.parallelteam;

import com.masterclass.parallelteam.agent.AnalysisAgent;
import com.masterclass.parallelteam.agent.CitationAgent;
import com.masterclass.parallelteam.agent.ResearchAgent;
import com.masterclass.parallelteam.agent.SynthesisAgent;
import com.masterclass.parallelteam.event.AgentEvent;
import com.masterclass.parallelteam.event.AgentEventBus;
import com.masterclass.parallelteam.model.TeamJob;
import com.masterclass.parallelteam.service.JobStore;
import com.masterclass.parallelteam.service.TeamCoordinator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamCoordinatorTest {

    @Test
    void parallelAgentsAllRunAndSynthesisReceivesAllOutputs() throws InterruptedException {
        var researchAgent = mock(ResearchAgent.class);
        var analysisAgent = mock(AnalysisAgent.class);
        var citationAgent = mock(CitationAgent.class);
        var synthesisAgent = mock(SynthesisAgent.class);
        var eventBus = new AgentEventBus();
        var jobStore = new JobStore();

        when(researchAgent.research(anyString(), anyString())).thenReturn("facts");
        when(analysisAgent.analyze(anyString(), anyString())).thenReturn("trends");
        when(citationAgent.findCitations(anyString(), anyString())).thenReturn("citations");
        when(synthesisAgent.synthesize(anyString(), anyString(), eq("facts"), eq("trends"), eq("citations")))
                .thenReturn("final-report");

        var coordinator = new TeamCoordinator(researchAgent, analysisAgent, citationAgent,
                synthesisAgent, eventBus, jobStore, new SimpleMeterRegistry());

        TeamJob job = coordinator.startJob("AI in healthcare", "user1");

        // Poll until job completes (max 5 s)
        long deadline = System.currentTimeMillis() + 5000;
        TeamJob completed = job;
        while (System.currentTimeMillis() < deadline) {
            completed = jobStore.find(job.jobId()).orElse(job);
            if (completed.status() != TeamJob.JobStatus.RUNNING) break;
            Thread.sleep(100);
        }

        assertThat(completed.status()).isEqualTo(TeamJob.JobStatus.COMPLETED);
        assertThat(completed.finalReport()).isEqualTo("final-report");
        assertThat(completed.completedAt()).isNotNull();

        verify(researchAgent).research(anyString(), eq("AI in healthcare"));
        verify(analysisAgent).analyze(anyString(), eq("AI in healthcare"));
        verify(citationAgent).findCitations(anyString(), eq("AI in healthcare"));
        verify(synthesisAgent).synthesize(anyString(), anyString(), eq("facts"), eq("trends"), eq("citations"));
    }

    @Test
    void whenOneAgentFailsJobStillCompletes() throws InterruptedException {
        var researchAgent = mock(ResearchAgent.class);
        var analysisAgent = mock(AnalysisAgent.class);
        var citationAgent = mock(CitationAgent.class);
        var synthesisAgent = mock(SynthesisAgent.class);
        var eventBus = new AgentEventBus();
        var jobStore = new JobStore();

        when(researchAgent.research(anyString(), anyString())).thenThrow(new RuntimeException("LLM timeout"));
        when(analysisAgent.analyze(anyString(), anyString())).thenReturn("trends");
        when(citationAgent.findCitations(anyString(), anyString())).thenReturn("citations");
        when(synthesisAgent.synthesize(anyString(), anyString(), contains("unavailable"), anyString(), anyString()))
                .thenReturn("partial-report");

        var coordinator = new TeamCoordinator(researchAgent, analysisAgent, citationAgent,
                synthesisAgent, eventBus, jobStore, new SimpleMeterRegistry());

        TeamJob job = coordinator.startJob("Quantum computing", "user1");

        long deadline = System.currentTimeMillis() + 5000;
        TeamJob completed = job;
        while (System.currentTimeMillis() < deadline) {
            completed = jobStore.find(job.jobId()).orElse(job);
            if (completed.status() != TeamJob.JobStatus.RUNNING) break;
            Thread.sleep(100);
        }

        assertThat(completed.status()).isEqualTo(TeamJob.JobStatus.COMPLETED);
        assertThat(completed.finalReport()).isEqualTo("partial-report");
    }
}
