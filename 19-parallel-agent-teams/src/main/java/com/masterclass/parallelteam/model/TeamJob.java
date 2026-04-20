package com.masterclass.parallelteam.model;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public record TeamJob(
        String jobId,
        String topic,
        String userId,
        Instant createdAt,
        JobStatus status,
        List<String> progressLog,
        String finalReport
) {
    public enum JobStatus { RUNNING, COMPLETED, FAILED }

    public static TeamJob create(String jobId, String topic, String userId) {
        return new TeamJob(jobId, topic, userId, Instant.now(),
                JobStatus.RUNNING, new CopyOnWriteArrayList<>(), null);
    }

    public TeamJob withCompleted(String report) {
        return new TeamJob(jobId, topic, userId, createdAt, JobStatus.COMPLETED, progressLog, report);
    }

    public TeamJob withFailed() {
        return new TeamJob(jobId, topic, userId, createdAt, JobStatus.FAILED, progressLog, null);
    }
}
