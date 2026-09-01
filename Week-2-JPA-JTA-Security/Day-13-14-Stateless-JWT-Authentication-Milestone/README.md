# Week 2 Milestone: Stateless JWT Authentication & REST Security Gateway

This integration milestone implements an end-to-end Stateless Security Gateway combining JPA 3.1 entity persistence, JTA declarative transactions, PBKDF2 password hashing, and RFC-7519 HMAC-SHA256 JSON Web Token (JWT) issuance, cryptographic signature validation, JAX-RS ContainerRequestFilter request interception, and declarative Role-Based Access Control (RBAC) authorization under package `com.ee.lab.jwt`.

## Technical Objectives

1. **JPA 3.1 & JTA Security Infrastructure**:
   * Storing user accounts, salted PBKDF2 credentials, assigned role collections (`@ElementCollection`), and refresh token entities.
   * Logging security audit events into an isolated database audit table using autonomous transactions (`REQUIRES_NEW`).
2. **Stateless JSON Web Tokens (RFC-7519)**:
   * Generating standard signed JWT access tokens containing claims (`sub`, `iss`, `iat`, `exp`, `roles`, `jti`) using HMAC-SHA256 cryptographic signatures.
   * Implementing constant-time signature comparison to eliminate side-channel timing attack vectors.
3. **JAX-RS Security Filter & RBAC Authorization**:
   * Binding `@Secured` via `@NameBinding` to intercept incoming REST requests and extract Bearer tokens.
   * Injecting `JwtSecurityContext` with `JwtPrincipal` and caller roles (`isUserInRole`).
   * Enforcing `@RolesAllowed`, `@PermitAll`, and `@DenyAll` access control rules and returning standardized RFC-7807 `ProblemDetails` error payloads on 401 Unauthorized or 403 Forbidden scenarios.
4. **Token Lifecycle & Refresh Mechanism**:
   * Issuing long-lived refresh tokens enabling clients to acquire fresh short-lived JWT access tokens without exposing user passwords.

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the complete milestone integration test suite:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (Database & PBKDF2 Setup)**: Registers users with dynamic cryptographic salts, 2048 PBKDF2 iterations, and role hierarchies.
2. **Scenario 2 (Authentication & JWT Issuance)**: Validates credentials and generates signed HMAC-SHA256 access tokens and refresh tokens.
3. **Scenario 3 (Authorized Dispatch)**: Validates Bearer token signature, sets `SecurityContext`, and allows administrative operations (`@RolesAllowed("ADMIN")`).
4. **Scenario 4 (RBAC Rejection)**: Rejects unauthorized callers attempting elevated operations with `403 Forbidden` and RFC-7807 problem details.
5. **Scenario 5 (Tampering Detection)**: Detects altered payload or signature bytes and rejects requests with `401 Unauthorized`.
6. **Scenario 6 (Expiration Enforcement)**: Identifies expired tokens and rejects requests with `401 Unauthorized`.
7. **Scenario 7 (Token Refresh Workflow)**: Verifies refresh token entity in database and issues fresh JWT access tokens.
8. **Scenario 8 (Autonomous Audit Trail)**: Queries the database and confirms all login attempts, failures, token refreshes, and security violations are captured in the persistent audit log.
