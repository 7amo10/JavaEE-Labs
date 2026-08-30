package com.ee.lab.jta;

import com.ee.lab.jta.entity.ClusterNode;
import com.ee.lab.jta.entity.TelemetryQuota;
import com.ee.lab.jta.entity.TransactionLog;
import com.ee.lab.jta.exception.QuotaExceededException;
import com.ee.lab.jta.service.AuditLogService;
import com.ee.lab.jta.service.ClusterTransactionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;

public class AppRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   WEEK 2 DAY 11: JTA DECLARATIVE TRANSACTIONS & CONCURRENCY CONTROL");
        System.out.println("   @Transactional | Propagation (REQUIRED vs REQUIRES_NEW) | Rollback | @Version");
        System.out.println("================================================================================");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("JpaTransactionPU");
        AuditLogService auditLogService = new AuditLogService(emf);
        ClusterTransactionService txService = new ClusterTransactionService(emf, auditLogService);

        try {
            // --------------------------------------------------------------------------------
            // SCENARIO 1: REQUIRED TRANSACTION PROPAGATION & SUCCESSFUL COMMIT
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 1: REQUIRED TRANSACTION PROPAGATION & ATOMIC COMMIT");
            System.out.println("--------------------------------------------------------------------------------");
            txService.provisionQuota("node-worker-01", 1024.0);

            EntityManager emCheck1 = emf.createEntityManager();
            TelemetryQuota q1 = emCheck1.createQuery("SELECT q FROM TelemetryQuota q WHERE q.nodeName = 'node-worker-01'", TelemetryQuota.class)
                    .getSingleResult();
            System.out.println(" [DATABASE VERIFICATION] Quota for " + q1.getNodeName() 
                + ": Allocated=" + q1.getAllocatedMb() + "MB, Consumed=" + q1.getConsumedMb() 
                + "MB (Version=" + q1.getVersion() + ")");
            emCheck1.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 2: UNCHECKED EXCEPTION AUTOMATIC ROLLBACK
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 2: UNCHECKED RUNTIME EXCEPTION & AUTOMATIC TRANSACTION ROLLBACK");
            System.out.println("--------------------------------------------------------------------------------");
            txService.executeFailingTaskWithUncheckedRollback("node-worker-01", 256.0);

            EntityManager emCheck2 = emf.createEntityManager();
            TelemetryQuota q2 = emCheck2.createQuery("SELECT q FROM TelemetryQuota q WHERE q.nodeName = 'node-worker-01'", TelemetryQuota.class)
                    .getSingleResult();
            System.out.println(" [DATABASE VERIFICATION POST-ROLLBACK] Consumed MB is still: " + q2.getConsumedMb() 
                + " MB (In-memory mutations were successfully discarded!)");
            emCheck2.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 3: REQUIRES_NEW AUTONOMOUS AUDIT LOG ISOLATION
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 3: REQUIRES_NEW AUDIT LOG ISOLATION VERIFICATION");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager emCheck3 = emf.createEntityManager();
            List<TransactionLog> logs = emCheck3.createQuery("SELECT l FROM TransactionLog l ORDER BY l.id ASC", TransactionLog.class)
                    .getResultList();
            System.out.println(" [AUDIT LOG VERIFICATION] Found " + logs.size() + " autonomous audit records in DB (Preserved across rollbacks):");
            for (TransactionLog log : logs) {
                System.out.println("   -> Log ID=" + log.getId() + " | Action=" + log.getAction() + " | Status=" + log.getStatus() + " | Details='" + log.getDetails() + "'");
            }
            emCheck3.close();

            // --------------------------------------------------------------------------------
            // SCENARIO 4: CHECKED EXCEPTION CUSTOM ROLLBACK (rollbackOn = QuotaExceededException.class)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 4: CHECKED EXCEPTION ROLLBACK CONFIGURATION");
            System.out.println("--------------------------------------------------------------------------------");
            try {
                txService.consumeQuotaWithCheckedException("node-worker-01", 5000.0, true);
            } catch (QuotaExceededException e) {
                System.out.println(" [CHECKED EXCEPTION CAUGHT] " + e.getMessage());
                System.out.println(" [TRANSACTION STATUS] Rolled back as configured via rollbackOn!");
            }

            // --------------------------------------------------------------------------------
            // SCENARIO 5: OPTIMISTIC CONCURRENCY CONTROL (@Version) COLLISION SIMULATION
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 5: OPTIMISTIC LOCKING COLLISION & @Version PROTECTION");
            System.out.println("--------------------------------------------------------------------------------");
            
            // Seed a ClusterNode
            EntityManager emSeed = emf.createEntityManager();
            emSeed.getTransaction().begin();
            ClusterNode node = new ClusterNode("node-concurrency-test", 10);
            emSeed.persist(node);
            emSeed.getTransaction().commit();
            Long testNodeId = node.getId();
            emSeed.close();

            // Execute concurrent conflicting updates
            txService.simulateOptimisticLockingCollision(testNodeId);

            // Final state verification
            EntityManager emFinal = emf.createEntityManager();
            ClusterNode finalNode = emFinal.find(ClusterNode.class, testNodeId);
            System.out.println(" [FINAL VERIFIED STATE] Node=" + finalNode.getNodeName() 
                + " | ActiveThreads=" + finalNode.getActiveThreads() 
                + " | Version=" + finalNode.getVersion());
            emFinal.close();

        } finally {
            emf.close();
            System.out.println("\n================================================================================");
            System.out.println("               JTA TRANSACTION LAB COMPLETED SUCCESSFULLY");
            System.out.println("================================================================================");
        }
    }
}
