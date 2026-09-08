package com.ee.lab.concurrency.service;

import com.ee.lab.concurrency.context.SecurityContextHolder;
import com.ee.lab.concurrency.managed.ManagedExecutorServiceImpl;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class TelemetryAnalyticsService {

    private final ManagedExecutorServiceImpl managedExecutor;

    public TelemetryAnalyticsService(ManagedExecutorServiceImpl managedExecutor) {
        this.managedExecutor = managedExecutor;
    }

    /**
     * Collects telemetry asynchronously from a single cluster node.
     * Propagates calling-thread SecurityContext and CorrelationId to the worker thread.
     */
    public CompletableFuture<NodeTelemetryReport> collectNodeTelemetryAsync(
            String nodeId, double cpu, double memoryMb, int threads, long simulatedLatencyMs) {

        return managedExecutor.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                if (simulatedLatencyMs > 0) {
                    Thread.sleep(simulatedLatencyMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long duration = System.currentTimeMillis() - startTime;
            String currentThread = Thread.currentThread().getName();
            String principal = SecurityContextHolder.getPrincipalName();
            String correlationId = SecurityContextHolder.getCorrelationId();

            return new NodeTelemetryReport(
                    nodeId,
                    cpu,
                    memoryMb,
                    threads,
                    principal,
                    correlationId,
                    currentThread,
                    duration
            );
        });
    }

    /**
     * Parallel Fan-Out / Fan-In:
     * Submits telemetry collection tasks across multiple nodes concurrently,
     * waits for all to complete via CompletableFuture.allOf(), and combines into ClusterAnalyticsResult.
     */
    public CompletableFuture<ClusterAnalyticsResult> fanOutClusterAnalysis(List<CompletableFuture<NodeTelemetryReport>> futures) {
        long startTime = System.currentTimeMillis();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        return allOf.thenApplyAsync(v -> {
            List<NodeTelemetryReport> reports = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            double totalCpu = reports.stream().mapToDouble(NodeTelemetryReport::cpuUsage).sum();
            double totalMem = reports.stream().mapToDouble(NodeTelemetryReport::memoryUsageMb).sum();
            int totalThreads = reports.stream().mapToInt(NodeTelemetryReport::activeThreads).sum();
            double avgCpu = reports.isEmpty() ? 0.0 : Math.round((totalCpu / reports.size()) * 10.0) / 10.0;
            long totalDuration = System.currentTimeMillis() - startTime;

            return new ClusterAnalyticsResult(
                    reports.size(),
                    avgCpu,
                    totalMem,
                    totalThreads,
                    reports,
                    totalDuration
            );
        }, managedExecutor);
    }

    /**
     * Non-blocking Timeout & Fallback:
     * If node collection exceeds timeoutMs, completeOnTimeout returns a degraded fallback report.
     */
    public CompletableFuture<NodeTelemetryReport> collectWithFallbackOnTimeout(
            String nodeId, long simulatedLatencyMs, long timeoutMs) {

        NodeTelemetryReport fallbackReport = new NodeTelemetryReport(
                nodeId,
                -1.0,
                0.0,
                0,
                SecurityContextHolder.getPrincipalName(),
                SecurityContextHolder.getCorrelationId(),
                "FALLBACK-TIMEOUT",
                timeoutMs
        );

        CompletableFuture<NodeTelemetryReport> future = collectNodeTelemetryAsync(nodeId, 45.0, 1024.0, 20, simulatedLatencyMs);
        return managedExecutor.applyTimeout(future, fallbackReport, timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Resilient Asynchronous Pipeline:
     * Catches asynchronous exceptions and recovers gracefully using .exceptionally().
     */
    public CompletableFuture<NodeTelemetryReport> collectWithExceptionRecovery(String nodeId, boolean triggerError) {
        return managedExecutor.supplyAsync(() -> {
            if (triggerError) {
                throw new RuntimeException("Hardware socket connection failure on " + nodeId);
            }
            return new NodeTelemetryReport(
                    nodeId, 25.0, 512.0, 10,
                    SecurityContextHolder.getPrincipalName(),
                    SecurityContextHolder.getCorrelationId(),
                    Thread.currentThread().getName(),
                    10
            );
        }).exceptionally(ex -> new NodeTelemetryReport(
                nodeId,
                0.0,
                0.0,
                0,
                SecurityContextHolder.getPrincipalName(),
                SecurityContextHolder.getCorrelationId(),
                "RECOVERED-EXCEPTION: " + ex.getMessage(),
                0
        ));
    }
}
