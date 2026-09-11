package com.ee.lab.tuning.entity;

public record GcPauseMetric(
        String gcName,
        long collectionCount,
        long collectionTimeMs,
        long memoryUsedMb,
        long memoryMaxMb
) {}
