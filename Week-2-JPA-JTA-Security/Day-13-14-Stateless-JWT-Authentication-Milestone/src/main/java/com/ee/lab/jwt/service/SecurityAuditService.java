package com.ee.lab.jwt.service;

import com.ee.lab.jwt.entity.SecurityAuditEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class SecurityAuditService {

    private final EntityManagerFactory emf;

    public SecurityAuditService(EntityManagerFactory emf) {
        this.emf = emf;
    }

    /**
     * Autonomous audit logger (Simulates @Transactional(TxType.REQUIRES_NEW)).
     * Commits audit entry independently of calling transaction state.
     */
    public void recordAudit(String action, String principalName, String ipAddress, String status, String details) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            SecurityAuditEntry entry = new SecurityAuditEntry(action, principalName, ipAddress, status, details);
            em.persist(entry);
            em.getTransaction().commit();
            System.out.println("  [AUDIT] Action: " + action + " | Principal: " + principalName + " | Status: " + status + " | IP: " + ipAddress);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Failed to write audit record: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
