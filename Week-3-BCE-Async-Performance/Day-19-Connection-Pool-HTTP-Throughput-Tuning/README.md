# Day 19: Database Connection Pool & HTTP Throughput Tuning

## Overview
This laboratory explores high-throughput enterprise systems engineering within Jakarta EE 10. It benchmarks and optimizes the interaction between container HTTP worker threads (Grizzly HTTP server), production connection pools (HikariCP), and JVM garbage collection characteristics.

## Key Technical Concepts Demonstrated
1. **HikariCP Pool Sizing & Contention**:
   - Applying the sizing formula: $\text{Pool Size} = (\text{Core Count} \times 2) + \text{Effective Spindle Count}$.
   - Detecting pool starvation: when worker threads outstrip active database connections, threads block in `getConnection()`, triggering connection timeout exceptions and request latency spikes.
2. **HTTP Worker Thread Pool Tuning**:
   - Matching Grizzly worker thread capacity to database connection limits to prevent saturation and context-switching churn.
3. **Correlating GC Pauses with P99 Latency**:
   - Capturing JVM garbage collection telemetry (`GarbageCollectorMXBean`, `MemoryMXBean`) and observing how minor/major GC stop-the-world pauses degrade 99th-percentile (P99) response latencies.
4. **Automated Load Generation**:
   - Running realistic HTTP load tests using `ab` (ApacheBench) or internal multi-threaded HTTP client benchmarks.

## Module Structure
- `com.ee.lab.tuning.entity`: Immutable records for telemetry (`PoolStatistics`, `GcPauseMetric`, `BenchmarkResult`).
- `com.ee.lab.tuning.control`:
  - `DataSourceManager`: Encapsulates HikariCP configuration, pool sizing, and JMX telemetry.
  - `HttpServerManager`: Configures Grizzly HTTP worker thread pools and network listeners.
  - `PerformanceTelemetryService`: Collects JVM memory and GC pause telemetry.
  - `BenchmarkExecutionService`: Drives load testing via `ab` or high-concurrency HTTP execution.
- `com.ee.lab.tuning.boundary`:
  - `MetricsThroughputResource`: JAX-RS 3.1 resource exposing `/metrics/query` and `/metrics/pool-status`.
- `com.ee.lab.tuning`:
  - `AppRunner`: Orchestrates four empirical test scenarios.

## How to Run
```bash
mvn clean compile exec:java
```
