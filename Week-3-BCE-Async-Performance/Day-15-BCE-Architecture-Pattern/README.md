# Week 3 Day 15: Adam Bien's BCE Pattern (Boundary-Control-Entity)

This module implements Adam Bien's Boundary-Control-Entity (BCE) architecture pattern for my JavaEE learning. It refactors enterprise service components away from bloated horizontal layers (controllers, services, DAOs, and multi-tier DTOs) into clean, maintainable, vertical feature packages under `com.ee.lab.bce`.

## Architectural Stereotypes

1. **Boundary (`boundary`)**:
   * The sole public entry point for external clients and REST communication (`NodeResource`, `NodeManagementBoundary`).
   * Manages transactional boundaries, coordinates business workflows, and directly transfers domain models.
2. **Control (`control`)**:
   * Encapsulates reusable business algorithms, metric scoring, and state machine validation (`HealthCalculator`, `NodeLifecycleController`).
   * Internal to the component package; invoked exclusively by Boundaries or peer Controls, never directly from external consumers.
3. **Entity (`entity`)**:
   * Represents persistent domain state (`ClusterNode`, `NodeStatus`) and lightweight read-only projections (`NodeSummary`).
   * Eliminates the traditional "DTO Explosion" by serving directly as the JSON-B transfer model, with internal operational fields protected by `@JsonbTransient`.

## Key Architectural Rules Verified

* **Strict Dependency Flow**: `Boundary -> Control -> Entity`. Controls never depend on Boundaries; Entities remain pure domain models without outward dependencies.
* **Elimination of Artificial Interfaces**: Concrete classes are injected directly via CDI 4.0, eliminating redundant single-implementation interfaces (e.g. `INodeService` / `NodeServiceImpl`).
* **Entity as DTO**: Direct serialization of JPA entities into REST responses, removing tedious, repetitive entity-to-DTO conversion boilerplate.
* **Projections via Java Records**: Leveraging Java 21 `record` constructs for read-only JPQL constructor projections.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile the module and run all 5 verification scenarios:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (Boundary Ingestion & Entity-as-DTO)**: Demonstrates registering cluster nodes directly using JPA entities without intermediary DTO mapping.
2. **Scenario 2 (Control Logic Execution)**: Evaluates weighted resource scoring and health classification isolated within the `HealthCalculator` control bean.
3. **Scenario 3 (Control-Enforced State Machine)**: Rejects illegal node state transitions with HTTP 409 Conflict and executes safe transition sequences (`ACTIVE` -> `DRAINING` -> `MAINTENANCE`).
4. **Scenario 4 (Read Projections via Records)**: Executes JPQL constructor queries returning immutable `NodeSummary` records.
5. **Scenario 5 (BCE Stereotype Integrity)**: Introspects component structure to confirm dependency directions and the complete absence of redundant interface wrappers.
