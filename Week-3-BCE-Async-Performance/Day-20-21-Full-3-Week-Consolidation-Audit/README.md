# Days 20-21: Full 3-Week Consolidation & Architectural Audit

## Overview
This milestone project performs an automated, end-to-end architectural and security audit across the entire 3-week Jakarta EE 10 curriculum. It inspects and validates the core engineering tenets covered across Weeks 1, 2, and 3.

## Architectural Audit Pillars
1. **Pillar 1: Data Access & SQL Injection Resistance**
   - Verification of parameterized JPQL queries resisting SQL injection payloads (`' OR '1'='1`).
   - Confirmation of safe literal handling and strong entity mapping.
2. **Pillar 2: Boundary-Control-Entity (BCE) & Security Pipeline**
   - Clean separation of Boundary (`AuditBoundaryResource`), Control (`UserAuditRepository`, `AuditHttpServer`), and Entity (`AuditedUser`, `AuditedSystemLog`).
   - Full security filter coverage via JAX-RS `ContainerRequestFilter` (`AuditSecurityFilter`), verifying 401 Unauthorized for missing tokens, 403 Forbidden for insufficient roles (RBAC), and 200 OK for authorized claims.
3. **Pillar 3: Zero JPA N+1 Select Proliferation**
   - Audited collection navigation ensuring zero N+1 query traps.
   - Comparative verification using Jakarta Persistence 3.1 `EntityGraph` and JPQL `JOIN FETCH` reducing statement execution from 21 queries down to exactly 1 query (-95.2% query reduction).

## How to Run
```bash
mvn clean compile exec:java
```
