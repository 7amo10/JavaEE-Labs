# Week 2 Day 08: JPA 3.1 Entity Mapping & Relationship Boundaries

This lab module explores Jakarta Persistence 3.1 (JPA 3.1) entity design, embedded value objects, element collections, bidirectional relationship mappings, cascade propagation, and persistence context boundaries under package `com.ee.lab.jpa`.

## Technical Objectives

1. **Entity Declarations and Identifiers**:
   * Map domain classes to relational tables using `@Entity`, `@Table`, and `@Id` with `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
   * Configure basic attribute columns with `@Column(name = "...", nullable = false, length = ...)`.
   * Map enums with explicit string preservation via `@Enumerated(EnumType.STRING)`.
2. **Embedded Types and Element Collections**:
   * Encapsulate value attributes into reusable components using `@Embeddable` and `@Embedded` (`HardwareSpec`).
   * Map primitive or String collections using `@ElementCollection` and `@CollectionTable` (`Set<String> tags`).
3. **Relationship Mapping and Ownership Semantics**:
   * **One-to-One (`@OneToOne`)**: Bidirectional relationship between `ClusterNode` (owning side with `@JoinColumn`) and `NodeDiagnostics` (inverse side with `mappedBy`).
   * **One-to-Many / Many-to-One (`@OneToMany` / `@ManyToOne`)**: Hierarchical relationships between `ServerRack` $\leftrightarrow$ `ClusterNode` and `ClusterNode` $\leftrightarrow$ `TelemetryRecord`.
   * **Many-to-Many (`@ManyToMany`)**: Associative relationships between `ClusterNode` and `SecurityGroup` using an explicit `@JoinTable`.
4. **Cascade Operations & Lifecycle Boundaries**:
   * Manage entity graphs through transitive persistence (`CascadeType.ALL`, `CascadeType.PERSIST`).
   * Enforce automatic cleanup of severed child entities using `orphanRemoval = true`.
   * Demonstrate `FetchType.LAZY` proxy initialization within an active transaction versus the `LazyInitializationException` boundary condition when traversing detached entities.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the complete entity mapping verification harness:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (Cascade Persistence)**: Persisting the root aggregate `ServerRack` cascades down to insert `ClusterNode`, `NodeDiagnostics`, `TelemetryRecord`, and `SecurityGroup` rows in a single atomic transaction.
2. **Scenario 2 (Graph Traversal)**: Traversing all mapped relationships within an active persistence context.
3. **Scenario 3 (Orphan Removal)**: Removing a `TelemetryRecord` from the parent node's collection triggers an automatic database `DELETE` on transaction commit.
4. **Scenario 4 (Persistence Boundary Violation)**: Traversing uninitialized lazy collections on a detached entity after closing the `EntityManager` cleanly raises and catches `LazyInitializationException`.
5. **Scenario 5 (Cascade Delete)**: Deleting `ServerRack` cascades deletion to dependent nodes and diagnostics, while preserving independent security groups.
