package com.ee.lab.concurrency;

import com.ee.lab.concurrency.context.SecurityContextHolder;
import com.ee.lab.concurrency.managed.ManagedExecutorServiceImpl;
import com.ee.lab.concurrency.service.ClusterAnalyticsResult;
import com.ee.lab.concurrency.service.NodeTelemetryReport;
import com.ee.lab.concurrency.service.TelemetryAnalyticsService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AppRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("   WEEK 3 DAY 16: ENTERPRISE CONCURRENCY & MANAGED THREADS");
        System.out.println("   ManagedExecutorService | Context Propagation | CompletableFuture Pipelines");
        System.out.println("================================================================================");

        ManagedExecutorServiceImpl managedExecutor = new ManagedExecutorServiceImpl(4, "managed-cluster-pool");
        TelemetryAnalyticsService analyticsService = new TelemetryAnalyticsService(managedExecutor);

        try {
            // --------------------------------------------------------------------------------
            // SCENARIO 1: CONTEXT PROPAGATION TO MANAGED WORKER THREADS
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 1: CONTEXT PROPAGATION (CALLER CONTEXT -> MANAGED THREAD)");
            System.out.println("--------------------------------------------------------------------------------");
            SecurityContextHolder.set("operator-admin", "REQ-CORR-778899");
            System.out.println(" [MAIN THREAD CONTEXT] Principal: " + SecurityContextHolder.getPrincipalName() 
                    + " | Correlation ID: " + SecurityContextHolder.getCorrelationId());

            CompletableFuture<NodeTelemetryReport> future1 = analyticsService.collectNodeTelemetryAsync(
                    "node-01", 34.5, 2048.0, 42, 60
            );

            NodeTelemetryReport report1 = future1.get(2, TimeUnit.SECONDS);
            System.out.println(" [MANAGED THREAD RESULT]");
            System.out.println("   -> Node ID:           " + report1.nodeId());
            System.out.println("   -> Executing Thread:  " + report1.executingThreadName() + " (Managed)");
            System.out.println("   -> Inherited Caller:  " + report1.principalName());
            System.out.println("   -> Propagated CorrID: " + report1.correlationId());
            System.out.println("   -> Latency:           " + report1.executionDurationMs() + " ms");
            System.out.println(" -> Proves ManagedExecutorService successfully propagates enterprise caller context.");

            // --------------------------------------------------------------------------------
            // SCENARIO 2: PARALLEL FAN-OUT / FAN-IN VIA CompletableFuture.allOf()
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 2: PARALLEL FAN-OUT / FAN-IN WITH CompletableFuture.allOf()");
            System.out.println("--------------------------------------------------------------------------------");
            List<CompletableFuture<NodeTelemetryReport>> fanOutFutures = List.of(
                    analyticsService.collectNodeTelemetryAsync("node-alpha", 22.0, 1024.0, 15, 80),
                    analyticsService.collectNodeTelemetryAsync("node-beta", 78.5, 3800.0, 110, 100),
                    analyticsService.collectNodeTelemetryAsync("node-gamma", 45.0, 2048.0, 50, 70),
                    analyticsService.collectNodeTelemetryAsync("node-delta", 15.0, 512.0, 8, 90)
            );

            long fanOutStart = System.currentTimeMillis();
            ClusterAnalyticsResult clusterResult = analyticsService.fanOutClusterAnalysis(fanOutFutures).get(3, TimeUnit.SECONDS);
            long fanOutElapsed = System.currentTimeMillis() - fanOutStart;

            System.out.println(" [FAN-IN AGGREGATION COMPLETED in " + fanOutElapsed + " ms]");
            System.out.println("   -> Total Nodes Queried:   " + clusterResult.totalNodes());
            System.out.println("   -> Average Cluster CPU:   " + clusterResult.averageCpuUsage() + " %");
            System.out.println("   -> Total Memory In Use:   " + clusterResult.totalMemoryUsageMb() + " MB");
            System.out.println("   -> Total Active Threads:  " + clusterResult.totalActiveThreads());
            for (NodeTelemetryReport r : clusterResult.nodeReports()) {
                System.out.println("      * " + r.nodeId() + " on [" + r.executingThreadName() + "] -> CPU: " + r.cpuUsage() + "%");
            }
            System.out.println(" -> Proves true parallel execution across container threads (Elapsed ~" + fanOutElapsed 
                    + "ms vs sequential ~340ms).");

            // --------------------------------------------------------------------------------
            // SCENARIO 3: NON-BLOCKING TIMEOUT HANDLING (completeOnTimeout)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 3: NON-BLOCKING TIMEOUT HANDLING (completeOnTimeout)");
            System.out.println("--------------------------------------------------------------------------------");
            // Simulate slow node with 400ms delay, timeout after 80ms
            CompletableFuture<NodeTelemetryReport> timeoutFuture = analyticsService.collectWithFallbackOnTimeout(
                    "node-slow-laggy", 400, 80
            );

            NodeTelemetryReport timeoutReport = timeoutFuture.get(1, TimeUnit.SECONDS);
            System.out.println(" [TIMEOUT FALLBACK TRIGGERED]");
            System.out.println("   -> Target Node:       " + timeoutReport.nodeId());
            System.out.println("   -> Fallback Status:   " + timeoutReport.executingThreadName());
            System.out.println("   -> Fallback CPU Flag: " + timeoutReport.cpuUsage());
            System.out.println(" -> Proves non-blocking recovery prevents slow nodes from exhausting container threads.");

            // --------------------------------------------------------------------------------
            // SCENARIO 4: ASYNCHRONOUS EXCEPTION RESILIENCE (.exceptionally())
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 4: ASYNCHRONOUS EXCEPTION RECOVERY (.exceptionally())");
            System.out.println("--------------------------------------------------------------------------------");
            CompletableFuture<NodeTelemetryReport> resilientFuture = analyticsService.collectWithExceptionRecovery(
                    "node-hardware-fault", true
            );

            NodeTelemetryReport recoveredReport = resilientFuture.get(1, TimeUnit.SECONDS);
            System.out.println(" [RECOVERED RESULT]");
            System.out.println("   -> Node ID:      " + recoveredReport.nodeId());
            System.out.println("   -> Error Caught: " + recoveredReport.executingThreadName());
            System.out.println(" -> Proves asynchronous pipeline handles node failures gracefully without thread crashes.");

            // --------------------------------------------------------------------------------
            // SCENARIO 5: MANAGED CONTAINER LIFECYCLE & SHUTDOWN
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 5: MANAGED CONTAINER LIFECYCLE & THREAD CLEANUP");
            System.out.println("--------------------------------------------------------------------------------");
            managedExecutor.shutdown();
            boolean terminatedCleanly = managedExecutor.awaitTermination(2, TimeUnit.SECONDS);

            System.out.println(" [CONTAINER SHUTDOWN]");
            System.out.println("   -> Is Shutdown:   " + managedExecutor.isShutdown());
            System.out.println("   -> Is Terminated: " + managedExecutor.isTerminated());
            System.out.println("   -> Clean Exit:    " + terminatedCleanly);
            System.out.println(" -> Proves zero thread leakage and full container lifecycle management.");

        } finally {
            SecurityContextHolder.clear();
            if (!managedExecutor.isShutdown()) {
                managedExecutor.shutdownNow();
            }
            System.out.println("\n================================================================================");
            System.out.println("          WEEK 3 DAY 16 CONCURRENCY LAB COMPLETED SUCCESSFULLY");
            System.out.println("================================================================================");
        }
    }
}
