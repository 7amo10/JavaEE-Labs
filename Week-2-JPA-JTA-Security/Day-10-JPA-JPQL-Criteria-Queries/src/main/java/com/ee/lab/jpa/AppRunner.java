package com.ee.lab.jpa;

import com.ee.lab.jpa.dto.NodeMetricSummaryDto;
import com.ee.lab.jpa.dto.NodeSearchCriteria;
import com.ee.lab.jpa.entity.ClusterNode;
import com.ee.lab.jpa.entity.NodeStatus;
import com.ee.lab.jpa.entity.ServerRack;
import com.ee.lab.jpa.entity.TelemetryRecord;
import com.ee.lab.jpa.repository.ClusterAnalyticsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;

public class AppRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   WEEK 2 DAY 10: JPA 3.1 DYNAMIC JPQL, PROJECTIONS & CRITERIA API");
        System.out.println("   Named Queries | DTO Projections | JOIN FETCH | CriteriaBuilder | Pagination");
        System.out.println("================================================================================");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("JpaQueryPU");

        try {
            seedDatabase(emf);

            // --------------------------------------------------------------------------------
            // SCENARIO 1: NAMED JPQL QUERIES WITH PARAMETERS
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 1: STATIC NAMED JPQL QUERIES WITH PARAMETERS");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em1 = emf.createEntityManager();
            ClusterAnalyticsRepository repo1 = new ClusterAnalyticsRepository(em1);

            List<ClusterNode> onlineNodes = repo1.findByNamedQuery(NodeStatus.ONLINE);
            System.out.println(" [NAMED QUERY RESULT] Found " + onlineNodes.size() + " ONLINE nodes:");
            for (ClusterNode node : onlineNodes) {
                System.out.println("   -> Node: " + node.getNodeName() + " | Status: " + node.getStatus());
            }
            em1.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 2: DTO PROJECTIONS & CONSTRUCTOR EXPRESSIONS (SELECT new ...)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 2: DTO PROJECTIONS & CONSTRUCTOR EXPRESSIONS (SELECT new ...)");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em2 = emf.createEntityManager();
            ClusterAnalyticsRepository repo2 = new ClusterAnalyticsRepository(em2);

            List<NodeMetricSummaryDto> summaries = repo2.findNodeMetricSummaries();
            System.out.println(" [DTO PROJECTION RESULT] Generated " + summaries.size() + " lightweight DTOs without loading entity graphs:");
            for (NodeMetricSummaryDto dto : summaries) {
                System.out.println("   -> " + dto);
            }
            em2.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 3: ELIMINATING N+1 PROBLEM VIA JOIN FETCH
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 3: ELIMINATING N+1 SELECTS WITH JOIN FETCH");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em3 = emf.createEntityManager();
            ClusterAnalyticsRepository repo3 = new ClusterAnalyticsRepository(em3);

            List<ClusterNode> eagerNodes = repo3.findWithEagerFetch(NodeStatus.ONLINE);
            System.out.println(" [FETCH COMPLETED] Loaded " + eagerNodes.size() + " nodes with associations in a SINGLE SQL statement.");
            em3.close(); // Close persistence context to detach nodes

            // Traverse lazy collections on detached nodes to verify JOIN FETCH initialized them
            System.out.println(" [DETACHED GRAPH TRAVERSAL] Traversing associations outside Persistence Context:");
            for (ClusterNode detachedNode : eagerNodes) {
                System.out.println("   -> Detached Node: " + detachedNode.getNodeName() 
                    + " | Rack: " + detachedNode.getRack().getRackTag() 
                    + " | Telemetry Records Count: " + detachedNode.getTelemetryRecords().size() + " (No LazyInitializationException!)");
            }

            // --------------------------------------------------------------------------------
            // SCENARIO 4: AGGREGATE JPQL WITH GROUP BY & HAVING
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 4: AGGREGATE JPQL WITH GROUP BY & HAVING");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em4 = emf.createEntityManager();
            ClusterAnalyticsRepository repo4 = new ClusterAnalyticsRepository(em4);

            List<ClusterNode> overloaded = repo4.findOverloadedNodes(75.0, 2);
            System.out.println(" [OVERLOADED NODES] Nodes having >= 2 telemetry samples with CPU > 75%:");
            for (ClusterNode node : overloaded) {
                System.out.println("   -> Overloaded Node: " + node.getNodeName() + " | Status: " + node.getStatus());
            }
            em4.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 5: DYNAMIC TYPE-SAFE CRITERIA API & PAGINATION
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 5: DYNAMIC TYPE-SAFE CRITERIA API WITH MULTI-PREDICATES & PAGINATION");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em5 = emf.createEntityManager();
            ClusterAnalyticsRepository repo5 = new ClusterAnalyticsRepository(em5);

            // Search Filter: ONLINE nodes in EU-WEST-ZONE-A having CPU >= 50.0%
            NodeSearchCriteria criteria = new NodeSearchCriteria();
            criteria.setStatus(NodeStatus.ONLINE);
            criteria.setDataCenterZone("EU-WEST-ZONE-A");
            criteria.setMinCpuLoad(50.0);

            Long matchCount = repo5.countByDynamicCriteria(criteria);
            System.out.println(" [CRITERIA COUNT] Total matching nodes in database: " + matchCount);

            // Execute paginated search: page 1 (first 2 records)
            List<ClusterNode> pagedResults = repo5.findByDynamicCriteria(criteria, 0, 2);
            System.out.println(" [CRITERIA PAGINATED (Page 1)] Fetched " + pagedResults.size() + " records (offset=0, limit=2):");
            for (ClusterNode node : pagedResults) {
                System.out.println("   -> Matched Node: " + node.getNodeName() + " in Zone: " + node.getRack().getDataCenterZone());
            }

            em5.close();

        } finally {
            emf.close();
            System.out.println("\n================================================================================");
            System.out.println("               JPA QUERY LAB COMPLETED SUCCESSFULLY");
            System.out.println("================================================================================");
        }
    }

    private static void seedDatabase(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // 1. Racks
        ServerRack rack1 = new ServerRack("RACK-ALPHA-01", "EU-WEST-ZONE-A");
        ServerRack rack2 = new ServerRack("RACK-BETA-02", "EU-WEST-ZONE-A");
        ServerRack rack3 = new ServerRack("RACK-GAMMA-03", "US-EAST-ZONE-B");

        // 2. Nodes
        ClusterNode node1 = new ClusterNode("node-worker-01", NodeStatus.ONLINE);
        node1.getTags().add("prod");
        node1.addTelemetryRecord(new TelemetryRecord(1024, 45.0));
        node1.addTelemetryRecord(new TelemetryRecord(2048, 85.0));
        node1.addTelemetryRecord(new TelemetryRecord(2560, 92.0));
        rack1.addNode(node1);

        ClusterNode node2 = new ClusterNode("node-worker-02", NodeStatus.ONLINE);
        node2.getTags().add("prod");
        node2.addTelemetryRecord(new TelemetryRecord(512, 25.0));
        node2.addTelemetryRecord(new TelemetryRecord(768, 55.0));
        rack1.addNode(node2);

        ClusterNode node3 = new ClusterNode("node-worker-03", NodeStatus.ONLINE);
        node3.getTags().add("stage");
        node3.addTelemetryRecord(new TelemetryRecord(4096, 88.0));
        node3.addTelemetryRecord(new TelemetryRecord(4200, 95.0));
        rack2.addNode(node3);

        ClusterNode node4 = new ClusterNode("node-edge-01", NodeStatus.DEGRADED);
        node4.getTags().add("edge");
        node4.addTelemetryRecord(new TelemetryRecord(256, 15.0));
        rack3.addNode(node4);

        ClusterNode node5 = new ClusterNode("node-edge-02", NodeStatus.MAINTENANCE);
        rack3.addNode(node5);

        em.persist(rack1);
        em.persist(rack2);
        em.persist(rack3);

        em.getTransaction().commit();
        em.close();
        System.out.println(" [SEED] Database populated with 3 Racks, 5 Nodes, and 8 Telemetry records.");
    }
}
