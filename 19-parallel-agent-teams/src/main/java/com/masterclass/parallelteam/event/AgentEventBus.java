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

    public void closeJob(String jobId) {
        var publisher = publishers.remove(jobId);
        if (publisher != null) publisher.close();
        eventHistory.remove(jobId);
    }

    public java.util.List<AgentEvent> getHistory(String jobId) {
        var history = eventHistory.get(jobId);
        return history == null ? java.util.List.of() : java.util.List.copyOf(history);
    }

    // Simple Flow.Subscriber adapter
    private record FlowSubscriberAdapter(Consumer<AgentEvent> listener)
            implements java.util.concurrent.Flow.Subscriber<AgentEvent> {

        private java.util.concurrent.Flow.Subscription subscription;

        @Override
        public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
            this.subscription.request(Long.MAX_VALUE);
        }

        // Workaround: store subscription in a mutable holder since record fields are final
        // FlowSubscriberAdapter is package-private; replace with a class if further mutation needed
        @Override
        public void onNext(AgentEvent event) { listener.accept(event); }
        @Override
        public void onError(Throwable t) { LoggerFactory.getLogger(FlowSubscriberAdapter.class).error("Event bus error", t); }
        @Override
        public void onComplete() {}
    }
}
