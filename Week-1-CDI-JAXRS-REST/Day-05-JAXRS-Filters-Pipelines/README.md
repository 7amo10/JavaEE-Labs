# Day 05 Lab: JAX-RS Filters and Request/Response Pipelines

This laboratory exercise explores Jakarta RESTful Web Services 3.1 (JAX-RS) filter architecture, Pre-matching vs Post-matching request filters, `@NameBinding` selective interception, request aborting semantics, and response header enrichment.

## Lab Architecture

The lab implements an enterprise security and telemetry filter pipeline under package `com.ee.lab.filters`:

1. **Pre-Matching Request Filter (`PreMatchingLoggingFilter`)**:
   * Annotated with `@PreMatching` and `@Priority(Priorities.AUTHENTICATION - 100)`.
   * Intercepts incoming requests before URI path template matching.
   * Generates or extracts `X-Correlation-Id` and tracks start timestamps in request context properties.
2. **Name-Bound Authentication Filter (`BearerTokenAuthFilter`)**:
   * Bound via custom `@Authenticated` name-binding annotation and prioritized with `@Priority(Priorities.AUTHENTICATION)`.
   * Evaluates `Authorization: Bearer <token>` headers on protected resources (`/metrics/secure`).
   * Demonstrates short-circuit request aborting using `requestContext.abortWith()` returning HTTP 401 Unauthorized with standard `WWW-Authenticate` and RFC-7807 problem details.
3. **Response Header Decorator Filter (`ResponseEnrichmentFilter`)**:
   * Prioritized with `@Priority(Priorities.HEADER_DECORATOR)`.
   * Computes request processing duration and injects `X-Correlation-Id`, `X-Execution-Time-Millis`, and security policies on all outgoing responses.
4. **Public vs Protected Resources**:
   * `PublicDiagnosticsResource` (`/diagnostics/ping`): Open diagnostic endpoint bypassing authentication.
   * `ProtectedMetricsResource` (`/metrics/secure`): Secured endpoint requiring valid Bearer tokens.
5. **Embedded Integration Test Runner (`AppRunner`)**:
   * Boots embedded Grizzly HTTP server running Jersey 3.1 and executes automated tests verifying filter execution orders, authentication aborts, and response header decorations.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the lab:

```bash
mvn clean compile exec:java
```

## Expected Verification Flow

1. **Public Ping**: `GET /api/v1/diagnostics/ping` succeeds (HTTP 200) without authentication, receiving `X-Correlation-Id` and `X-Execution-Time-Millis` headers.
2. **Unauthenticated Protected Request**: `GET /api/v1/metrics/secure` without token is aborted by `BearerTokenAuthFilter`, returning HTTP 401 Unauthorized without invoking the resource method.
3. **Invalid Token Request**: `GET /api/v1/metrics/secure` with invalid Bearer token is rejected with HTTP 401.
4. **Authorized Request**: `GET /api/v1/metrics/secure` with valid Bearer token executes the resource, returning HTTP 200 with sensitive JVM telemetry metrics and decorated headers.
