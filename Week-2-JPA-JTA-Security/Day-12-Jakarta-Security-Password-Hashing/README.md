# Week 2 Day 12: Jakarta Security 3.0 & Password Hashing

This lab module explores modern Jakarta Security 3.0 (`jakarta.security.enterprise`), cryptographic password hashing using `Pbkdf2PasswordHash`, custom `IdentityStore` authentication mechanisms, and declarative Role-Based Access Control (RBAC) authorization using `@DeclareRoles`, `@RolesAllowed`, `@PermitAll`, and `@DenyAll` under package `com.ee.lab.security`.

## Technical Objectives

1. **Cryptographic Password Hashing (`Pbkdf2PasswordHash`)**:
   * Implementing salt-based PBKDF2 hashing with key stretching (2048 iterations) to mitigate dictionary, rainbow table, and brute-force GPU attacks.
   * Enforcing constant-time comparison algorithms during password verification to prevent side-channel timing attacks.
2. **Custom IdentityStore Authentication**:
   * Implementing `jakarta.security.enterprise.identitystore.IdentityStore` to validate `UsernamePasswordCredential`.
   * Standardizing `CredentialValidationResult` responses (`VALID` with `CallerPrincipal` and role memberships, `INVALID` on authentication failure, and `NOT_VALIDATED` for store chaining).
3. **Declarative Role-Based Access Control (RBAC)**:
   * Securing enterprise services using `@DeclareRoles`, `@RolesAllowed`, `@PermitAll`, and `@DenyAll`.
   * Evaluating dynamic authorization through `SecurityContext` methods (`getCallerPrincipal()`, `isCallerInRole()`).

## Prerequisites

* Java 21 LTS
* Apache Maven 3.8+

## Build and Run

To compile and execute the complete security verification test suite:

```bash
mvn clean compile exec:java
```

## Verification Scenarios

1. **Scenario 1 (PBKDF2 Hashing & Salt Verification)**: Confirms dynamic cryptographic salt generation and constant-time verification against valid and invalid inputs.
2. **Scenario 2 (IdentityStore Success)**: Validates credentials for `admin_master` and confirms principal and role resolution (`ADMIN`, `OPERATOR`, `VIEWER`).
3. **Scenario 3 (IdentityStore Rejection)**: Confirms that invalid passwords produce `INVALID` validation results.
4. **Scenario 4 (Delegation & Disabled Accounts)**: Confirms `NOT_VALIDATED` for unknown users (enabling multi-store delegation) and `INVALID` for disabled accounts.
5. **Scenario 5 (RBAC Authorization Enforcement)**: Evaluates access to administrative, operational, public, and denied endpoints across multiple security contexts (`Admin`, `Operator`, `Viewer`, `Anonymous`).
