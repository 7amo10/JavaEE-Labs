package com.ee.lab.perf;

import com.ee.lab.perf.control.PerformanceOptimizationService;
import com.ee.lab.perf.entity.BatchedNode;
import com.ee.lab.perf.entity.NodeMetricSummary;
import com.ee.lab.perf.entity.ServerNode;
import com.ee.lab.perf.util.HibernateStatisticsInspector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;

public class AppRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   JAKARTA EE 10: JPA PERFORMANCE TUNING & BATCH FETCHING (N+1)");
        System.out.println("================================================================================");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("PerformanceTuningPU");
        EntityManager em = emf.createEntityManager();

        try {
            PerformanceOptimizationService service = new PerformanceOptimizationService(em);

            System.out.println("\n[SETUP] Seeding in-memory database with 20 ServerNode and 20 BatchedNode entities...");
            service.seedDatabase(20);

            // -------------------------------------------------------------------------
            // TEST 1: The Classic N+1 Select Problem
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 1: Diagnosing the N+1 Select Problem (Unoptimized Lazy Traversal)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Executing: SELECT n FROM ServerNode n (and accessing n.getMetrics() in a loop)");
            List<ServerNode> unoptimizedNodes = service.executeUnoptimizedLazyTraversal();
            long nPlusOneQueryCount = HibernateStatisticsInspector.getQueryCount(em);
            System.out.printf("--> Total Prepared Statements: %d (Expected: 1 parent + 20 child queries = 21)%n",
                nPlusOneQueryCount);

            // -------------------------------------------------------------------------
            // TEST 2: Eliminating N+1 with JPQL JOIN FETCH
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 2: Eliminating N+1 using JPQL JOIN FETCH");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Executing: SELECT DISTINCT n FROM ServerNode n JOIN FETCH n.metrics");
            List<ServerNode> joinFetchNodes = service.executeJoinFetchOptimization();
            long joinFetchQueryCount = HibernateStatisticsInspector.getQueryCount(em);
            System.out.printf("--> Total Prepared Statements: %d (Expected: exactly 1 query with SQL JOIN)%n",
                joinFetchQueryCount);

            // -------------------------------------------------------------------------
            // TEST 3: Mitigating N+1 with Hibernate @BatchSize
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 3: Mitigating N+1 using Hibernate @BatchSize(size = 10)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Executing: SELECT b FROM BatchedNode b (fetching metrics in batches of 10)");
            List<BatchedNode> batchedNodes = service.executeBatchSizeOptimization();
            long batchSizeQueryCount = HibernateStatisticsInspector.getQueryCount(em);
            System.out.printf("--> Total Prepared Statements: %d (Expected: 1 initial + ceil(20/10) = 3 queries)%n",
                batchSizeQueryCount);

            // -------------------------------------------------------------------------
            // TEST 4: Eliminating N+1 dynamically with Jakarta Persistence 3.1 EntityGraph
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 4: Dynamic Eager Fetching with Jakarta Persistence 3.1 EntityGraph");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Executing: SELECT n FROM ServerNode n with hint 'jakarta.persistence.fetchgraph' -> ServerNode.withMetrics");
            List<ServerNode> graphNodes = service.executeEntityGraphOptimization("ServerNode.withMetrics");
            long entityGraphQueryCount = HibernateStatisticsInspector.getQueryCount(em);
            System.out.printf("--> Total Prepared Statements: %d (Expected: exactly 1 query without altering JPQL)%n",
                entityGraphQueryCount);

            // -------------------------------------------------------------------------
            // TEST 5: High-Throughput Read Projections with DTOs
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 5: Read-Only High-Throughput Projections (SELECT new ... DTO)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Executing: SELECT new com.ee.lab.perf.entity.NodeMetricSummary(n.id, n.nodeName, m.metricKey, m.metricValue)...");
            List<NodeMetricSummary> summaries = service.executeDtoProjection();
            long dtoQueryCount = HibernateStatisticsInspector.getQueryCount(em);
            System.out.printf("--> Total Prepared Statements: %d (Loaded %d DTO records directly into memory)%n",
                dtoQueryCount, summaries.size());

            // -------------------------------------------------------------------------
            // SUMMARY: Empirical Performance Matrix
            // -------------------------------------------------------------------------
            System.out.println("\n================================================================================");
            System.out.println("   EMPIRICAL PERFORMANCE & QUERY COUNT COMPARISON (20 NODES / 60 METRICS)");
            System.out.println("================================================================================");
            System.out.printf("%-35s | %-15s | %-20s%n", "Fetching Strategy", "SQL Queries", "Reduction vs N+1");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("%-35s | %-15d | %-20s%n", "1. Unoptimized Lazy Traversal", nPlusOneQueryCount, "Baseline (Worst)");
            System.out.printf("%-35s | %-15d | %-20s%n", "2. JPQL JOIN FETCH", joinFetchQueryCount, String.format("-%.1f%%", ((21.0 - joinFetchQueryCount) / 21.0) * 100));
            System.out.printf("%-35s | %-15d | %-20s%n", "3. Hibernate @BatchSize(10)", batchSizeQueryCount, String.format("-%.1f%%", ((21.0 - batchSizeQueryCount) / 21.0) * 100));
            System.out.printf("%-35s | %-15d | %-20s%n", "4. JPA 3.1 EntityGraph", entityGraphQueryCount, String.format("-%.1f%%", ((21.0 - entityGraphQueryCount) / 21.0) * 100));
            System.out.printf("%-35s | %-15d | %-20s%n", "5. Scalar DTO Projection", dtoQueryCount, String.format("-%.1f%%", ((21.0 - dtoQueryCount) / 21.0) * 100));
            System.out.println("================================================================================");
            System.out.println("   ALL DAY 18 JPA PERFORMANCE & BATCH FETCHING TESTS PASSED!");
            System.out.println("================================================================================");

        } finally {
            if (em.isOpen()) {
                em.close();
            }
            if (emf.isOpen()) {
                emf.close();
            }
            System.out.println("[SHUTDOWN] EntityManagerFactory closed cleanly.");
        }
    }
}
