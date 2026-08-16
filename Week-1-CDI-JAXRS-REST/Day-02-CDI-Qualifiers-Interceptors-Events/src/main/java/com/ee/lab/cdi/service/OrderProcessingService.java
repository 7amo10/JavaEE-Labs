package com.ee.lab.cdi.service;

import com.ee.lab.cdi.events.AuditEvent;
import com.ee.lab.cdi.interceptor.Monitored;
import com.ee.lab.cdi.producers.TelemetryChannel;
import com.ee.lab.cdi.qualifiers.EngineType;
import com.ee.lab.cdi.qualifiers.MetricEngine;
import com.ee.lab.cdi.qualifiers.TelemetryProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class OrderProcessingService {

    @Inject
    @MetricEngine(EngineType.HIGH_THROUGHPUT)
    private TelemetryProcessor highThroughputProcessor;

    @Inject
    @MetricEngine(EngineType.STANDARD)
    private TelemetryProcessor standardProcessor;

    @Inject
    private TelemetryChannel telemetryChannel;

    @Inject
    private Event<AuditEvent> auditEventBus;

    @Monitored
    public String processOrder(String orderId, double amount) {
        System.out.println(" [BUSINESS LOGIC] Executing processOrder(" + orderId + ", $" + amount + ")");
        System.out.println("   Active High-Throughput Processor: " + highThroughputProcessor.getProcessorType());
        System.out.println("   Active Standard Processor:       " + standardProcessor.getProcessorType());

        // Use produced telemetry channel
        telemetryChannel.transmit("ORDER_CREATED[id=" + orderId + ", amount=" + amount + "]");

        // Record metrics via qualified beans
        highThroughputProcessor.recordMetric("order.amount", amount);
        standardProcessor.recordMetric("order.count", 1.0);

        // Simulate small computational work
        long start = System.nanoTime();
        double checksum = 0;
        for (int i = 0; i < 50_000; i++) {
            checksum += Math.sin(i);
        }
        long duration = System.nanoTime() - start;

        // Dispatch Decoupled Synchronous Event
        AuditEvent syncEvent = new AuditEvent(
            UUID.randomUUID().toString(),
            "ORDER_PROCESSED_SYNC",
            "SUCCESS (checksum=" + String.format("%.2f", checksum) + ")",
            duration,
            Instant.now(),
            Thread.currentThread().getName()
        );
        auditEventBus.fire(syncEvent);

        // Dispatch Decoupled Asynchronous Event
        AuditEvent asyncEvent = new AuditEvent(
            UUID.randomUUID().toString(),
            "ORDER_AUDIT_ASYNC",
            "PERSISTED_TO_ANALYTICS",
            duration,
            Instant.now(),
            Thread.currentThread().getName()
        );
        auditEventBus.fireAsync(asyncEvent);

        return "CONFIRMED-" + orderId;
    }
}
