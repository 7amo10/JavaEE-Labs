package com.ee.lab.cdi.events;

import java.time.Instant;

public record AuditEvent(
    String eventId,
    String action,
    String status,
    long durationNanos,
    Instant timestamp,
    String originatingThread
) {}
