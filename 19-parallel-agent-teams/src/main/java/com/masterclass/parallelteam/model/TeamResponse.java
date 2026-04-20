package com.masterclass.parallelteam.model;

import java.time.Instant;

public record TeamResponse(
        String jobId,
        String topic,
        TeamJob.JobStatus status,
        String finalReport,
        Instant completedAt,
        String streamUrl
) {}
