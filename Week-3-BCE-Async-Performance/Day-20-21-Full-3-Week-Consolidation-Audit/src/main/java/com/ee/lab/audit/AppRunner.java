package com.ee.lab.audit;

import com.ee.lab.audit.boundary.AuditBoundaryResource;
import com.ee.lab.audit.control.AuditHttpServer;
import com.ee.lab.audit.control.AuditStatementInspector;
import com.ee.lab.audit.control.UserAuditRepository;
import com.ee.lab.audit.entity.AuditItemReport;
import com.ee.lab.audit.entity.AuditedUser;
import com.ee.lab.audit.security.SecurityAuditService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AppRunner {

    private static final Logger LOGGER = Logger.getLogger(AppRunner.class.getName());
    private static final int PORT = 8086;
    private static final String BASE_URL = "http://localhost:" + PORT + "/api/audit";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   DAYS 20-21: FULL 3-WEEK CONSOLIDATION & ARCHITECTURAL AUDIT                   ");
        System.out.println("================================================================================\n");

        List<AuditItemReport> auditReports = new ArrayList<>();
        UserAuditRepository repository = new UserAuditRepository();
        AuditBoundaryResource.setRepository(repository);
        SecurityAuditService securityService = new SecurityAuditService();
        AuditHttpServer server = new AuditHttpServer(PORT);

        try {
            // Seed 20 users with 5 audit logs each (100 logs total)
            repository.seedInitialData(20, 5);
            server.start();
            HttpClient client = HttpClient.newHttpClient();

            // -------------------------------------------------------------------------
            // PILLAR 1: Data Access & SQL Injection Resistance Audit
            // -------------------------------------------------------------------------
            System.out.println("[AUDIT PILLAR 1] Data Access Layer & SQL Injection Resistance");
            String maliciousPayload = "engineer_1' OR '1'='1";
            boolean injectionResisted = repository.testSqlInjectionResistance(maliciousPayload);
            AuditedUser validUser = repository.findByUsernameSecure("engineer_1");
            boolean validFound = (validUser != null);

            boolean p1Passed = injectionResisted && validFound;
            auditReports.add(new AuditItemReport(
                    "Pillar 1: Data Layer",
                    "Parameterized Query Injection Shield",
                    p1Passed,
                    "Tested payload: \"engineer_1' OR '1'='1\". Parameterized JPQL treated payload as literal value.",
                    "Zero Unescaped Queries"
            ));
            System.out.printf("   - SQL Injection Payload Rejected: %b | Valid User Hydrated: %b\n", injectionResisted, validFound);
            System.out.printf("   - Status: %s\n\n", p1Passed ? "PASSED" : "FAILED");

            // -------------------------------------------------------------------------
            // PILLAR 2: Boundary-Control-Entity (BCE) Decoupling & Security Pipeline Audit
            // -------------------------------------------------------------------------
            System.out.println("[AUDIT PILLAR 2] Boundary-Control-Entity (BCE) & Security Filter Audit");
            // Test 1: Public endpoint
            HttpRequest pubReq = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/public/ping")).GET().build();
            HttpResponse<String> pubResp = client.send(pubReq, HttpResponse.BodyHandlers.ofString());

            // Test 2: Secured endpoint without token (should be 401)
            HttpRequest unauthReq = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/secured/user/engineer_1")).GET().build();
            HttpResponse<String> unauthResp = client.send(unauthReq, HttpResponse.BodyHandlers.ofString());

            // Test 3: Authenticate valid user & access secured profile
            String token = securityService.createStatelessToken("engineer_1", "OPERATOR");
            HttpRequest authReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/secured/user/engineer_1"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> authResp = client.send(authReq, HttpResponse.BodyHandlers.ofString());

            // Test 4: Access admin endpoint with OPERATOR token (should be 403)
            HttpRequest adminDeniedReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/system-overview"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> adminDeniedResp = client.send(adminDeniedReq, HttpResponse.BodyHandlers.ofString());

            // Test 5: Access admin endpoint with ADMIN token (should be 200)
            String adminToken = securityService.createStatelessToken("engineer_2", "ADMIN");
            HttpRequest adminAllowedReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/system-overview"))
                    .header("Authorization", "Bearer " + adminToken)
                    .GET()
                    .build();
            HttpResponse<String> adminAllowedResp = client.send(adminAllowedReq, HttpResponse.BodyHandlers.ofString());

            boolean p2Passed = (pubResp.statusCode() == 200) &&
                               (unauthResp.statusCode() == 401) &&
                               (authResp.statusCode() == 200) &&
                               (adminDeniedResp.statusCode() == 403) &&
                               (adminAllowedResp.statusCode() == 200);

            auditReports.add(new AuditItemReport(
                    "Pillar 2: BCE & Security",
                    "Stateless JWT & ContainerRequestFilter Pipeline",
                    p2Passed,
                    String.format("Public: %d, Unauth: %d, Secured: %d, RBAC Denied: %d, RBAC Allowed: %d",
                            pubResp.statusCode(), unauthResp.statusCode(), authResp.statusCode(),
                            adminDeniedResp.statusCode(), adminAllowedResp.statusCode()),
                    "100% Endpoint RBAC Protection"
            ));
            System.out.printf("   - Public HTTP: %d | Unauth Rejection: %d | Secured Access: %d\n",
                    pubResp.statusCode(), unauthResp.statusCode(), authResp.statusCode());
            System.out.printf("   - RBAC Operator Forbidden: %d | RBAC Admin Authorized: %d\n",
                    adminDeniedResp.statusCode(), adminAllowedResp.statusCode());
            System.out.printf("   - Status: %s\n\n", p2Passed ? "PASSED" : "FAILED");

            // -------------------------------------------------------------------------
            // PILLAR 3: JPA Performance & Zero N+1 Select Audit
            // -------------------------------------------------------------------------
            System.out.println("[AUDIT PILLAR 3] JPA Performance: N+1 Trap Verification & Elimination");
            AuditStatementInspector.reset();

            // Scenario 3A: EntityGraph Fetch Plan
            List<AuditedUser> usersWithLogs = repository.findAllWithLogsEntityGraph();
            int totalLogsRead = 0;
            for (AuditedUser u : usersWithLogs) {
                totalLogsRead += u.getSystemLogs().size();
            }
            long queryCountGraph = AuditStatementInspector.getCount();

            // Scenario 3B: JOIN FETCH Fetch Plan
            AuditStatementInspector.reset();
            List<AuditedUser> usersWithLogsJoin = repository.findAllWithLogsJoinFetch();
            long queryCountJoin = AuditStatementInspector.getCount();

            boolean p3Passed = (queryCountGraph == 1) && (queryCountJoin == 1) && (totalLogsRead == 100);
            auditReports.add(new AuditItemReport(
                    "Pillar 3: ORM Performance",
                    "Zero N+1 Select Traversal (EntityGraph & JOIN FETCH)",
                    p3Passed,
                    String.format("EntityGraph Queries: %d, JOIN FETCH Queries: %d (hydrated 20 users & 100 logs in single queries)",
                            queryCountGraph, queryCountJoin),
                    "1 SQL Query (-95.2% vs N+1)"
            ));
            System.out.printf("   - EntityGraph SQL Statements: %d | JOIN FETCH SQL Statements: %d\n",
                    queryCountGraph, queryCountJoin);
            System.out.printf("   - Total Entities Hydrated: 20 users, %d audit logs\n", totalLogsRead);
            System.out.printf("   - Status: %s\n\n", p3Passed ? "PASSED" : "FAILED");

            // -------------------------------------------------------------------------
            // SUMMARY AUDIT SCORECARD
            // -------------------------------------------------------------------------
            System.out.println("================================================================================");
            System.out.println("                   FULL 3-WEEK ARCHITECTURAL AUDIT SCORECARD                    ");
            System.out.println("================================================================================");
            System.out.printf("%-24s | %-32s | %-8s | %-16s\n", "Audit Pillar", "Inspection Domain", "Status", "Metric");
            System.out.println("--------------------------------------------------------------------------------");
            for (AuditItemReport report : auditReports) {
                System.out.printf("%-24s | %-32s | %-8s | %-16s\n",
                        report.pillarName(), report.testCase(),
                        report.passed() ? "PASSED" : "FAILED", report.metric());
                System.out.printf("  > Findings: %s\n\n", report.findings());
            }

            boolean overallCompliant = auditReports.stream().allMatch(AuditItemReport::passed);
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("OVERALL ARCHITECTURAL COMPLIANCE: %s (3/3 Pillars Certified)\n",
                    overallCompliant ? "100% PASSED" : "NON-COMPLIANT");
            System.out.println("================================================================================\n");

        } catch (Exception e) {
            LOGGER.severe("Error during architectural audit: " + e.getMessage());
            e.printStackTrace();
        } finally {
            server.stop();
            repository.close();
        }
    }
}
