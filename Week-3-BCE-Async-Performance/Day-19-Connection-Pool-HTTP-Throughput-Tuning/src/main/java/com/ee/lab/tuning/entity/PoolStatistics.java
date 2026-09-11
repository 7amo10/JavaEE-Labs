package com.ee.lab.tuning.entity;

public record PoolStatistics(
        String poolName,
        int totalConnections,
        int activeConnections,
        int idleConnections,
        int threadsAwaitingConnection,
        int maxPoolSize,
        int minIdle
) {}
