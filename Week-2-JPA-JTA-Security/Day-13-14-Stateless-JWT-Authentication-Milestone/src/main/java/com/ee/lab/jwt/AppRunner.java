package com.ee.lab.jwt;

import com.ee.lab.jwt.dto.AuthResponse;
import com.ee.lab.jwt.dto.LoginRequest;
import com.ee.lab.jwt.dto.ProblemDetails;
import com.ee.lab.jwt.entity.SecurityAuditEntry;
import com.ee.lab.jwt.rest.RestSecurityDispatcher;
import com.ee.lab.jwt.security.JwtTokenService;
import com.ee.lab.jwt.security.Pbkdf2PasswordService;
import com.ee.lab.jwt.service.AuthenticationService;
import com.ee.lab.jwt.service.SecurityAuditService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Set;

public class AppRunner {

    private static final String SECRET_KEY = "EnterpriseSecretSigningKeyForJwtAuthenticationLab2026!";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   WEEK 2 MILESTONE: STATELESS JWT AUTHENTICATION & REST SECURITY GATEWAY");
        System.out.println("   JPA 3.1 | JTA Transactions | PBKDF2 Hashing | JWT HMAC-SHA256 | JAX-RS RBAC");
        System.out.println("================================================================================");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("JwtSecurityPU");
        Pbkdf2PasswordService passwordService = new Pbkdf2PasswordService();
        JwtTokenService jwtTokenService = new JwtTokenService(SECRET_KEY);
        SecurityAuditService auditService = new SecurityAuditService(emf);
        AuthenticationService authService = new AuthenticationService(emf, passwordService, jwtTokenService, auditService);
        RestSecurityDispatcher dispatcher = new RestSecurityDispatcher(jwtTokenService, auditService);

        try {
            // --------------------------------------------------------------------------------
            // SCENARIO 1: DATABASE INITIALIZATION & PBKDF2 USER SEEDING
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 1: DATABASE INITIALIZATION & PBKDF2 USER REGISTRATION");
            System.out.println("--------------------------------------------------------------------------------");
            authService.seedUser("admin_master", "AdminSecret#2026", "admin@jvmpulse.io", Set.of("ADMIN", "OPERATOR", "VIEWER"));
            authService.seedUser("operator_bob", "OperatorPass#123", "bob@jvmpulse.io", Set.of("OPERATOR", "VIEWER"));
            authService.seedUser("viewer_alice", "ViewerPass#456", "alice@jvmpulse.io", Set.of("VIEWER"));

            System.out.println(" [SETUP COMPLETE] Registered 3 users with salted PBKDF2 hashes and role hierarchies.");

            // --------------------------------------------------------------------------------
            // SCENARIO 2: SUCCESSFUL AUTHENTICATION & JWT TOKEN ISSUANCE
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 2: AUTHENTICATION & STATELESS JWT TOKEN GENERATION");
            System.out.println("--------------------------------------------------------------------------------");
            LoginRequest loginReq = new LoginRequest("admin_master", "AdminSecret#2026");
            AuthResponse adminAuth = authService.authenticate(loginReq, "192.168.1.100");

            System.out.println(" [LOGIN SUCCESS] Authenticated Principal: " + adminAuth.getUsername());
            System.out.println(" [TOKEN TYPE]    " + adminAuth.getTokenType());
            System.out.println(" [EXPIRES IN]    " + adminAuth.getExpiresInSeconds() + " seconds");
            System.out.println(" [ACCESS JWT]    " + adminAuth.getAccessToken());
            System.out.println(" [REFRESH TOKEN] " + adminAuth.getRefreshToken());

            // --------------------------------------------------------------------------------
            // SCENARIO 3: AUTHORIZED ADMINISTRATIVE ACCESS (@RolesAllowed({"ADMIN"}))
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 3: AUTHORIZED ENDPOINT DISPATCH (ADMIN JWT -> @RolesAllowed(\"ADMIN\"))");
            System.out.println("--------------------------------------------------------------------------------");
            String adminBearer = "Bearer " + adminAuth.getAccessToken();
            Response res1 = dispatcher.dispatchRequest("POST", "/api/v1/control/nodes/worker-01/restart", adminBearer, "192.168.1.100", "worker-01");

            System.out.println(" [HTTP STATUS] " + res1.getStatus() + " " + res1.getStatusInfo());
            System.out.println(" [RESPONSE]    " + res1.getEntity());

            // --------------------------------------------------------------------------------
            // SCENARIO 4: ROLE-BASED ACCESS REJECTION (403 FORBIDDEN WITH RFC-7807)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 4: RBAC AUTHORIZATION REJECTION (VIEWER ATTEMPTS ADMIN OPERATION)");
            System.out.println("--------------------------------------------------------------------------------");
            AuthResponse viewerAuth = authService.authenticate(new LoginRequest("viewer_alice", "ViewerPass#456"), "192.168.1.105");
            String viewerBearer = "Bearer " + viewerAuth.getAccessToken();

            Response res2 = dispatcher.dispatchRequest("POST", "/api/v1/control/nodes/worker-01/restart", viewerBearer, "192.168.1.105", "worker-01");
            System.out.println(" [HTTP STATUS] " + res2.getStatus() + " " + res2.getStatusInfo());
            if (res2.getEntity() instanceof ProblemDetails problem) {
                System.out.println(" [RFC-7807 TITLE]  " + problem.getTitle());
                System.out.println(" [RFC-7807 DETAIL] " + problem.getDetail());
                System.out.println(" [RFC-7807 PATH]   " + problem.getInstance());
            }

            // --------------------------------------------------------------------------------
            // SCENARIO 5: TOKEN TAMPERING DETECTION (401 UNAUTHORIZED)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 5: CRYPTOGRAPHIC SIGNATURE TAMPERING DETECTION");
            System.out.println("--------------------------------------------------------------------------------");
            String validToken = adminAuth.getAccessToken();
            // Tamper with payload
            String[] parts = validToken.split("\\.");
            String tamperedToken = parts[0] + "." + parts[1] + "X." + parts[2];
            String tamperedBearer = "Bearer " + tamperedToken;

            Response res3 = dispatcher.dispatchRequest("POST", "/api/v1/control/nodes/worker-01/restart", tamperedBearer, "192.168.1.200", "worker-01");
            System.out.println(" [HTTP STATUS] " + res3.getStatus() + " " + res3.getStatusInfo());
            if (res3.getEntity() instanceof ProblemDetails problem) {
                System.out.println(" [SECURITY REJECTION] " + problem.getTitle() + " -> " + problem.getDetail());
            }

            // --------------------------------------------------------------------------------
            // SCENARIO 6: TOKEN EXPIRATION ENFORCEMENT (401 UNAUTHORIZED)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 6: TOKEN EXPIRATION ENFORCEMENT");
            System.out.println("--------------------------------------------------------------------------------");
            // Generate token with 0ms lifespan (already expired)
            String expiredToken = jwtTokenService.generateToken("admin_master", Set.of("ADMIN"), -1000);
            String expiredBearer = "Bearer " + expiredToken;

            Response res4 = dispatcher.dispatchRequest("GET", "/api/v1/control/nodes", expiredBearer, "192.168.1.100");
            System.out.println(" [HTTP STATUS] " + res4.getStatus() + " " + res4.getStatusInfo());
            if (res4.getEntity() instanceof ProblemDetails problem) {
                System.out.println(" [SECURITY REJECTION] " + problem.getTitle() + " -> " + problem.getDetail());
            }

            // --------------------------------------------------------------------------------
            // SCENARIO 7: REFRESH TOKEN WORKFLOW (ISSUING NEW ACCESS JWT)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 7: STATELESS TOKEN REFRESH WORKFLOW");
            System.out.println("--------------------------------------------------------------------------------");
            AuthResponse refreshedAuth = authService.refreshAccessToken(adminAuth.getRefreshToken(), "192.168.1.100");
            System.out.println(" [REFRESH SUCCESS] Issued fresh access token for " + refreshedAuth.getUsername());
            System.out.println(" [NEW ACCESS JWT] " + refreshedAuth.getAccessToken());

            // Test dispatch with refreshed token
            Response res5 = dispatcher.dispatchRequest("GET", "/api/v1/control/nodes", "Bearer " + refreshedAuth.getAccessToken(), "192.168.1.100");
            System.out.println(" [HTTP STATUS]    " + res5.getStatus() + " " + res5.getStatusInfo());
            System.out.println(" [METRICS DATA]   " + res5.getEntity());

            // --------------------------------------------------------------------------------
            // SCENARIO 8: AUTONOMOUS AUDIT LOG INSPECTION (PERSISTENT SECURITY TRAIL)
            // --------------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 8: PERSISTENT SECURITY AUDIT TRAIL VERIFICATION");
            System.out.println("--------------------------------------------------------------------------------");
            EntityManager emAudit = emf.createEntityManager();
            List<SecurityAuditEntry> auditLogs = emAudit.createQuery("SELECT a FROM SecurityAuditEntry a ORDER BY a.id ASC", SecurityAuditEntry.class)
                    .getResultList();

            System.out.println(" [AUDIT TRAIL] Found " + auditLogs.size() + " recorded security events in database:");
            for (SecurityAuditEntry log : auditLogs) {
                System.out.println("   -> ID=" + log.getId() + " | Action=" + log.getAction() 
                        + " | Principal=" + log.getPrincipalName() + " | IP=" + log.getIpAddress() 
                        + " | Status=" + log.getStatus() + " | Details='" + log.getDetails() + "'");
            }
            emAudit.close();

        } finally {
            emf.close();
            System.out.println("\n================================================================================");
            System.out.println("          WEEK 2 INTEGRATION MILESTONE COMPLETED SUCCESSFULLY");
            System.out.println("================================================================================");
        }
    }
}
