package com.ee.lab.pulse.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

@ApplicationScoped
public class PlatformProducer {

    @Produces
    @ApplicationScoped
    public MemoryMXBean produceMemoryMXBean() {
        return ManagementFactory.getMemoryMXBean();
    }

    @Produces
    @ApplicationScoped
    public ThreadMXBean produceThreadMXBean() {
        return ManagementFactory.getThreadMXBean();
    }

    @Produces
    @ApplicationScoped
    public OperatingSystemMXBean produceOperatingSystemMXBean() {
        return ManagementFactory.getOperatingSystemMXBean();
    }
}
