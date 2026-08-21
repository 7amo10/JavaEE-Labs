package com.ee.lab.pulse.service;

import com.ee.lab.pulse.event.TelemetryAnomalyEvent;
import com.ee.lab.pulse.interceptor.Monitored;
import com.ee.lab.pulse.model.HealthStatus;
import com.ee.lab.pulse.model.JvmSnapshot;
import com.ee.lab.pulse.qualifier.EngineType;
import com.ee.lab.pulse.qualifier.MetricEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.UUID;

@ApplicationScoped
@MetricEngine(EngineType.HIGH_PRECISION)
public class HighPrecisionTelemetryCollector implements TelemetryCollector {

    @Inject
    private MemoryMXBean memoryMXBean;

    @Inject
    private ThreadMXBean threadMXBean;

    @Inject
    private OperatingSystemMXBean osMXBean;

    @Inject
    private Event<TelemetryAnomalyEvent> anomalyEvent;

    @Override
    @Monitored
    public JvmSnapshot captureSnapshot(String nodeName) {
        long heapUsedMb = memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMaxMb = memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        int threadCount = threadMXBean.getThreadCount();
        double cpuLoad = osMXBean.getSystemLoadAverage();
        if (cpuLoad < 0) cpuLoad = 12.5; // Fallback for environments without load average

        HealthStatus status = HealthStatus.OPTIMAL;
        if (heapUsedMb > 500 || threadCount > 50) {
            status = HealthStatus.CRITICAL;
        } else if (heapUsedMb > 200 || threadCount > 25) {
            status = HealthStatus.WARNING;
        }

        String snapshotId = "SNAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        JvmSnapshot snapshot = new JvmSnapshot(
            snapshotId,
            nodeName != null ? nodeName : "node-core-alpha",
            status,
            heapUsedMb,
            heapMaxMb,
            threadCount,
            cpuLoad
        );

        // Fire CDI Events if thresholds are reached
        if (threadCount >= 20) {
            TelemetryAnomalyEvent event = new TelemetryAnomalyEvent(snapshotId, "ACTIVE_THREADS", threadCount, 20.0);
            anomalyEvent.fire(event); // Synchronous
            anomalyEvent.fireAsync(event); // Asynchronous
        }

        return snapshot;
    }
}
