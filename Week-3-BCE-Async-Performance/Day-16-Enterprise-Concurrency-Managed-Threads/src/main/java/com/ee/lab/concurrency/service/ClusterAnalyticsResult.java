package com.ee.lab.concurrency.service;

import java.util.List;

public record ClusterAnalyticsResult(
        int totalNodes,
        double averageCpuUsage,
        double totalMemoryUsageMb,
        int totalActiveThreads,
        List<NodeTelemetryReport> nodeReports,
        long totalDurationMs
) {}
