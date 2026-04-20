package com.masterclass.parallelteam.service;

import com.masterclass.parallelteam.model.TeamJob;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobStore {

    private final ConcurrentHashMap<String, TeamJob> jobs = new ConcurrentHashMap<>();

    public void save(TeamJob job) {
        jobs.put(job.jobId(), job);
    }

    public Optional<TeamJob> find(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public void update(TeamJob job) {
        jobs.put(job.jobId(), job);
    }

    public void remove(String jobId) {
        jobs.remove(jobId);
    }
}
