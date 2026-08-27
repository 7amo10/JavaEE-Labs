# Week 2 Day 10: Dynamic JPQL Queries, Projections & Criteria API

This lab module explores Jakarta Persistence 3.1 (JPA 3.1) query mechanisms: Static Named JPQL Queries (`@NamedQuery`), DTO Projections via Constructor Expressions (`SELECT new ...`), eliminating the N+1 Select Problem with `JOIN FETCH`, Aggregate Grouping (`GROUP BY` and `HAVING`), and Dynamic Type-Safe Criteria API query construction under package `com.ee.lab.jpa`.

## Technical Objectives

1. **Static Named Queries (`@NamedQuery`)**:
   * Pre-compiled, validated JPQL queries declared on entity classes with strongly-typed named parameters (`:status`).
2. **DTO Projections & Constructor Expressions (`SELECT new ...`)**:
   * Executing lightweight projection queries directly instantiating immutable Data Transfer Objects (`NodeMetricSummaryDto`) without loading heavy managed entity graphs into the First-Level Cache.
3. **Eliminating the N+1 Select Problem with `JOIN FETCH`**:
   * Initializing associated lazy entity collections and parent relationships (`n.telemetryRecords`, `n.rack`) in a single SQL query to prevent iterative downstream database round-trips and avoid `LazyInitializationException` on detached graphs.
4. **Aggregate Calculations with `GROUP BY` and `HAVING`**:
   * Calculating aggregate statistics (`AVG(t.heapUsedMb)`, `MAX(t.cpuLoad)`, `COUNT(t.id)`) and filtering aggregated groups using the `HAVING` clause.
5. **Dynamic Type-Safe Criteria API**:
   * Programmatically constructing queries using `CriteriaBuilder`, `CriteriaQuery`, and `Root`, accumulating predicates dynamically based on multi-field search criteria (`NodeSearchCriteria`).
6. **Paginated Query Execution**:
   * Applying query offset and limit boundaries via `setFirstResult(offset)` and `setMaxResults(limit)`.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the complete query test suite:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (Named Queries)**: Executes `@NamedQuery("ClusterNode.findByStatus")` with parameter binding.
2. **Scenario 2 (DTO Projections)**: Executes `SELECT new com.ee.lab.jpa.dto.NodeMetricSummaryDto(...)` and maps aggregate expressions directly into DTOs.
3. **Scenario 3 (JOIN FETCH)**: Fetches `ClusterNode` with `telemetryRecords` and `rack` in a single query, detaches the instances, and traverses the graph outside the persistence context.
4. **Scenario 4 (Aggregate Queries)**: Filters overloaded nodes using `GROUP BY n HAVING COUNT(t) >= :minRecords`.
5. **Scenario 5 (Criteria API & Pagination)**: Dynamically builds multi-predicate queries and executes paginated results.
