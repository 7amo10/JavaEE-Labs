# Week 2 Day 11: JTA Declarative Transactions & Concurrency Control

This lab module explores Jakarta Transactions 2.0 (JTA 2.0) declarative transaction boundaries (`@Transactional`), propagation semantics (`REQUIRED` vs. `REQUIRES_NEW`), exception rollback rules, and Optimistic Concurrency Control using `@Version` and `OptimisticLockException` under package `com.ee.lab.jta`.

## Technical Objectives

1. **Declarative Transaction Management (`@Transactional`)**:
   * Standardizing ACID transactional boundaries across enterprise services using declarative transaction attributes.
2. **Transaction Propagation Semantics**:
   * `TxType.REQUIRED` (default): Joins the active caller transaction or initiates a new physical database transaction if none exists.
   * `TxType.REQUIRES_NEW`: Suspends any outer transaction and executes within an independent, isolated transaction (ensuring audit logs and failure tracking persist even if the outer business transaction rolls back).
3. **Transaction Rollback Rules**:
   * Default behavior: Automatic rollback upon unchecked exceptions (`RuntimeException`, `Error`).
   * Custom rollback configuration: Explicitly triggering rollback on checked business exceptions using `rollbackOn = {QuotaExceededException.class}` or preventing rollback via `dontRollbackOn`.
4. **Optimistic Concurrency Control (`@Version`)**:
   * Declaring version fields (`@Version private Long version;`) to detect and prevent lost updates in high-concurrency environments.
   * Demonstrating automatic version incrementation on SQL `UPDATE` statements and catching `OptimisticLockException` when concurrent transactions submit conflicting version snapshots.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the complete transaction verification test suite:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (REQUIRED Propagation & Commit)**: Demonstrates atomic database write and version initialization under standard propagation.
2. **Scenario 2 (Unchecked Exception Rollback)**: Demonstrates automatic rollback when an unchecked `NodeFailureException` occurs, discarding all uncommitted in-memory mutations.
3. **Scenario 3 (REQUIRES_NEW Isolation)**: Verifies that autonomous audit logs remain committed in the database despite the caller transaction rolling back.
4. **Scenario 4 (Checked Exception Custom Rollback)**: Demonstrates explicit rollback enforcement on checked exceptions (`QuotaExceededException`).
5. **Scenario 5 (Optimistic Locking & Collision Detection)**: Simulates concurrent conflicting updates on a single entity and verifies that `OptimisticLockException` prevents database corruption.
