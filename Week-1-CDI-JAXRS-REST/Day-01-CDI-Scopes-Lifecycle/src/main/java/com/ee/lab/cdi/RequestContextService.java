package com.ee.lab.cdi;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class RequestContextService {

    @Inject
    private ApplicationCounter applicationCounter;

    @Inject
    private DependentHelper dependentHelper;

    public RequestContextService() {
        System.out.println("[RequestContextService] Target Instance Constructor invoked | Hash: " + System.identityHashCode(this));
    }

    @PostConstruct
    public void init() {
        System.out.println("[RequestContextService] Target Instance @PostConstruct invoked | Hash: " + System.identityHashCode(this));
    }

    public void processRequest(String requestId) {
        int currentCount = applicationCounter.incrementAndGet();
        String details = dependentHelper.formatDetails(requestId);
        System.out.println(" --> Processing Request: " + requestId 
            + " | RequestService Hash: " + System.identityHashCode(this)
            + " | AppCounter Hash: " + System.identityHashCode(applicationCounter)
            + " | Global Call #" + currentCount
            + " | " + details);
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("[RequestContextService] Target Instance @PreDestroy invoked | Hash: " + System.identityHashCode(this));
    }
}
