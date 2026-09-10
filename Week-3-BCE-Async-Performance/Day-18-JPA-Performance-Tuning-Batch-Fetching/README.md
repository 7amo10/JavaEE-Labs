# Week 3 Day 18: Performance Tuning: JPA N+1 & Batch Fetching

This module implements comprehensive Object-Relational Mapping (ORM) performance profiling and query optimization using **Jakarta Persistence 3.1 (JPA)** and **Hibernate ORM 6.4**. It focuses on diagnosing and eliminating the infamous **N+1 Select Problem** through declarative and dynamic fetching mechanisms.

## Core Architectural Concepts

1. **The N+1 Select Problem**:
   * Occurs when querying a collection of parent entities (`ServerNode`) configured with lazy relationships (`@OneToMany(fetch = FetchType.LAZY)`).
   * Traversing child collections (`node.getMetrics()`) triggers an individual `SELECT` statement for every single parent entity, leading to $1 + N$ queries, database connection saturation, and severe latency degradation.
2. **JPQL `JOIN FETCH`**:
   * Overrides lazy loading at the query level using an explicit SQL `JOIN`:
     ```sql
     SELECT DISTINCT n FROM ServerNode n JOIN FETCH n.metrics
     ```
   * Hydrates parent entities and their associated child collections in a single round-trip query.
3. **Hibernate Batch Fetching (`@BatchSize`)**:
   * Configured declaratively via `@BatchSize(size = 10)` on collection fields or globally via `hibernate.default_batch_fetch_size`.
   * Instead of querying children one-by-one, Hibernate batches primary keys using SQL `WHERE node_id IN (?, ?, ..., ?)`.
   * Reduces query count from $1 + N$ to $1 + \lceil N / \text{batchSize} \rceil$.
4. **Jakarta Persistence 3.1 Entity Graphs (`EntityGraph`)**:
   * Dynamic, query-time fetch plan declared via `@NamedEntityGraph` or built programmatically via `em.createEntityGraph()`.
   * Passed as query hints (`jakarta.persistence.fetchgraph` or `jakarta.persistence.loadgraph`) without modifying underlying entity annotations or rewriting JPQL strings.
5. **Scalar DTO Projections (`SELECT new ...`)**:
   * High-throughput read optimization instantiating lightweight Java records or DTOs directly from the database result set.
   * Completely bypasses entity hydration, proxy generation, and Persistence Context tracking.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile the module and execute the empirical performance profiling suite:

```bash
mvn clean compile exec:java
```

## Empirical Performance Results (20 Nodes / 60 Metrics)

| Fetching Strategy | SQL Queries | Query Count Reduction | Architectural Impact |
| :--- | :--- | :--- | :--- |
| **1. Unoptimized Lazy Traversal** | **21** | Baseline (Worst) | 1 initial + 20 individual child queries ($N+1$) |
| **2. JPQL `JOIN FETCH`** | **1** | **-95.2%** | Hydrates entire collection graph in 1 query |
| **3. Hibernate `@BatchSize(10)`** | **3** | **-85.7%** | $1 + \lceil 20 / 10 \rceil$ queries using `WHERE IN (...)` |
| **4. JPA 3.1 `EntityGraph`** | **1** | **-95.2%** | Dynamic query-time eager fetch without JPQL changes |
| **5. Scalar DTO Projection** | **1** | **-95.2%** | Direct record projection with 0 entity tracking overhead |
