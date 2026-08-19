package com.ee.lab.json.repository;

import com.ee.lab.json.model.AnalysisJob;
import com.ee.lab.json.model.JobStatus;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AnalysisJobRepository {
    private static final AnalysisJobRepository INSTANCE = new AnalysisJobRepository();
    private final Map<String, AnalysisJob> store = new ConcurrentHashMap<>();

    private AnalysisJobRepository() {
        // Seed default record
        save(new AnalysisJob("JOB-101", "com/engine/HelixBootstrap.class", JobStatus.COMPLETED, 1420, 3.45));
    }

    public static AnalysisJobRepository getInstance() {
        return INSTANCE;
    }

    public Optional<AnalysisJob> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public boolean exists(String id) {
        return store.containsKey(id);
    }

    public AnalysisJob save(AnalysisJob job) {
        store.put(job.getJobId(), job);
        return job;
    }
}
