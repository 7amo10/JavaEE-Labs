package com.ee.lab.pulse;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AppRunner {

    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   WEEK 1 INTEGRATION MILESTONE: JVM-PULSE CORE MICROSERVICE");
        System.out.println("   CDI 4.0 | JAX-RS 3.1 | JSON-B 3.0 | JSON-P 2.1 | RFC-7807 EXCEPTION MAPPING");
        System.out.println("================================================================================");

        // 1. Initialize CDI 4.0 Weld Container
        Weld weld = new Weld();
        WeldContainer cdiContainer = weld.initialize();
        System.out.println("[CDI ENGINE] Weld SE 5.1.2 Initialized. Injecting CDI Managed Beans...");

        // 2. Initialize JAX-RS 3.1 Grizzly HTTP Container
        RestApplication applicationConfig = new RestApplication();
        HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), applicationConfig);
        System.out.println("[REST ENGINE] JAX-RS 3.1 Microservice listening on " + BASE_URI);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 1: GET LIVE TELEMETRY SNAPSHOT (CDI Collector, Interceptor & Event Fire)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest liveReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "telemetry/live"))
                    .GET()
                    .build();

            HttpResponse<String> liveRes = httpClient.send(liveReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /telemetry/live", liveRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 2: POST CREATE POINT-IN-TIME SNAPSHOT (201 Created + Canonical Location)");
            System.out.println("--------------------------------------------------------------------------------");
            String snapshotJson = """
                {
                    "node_name": "node-worker-gamma",
                    "health_status": "OPTIMAL",
                    "heap_used_mb": 312,
                    "heap_max_mb": 2048,
                    "thread_count": 28,
                    "cpu_load_pct": 14.8
                }
                """;

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "telemetry/snapshots"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(snapshotJson))
                    .build();

            HttpResponse<String> postRes = httpClient.send(postReq, HttpResponse.BodyHandlers.ofString());
            printResponse("POST /telemetry/snapshots", postRes);
            String createdLocation = postRes.headers().firstValue("Location").orElse(BASE_URI + "telemetry/snapshots/SNAP-INIT-01");

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 3: CONTENT NEGOTIATION (GET Summary with Accept: text/plain)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest summaryReq = HttpRequest.newBuilder()
                    .uri(URI.create(createdLocation + "/summary"))
                    .header("Accept", "text/plain")
                    .GET()
                    .build();

            HttpResponse<String> summaryRes = httpClient.send(summaryReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET " + createdLocation + "/summary (Accept: text/plain)", summaryRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 4: GET DYNAMIC JSON-P MEMORY MAP (DOM Tree Construction)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest memoryMapReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "telemetry/diagnostics/memory-map"))
                    .GET()
                    .build();

            HttpResponse<String> memoryMapRes = httpClient.send(memoryMapReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /telemetry/diagnostics/memory-map (JSON-P)", memoryMapRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 5: POST INVALID SNAPSHOT PAYLOAD (RFC-7807 400 Bad Request Exception Mapping)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest invalidPostReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "telemetry/snapshots"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> invalidPostRes = httpClient.send(invalidPostReq, HttpResponse.BodyHandlers.ofString());
            printResponse("POST /telemetry/snapshots (Empty Payload)", invalidPostRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 6: PROTECTED ADMIN AUDIT WITHOUT TOKEN (Trigger Name-Bound Filter -> 401)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest unauthReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "telemetry/admin/audit"))
                    .GET()
                    .build();

            HttpResponse<String> unauthRes = httpClient.send(unauthReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /telemetry/admin/audit (No Auth Header)", unauthRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 7: PROTECTED ADMIN AUDIT WITH TOKEN (Authenticated -> 200 OK)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest adminReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "telemetry/admin/audit"))
                    .header("Authorization", "Bearer valid-pulse-admin-token-2026")
                    .header("X-Correlation-Id", "ADMIN-SESSION-001")
                    .GET()
                    .build();

            HttpResponse<String> adminRes = httpClient.send(adminReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /telemetry/admin/audit (Valid Bearer Token)", adminRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 8: DELETE SNAPSHOT (204 No Content)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest deleteReq = HttpRequest.newBuilder()
                    .uri(URI.create(createdLocation))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteRes = httpClient.send(deleteReq, HttpResponse.BodyHandlers.ofString());
            printResponse("DELETE " + createdLocation, deleteRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 9: GET SNAPSHOT POST-DELETION (RFC-7807 404 Not Found)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest getDeletedReq = HttpRequest.newBuilder()
                    .uri(URI.create(createdLocation))
                    .GET()
                    .build();

            HttpResponse<String> getDeletedRes = httpClient.send(getDeletedReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET " + createdLocation + " (Post-Deletion)", getDeletedRes);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n================================================================================");
            System.out.println("               CONTAINER SHUTDOWN & CLEANUP");
            System.out.println("================================================================================");
            httpServer.shutdownNow();
            System.out.println("[REST ENGINE] Grizzly HTTP Server stopped.");
            weld.shutdown();
            System.out.println("[CDI ENGINE] Weld SE Container terminated cleanly.");
        }
    }

    private static void printResponse(String label, HttpResponse<String> res) {
        System.out.println(" [CLIENT REQUEST]  " + label);
        System.out.println(" [CLIENT STATUS]   HTTP " + res.statusCode());
        res.headers().firstValue("Location").ifPresent(loc -> System.out.println(" [HDR Location]    " + loc));
        res.headers().firstValue("X-Correlation-Id").ifPresent(v -> System.out.println(" [HDR Correlation] " + v));
        res.headers().firstValue("X-Execution-Time-Millis").ifPresent(v -> System.out.println(" [HDR Exec-Time]   " + v + " ms"));
        res.headers().firstValue("X-Microservice-Version").ifPresent(v -> System.out.println(" [HDR Version]     " + v));
        res.headers().firstValue("Content-Type").ifPresent(v -> System.out.println(" [HDR Content-Type]" + v));
        System.out.println(" [CLIENT BODY]     " + res.body());
    }
}
