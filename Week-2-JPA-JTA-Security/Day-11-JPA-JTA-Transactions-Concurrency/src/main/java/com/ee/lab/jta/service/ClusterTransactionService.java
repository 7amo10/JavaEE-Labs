package com.ee.lab.jta.service;

import com.ee.lab.jta.entity.ClusterNode;
import com.ee.lab.jta.entity.TelemetryQuota;
import com.ee.lab.jta.exception.NodeFailureException;
import com.ee.lab.jta.exception.QuotaExceededException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

public class ClusterTransactionService {

    private final EntityManagerFactory emf;
    private final AuditLogService auditLogService;

    public ClusterTransactionService(EntityManagerFactory emf, AuditLogService auditLogService) {
        this.emf = emf;
        this.auditLogService = auditLogService;
    }

    /**
     * Simulates @Transactional(TxType.REQUIRED)
     * Joins current transaction or starts a new one.
     */
    public void provisionQuota(String nodeName, double amountMb) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            TelemetryQuota quota = new TelemetryQuota(nodeName, amountMb);
            em.persist(quota);
            em.getTransaction().commit();
            auditLogService.logAutonomous("PROVISION_QUOTA", "SUCCESS", "Allocated " + amountMb + "MB to " + nodeName);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Demonstrates Default Rollback on Unchecked Exception vs REQUIRES_NEW Audit.
     * When an unchecked RuntimeException occurs, the outer transaction rolls back automatically,
     * but the REQUIRES_NEW audit log remains safely committed in the database.
     */
    public void executeFailingTaskWithUncheckedRollback(String nodeName, double consumeMb) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            System.out.println(" [OUTER TX BEGIN] Started business transaction for node: " + nodeName);

            TelemetryQuota quota = em.createQuery("SELECT q FROM TelemetryQuota q WHERE q.nodeName = :name", TelemetryQuota.class)
                    .setParameter("name", nodeName)
                    .getSingleResult();

            quota.consume(consumeMb);
            System.out.println(" [DIRTY IN-MEMORY] Quota mutated in memory: consumed=" + quota.getConsumedMb() + "MB");

            // Autonomous audit log (REQUIRES_NEW)
            auditLogService.logAutonomous("CONSUME_ATTEMPT", "STARTED", "Attempting consumption of " + consumeMb + "MB");

            // Simulate catastrophic runtime crash
            System.out.println(" [RUNTIME EXCEPTION] Simulating node crash with unchecked NodeFailureException...");
            throw new NodeFailureException("Hardware watchdog timer expired on " + nodeName);

        } catch (NodeFailureException nfe) {
            System.out.println(" [OUTER TX ROLLBACK] Caught unchecked exception: " + nfe.getMessage());
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
                System.out.println(" [OUTER TX ROLLED BACK] All in-memory modifications discarded!");
            }
            // Record failure in audit log via REQUIRES_NEW
            auditLogService.logAutonomous("CONSUME_FAILURE", "ROLLED_BACK", "Unchecked exception: " + nfe.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Demonstrates Checked Exception Rollback Semantics:
     * Checked exceptions do NOT trigger rollback by default unless explicitly configured.
     */
    public void consumeQuotaWithCheckedException(String nodeName, double consumeMb, boolean rollbackOnChecked) throws QuotaExceededException {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            TelemetryQuota quota = em.createQuery("SELECT q FROM TelemetryQuota q WHERE q.nodeName = :name", TelemetryQuota.class)
                    .setParameter("name", nodeName)
                    .getSingleResult();

            if (quota.getConsumedMb() + consumeMb > quota.getAllocatedMb()) {
                if (rollbackOnChecked) {
                    // Simulates @Transactional(rollbackOn = QuotaExceededException.class)
                    em.getTransaction().rollback();
                    auditLogService.logAutonomous("QUOTA_CHECK", "ROLLED_BACK", "Exceeded limit of " + quota.getAllocatedMb() + "MB");
                    throw new QuotaExceededException("Quota exceeded for node " + nodeName);
                } else {
                    // Default JPA behavior: Checked exception without rollback commits partial work
                    em.getTransaction().commit();
                    throw new QuotaExceededException("Quota exceeded (committed partial state)");
                }
            }

            quota.consume(consumeMb);
            em.getTransaction().commit();
        } catch (QuotaExceededException qee) {
            throw qee;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Demonstrates Optimistic Concurrency Control (@Version)
     * Simulates two concurrent sessions attempting to update the same ClusterNode version.
     */
    public void simulateOptimisticLockingCollision(Long nodeId) {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();

        try {
            // Session 1 reads node (version = 0)
            ClusterNode node1 = em1.find(ClusterNode.class, nodeId);
            System.out.println(" [SESSION 1] Loaded Node ID=" + nodeId + " (Version=" + node1.getVersion() + ", Threads=" + node1.getActiveThreads() + ")");

            // Session 2 reads same node (version = 0)
            ClusterNode node2 = em2.find(ClusterNode.class, nodeId);
            System.out.println(" [SESSION 2] Loaded Node ID=" + nodeId + " (Version=" + node2.getVersion() + ", Threads=" + node2.getActiveThreads() + ")");

            // Session 1 modifies and commits (version increments 0 -> 1)
            em1.getTransaction().begin();
            node1.setActiveThreads(node1.getActiveThreads() + 10);
            em1.getTransaction().commit();
            System.out.println(" [SESSION 1 COMMITTED] Updated activeThreads -> " + node1.getActiveThreads() + " (Version incremented to " + node1.getVersion() + ")");

            // Session 2 attempts to modify and commit with stale version 0
            System.out.println(" [SESSION 2 COMMIT ATTEMPT] Attempting commit with stale version snapshot (version=0)...");
            em2.getTransaction().begin();
            node2.setActiveThreads(node2.getActiveThreads() + 25);
            em2.getTransaction().commit(); // Throws OptimisticLockException!

        } catch (OptimisticLockException | jakarta.persistence.RollbackException e) {
            System.out.println(" [COLLISION DETECTED] OptimisticLockException caught: Database version was modified by Session 1!");
            System.out.println(" [OPTIMISTIC LOCK RECOVERY] Session 2 aborted without corrupting concurrent database state.");
            if (em2.getTransaction().isActive()) em2.getTransaction().rollback();
        } finally {
            em1.close();
            em2.close();
        }
    }
}
