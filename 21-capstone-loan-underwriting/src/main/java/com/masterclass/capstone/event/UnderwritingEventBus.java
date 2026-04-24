package com.masterclass.capstone.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Consumer;

@Component
public class UnderwritingEventBus {

    private static final Logger log = LoggerFactory.getLogger(UnderwritingEventBus.class);

    private final ConcurrentHashMap<String, SubmissionPublisher<UnderwritingEvent>> publishers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<UnderwritingEvent>> history = new ConcurrentHashMap<>();

    public void createJob(String jobId) {
        publishers.put(jobId, new SubmissionPublisher<>());
        history.put(jobId, new CopyOnWriteArrayList<>());
    }

    public void publish(UnderwritingEvent event) {
        var h = history.get(event.jobId());
        if (h != null) h.add(event);
        var pub = publishers.get(event.jobId());
        if (pub != null) pub.submit(event);
        log.debug("[job={}] event: {}", event.jobId(), event.getClass().getSimpleName());
    }

    public void subscribe(String jobId, Consumer<UnderwritingEvent> listener) {
        var pub = publishers.get(jobId);
        if (pub == null) throw new IllegalArgumentException("Unknown job: " + jobId);
        pub.subscribe(new Adapter(listener));
    }

    public void closeJob(String jobId) {
        var pub = publishers.remove(jobId);
        if (pub != null) pub.close();
    }

    public List<UnderwritingEvent> getHistory(String jobId) {
        var h = history.get(jobId);
        return h == null ? List.of() : List.copyOf(h);
    }

    public boolean isOpen(String jobId) {
        return publishers.containsKey(jobId);
    }

    private static final class Adapter implements Flow.Subscriber<UnderwritingEvent> {
        private final Consumer<UnderwritingEvent> listener;
        private Flow.Subscription sub;

        Adapter(Consumer<UnderwritingEvent> listener) { this.listener = listener; }

        @Override public void onSubscribe(Flow.Subscription s) { (sub = s).request(Long.MAX_VALUE); }
        @Override public void onNext(UnderwritingEvent e) { listener.accept(e); }
        @Override public void onError(Throwable t) { log.error("Event bus error", t); }
        @Override public void onComplete() {}
    }
}
