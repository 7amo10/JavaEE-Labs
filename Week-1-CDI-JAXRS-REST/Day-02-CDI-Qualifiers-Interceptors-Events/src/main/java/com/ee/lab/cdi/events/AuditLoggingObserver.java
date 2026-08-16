package com.ee.lab.cdi.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

@ApplicationScoped
public class AuditLoggingObserver {

    public void onSyncAudit(@Observes AuditEvent event) {
        System.out.println("  [SYNC OBSERVER] Received event: " + event.action() 
            + " | Status: " + event.status()
            + " | Thread: " + Thread.currentThread().getName() + " [ID: " + Thread.currentThread().threadId() + "]"
            + " (Runs on the SAME thread as caller)");
    }

    public void onAsyncAudit(@ObservesAsync AuditEvent event) {
        System.out.println("  [ASYNC OBSERVER] Processing async audit -> EventID: " + event.eventId()
            + " | Action: " + event.action()
            + " | Duration: " + event.durationNanos() + " ns"
            + " | Thread: " + Thread.currentThread().getName() + " [ID: " + Thread.currentThread().threadId() + "]"
            + " (Runs on SEPARATE worker thread pool)");
    }
}
