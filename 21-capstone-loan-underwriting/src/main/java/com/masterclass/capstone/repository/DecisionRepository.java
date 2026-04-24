package com.masterclass.capstone.repository;

import com.masterclass.capstone.domain.UnderwritingJob;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DecisionRepository {

    private final ConcurrentHashMap<String, UnderwritingJob> jobs = new ConcurrentHashMap<>();

    public void save(UnderwritingJob job) { jobs.put(job.jobId(), job); }
    public Optional<UnderwritingJob> find(String jobId) { return Optional.ofNullable(jobs.get(jobId)); }
}
