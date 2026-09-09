package com.ee.lab.sse.entity;

public record ClusterAlertEvent(
    String alertId,
    String severity,
    String description,
    String sourceNode,
    long timestamp
) {
    public static ClusterAlertEvent of(String alertId, String severity, String description, String sourceNode) {
        return new ClusterAlertEvent(alertId, severity, description, sourceNode, System.currentTimeMillis());
    }
}
