# Week 3 Day 16: Enterprise Concurrency & Managed Threads

This module implements Jakarta Concurrency 3.0 (`jakarta.enterprise.concurrent`) for my JavaEE learning. It demonstrates why raw Java threads (`new Thread()`, unmanaged `Executors`) are strictly forbidden within enterprise application servers, and how container-managed concurrency utilities govern thread lifecycles, propagate runtime contexts, and orchestrate non-blocking asynchronous workflows using Java 21 `CompletableFuture` under package `com.ee.lab.concurrency`.

## Core Architectural Concepts

1. **ManagedExecutorService (MES)**:
   * Container-managed executor service that wraps worker task execution with automated context propagation (Security Principal and request Correlation ID).
   * Ensures worker threads execute under proper container governance and resource limits.
2. **ManagedThreadFactory**:
   * Factory responsible for producing container-aware worker threads with standardized naming (`managed-cluster-pool-worker-X`) and priority.
3. **Context Propagation**:
   * Captures calling thread state (`SecurityContextHolder`) at task submission time and restores it onto the background worker thread before execution, guaranteeing auditability and security consistency.
4. **Modern Asynchronous Pipelines (`CompletableFuture`)**:
   * Fan-out / Fan-in parallel task orchestration using `CompletableFuture.allOf(...)`.
   * Non-blocking timeout recovery using scheduled fallback handlers.
   * Resilient error handling via `.exceptionally(...)` pipeline stages.
5. **Managed Lifecycle Governance**:
   * Graceful thread termination on container shutdown, eliminating thread leaks and orphaned processes.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile the module and run all 5 verification scenarios:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (Context Propagation)**: Verifies that caller security identity (`operator-admin`) and request correlation ID (`REQ-CORR-778899`) are seamlessly propagated to the managed worker thread.
2. **Scenario 2 (Parallel Fan-Out / Fan-In)**: Concurrently collects metrics across 4 cluster nodes and combines them into an aggregated `ClusterAnalyticsResult` in parallel elapsed time (~175ms vs ~340ms sequential).
3. **Scenario 3 (Non-Blocking Timeout Handling)**: Applies non-blocking timeout fallback to prevent slow or unresponsive nodes from tying up worker threads.
4. **Scenario 4 (Asynchronous Exception Resilience)**: Recovers gracefully from unhandled node socket errors using `.exceptionally(...)` without crashing calling threads.
5. **Scenario 5 (Managed Container Lifecycle)**: Confirms orderly shutdown and complete thread termination with zero lingering worker threads.
