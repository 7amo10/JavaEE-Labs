package com.ee.lab.sse.control;

import com.ee.lab.sse.entity.ClusterAlertEvent;
import com.ee.lab.sse.entity.ClusterTelemetryEvent;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.concurrent.atomic.AtomicInteger;

public class ClusterBroadcasterManager {

    private static final ClusterBroadcasterManager INSTANCE = new ClusterBroadcasterManager();

    private SseBroadcaster broadcaster;
    private final AtomicInteger activeSinksCount = new AtomicInteger(0);
    private final AtomicInteger eventSequence = new AtomicInteger(1);
    private boolean initialized = false;

    public static ClusterBroadcasterManager getInstance() {
        return INSTANCE;
    }

    public synchronized void registerSink(SseEventSink sink, Sse sse) {
        ensureInitialized(sse);

        broadcaster.register(sink);
        int currentCount = activeSinksCount.incrementAndGet();
        System.out.printf("[BROADCASTER] Registered new SseEventSink. Total active sinks: %d%n", currentCount);

        // Send initial connection handshake event
        OutboundSseEvent welcomeEvent = sse.newEventBuilder()
            .name("cluster-welcome")
            .id("sys-" + eventSequence.getAndIncrement())
            .reconnectDelay(5000)
            .mediaType(MediaType.TEXT_PLAIN_TYPE)
            .data(String.class, "CONNECTED_TO_CLUSTER_BROADCAST_CHANNEL")
            .comment("Handshake event")
            .build();

        sink.send(welcomeEvent);
    }

    public synchronized void broadcastTelemetry(Sse sse, ClusterTelemetryEvent telemetry) {
        ensureInitialized(sse);

        OutboundSseEvent event = sse.newEventBuilder()
            .name("cluster-telemetry")
            .id("telem-" + eventSequence.getAndIncrement())
            .reconnectDelay(3000)
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .data(ClusterTelemetryEvent.class, telemetry)
            .comment("Periodic cluster node telemetry")
            .build();

        System.out.printf("[BROADCASTER] Broadcasting telemetry to %d active client(s): nodeId=%s, cpu=%.1f%%%n",
            activeSinksCount.get(), telemetry.nodeId(), telemetry.cpuLoadPercent());

        broadcaster.broadcast(event);
    }

    public synchronized void broadcastAlert(Sse sse, ClusterAlertEvent alert) {
        ensureInitialized(sse);

        OutboundSseEvent event = sse.newEventBuilder()
            .name("cluster-alert")
            .id("alert-" + eventSequence.getAndIncrement())
            .reconnectDelay(3000)
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .data(ClusterAlertEvent.class, alert)
            .comment("High priority cluster security/health alert")
            .build();

        System.out.printf("[BROADCASTER] Broadcasting ALERT [%s] to %d active client(s): %s%n",
            alert.severity(), activeSinksCount.get(), alert.description());

        broadcaster.broadcast(event);
    }

    public int getActiveSinksCount() {
        return activeSinksCount.get();
    }

    private synchronized void ensureInitialized(Sse sse) {
        if (!initialized) {
            this.broadcaster = sse.newBroadcaster();

            this.broadcaster.onClose(sink -> {
                int remaining = activeSinksCount.decrementAndGet();
                if (remaining < 0) {
                    activeSinksCount.set(0);
                    remaining = 0;
                }
                System.out.printf("[BROADCASTER-HOOK] Client SseEventSink disconnected/closed. Remaining sinks: %d%n", remaining);
            });

            this.broadcaster.onError((sink, throwable) -> {
                System.err.printf("[BROADCASTER-HOOK] Error on SseEventSink (%s): %s%n",
                    sink, throwable.getMessage());
            });

            this.initialized = true;
            System.out.println("[BROADCASTER] SseBroadcaster initialized with onClose/onError lifecycle listeners.");
        }
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (broadcaster != null) {
            System.out.println("[BROADCASTER] Shutting down SseBroadcaster.");
            broadcaster.close();
            activeSinksCount.set(0);
        }
    }
}
