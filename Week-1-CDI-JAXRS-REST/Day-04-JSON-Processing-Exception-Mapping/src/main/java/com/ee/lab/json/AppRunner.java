package com.ee.lab.json;

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
        System.out.println("   JAKARTA EE 10: JSON-B, JSON-P & UNIFIED RFC-7807 EXCEPTION MAPPING LAB");
        System.out.println("================================================================================");

        HttpServer server = startServer();
        System.out.println("[SERVER] JAX-RS Container initialized with ExceptionMappers at " + BASE_URI);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        try {
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 1: POST SUBMIT ANALYSIS JOB (JSON-B Custom Deserialization & 201 Created)");
            System.out.println("--------------------------------------------------------------------------------");
            String validJobJson = """
                {
                    "job_id": "JOB-200",
                    "target_binary": "com/engine/BytecodeOptimizer.class",
                    "job_status": "STATE_QUEUED",
                    "opcode_count": 520,
                    "cyclomatic_complexity": 2.15
                }
                """;

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "jobs"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(validJobJson))
                    .build();

            HttpResponse<String> postRes = client.send(postReq, HttpResponse.BodyHandlers.ofString());
            printResponse("POST /jobs (Valid Payload)", postRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 2: POST DUPLICATE JOB (Trigger DuplicateResourceExceptionMapper -> 409 Conflict)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpResponse<String> dupRes = client.send(postReq, HttpResponse.BodyHandlers.ofString());
            printResponse("POST /jobs (Duplicate Key JOB-200)", dupRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 3: POST INVALID PAYLOAD (Trigger InvalidPayloadExceptionMapper -> 400 Bad Request)");
            System.out.println("--------------------------------------------------------------------------------");
            String invalidJson = "{}";
            HttpRequest invalidReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "jobs"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                    .build();

            HttpResponse<String> invalidRes = client.send(invalidReq, HttpResponse.BodyHandlers.ofString());
            printResponse("POST /jobs (Empty Payload)", invalidRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 4: GET NON-EXISTENT JOB (Trigger ResourceNotFoundExceptionMapper -> 404 Not Found)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest notFoundReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "jobs/JOB-9999"))
                    .GET()
                    .build();

            HttpResponse<String> notFoundRes = client.send(notFoundReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /jobs/JOB-9999 (Non-Existent)", notFoundRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 5: GET EXISTING JOB (Verify JSON-B Adapter, DateFormat & @JsonbTransient Exclusion)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest getJobReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "jobs/JOB-101"))
                    .GET()
                    .build();

            HttpResponse<String> getJobRes = client.send(getJobReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /jobs/JOB-101 (JSON-B Inspection)", getJobRes);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" TEST 6: GET DYNAMIC JSON-P AST (Verify Dynamic Tree Construction)");
            System.out.println("--------------------------------------------------------------------------------");
            HttpRequest astReq = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URI + "jobs/JOB-101/ast"))
                    .GET()
                    .build();

            HttpResponse<String> astRes = client.send(astReq, HttpResponse.BodyHandlers.ofString());
            printResponse("GET /jobs/JOB-101/ast (JSON-P DOM Model)", astRes);

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
        System.out.println(" [BODY]     " + res.body());
    }
}
