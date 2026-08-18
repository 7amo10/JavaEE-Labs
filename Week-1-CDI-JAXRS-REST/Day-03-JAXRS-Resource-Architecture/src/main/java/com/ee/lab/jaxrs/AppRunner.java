package com.ee.lab.jaxrs;

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
        System.out.println("   JAKARTA EE 10: JAX-RS 3.1 REST RESOURCE ARCHITECTURE LAB");
        System.out.println("================================================================================");

        HttpServer server = startServer();
        System.out.println("[SERVER] JAX-RS 3.1 Container initialized at " + BASE_URI);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 1: GET ALL TELEMETRY REPORTS (Query Param Filter & Default Limit)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest getReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "telemetry?limit=5"))
                    .GET()
                    .build();

            HttpResponse<String> getRes = client.send(getReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /telemetry?limit=5", getRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 2: POST CREATE NEW TELEMETRY REPORT (201 Created + Location Header)");
            System.out.println("--------------------------------------------------------------------------------");
            String newReportJson = """
                {
                    "nodeId": "node-worker-delta",
                    "cpuLoad": 64.2,
                    "heapUsedMb": 768,
                    "status": "HEALTHY"
                }
                """;

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "telemetry"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(newReportJson))
                    .build();

            HttpResponse<String> postRes = client.send(postReq, HttpResponse.BodyHandlers.ofString());
            printResponse("POST /telemetry", postRes);

            String createdLocation = postRes.headers().firstValue("Location").orElse(BASE_URI + "telemetry/TEL-101");
            System.out.println(" [CLIENT] Received Created Resource Location: " + createdLocation);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 3: GET BY ID WITH CUSTOM HEADER (PathParam & HeaderParam Inspection)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest getByIdReq = HttpRequest.newBuilder()
                    .uri(URI.create(createdLocation))
                    .header("X-Client-Id", "EnterpriseMonitoringConsole-v1")
                    .GET()
                    .build();

            HttpResponse<String> getByIdRes = client.send(getByIdReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET " + createdLocation, getByIdRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 4: PUT UPDATE TELEMETRY REPORT (200 OK + Updated Entity)");
            System.out.println("--------------------------------------------------------------------------------");
            String updatedJson = """
                {
                    "nodeId": "node-worker-delta",
                    "cpuLoad": 92.7,
                    "heapUsedMb": 1536,
                    "status": "CRITICAL"
                }
                """;

            HttpRequest putReq = HttpRequest.newBuilder()
                    .uri(URI.create(createdLocation))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(updatedJson))
                    .build();

            HttpResponse<String> putRes = client.send(putReq, HttpResponse.BodyHandlers.ofString());
            printResponse("PUT " + createdLocation, putRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 5: CONTENT NEGOTIATION (GET Summary with Accept: text/plain)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest summaryReq = HttpRequest.newBuilder()
                    .uri(URI.create(createdLocation + "/summary"))
                    .header("Accept", "text/plain")
                    .GET()
                    .build();

            HttpResponse<String> summaryRes = client.send(summaryReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET " + createdLocation + "/summary (Accept: text/plain)", summaryRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 6: DELETE RESOURCE (204 No Content)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest deleteReq = HttpRequest.newBuilder()
                    .uri(URI.create(createdLocation))
                    .DELETE()
                    .build();

            HttpResponse<String> deleteRes = client.send(deleteReq, HttpResponse.BodyHandlers.ofString());
            printResponse("DELETE " + createdLocation, deleteRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 7: GET AFTER DELETE (Verify 404 Not Found)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest verify404Req = HttpRequest.newBuilder()
                    .uri(URI.create(createdLocation))
                    .GET()
                    .build();

            HttpResponse<String> verify404Res = client.send(verify404Req, HttpResponse.BodyHandlers.ofString());
            printResponse("GET " + createdLocation + " (Post-Deletion)", verify404Res);

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
        System.out.println(" [REQUEST]  " + label);
        System.out.println(" [STATUS]   HTTP " + res.statusCode());
        res.headers().firstValue("Location").ifPresent(loc -> System.out.println(" [LOCATION] " + loc));
        res.headers().firstValue("Content-Type").ifPresent(ct -> System.out.println(" [TYPE]     " + ct));
        System.out.println(" [BODY]     " + (res.body().isEmpty() ? "<EMPTY BODY - 204 NO CONTENT>" : res.body()));
    }
}
