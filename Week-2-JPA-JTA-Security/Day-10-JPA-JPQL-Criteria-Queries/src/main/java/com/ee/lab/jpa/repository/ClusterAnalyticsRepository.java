package com.ee.lab.jpa.repository;

import com.ee.lab.jpa.dto.NodeMetricSummaryDto;
import com.ee.lab.jpa.dto.NodeSearchCriteria;
import com.ee.lab.jpa.entity.ClusterNode;
import com.ee.lab.jpa.entity.NodeStatus;
import com.ee.lab.jpa.entity.TelemetryRecord;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;

public class ClusterAnalyticsRepository {

    private final EntityManager em;

    public ClusterAnalyticsRepository(EntityManager em) {
        this.em = em;
    }

    // 1. Static Named Query with Bind Parameters
    public List<ClusterNode> findByNamedQuery(NodeStatus status) {
        return em.createNamedQuery("ClusterNode.findByStatus", ClusterNode.class)
                .setParameter("status", status)
                .getResultList();
    }

    // 2. JOIN FETCH to eliminate N+1 query problem
    public List<ClusterNode> findWithEagerFetch(NodeStatus status) {
        return em.createNamedQuery("ClusterNode.findWithTelemetryFetch", ClusterNode.class)
                .setParameter("status", status)
                .getResultList();
    }

    // 3. DTO Constructor Expression Projection (SELECT new ...)
    public List<NodeMetricSummaryDto> findNodeMetricSummaries() {
        String jpql = """
            SELECT new com.ee.lab.jpa.dto.NodeMetricSummaryDto(
                n.id,
                n.nodeName,
                n.status,
                AVG(t.heapUsedMb),
                MAX(t.cpuLoad),
                COUNT(t.id)
            )
            FROM ClusterNode n
            LEFT JOIN n.telemetryRecords t
            GROUP BY n.id, n.nodeName, n.status
            ORDER BY n.nodeName ASC
        """;
        return em.createQuery(jpql, NodeMetricSummaryDto.class).getResultList();
    }

    // 4. Aggregate Query with GROUP BY & HAVING
    public List<ClusterNode> findOverloadedNodes(double minCpuLoad, long minRecords) {
        return em.createNamedQuery("ClusterNode.findOverloadedNodes", ClusterNode.class)
                .setParameter("minCpu", minCpuLoad)
                .setParameter("minRecords", minRecords)
                .getResultList();
    }

    // 5. Dynamic Type-Safe Criteria API Multi-Predicate Query with Pagination
    public List<ClusterNode> findByDynamicCriteria(NodeSearchCriteria filter, int offset, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ClusterNode> cq = cb.createQuery(ClusterNode.class);
        Root<ClusterNode> root = cq.from(ClusterNode.class);

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), filter.getStatus()));
        }

        if (filter.getDataCenterZone() != null && !filter.getDataCenterZone().isBlank()) {
            predicates.add(cb.equal(root.get("rack").get("dataCenterZone"), filter.getDataCenterZone()));
        }

        if (filter.getNameKeyword() != null && !filter.getNameKeyword().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("nodeName")), "%" + filter.getNameKeyword().toLowerCase() + "%"));
        }

        if (filter.getMinCpuLoad() != null) {
            Join<ClusterNode, TelemetryRecord> telemetry = root.join("telemetryRecords", JoinType.INNER);
            predicates.add(cb.greaterThanOrEqualTo(telemetry.get("cpuLoad"), filter.getMinCpuLoad()));
            cq.distinct(true);
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        cq.orderBy(cb.asc(root.get("nodeName")));

        TypedQuery<ClusterNode> query = em.createQuery(cq);
        query.setFirstResult(offset);
        query.setMaxResults(limit);

        return query.getResultList();
    }

    // 6. Dynamic Criteria API Count Query
    public Long countByDynamicCriteria(NodeSearchCriteria filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<ClusterNode> root = cq.from(ClusterNode.class);

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), filter.getStatus()));
        }

        if (filter.getDataCenterZone() != null && !filter.getDataCenterZone().isBlank()) {
            predicates.add(cb.equal(root.get("rack").get("dataCenterZone"), filter.getDataCenterZone()));
        }

        if (filter.getNameKeyword() != null && !filter.getNameKeyword().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("nodeName")), "%" + filter.getNameKeyword().toLowerCase() + "%"));
        }

        if (filter.getMinCpuLoad() != null) {
            Join<ClusterNode, TelemetryRecord> telemetry = root.join("telemetryRecords", JoinType.INNER);
            predicates.add(cb.greaterThanOrEqualTo(telemetry.get("cpuLoad"), filter.getMinCpuLoad()));
        }

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        cq.select(cb.countDistinct(root));
        return em.createQuery(cq).getSingleResult();
    }
}
