package com.ee.lab.cdi;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

@Dependent
public class DependentHelper {

    public DependentHelper() {
        System.out.println("  [DependentHelper] Constructor invoked | Hash: " + System.identityHashCode(this));
    }

    @PostConstruct
    public void init() {
        System.out.println("  [DependentHelper] @PostConstruct invoked | Hash: " + System.identityHashCode(this));
    }

    public String formatDetails(String requestId) {
        return "FormattedDetails[req=" + requestId + ", helperHash=" + System.identityHashCode(this) + "]";
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("  [DependentHelper] @PreDestroy invoked | Hash: " + System.identityHashCode(this));
    }
}
