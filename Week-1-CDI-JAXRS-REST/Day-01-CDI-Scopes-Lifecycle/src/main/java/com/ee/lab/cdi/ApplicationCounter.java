package com.ee.lab.cdi;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class ApplicationCounter {

    private final AtomicInteger globalCounter = new AtomicInteger(0);

    public ApplicationCounter() {
        System.out.println("[ApplicationCounter] Constructor invoked | Hash: " + System.identityHashCode(this));
    }

    @PostConstruct
    public void init() {
        System.out.println("[ApplicationCounter] @PostConstruct invoked | Hash: " + System.identityHashCode(this));
    }

    public int incrementAndGet() {
        return globalCounter.incrementAndGet();
    }

    public int getCount() {
        return globalCounter.get();
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("[ApplicationCounter] @PreDestroy invoked | Hash: " + System.identityHashCode(this));
    }
}
