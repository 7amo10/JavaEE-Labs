package com.ee.lab.pulse.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

@ApplicationScoped
public class AnomalyAlertObserver {

    public void onSyncAlert(@Observes TelemetryAnomalyEvent event) {
        System.out.println("  [SYNC OBSERVER] Real-time SLA Trigger: " + event.getMetricName() 
            + " exceeded limit (" + event.getCurrentValue() + " > " + event.getThreshold() 
            + ") on Snapshot: " + event.getSnapshotId() 
            + " | Thread: " + Thread.currentThread().getName());
    }

    public void onAsyncAudit(@ObservesAsync TelemetryAnomalyEvent event) {
        System.out.println("  [ASYNC OBSERVER] Asynchronous Audit Log recorded: " + event.getMetricName() 
            + " for Snapshot: " + event.getSnapshotId() 
            + " | Thread: " + Thread.currentThread().getName());
    }
}
