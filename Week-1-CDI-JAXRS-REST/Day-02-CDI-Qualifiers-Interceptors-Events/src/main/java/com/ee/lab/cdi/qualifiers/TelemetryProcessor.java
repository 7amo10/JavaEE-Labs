package com.ee.lab.cdi.qualifiers;

public interface TelemetryProcessor {
    void recordMetric(String metricName, double value);
    String getProcessorType();
}
