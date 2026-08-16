# Day 02 Lab: CDI Qualifiers, Producers, Interceptors, and Decoupled Events

This laboratory exercise explores advanced CDI 4.0 component wiring and runtime interception mechanics in Jakarta EE 10 using Weld SE.

## Lab Architecture

The lab implements five enterprise patterns in package `com.ee.lab.cdi`:

1. **Custom Qualifiers (`@MetricEngine`)**: Resolves ambiguous dependency injection between multiple implementations (`HighThroughputTelemetryProcessor` vs `StandardTelemetryProcessor`) without brittle string-based bean names.
2. **Producers and Disposers (`@Produces`, `@Disposes`)**: Dynamic instantiation of external infrastructure (`ThreadMXBean`) and managed resources (`TelemetryChannel`) with lifecycle cleanup callbacks upon context teardown.
3. **Around-Invoke Interceptors (`@Monitored`)**: Non-invasive performance profiling that transparently intercepts method invocations, measuring wall-clock duration and thread CPU consumption via `ThreadMXBean`.
4. **Decoupled Synchronous and Asynchronous Events (`@Observes`, `@ObservesAsync`)**: Event-driven decoupling using `Event<AuditEvent>`, contrasting same-thread synchronous execution with worker-pool asynchronous execution.
5. **Business Service (`OrderProcessingService`)**: Glues all components into an end-to-end transactional workflow.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the lab:

```bash
mvn clean compile exec:java
```

## Expected Runtime Output Analysis

When executing `AppRunner`, verify the following execution sequence:

1. **Interceptor Invocation**: `PerformanceMonitoringInterceptor` wraps `processOrder()`, logging method entry and exit with duration in milliseconds and nanoseconds.
2. **Qualifier Resolution**: CDI injects the specific implementation requested by `@MetricEngine(EngineType.HIGH_THROUGHPUT)` without injection ambiguity errors (`AmbiguousResolutionException`).
3. **Synchronous vs Asynchronous Events**:
   * Synchronous observer (`@Observes`) executes immediately on the main caller thread.
   * Asynchronous observer (`@ObservesAsync`) executes independently on a separate worker thread.
4. **Resource Disposal**: When the container shuts down, `@Disposes` is triggered automatically on `SystemInfrastructureProducer.closeTelemetryChannel()`, ensuring clean resource deallocation.
