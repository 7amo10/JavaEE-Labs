package com.ee.lab.concurrency.context;

/**
 * Snapshot of calling thread context (Security Principal & Correlation ID)
 * captured for propagation across Managed Executor threads.
 */
public record CapturedContext(
        String principalName,
        String correlationId
) {}
