package com.ee.lab.cdi.qualifiers;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@MetricEngine(EngineType.HIGH_THROUGHPUT)
public class HighThroughputTelemetryProcessor implements TelemetryProcessor {

    @Override
    public void recordMetric(String metricName, double value) {
        System.out.println("  [HighThroughputProcessor] Dispatched high-priority metric -> " + metricName + " = " + value);
    }

    @Override
    public String getProcessorType() {
        return "HIGH_THROUGHPUT (Lock-Free RingBuffer)";
    }
}
