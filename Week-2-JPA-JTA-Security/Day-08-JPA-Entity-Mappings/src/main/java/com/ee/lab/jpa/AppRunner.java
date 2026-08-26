package com.ee.lab.jpa;

import com.ee.lab.jpa.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.LazyInitializationException;

import java.util.List;

public class AppRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   WEEK 2 DAY 08: JPA 3.1 ENTITY MAPPINGS & RELATIONSHIP BOUNDARIES");
        System.out.println("   @Entity | @Embeddable | @ElementCollection | 1:1 | 1:N | N:1 | N:M | Cascades");
        System.out.println("================================================================================");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("JpaLabPU");
        Long createdRackId = null;
        Long createdNodeId = null;

        try {
            // --------------------------------------------------------------------------------
            // SCENARIO 1: PERSISTENCE & CASCADE PROPAGATION
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 1: CASCADE PERSISTENCE (Rack -> Nodes -> Diagnostics & Telemetry)");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em1 = emf.createEntityManager();
            em1.getTransaction().begin();

            ServerRack rack = new ServerRack("RACK-ALPHA-01", "EU-WEST-ZONE-A");

            ClusterNode node = new ClusterNode(
                "node-worker-01",
                NodeStatus.ONLINE,
                new HardwareSpec(32, 128, "NVMe-SSD")
            );

            node.getTags().add("production");
            node.getTags().add("k8s-worker");
            node.getTags().add("telemetry-active");

            NodeDiagnostics diagnostics = new NodeDiagnostics("v4.1.2-alpha", "6.5.0-28-generic", 864000L);
            node.setDiagnostics(diagnostics);

            TelemetryRecord rec1 = new TelemetryRecord(1240, 45.2);
            TelemetryRecord rec2 = new TelemetryRecord(1890, 78.4);
            node.addTelemetryRecord(rec1);
            node.addTelemetryRecord(rec2);

            SecurityGroup secGroup = new SecurityGroup("secgrp-telemetry-nodes", "ALLOW_TLS_INBOUND_9090");
            node.addSecurityGroup(secGroup);

            rack.addNode(node);

            // Persisting the root aggregate ServerRack cascades down to all related entities
            em1.persist(rack);
            em1.getTransaction().commit();

            createdRackId = rack.getId();
            createdNodeId = node.getId();
            em1.close();

            System.out.println(" [SUCCESS] Persisted ServerRack (ID: " + createdRackId + ") with Node (ID: " + createdNodeId + ")");

            // --------------------------------------------------------------------------------
            // SCENARIO 2: FETCHING & GRAPH TRAVERSAL WITHIN ACTIVE PERSISTENCE CONTEXT
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 2: GRAPH TRAVERSAL INSIDE ACTIVE PERSISTENCE CONTEXT");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em2 = emf.createEntityManager();
            em2.getTransaction().begin();

            ClusterNode fetchedNode = em2.find(ClusterNode.class, createdNodeId);
            System.out.println(" [FETCHED NODE] " + fetchedNode.getNodeName() + " | Status: " + fetchedNode.getStatus());
            System.out.println(" [EMBEDDED SPEC] " + fetchedNode.getHardwareSpec());
            System.out.println(" [ELEMENT COLLECTION TAGS] " + fetchedNode.getTags());
            System.out.println(" [1:1 DIAGNOSTICS] Firmware: " + fetchedNode.getDiagnostics().getFirmwareVersion() 
                + " | Kernel: " + fetchedNode.getDiagnostics().getKernelVersion());
            System.out.println(" [N:1 RACK] " + fetchedNode.getRack().getRackTag() + " in " + fetchedNode.getRack().getDataCenterZone());
            System.out.println(" [1:N TELEMETRY COUNT] " + fetchedNode.getTelemetryRecords().size() + " records");
            for (TelemetryRecord r : fetchedNode.getTelemetryRecords()) {
                System.out.println("   -> Record #" + r.getId() + ": Heap=" + r.getHeapUsedMb() + "MB, CPU=" + r.getCpuLoad() + "%");
            }
            System.out.println(" [N:M SECURITY GROUPS] " + fetchedNode.getSecurityGroups().iterator().next().getGroupName());

            em2.getTransaction().commit();
            em2.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 3: ORPHAN REMOVAL (Deleting Child by Collection Severance)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 3: ORPHAN REMOVAL (Severing TelemetryRecord from Collection)");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em3 = emf.createEntityManager();
            em3.getTransaction().begin();

            ClusterNode nodeForOrphanTest = em3.find(ClusterNode.class, createdNodeId);
            int initialCount = nodeForOrphanTest.getTelemetryRecords().size();
            System.out.println(" [BEFORE REMOVAL] Telemetry records count: " + initialCount);

            // Sever first record from relationship
            TelemetryRecord removedRecord = nodeForOrphanTest.getTelemetryRecords().get(0);
            Long removedRecordId = removedRecord.getId();
            nodeForOrphanTest.removeTelemetryRecord(removedRecord);

            em3.getTransaction().commit();
            em3.close();

            // Verify record is physically deleted from database
            EntityManager emVerify = emf.createEntityManager();
            TelemetryRecord deletedCheck = emVerify.find(TelemetryRecord.class, removedRecordId);
            System.out.println(" [AFTER REMOVAL] Querying removed record (ID: " + removedRecordId + ") -> Found: " + (deletedCheck != null));
            emVerify.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 4: PERSISTENCE BOUNDARY & LazyInitializationException
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 4: PERSISTENCE BOUNDARY & LazyInitializationException DEMONSTRATION");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em4 = emf.createEntityManager();
            ClusterNode detachedNode = em4.find(ClusterNode.class, createdNodeId);
            em4.close(); // Close persistence context!

            System.out.println(" [PERSISTENCE CONTEXT CLOSED] Node object is now detached: " + detachedNode.getNodeName());
            try {
                // Attempting to traverse uninitialized lazy collection outside open session
                System.out.println(" [ATTEMPTING ACCESS] Traversing detached lazy collection 'tags'...");
                int tagCount = detachedNode.getTags().size();
                System.out.println(" Tags count: " + tagCount);
            } catch (LazyInitializationException ex) {
                System.out.println(" [EXPECTED EXCEPTION CAUGHT] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                System.out.println(" [EDUCATIONAL NOTE] Lazy collections require an open EntityManager/Session to initialize database proxies!");
            }

            // --------------------------------------------------------------------------------
            // SCENARIO 5: CASCADE DELETE (Removing ServerRack cleans all child nodes & telemetry)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 5: CASCADE REMOVAL (ServerRack -> ClusterNode -> Diagnostics)");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager em5 = emf.createEntityManager();
            em5.getTransaction().begin();

            ServerRack rackToDelete = em5.find(ServerRack.class, createdRackId);
            em5.remove(rackToDelete);

            em5.getTransaction().commit();
            em5.close();

            EntityManager emVerifyCascade = emf.createEntityManager();
            ClusterNode nodeCheck = emVerifyCascade.find(ClusterNode.class, createdNodeId);
            List<SecurityGroup> secGroups = emVerifyCascade.createQuery("SELECT g FROM SecurityGroup g", SecurityGroup.class).getResultList();

            System.out.println(" [CASCADE RESULT] ServerRack deleted.");
            System.out.println(" [CASCADE RESULT] ClusterNode (ID: " + createdNodeId + ") deleted: " + (nodeCheck == null));
            System.out.println(" [INDEPENDENT ENTITY] SecurityGroup preserved: " + !secGroups.isEmpty() + " (" + secGroups.get(0).getGroupName() + ")");
            emVerifyCascade.close();

        } finally {
            emf.close();
            System.out.println("\n================================================================================");
            System.out.println("               JPA LAB EXECUTION COMPLETED SUCCESSFULLY");
            System.out.println("================================================================================");
        }
    }
}
