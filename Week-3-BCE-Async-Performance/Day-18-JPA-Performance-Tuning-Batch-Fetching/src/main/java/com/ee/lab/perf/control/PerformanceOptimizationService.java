package com.ee.lab.perf.control;

import com.ee.lab.perf.entity.BatchedNode;
import com.ee.lab.perf.entity.NodeMetricSummary;
import com.ee.lab.perf.entity.SecurityAuditLog;
import com.ee.lab.perf.entity.ServerNode;
import com.ee.lab.perf.entity.TelemetryMetric;
import com.ee.lab.perf.util.HibernateStatisticsInspector;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.time.Instant;
import java.util.List;

public class PerformanceOptimizationService {

    private final EntityManager em;

    public PerformanceOptimizationService(EntityManager em) {
        this.em = em;
    }

    public void seedDatabase(int nodeCount) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        for (int i = 1; i <= nodeCount; i++) {
            // Standard ServerNode
            ServerNode node = new ServerNode("prod-cluster-worker-" + String.format("%02d", i), "us-east-1", "ONLINE");
            node.addMetric(new TelemetryMetric("CPU_USAGE", 35.0 + (i % 30), Instant.now()));
            node.addMetric(new TelemetryMetric("HEAP_MEMORY_MB", 2048.0 + (i * 64), Instant.now()));
            node.addMetric(new TelemetryMetric("ACTIVE_THREADS", 40.0 + (i * 2), Instant.now()));
            node.addAuditLog(new SecurityAuditLog("POLICY_EVALUATION", "INFO", Instant.now()));
            node.addAuditLog(new SecurityAuditLog("CERT_CHECK", "INFO", Instant.now()));
            em.persist(node);

            // BatchedNode (Annotated with @BatchSize(size = 10))
            BatchedNode bNode = new BatchedNode("batch-cluster-worker-" + String.format("%02d", i), "eu-west-1");
            bNode.addMetric(new TelemetryMetric("CPU_USAGE", 20.0 + (i % 20), Instant.now()));
            bNode.addMetric(new TelemetryMetric("HEAP_MEMORY_MB", 1024.0 + (i * 32), Instant.now()));
            bNode.addMetric(new TelemetryMetric("ACTIVE_THREADS", 25.0 + i, Instant.now()));
            em.persist(bNode);
        }

        tx.commit();
        em.clear(); // Clear L1 cache to ensure fresh queries
        System.out.printf("[DATABASE-SEED] Successfully seeded %d ServerNodes and %d BatchedNodes.%n", nodeCount, nodeCount);
    }

    /**
     * Demonstrates the classic N+1 query problem.
     * Fires 1 query for all nodes + N queries to fetch metrics for each node lazily.
     */
    public List<ServerNode> executeUnoptimizedLazyTraversal() {
        em.clear();
        HibernateStatisticsInspector.reset(em);

        long start = System.nanoTime();
        List<ServerNode> nodes = em.createQuery("SELECT n FROM ServerNode n", ServerNode.class)
            .getResultList();

        int totalMetricsCount = 0;
        for (ServerNode node : nodes) {
            // Triggering lazy collection initialization
            totalMetricsCount += node.getMetrics().size();
        }

        long elapsedMicros = (System.nanoTime() - start) / 1000;
        long sqlCount = HibernateStatisticsInspector.getQueryCount(em);
        System.out.printf("  [UNOPTIMIZED N+1] Loaded %d nodes (%d metrics) | SQL Queries: %d | Time: %d us%n",
            nodes.size(), totalMetricsCount, sqlCount, elapsedMicros);

        return nodes;
    }

    /**
     * Solves N+1 using JPQL JOIN FETCH.
     * Fires exactly 1 SQL query with an INNER/LEFT JOIN to hydrate metrics eagerly.
     */
    public List<ServerNode> executeJoinFetchOptimization() {
        em.clear();
        HibernateStatisticsInspector.reset(em);

        long start = System.nanoTime();
        List<ServerNode> nodes = em.createQuery(
            "SELECT DISTINCT n FROM ServerNode n JOIN FETCH n.metrics", ServerNode.class)
            .getResultList();

        int totalMetricsCount = 0;
        for (ServerNode node : nodes) {
            totalMetricsCount += node.getMetrics().size();
        }

        long elapsedMicros = (System.nanoTime() - start) / 1000;
        long sqlCount = HibernateStatisticsInspector.getQueryCount(em);
        System.out.printf("  [JOIN FETCH] Loaded %d nodes (%d metrics) | SQL Queries: %d | Time: %d us%n",
            nodes.size(), totalMetricsCount, sqlCount, elapsedMicros);

        return nodes;
    }

    /**
     * Solves N+1 using Hibernate @BatchSize(size = 10).
     * Batches subselects using WHERE node_id IN (?, ?, ..., ?).
     * For 20 nodes with batch size 10: 1 query for nodes + 2 queries for metrics = 3 queries.
     */
    public List<BatchedNode> executeBatchSizeOptimization() {
        em.clear();
        HibernateStatisticsInspector.reset(em);

        long start = System.nanoTime();
        List<BatchedNode> nodes = em.createQuery("SELECT b FROM BatchedNode b", BatchedNode.class)
            .getResultList();

        int totalMetricsCount = 0;
        for (BatchedNode node : nodes) {
            totalMetricsCount += node.getMetrics().size();
        }

        long elapsedMicros = (System.nanoTime() - start) / 1000;
        long sqlCount = HibernateStatisticsInspector.getQueryCount(em);
        System.out.printf("  [@BatchSize(10)] Loaded %d batched nodes (%d metrics) | SQL Queries: %d | Time: %d us%n",
            nodes.size(), totalMetricsCount, sqlCount, elapsedMicros);

        return nodes;
    }

    /**
     * Solves N+1 dynamically at query time using Jakarta Persistence 3.1 EntityGraph.
     * Overrides lazy fetching without rewriting JPQL or modifying entity annotations.
     */
    public List<ServerNode> executeEntityGraphOptimization(String graphName) {
        em.clear();
        HibernateStatisticsInspector.reset(em);

        long start = System.nanoTime();
        EntityGraph<?> entityGraph = em.getEntityGraph(graphName);

        List<ServerNode> nodes = em.createQuery("SELECT n FROM ServerNode n", ServerNode.class)
            .setHint("jakarta.persistence.fetchgraph", entityGraph)
            .getResultList();

        int totalMetricsCount = 0;
        for (ServerNode node : nodes) {
            totalMetricsCount += node.getMetrics().size();
        }

        long elapsedMicros = (System.nanoTime() - start) / 1000;
        long sqlCount = HibernateStatisticsInspector.getQueryCount(em);
        System.out.printf("  [EntityGraph] Loaded %d nodes via '%s' | SQL Queries: %d | Time: %d us%n",
            nodes.size(), graphName, sqlCount, elapsedMicros);

        return nodes;
    }

    /**
     * Maximizes performance using scalar DTO projections (SELECT new ...).
     * Avoids entity instantiation, dirty-checking, and Persistence Context tracking.
     */
    public List<NodeMetricSummary> executeDtoProjection() {
        em.clear();
        HibernateStatisticsInspector.reset(em);

        long start = System.nanoTime();
        List<NodeMetricSummary> summaries = em.createQuery(
            "SELECT new com.ee.lab.perf.entity.NodeMetricSummary(n.id, n.nodeName, m.metricKey, m.metricValue) " +
            "FROM ServerNode n JOIN n.metrics m", NodeMetricSummary.class)
            .getResultList();

        long elapsedMicros = (System.nanoTime() - start) / 1000;
        long sqlCount = HibernateStatisticsInspector.getQueryCount(em);
        System.out.printf("  [DTO PROJECTION] Loaded %d metric summaries | SQL Queries: %d | Time: %d us%n",
            summaries.size(), sqlCount, elapsedMicros);

        return summaries;
    }
}
