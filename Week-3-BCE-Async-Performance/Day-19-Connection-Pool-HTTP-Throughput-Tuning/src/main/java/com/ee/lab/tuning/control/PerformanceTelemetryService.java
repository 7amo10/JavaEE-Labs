package com.ee.lab.tuning.control;

import com.ee.lab.tuning.entity.GcPauseMetric;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

public class PerformanceTelemetryService {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    public List<GcPauseMetric> captureGcMetrics() {
        List<GcPauseMetric> metrics = new ArrayList<>();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long usedMb = heapUsage.getUsed() / (1024 * 1024);
        long maxMb = heapUsage.getMax() / (1024 * 1024);

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            metrics.add(new GcPauseMetric(
                    gcBean.getName(),
                    gcBean.getCollectionCount(),
                    gcBean.getCollectionTime(),
                    usedMb,
                    maxMb
            ));
        }
        return metrics;
    }

    public long getTotalGcPauseTimeMs() {
        long totalMs = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long time = gcBean.getCollectionTime();
            if (time > 0) {
                totalMs += time;
            }
        }
        return totalMs;
    }

    public void triggerSimulatedGcPressure(int objectAllocationCount) {
        // Creates ephemeral objects to exercise eden generation and young GC
        List<byte[]> pressureList = new ArrayList<>(objectAllocationCount);
        for (int i = 0; i < objectAllocationCount; i++) {
            pressureList.add(new byte[1024 * 64]); // 64 KB chunk
        }
        pressureList.clear();
    }
}
