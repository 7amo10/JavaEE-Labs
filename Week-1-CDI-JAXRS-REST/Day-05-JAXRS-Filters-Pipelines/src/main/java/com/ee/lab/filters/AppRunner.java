package com.ee.lab.filters;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AppRunner {

    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static HttpServer startServer() {
        final RestApplication config = new RestApplication();
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   JAKARTA EE 10: JAX-RS 3.1 FILTERS & REQUEST/RESPONSE PIPELINES LAB");
        System.out.println("================================================================================");

        HttpServer server = startServer();
        System.out.println("[SERVER] JAX-RS Container initialized with filter pipeline at " + BASE_URI);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 1: PUBLIC ENDPOINT (Bypasses Auth Filter & Verifies Response Decoration)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest pingReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "diagnostics/ping"))
                    .GET()
                    .build();

            HttpResponse<String> pingRes = client.send(pingReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /diagnostics/ping", pingRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 2: PROTECTED ENDPOINT WITHOUT TOKEN (Trigger Auth Filter -> 401 Abort)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest unauthReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "metrics/secure"))
                    .GET()
                    .build();

            HttpResponse<String> unauthRes = client.send(unauthReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /metrics/secure (No Auth Header)", unauthRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 3: PROTECTED ENDPOINT WITH INVALID TOKEN (Trigger Auth Filter -> 401 Abort)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest invalidTokenReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "metrics/secure"))
                    .header("Authorization", "Bearer bad-expired-token-123")
                    .GET()
                    .build();

            HttpResponse<String> invalidTokenRes = client.send(invalidTokenReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /metrics/secure (Invalid Bearer Token)", invalidTokenRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 4: PROTECTED ENDPOINT WITH VALID TOKEN (Authentication Success -> 200 OK)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest validTokenReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "metrics/secure"))
                    .header("Authorization", "Bearer valid-telemetry-token-2026")
                    .header("X-Correlation-Id", "CUSTOM-TRACE-999")
                    .GET()
                    .build();

            HttpResponse<String> validTokenRes = client.send(validTokenReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /metrics/secure (Valid Token + Custom Correlation Header)", validTokenRes);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n================================================================================");
            System.out.println("               CONTAINER SHUTDOWN & CLEANUP");
            System.out.println("================================================================================");
            server.shutdownNow();
            System.out.println("[SERVER] JAX-RS Container stopped cleanly.");
        }
    }

    private static void printResponse(String label, HttpResponse<String> res) {
        System.out.println(" [CLIENT REQUEST]  " + label);
        System.out.println(" [CLIENT STATUS]   HTTP " + res.statusCode());
        res.headers().firstValue("X-Correlation-Id").ifPresent(v -> System.out.println(" [HDR Correlation] " + v));
        res.headers().firstValue("X-Execution-Time-Millis").ifPresent(v -> System.out.println(" [HDR Exec-Time]   " + v + " ms"));
        res.headers().firstValue("X-Security-Policy").ifPresent(v -> System.out.println(" [HDR Security]    " + v));
        res.headers().firstValue("WWW-Authenticate").ifPresent(v -> System.out.println(" [HDR WWW-Auth]    " + v));
        res.headers().firstValue("Content-Type").ifPresent(v -> System.out.println(" [HDR Content-Type]" + v));
        System.out.println(" [CLIENT BODY]     " + res.body());
    }
}
