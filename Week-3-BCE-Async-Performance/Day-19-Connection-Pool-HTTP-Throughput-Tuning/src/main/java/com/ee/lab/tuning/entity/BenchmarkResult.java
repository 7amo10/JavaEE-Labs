package com.ee.lab.tuning.entity;

public record BenchmarkResult(
        String profileName,
        int poolMaxSize,
        int workerThreads,
        int totalRequests,
        int concurrency,
        double requestsPerSecond,
        double meanLatencyMs,
        double p99LatencyMs,
        long failedRequests,
        long poolTimeoutErrors
) {}
