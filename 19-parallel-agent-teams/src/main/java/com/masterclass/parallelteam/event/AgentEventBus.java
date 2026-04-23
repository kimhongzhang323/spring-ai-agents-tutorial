package com.masterclass.parallelteam.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Consumer;

/**
 * In-process event bus using Java Flow API (SubmissionPublisher).
 * Each job gets its own publisher so parallel jobs don't interfere.
 * Subscribers are called on virtual threads — never block inside a subscriber.
 */
@Component
public class AgentEventBus {

    private static final Logger log = LoggerFactory.getLogger(AgentEventBus.class);

    private final ConcurrentHashMap<String, SubmissionPublisher<AgentEvent>> publishers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<AgentEvent>> eventHistory = new ConcurrentHashMap<>();

    public void createJob(String jobId) {
        publishers.put(jobId, new SubmissionPublisher<>());
        eventHistory.put(jobId, new CopyOnWriteArrayList<>());
    }

    public void publish(AgentEvent event) {
        log.debug("Publishing event {} for job {}", event.getClass().getSimpleName(), event.jobId());
        var history = eventHistory.get(event.jobId());
        if (history != null) history.add(event);
        var publisher = publishers.get(event.jobId());
        if (publisher != null) publisher.submit(event);
    }

    public void subscribe(String jobId, Consumer<AgentEvent> listener) {
        var publisher = publishers.get(jobId);
        if (publisher == null) throw new IllegalArgumentException("Unknown jobId: " + jobId);
        publisher.subscribe(new FlowSubscriberAdapter(listener));
    }

    /**
     * Closes the publisher for a job (signals onComplete to all live subscribers) but
     * retains event history for late-joining SSE clients to replay.
     */
    public void closeJob(String jobId) {
        var publisher = publishers.remove(jobId);
        if (publisher != null) publisher.close();
        // History is kept intentionally for replay — call purgeHistory() for full cleanup
    }

    public void purgeHistory(String jobId) {
        eventHistory.remove(jobId);
    }

    public java.util.List<AgentEvent> getHistory(String jobId) {
        var history = eventHistory.get(jobId);
        return history == null ? java.util.List.of() : java.util.List.copyOf(history);
    }

    public boolean isPublisherOpen(String jobId) {
        return publishers.containsKey(jobId);
    }

    // Flow.Subscriber adapter — must be a class (not a record) because subscription is mutable
    private static final class FlowSubscriberAdapter
            implements java.util.concurrent.Flow.Subscriber<AgentEvent> {

        private static final Logger log = LoggerFactory.getLogger(FlowSubscriberAdapter.class);

        private final Consumer<AgentEvent> listener;
        private java.util.concurrent.Flow.Subscription subscription;

        FlowSubscriberAdapter(Consumer<AgentEvent> listener) {
            this.listener = listener;
        }

        @Override
        public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
            this.subscription = s;
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(AgentEvent event) { listener.accept(event); }

        @Override
        public void onError(Throwable t) { log.error("Event bus subscriber error", t); }

        @Override
        public void onComplete() {}
    }
}
