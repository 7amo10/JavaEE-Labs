package com.ee.lab.cdi.qualifiers;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@MetricEngine(EngineType.STANDARD)
public class StandardTelemetryProcessor implements TelemetryProcessor {

    @Override
    public void recordMetric(String metricName, double value) {
        System.out.println("  [StandardProcessor] Dispatched standard metric -> " + metricName + " = " + value);
    }

    @Override
    public String getProcessorType() {
        return "STANDARD (Sequential Blocking Queue)";
    }
}
