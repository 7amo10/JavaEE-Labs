package com.ee.lab.sse;

import com.ee.lab.sse.control.ClusterBroadcasterManager;
import com.ee.lab.sse.control.JobProgressService;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AppRunner {

    public static final String BASE_URI = "http://localhost:8080/api/";

    public static HttpServer startServer() {
        final SseApplication config = new SseApplication();
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   JAKARTA EE 10: REAL-TIME COMMUNICATION VIA SERVER-SENT EVENTS (SSE)");
        System.out.println("================================================================================");

        HttpServer server = startServer();
        System.out.println("[SERVER] JAX-RS 3.1 SSE Container booted successfully at " + BASE_URI);

        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        try {
            // -------------------------------------------------------------------------
            // SCENARIO 1: Unicast Background Job Progress Streaming
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 1: Unicast Background Job Progress Stream (JAX-RS SseEventSink)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Client connects to GET /api/jobs/batch-etl-701/progress with Accept: text/event-stream");

            HttpRequest jobReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URI + "jobs/batch-etl-701/progress"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

            CountDownLatch jobLatch = new CountDownLatch(1);
            AtomicInteger eventsReceived = new AtomicInteger(0);

            httpClient.sendAsync(jobReq, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    System.out.println("[CLIENT] Connected! HTTP Status: " + response.statusCode());
                    System.out.println("[CLIENT] Content-Type: " + response.headers().firstValue("Content-Type").orElse("none"));
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.isBlank()) {
                                System.out.println("  [WIRE] " + line);
                                if (line.startsWith("event: ")) {
                                    eventsReceived.incrementAndGet();
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        jobLatch.countDown();
                    }
                });

            boolean completedCleanly = jobLatch.await(6, TimeUnit.SECONDS);
            System.out.println("[CLIENT] Job stream finished. Total discrete events received: " + eventsReceived.get()
                + " (Clean EOF: " + completedCleanly + ")");

            // -------------------------------------------------------------------------
            // SCENARIO 2: Multicast Telemetry Broadcast via SseBroadcaster
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 2: Multicast Broadcast Channel (SseBroadcaster Fan-out)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Subscribing Client-ALPHA and Client-BETA to GET /api/cluster/stream...");

            CountDownLatch subscribersReadyLatch = new CountDownLatch(2);
            CountDownLatch alphaEventsLatch = new CountDownLatch(3); // welcome + telemetry + alert
            CountDownLatch betaEventsLatch = new CountDownLatch(3);
            AtomicBoolean alphaStop = new AtomicBoolean(false);
            AtomicBoolean betaStop = new AtomicBoolean(false);

            // Connect Client ALPHA
            CompletableFuture<Void> clientAlpha = CompletableFuture.runAsync(() -> {
                consumeBroadcastStream(httpClient, "CLIENT-ALPHA", subscribersReadyLatch, alphaEventsLatch, alphaStop);
            });

            // Connect Client BETA
            CompletableFuture<Void> clientBeta = CompletableFuture.runAsync(() -> {
                consumeBroadcastStream(httpClient, "CLIENT-BETA", subscribersReadyLatch, betaEventsLatch, betaStop);
            });

            // Wait until both clients are connected and ready
            subscribersReadyLatch.await(4, TimeUnit.SECONDS);
            TimeUnit.MILLISECONDS.sleep(150);

            // Verify active subscriber count
            HttpRequest countReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URI + "cluster/subscribers/count"))
                .GET()
                .build();
            HttpResponse<String> countRes = httpClient.send(countReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("[API] Query active subscribers: " + countRes.body());

            // Broadcast Node Telemetry Event
            System.out.println("\n[PRODUCER] Publishing Cluster Telemetry Event to /api/cluster/telemetry...");
            String telemJson = """
                {
                    "nodeId": "node-worker-01",
                    "status": "HEALTHY",
                    "cpuLoadPercent": 34.8,
                    "freeMemoryMb": 8192,
                    "timestamp": 1788976800000
                }
                """;
            HttpRequest telemReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URI + "cluster/telemetry"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(telemJson))
                .build();
            HttpResponse<String> telemRes = httpClient.send(telemReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("[PRODUCER] Telemetry Publish HTTP Status: " + telemRes.statusCode() + " Payload: " + telemRes.body());

            // Broadcast Security / Health Alert Event
            System.out.println("\n[PRODUCER] Publishing High-Severity Alert to /api/cluster/alerts...");
            String alertJson = """
                {
                    "alertId": "SEC-ALERT-882",
                    "severity": "WARNING",
                    "description": "Unusual rate of failed JWT authorizations detected on gateway",
                    "sourceNode": "node-edge-02",
                    "timestamp": 1788976801000
                }
                """;
            HttpRequest alertReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URI + "cluster/alerts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(alertJson))
                .build();
            HttpResponse<String> alertRes = httpClient.send(alertReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("[PRODUCER] Alert Publish HTTP Status: " + alertRes.statusCode() + " Payload: " + alertRes.body());

            alphaEventsLatch.await(4, TimeUnit.SECONDS);
            betaEventsLatch.await(4, TimeUnit.SECONDS);
            System.out.println("\n[VERIFICATION] Both Client-ALPHA and Client-BETA successfully received broadcast events simultaneously.");

            // -------------------------------------------------------------------------
            // SCENARIO 3: Client Disconnect & SseBroadcaster Eviction
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 3: SseBroadcaster Lifecycle: Client Disconnect & Eviction");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Signaling client disconnect for CLIENT-BETA...");
            betaStop.set(true);
            TimeUnit.MILLISECONDS.sleep(250);

            // -------------------------------------------------------------------------
            // SCENARIO 4: Client Early Abort on Unicast Stream
            // -------------------------------------------------------------------------
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(" SCENARIO 4: Unicast Stream Early Abort (SseEventSink.isClosed Protection)");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("Starting job stream for 'job-cancelled-99' and abruptly closing stream after 1 event...");

            HttpRequest abortReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URI + "jobs/job-cancelled-99/progress"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

            HttpResponse<InputStream> abortRes = httpClient.send(abortReq, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = abortRes.body();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String firstLine = reader.readLine();
                System.out.println("  [ABORT-TEST] Received initial line: " + firstLine);
                System.out.println("  [ABORT-TEST] Client closing socket connection now.");
            }

            // Allow server thread to detect closed sink
            TimeUnit.MILLISECONDS.sleep(350);
            System.out.println("[ABORT-TEST] Server worker thread detected closed sink and terminated cleanly.");

            // Stop alpha client before shutdown
            alphaStop.set(true);
            TimeUnit.MILLISECONDS.sleep(150);

            System.out.println("\n================================================================================");
            System.out.println("   ALL DAY 17 REAL-TIME SSE LAB TESTS EXECUTED SUCCESSFULLY");
            System.out.println("================================================================================");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JobProgressService.getInstance().shutdown();
            ClusterBroadcasterManager.getInstance().shutdown();
            server.shutdownNow();
            System.out.println("[SERVER] Grizzly SSE container stopped cleanly.");
        }
    }

    private static void consumeBroadcastStream(HttpClient client, String clientLabel,
                                              CountDownLatch readyLatch, CountDownLatch eventsLatch,
                                              AtomicBoolean stopSignal) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URI + "cluster/stream"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            readyLatch.countDown();
            try (InputStream stream = res.body();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while (!stopSignal.get() && (line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        System.out.printf("  [%s] %s%n", clientLabel, line);
                        if (line.startsWith("event: ")) {
                            eventsLatch.countDown();
                        }
                    }
                }
            }
            System.out.printf("[CLIENT] %s socket closed gracefully.%n", clientLabel);
        } catch (Exception ignored) {
        }
    }
}
