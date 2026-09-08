package com.ee.lab.concurrency.service;

public record NodeTelemetryReport(
        String nodeId,
        double cpuUsage,
        double memoryUsageMb,
        int activeThreads,
        String principalName,
        String correlationId,
        String executingThreadName,
        long executionDurationMs
) {}
