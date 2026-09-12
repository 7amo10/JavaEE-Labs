package com.ee.lab.audit.control;

import com.ee.lab.audit.entity.AuditedSystemLog;
import com.ee.lab.audit.entity.AuditedUser;
import com.ee.lab.audit.security.SecurityAuditService;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;
import java.util.logging.Logger;

public class UserAuditRepository {

    private static final Logger LOGGER = Logger.getLogger(UserAuditRepository.class.getName());
    private final EntityManagerFactory emf;
    private final SecurityAuditService securityService;

    public UserAuditRepository() {
        this.emf = Persistence.createEntityManagerFactory("AuditPU");
        this.securityService = new SecurityAuditService();
    }

    public void seedInitialData(int userCount, int logsPerUser) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (int i = 1; i <= userCount; i++) {
                String role = (i % 2 == 0) ? "ADMIN" : "OPERATOR";
                String passHash = securityService.generateSaltedHash("SecretPass" + i);
                AuditedUser user = new AuditedUser("engineer_" + i, passHash, role);

                for (int j = 1; j <= logsPerUser; j++) {
                    AuditedSystemLog log = new AuditedSystemLog("EXECUTE_COMMAND_" + j, "192.168.1." + (10 + i));
                    user.addLog(log);
                }
                em.persist(user);
            }
            em.getTransaction().commit();
            LOGGER.info(String.format("Seeded database with %d users and %d total logs", userCount, userCount * logsPerUser));
        } finally {
            em.close();
        }
    }

    // Secure parameterized query resisting SQL injection
    public AuditedUser findByUsernameSecure(String username) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT u FROM AuditedUser u LEFT JOIN FETCH u.systemLogs WHERE u.username = :username", AuditedUser.class)
                    .setParameter("username", username)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    // Attempted SQL injection simulation test
    public boolean testSqlInjectionResistance(String maliciousPayload) {
        EntityManager em = emf.createEntityManager();
        try {
            List<AuditedUser> users = em.createQuery(
                    "SELECT u FROM AuditedUser u WHERE u.username = :input", AuditedUser.class)
                    .setParameter("input", maliciousPayload)
                    .getResultList();
            // Parameterized query treats input strictly as literal string
            return users.isEmpty();
        } finally {
            em.close();
        }
    }

    // Optimized Fetching: EntityGraph
    public List<AuditedUser> findAllWithLogsEntityGraph() {
        EntityManager em = emf.createEntityManager();
        try {
            EntityGraph<?> eg = em.getEntityGraph("AuditedUser.withLogs");
            return em.createQuery("SELECT u FROM AuditedUser u", AuditedUser.class)
                    .setHint("jakarta.persistence.fetchgraph", eg)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Optimized Fetching: JOIN FETCH
    public List<AuditedUser> findAllWithLogsJoinFetch() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT DISTINCT u FROM AuditedUser u JOIN FETCH u.systemLogs", AuditedUser.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
