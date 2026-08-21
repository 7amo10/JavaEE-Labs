package com.ee.lab.pulse.event;

import java.time.Instant;

public class TelemetryAnomalyEvent {
    private final String snapshotId;
    private final String metricName;
    private final double currentValue;
    private final double threshold;
    private final String timestamp;

    public TelemetryAnomalyEvent(String snapshotId, String metricName, double currentValue, double threshold) {
        this.snapshotId = snapshotId;
        this.metricName = metricName;
        this.currentValue = currentValue;
        this.threshold = threshold;
        this.timestamp = Instant.now().toString();
    }

    public String getSnapshotId() { return snapshotId; }
    public String getMetricName() { return metricName; }
    public double getCurrentValue() { return currentValue; }
    public double getThreshold() { return threshold; }
    public String getTimestamp() { return timestamp; }
}
