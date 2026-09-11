package com.ee.lab.tuning;

import com.ee.lab.tuning.boundary.MetricsThroughputResource;
import com.ee.lab.tuning.control.BenchmarkExecutionService;
import com.ee.lab.tuning.control.DataSourceManager;
import com.ee.lab.tuning.control.HttpServerManager;
import com.ee.lab.tuning.control.PerformanceTelemetryService;
import com.ee.lab.tuning.entity.BenchmarkResult;
import com.ee.lab.tuning.entity.GcPauseMetric;
import com.ee.lab.tuning.entity.PoolStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AppRunner {

    private static final Logger LOGGER = Logger.getLogger(AppRunner.class.getName());
    private static final int SERVER_PORT = 8085;
    private static final String BASE_URL = "http://localhost:" + SERVER_PORT + "/metrics/query";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   WEEK 3 DAY 19: DATABASE CONNECTION POOL & HTTP THROUGHPUT TUNING LAB         ");
        System.out.println("================================================================================\n");

        BenchmarkExecutionService benchmarkService = new BenchmarkExecutionService();
        PerformanceTelemetryService telemetryService = new PerformanceTelemetryService();
        List<BenchmarkResult> results = new ArrayList<>();

        try {
            // -------------------------------------------------------------------------
            // SCENARIO 1: Connection Pool Starvation & Saturation
            // -------------------------------------------------------------------------
            System.out.println("[SCENARIO 1] Connection Pool Starvation & Saturation Baseline");
            System.out.println("Config: Grizzly Worker Threads = 32, HikariCP MaxPoolSize = 2, Concurrency = 25");
            System.out.println("Explanation: Heavy thread contention on an undersized pool triggers queue wait & timeouts.");

            HttpServerManager server1 = new HttpServerManager(SERVER_PORT, 32);
            DataSourceManager ds1 = new DataSourceManager("StarvedPool", 2, 1, 250); // very low pool size & low timeout
            MetricsThroughputResource.setActiveDataSource(ds1);
            MetricsThroughputResource.resetCounters();
            server1.start();

            // Warmup
            benchmarkService.runBenchmark("Warmup", 2, 32, BASE_URL + "?delay=2", 20, 2);
            MetricsThroughputResource.resetCounters();

            // Run load: 150 requests with 25 concurrent clients and 8ms DB delay
            BenchmarkResult res1 = benchmarkService.runBenchmark(
                    "Starved-Pool", 2, 32, BASE_URL + "?delay=8", 150, 25);
            results.add(res1);

            PoolStatistics stats1 = ds1.getPoolStatistics();
            System.out.printf("   Throughput: %.2f req/s | Mean Latency: %.2f ms | P99 Latency: %.2f ms\n",
                    res1.requestsPerSecond(), res1.meanLatencyMs(), res1.p99LatencyMs());
            System.out.printf("   Pool Threads Awaiting Conn: %d | Total Timeouts/Failures: %d\n\n",
                    stats1.threadsAwaitingConnection(), res1.poolTimeoutErrors());

            server1.stop();
            ds1.close();
            Thread.sleep(500);

            // -------------------------------------------------------------------------
            // SCENARIO 2: Tuned Production Ratio (Pool Size = 16, Threads = 16)
            // -------------------------------------------------------------------------
            System.out.println("[SCENARIO 2] Tuned Production Balance (HikariCP / Thread Sizing Formula)");
            System.out.println("Formula: Pool Size = (Core Count * 2) + Spindle Effective Count");
            System.out.println("Config: Grizzly Worker Threads = 16, HikariCP MaxPoolSize = 16, Concurrency = 25");

            HttpServerManager server2 = new HttpServerManager(SERVER_PORT, 16);
            DataSourceManager ds2 = new DataSourceManager("TunedPool", 16, 8, 3000);
            MetricsThroughputResource.setActiveDataSource(ds2);
            MetricsThroughputResource.resetCounters();
            server2.start();

            BenchmarkResult res2 = benchmarkService.runBenchmark(
                    "Tuned-Balanced", 16, 16, BASE_URL + "?delay=8", 150, 25);
            results.add(res2);

            PoolStatistics stats2 = ds2.getPoolStatistics();
            System.out.printf("   Throughput: %.2f req/s | Mean Latency: %.2f ms | P99 Latency: %.2f ms\n",
                    res2.requestsPerSecond(), res2.meanLatencyMs(), res2.p99LatencyMs());
            System.out.printf("   Pool Active: %d | Idle: %d | Timeouts/Failures: %d\n\n",
                    stats2.activeConnections(), stats2.idleConnections(), res2.poolTimeoutErrors());

            server2.stop();
            ds2.close();
            Thread.sleep(500);

            // -------------------------------------------------------------------------
            // SCENARIO 3: Oversized Connection Pool & Context Switch Contention
            // -------------------------------------------------------------------------
            System.out.println("[SCENARIO 3] Oversized Connection Pool Anti-Pattern");
            System.out.println("Config: Grizzly Worker Threads = 64, HikariCP MaxPoolSize = 80, Concurrency = 25");
            System.out.println("Explanation: Excessive database connections cause CPU context switching and lock overhead.");

            HttpServerManager server3 = new HttpServerManager(SERVER_PORT, 64);
            DataSourceManager ds3 = new DataSourceManager("OversizedPool", 80, 20, 5000);
            MetricsThroughputResource.setActiveDataSource(ds3);
            MetricsThroughputResource.resetCounters();
            server3.start();

            BenchmarkResult res3 = benchmarkService.runBenchmark(
                    "Oversized-Pool", 80, 64, BASE_URL + "?delay=8", 150, 25);
            results.add(res3);

            PoolStatistics stats3 = ds3.getPoolStatistics();
            System.out.printf("   Throughput: %.2f req/s | Mean Latency: %.2f ms | P99 Latency: %.2f ms\n",
                    res3.requestsPerSecond(), res3.meanLatencyMs(), res3.p99LatencyMs());
            System.out.printf("   Total Connections Open: %d | Idle: %d\n\n",
                    stats3.totalConnections(), stats3.idleConnections());

            server3.stop();
            ds3.close();
            Thread.sleep(500);

            // -------------------------------------------------------------------------
            // SCENARIO 4: GC Pauses Correlated with Latency Spikes
            // -------------------------------------------------------------------------
            System.out.println("[SCENARIO 4] Correlating Garbage Collection Pauses with Latency Spikes");
            System.out.println("Config: Tuned Pool (16) with Memory Pressure Allocations on Request Path");

            HttpServerManager server4 = new HttpServerManager(SERVER_PORT, 16);
            DataSourceManager ds4 = new DataSourceManager("GcTunedPool", 16, 8, 3000);
            MetricsThroughputResource.setActiveDataSource(ds4);
            MetricsThroughputResource.resetCounters();
            server4.start();

            long initialGcTime = telemetryService.getTotalGcPauseTimeMs();

            BenchmarkResult res4 = benchmarkService.runBenchmark(
                    "Gc-Pressure-Load", 16, 16, BASE_URL + "?delay=8&gcPressure=25", 150, 25);
            results.add(res4);

            long finalGcTime = telemetryService.getTotalGcPauseTimeMs();
            long deltaGcTime = finalGcTime - initialGcTime;

            System.out.printf("   Throughput: %.2f req/s | Mean Latency: %.2f ms | P99 Latency: %.2f ms\n",
                    res4.requestsPerSecond(), res4.meanLatencyMs(), res4.p99LatencyMs());
            System.out.printf("   GC Pause Delta during load: %d ms across collectors\n", deltaGcTime);

            List<GcPauseMetric> gcMetrics = telemetryService.captureGcMetrics();
            for (GcPauseMetric m : gcMetrics) {
                System.out.printf("     - Collector [%s]: Count=%d, Accumulated Time=%d ms (Heap: %d MB / %d MB)\n",
                        m.gcName(), m.collectionCount(), m.collectionTimeMs(), m.memoryUsedMb(), m.memoryMaxMb());
            }

            server4.stop();
            ds4.close();

            // -------------------------------------------------------------------------
            // COMPARATIVE SUMMARY
            // -------------------------------------------------------------------------
            System.out.println("\n================================================================================");
            System.out.println("                EMPIRICAL PERFORMANCE COMPARISON SUMMARY                        ");
            System.out.println("================================================================================");
            System.out.printf("%-18s | %-9s | %-8s | %-12s | %-10s | %-9s\n",
                    "Profile", "Pool Size", "Workers", "Throughput", "Mean Latency", "P99 Latency");
            System.out.println("--------------------------------------------------------------------------------");
            for (BenchmarkResult r : results) {
                System.out.printf("%-18s | %-9d | %-8d | %8.1f rps | %8.2f ms | %7.2f ms\n",
                        r.profileName(), r.poolMaxSize(), r.workerThreads(),
                        r.requestsPerSecond(), r.meanLatencyMs(), r.p99LatencyMs());
            }
            System.out.println("================================================================================\n");

        } catch (Exception e) {
            LOGGER.severe("Error executing lab scenarios: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
