package com.ee.lab.sse.entity;

public record ClusterTelemetryEvent(
    String nodeId,
    String status,
    double cpuLoadPercent,
    long freeMemoryMb,
    long timestamp
) {
    public static ClusterTelemetryEvent of(String nodeId, String status, double cpuLoad, long memoryMb) {
        return new ClusterTelemetryEvent(nodeId, status, cpuLoad, memoryMb, System.currentTimeMillis());
    }
}
