package com.ee.lab.jta.service;

import com.ee.lab.jta.entity.TransactionLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class AuditLogService {

    private final EntityManagerFactory emf;

    public AuditLogService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Simulates @Transactional(TxType.REQUIRES_NEW)
     * Suspends any outer transaction and executes in an isolated, autonomous transaction.
     * Persists audit logs to database regardless of outer transaction rollback.
     */
    public void logAutonomous(String action, String status, String details) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            TransactionLog log = new TransactionLog(action, status, details);
            em.persist(log);
            em.getTransaction().commit();
            System.out.println("  [REQUIRES_NEW AUDIT] Autonomous log persisted: action='" + action + "' | status='" + status + "'");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
