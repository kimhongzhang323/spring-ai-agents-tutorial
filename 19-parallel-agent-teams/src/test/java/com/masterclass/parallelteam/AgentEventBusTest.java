package com.masterclass.parallelteam;

import com.masterclass.parallelteam.event.AgentEvent;
import com.masterclass.parallelteam.event.AgentEventBus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentEventBusTest {

    // ── Subscriber receives events published after subscribe ────────────────

    @Test
    void subscriber_receivesLiveEvents() throws InterruptedException {
        AgentEventBus bus = new AgentEventBus();
        bus.createJob("job1");

        List<AgentEvent> received = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        bus.subscribe("job1", event -> {
            received.add(event);
            latch.countDown();
        });

        bus.publish(new AgentEvent.ResearchCompleted("job1", "facts", Instant.now()));

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isInstanceOf(AgentEvent.ResearchCompleted.class);
    }

    // ── History replay after publisher closed (the SSE race-condition fix) ──

    @Test
    void history_retainedAfterCloseJob() {
        AgentEventBus bus = new AgentEventBus();
        bus.createJob("job2");

        bus.publish(new AgentEvent.ResearchCompleted("job2", "facts", Instant.now()));
        bus.publish(new AgentEvent.SynthesisCompleted("job2", "report", Instant.now()));

        bus.closeJob("job2");

        // Publisher is gone but history should still be readable
        List<AgentEvent> history = bus.getHistory("job2");
        assertThat(history).hasSize(2);
        assertThat(history.get(0)).isInstanceOf(AgentEvent.ResearchCompleted.class);
        assertThat(history.get(1)).isInstanceOf(AgentEvent.SynthesisCompleted.class);
        assertThat(bus.isPublisherOpen("job2")).isFalse();
    }

    // ── purgeHistory fully cleans up ─────────────────────────────────────────

    @Test
    void purgeHistory_removesAllState() {
        AgentEventBus bus = new AgentEventBus();
        bus.createJob("job3");
        bus.publish(new AgentEvent.ResearchCompleted("job3", "facts", Instant.now()));
        bus.closeJob("job3");
        bus.purgeHistory("job3");

        assertThat(bus.getHistory("job3")).isEmpty();
    }

    // ── Subscribe to unknown jobId throws ────────────────────────────────────

    @Test
    void subscribe_unknownJobId_throws() {
        AgentEventBus bus = new AgentEventBus();
        assertThatThrownBy(() -> bus.subscribe("no-such-job", event -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-such-job");
    }

    // ── Jobs don't share publishers ──────────────────────────────────────────

    @Test
    void twoJobs_dontShareEvents() throws InterruptedException {
        AgentEventBus bus = new AgentEventBus();
        bus.createJob("alpha");
        bus.createJob("beta");

        List<AgentEvent> alphaEvents = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        bus.subscribe("alpha", e -> { alphaEvents.add(e); latch.countDown(); });

        bus.publish(new AgentEvent.ResearchCompleted("beta", "beta-facts", Instant.now()));
        bus.publish(new AgentEvent.ResearchCompleted("alpha", "alpha-facts", Instant.now()));

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(alphaEvents).hasSize(1);
        assertThat(((AgentEvent.ResearchCompleted) alphaEvents.getFirst()).facts()).isEqualTo("alpha-facts");
    }
}
