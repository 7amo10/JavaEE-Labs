package com.ee.lab.bce.boundary;

import com.ee.lab.bce.control.HealthCalculator;
import com.ee.lab.bce.control.NodeLifecycleController;
import com.ee.lab.bce.entity.ClusterNode;
import com.ee.lab.bce.entity.NodeStatus;
import com.ee.lab.bce.entity.NodeSummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.List;
import java.util.Optional;

/**
 * Boundary stereotype in Adam Bien's BCE pattern.
 * Serves as the single transactional entry point into the Node component.
 * Coordinates Controls (HealthCalculator, NodeLifecycleController) and Entities (ClusterNode).
 * No redundant interfaces (INodeManagementBoundary) are created.
 */
@ApplicationScoped
public class NodeManagementBoundary {

    @Inject
    private HealthCalculator healthCalculator;

    @Inject
    private NodeLifecycleController lifecycleController;

    private EntityManagerFactory emf;

    public NodeManagementBoundary() {
        // Fallback for standalone runner or container injection
        this.emf = Persistence.createEntityManagerFactory("BcePU");
    }

    public NodeManagementBoundary(EntityManagerFactory emf, HealthCalculator healthCalculator, NodeLifecycleController lifecycleController) {
        this.emf = emf;
        this.healthCalculator = healthCalculator;
        this.lifecycleController = lifecycleController;
    }

    /**
     * Registers a new node directly from the Entity model (No DTO conversion boilerplate).
     */
    public ClusterNode registerNode(ClusterNode node) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(node);
            em.getTransaction().commit();
            return node;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<ClusterNode> findNode(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return Optional.ofNullable(em.find(ClusterNode.class, id));
        } finally {
            em.close();
        }
    }

    public List<ClusterNode> findAllNodes() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT n FROM ClusterNode n ORDER BY n.id ASC", ClusterNode.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Read-only projection returning lightweight Java 21 records directly from JPQL constructor expressions.
     */
    public List<NodeSummary> findNodeSummaries() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT new com.ee.lab.bce.entity.NodeSummary(n.id, n.nodeName, n.status, n.cpuUsage) " +
                    "FROM ClusterNode n ORDER BY n.id ASC", NodeSummary.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Updates telemetry metrics on the rich domain entity.
     */
    public ClusterNode updateNodeMetrics(Long id, double cpu, double memoryMb, int threads) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ClusterNode node = em.find(ClusterNode.class, id);
            if (node == null) {
                throw new IllegalArgumentException("Node not found with ID: " + id);
            }
            node.updateMetrics(cpu, memoryMb, threads);
            em.getTransaction().commit();
            return node;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Delegates state transitions to the internal NodeLifecycleController control bean.
     */
    public ClusterNode transitionStatus(Long id, NodeStatus targetStatus) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            ClusterNode node = em.find(ClusterNode.class, id);
            if (node == null) {
                throw new IllegalArgumentException("Node not found with ID: " + id);
            }
            lifecycleController.transitionStatus(node, targetStatus);
            em.getTransaction().commit();
            return node;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Coordinates health assessment by invoking the HealthCalculator control bean.
     */
    public HealthCalculator.HealthAssessment assessNodeHealth(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            ClusterNode node = em.find(ClusterNode.class, id);
            if (node == null) {
                throw new IllegalArgumentException("Node not found with ID: " + id);
            }
            return healthCalculator.assessHealth(node);
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
