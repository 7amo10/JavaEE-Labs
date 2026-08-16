package com.ee.lab.cdi.producers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.UUID;

@ApplicationScoped
public class SystemInfrastructureProducer {

    @Produces
    @ApplicationScoped
    public ThreadMXBean produceThreadMXBean() {
        System.out.println("[PRODUCER] Producing global ThreadMXBean instance");
        return ManagementFactory.getThreadMXBean();
    }

    @Produces
    @Dependent
    public TelemetryChannel produceTelemetryChannel() {
        String channelId = "CH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("[PRODUCER] Factory producing TelemetryChannel [" + channelId + "]");
        return new TelemetryChannel(channelId);
    }

    public void closeTelemetryChannel(@Disposes TelemetryChannel channel) {
        System.out.println("[DISPOSER] @Disposes invoked for TelemetryChannel [" + channel.getChannelId() + "]");
        channel.close();
    }
}
