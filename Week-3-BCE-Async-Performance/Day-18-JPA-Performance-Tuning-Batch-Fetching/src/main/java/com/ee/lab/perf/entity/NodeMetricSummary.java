package com.ee.lab.perf.entity;

public record NodeMetricSummary(
    Long nodeId,
    String nodeName,
    String metricKey,
    double metricValue
) {}
