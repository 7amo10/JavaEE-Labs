# Week 1 Integration Milestone: JVM-Pulse Core Telemetry Microservice

This milestone project integrates all core concepts from Week 1 (CDI 4.0, JAX-RS 3.1, JSON-B 3.0, JSON-P 2.1, unified RFC-7807 exception handling, and request/response filter pipelines) into a unified enterprise microservice under package `com.ee.lab.pulse`.

## Milestone Architecture

The microservice consolidates the following enterprise design patterns:

1. **Contexts and Dependency Injection (CDI 4.0)**:
   * **Scopes**: `@ApplicationScoped` singleton repository (`TelemetryStore`).
   * **Qualifiers**: `@MetricEngine(EngineType.HIGH_PRECISION)` for disambiguating metric collectors.
   * **Producers**: `@Produces` in `PlatformProducer` exposing `MemoryMXBean`, `ThreadMXBean`, and `OperatingSystemMXBean`.
   * **Interceptors**: `@Monitored` + `PerformanceMonitorInterceptor` measuring execution wall-clock time.
   * **Decoupled Events**: Synchronous (`@Observes`) SLA alert handlers and asynchronous (`@ObservesAsync`) audit trail consumers fired via `Event<TelemetryAnomalyEvent>`.
2. **RESTful Resource Architecture (JAX-RS 3.1)**:
   * REST endpoints supporting full CRUD lifecycle on JVM telemetry snapshots.
   * HTTP status semantics: `200 OK`, `201 Created` with canonical `Location` header, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, and `404 Not Found`.
   * Dynamic content negotiation on `/telemetry/snapshots/{id}/summary` supporting `text/plain` and `application/json`.
3. **JSON Binding & Processing (JSON-B 3.0 & JSON-P 2.1)**:
   * Declarative JSON-B mapping with `@JsonbProperty`, `@JsonbDateFormat`, and `@JsonbTransient` security redaction.
   * Low-level dynamic JSON-P Document Object Model (DOM) generation for JVM memory pool diagnostic maps (`/telemetry/diagnostics/memory-map`).
4. **Unified Exception Mapping**:
   * JAX-RS `@Provider ExceptionMapper` classes mapping domain exceptions (`SnapshotNotFoundException`, `InvalidSnapshotPayloadException`) to standard RFC-7807 problem details payloads.
5. **Filter and Security Pipeline**:
   * `@PreMatching` global correlation filter (`PreMatchingCorrelationFilter`) assigning `X-Correlation-Id`.
   * `@NameBinding` security filter (`BearerAuthFilter`) enforcing Bearer token authentication on admin routes (`/telemetry/admin/audit`).
   * `ContainerResponseFilter` (`ResponseEnrichmentFilter`) decorating response headers with execution metrics.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the complete integrated test suite:

```bash
mvn clean compile exec:java
```

## Integration Test Scenarios

1. **Live Telemetry Capture**: `GET /telemetry/live` executes CDI collector, interceptor, and event notifications.
2. **Snapshot Creation**: `POST /telemetry/snapshots` persists snapshot and returns `201 Created` with `Location` header.
3. **Content Negotiation**: `GET /telemetry/snapshots/{id}/summary` returns formatted plain text when requested with `Accept: text/plain`.
4. **Dynamic JSON-P AST**: `GET /telemetry/diagnostics/memory-map` builds memory pool trees on the fly.
5. **Validation Exception Mapping**: `POST /telemetry/snapshots` with empty payload triggers `InvalidSnapshotPayloadMapper` returning `400 Bad Request` in RFC-7807 format.
6. **Authentication Abort**: `GET /telemetry/admin/audit` without credentials returns `401 Unauthorized`.
7. **Privileged Admin Access**: `GET /telemetry/admin/audit` with valid Bearer token returns `200 OK`.
8. **Deletion Lifecycle**: `DELETE` returns `204 No Content`, followed by `404 Not Found` upon subsequent `GET`.
