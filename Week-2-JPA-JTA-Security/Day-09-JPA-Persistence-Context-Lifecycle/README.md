# Week 2 Day 09: JPA 3.1 Persistence Context & Entity Lifecycle Events

This lab module explores Jakarta Persistence 3.1 (JPA 3.1) entity lifecycle transitions, First-Level Cache (Identity Map) mechanics, automatic dirty checking, entity synchronization (`flush()` and `refresh()`), and lifecycle callback annotations with external `@EntityListeners` under package `com.ee.lab.jpa`.

## Technical Objectives

1. **The Four Entity Lifecycle States**:
   * **Transient (New)**: Entities instantiated in memory with no database identity and no persistence context tracking.
   * **Managed**: Entities actively associated with an open `EntityManager` persistence context.
   * **Detached**: Entities with established database identities whose persistence context has been closed, cleared, or detached.
   * **Removed**: Entities marked for physical SQL deletion upon transaction commit.
2. **First-Level Cache (Identity Map)**:
   * Verification that repeated `find()` operations on the same entity ID return identical in-memory Java references (`ref1 == ref2`) with zero duplicate SQL queries.
3. **Automatic Dirty Checking**:
   * Demonstrating that modifications to managed entities during an active transaction are automatically detected by the provider and flushed as SQL `UPDATE` statements without explicit `merge()` calls.
4. **Synchronization Mechanics (`flush()` vs. `refresh()`)**:
   * Forcing in-flight SQL statements to the database engine before transaction commit using `em.flush()`.
   * Discarding uncommitted in-memory mutations and restoring original database state using `em.refresh()`.
5. **Entity Lifecycle Callbacks and Listeners**:
   * Internal callbacks: `@PrePersist`, `@PreUpdate`, and `@PostLoad` (for computing transient properties such as `durationOpenSeconds`).
   * External decoupled auditing via `@EntityListeners(AuditListener.class)` capturing `@PostPersist`, `@PostUpdate`, and `@PostRemove` events.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the complete lifecycle verification test suite:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (Lifecycle Transitions & Merge)**: Demonstrates Transient $\to$ Managed $\to$ Detached $\to$ Merged state progressions.
2. **Scenario 2 (L1 Cache & Dirty Checking)**: Demonstrates object deduplication in the persistence context and automated SQL updates.
3. **Scenario 3 (Flush & Refresh)**: Demonstrates pushing SQL updates before commit via `flush()` and reverting in-memory mutations via `refresh()`.
4. **Scenario 4 (Lifecycle Callbacks & PostLoad)**: Demonstrates timestamp initialization, revision incrementation, and `@Transient` property calculation.
5. **Scenario 5 (Removed State)**: Demonstrates deletion scheduling via `em.remove()` and subsequent database verification.
