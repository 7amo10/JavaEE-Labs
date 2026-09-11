package com.ee.lab.tuning.control;

import com.ee.lab.tuning.boundary.MetricsThroughputResource;
import com.ee.lab.tuning.entity.BenchmarkResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BenchmarkExecutionService {

    private static final Logger LOGGER = Logger.getLogger(BenchmarkExecutionService.class.getName());

    public BenchmarkResult runBenchmark(String profileName, int poolMaxSize, int workerThreads,
                                        String targetUrl, int totalRequests, int concurrency) {
        // First try to run ab (ApacheBench) if installed
        BenchmarkResult abResult = tryRunApacheBench(profileName, poolMaxSize, workerThreads, targetUrl, totalRequests, concurrency);
        if (abResult != null) {
            return abResult;
        }

        // Fallback to high-concurrency internal Java HTTP client benchmark
        return runInternalJavaBenchmark(profileName, poolMaxSize, workerThreads, targetUrl, totalRequests, concurrency);
    }

    private BenchmarkResult tryRunApacheBench(String profileName, int poolMaxSize, int workerThreads,
                                              String targetUrl, int totalRequests, int concurrency) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ab",
                    "-n", String.valueOf(totalRequests),
                    "-c", String.valueOf(concurrency),
                    "-s", "5",
                    targetUrl
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            String outStr = output.toString();

            if (exitCode == 0 || outStr.contains("Requests per second:")) {
                double rps = extractRegexDouble(outStr, "Requests per second:\\s+([0-9.]+)");
                double meanLatency = extractRegexDouble(outStr, "Time per request:\\s+([0-9.]+)\\s+\\[ms\\]\\s+\\(mean\\)");
                double p99Latency = extractRegexDouble(outStr, "99%\\s+([0-9]+)");
                long failedRequests = (long) extractRegexDouble(outStr, "Failed requests:\\s+([0-9]+)");

                LOGGER.info("ApacheBench output captured successfully for profile: " + profileName);
                return new BenchmarkResult(
                        profileName,
                        poolMaxSize,
                        workerThreads,
                        totalRequests,
                        concurrency,
                        rps,
                        meanLatency,
                        p99Latency > 0 ? p99Latency : meanLatency * 1.5,
                        failedRequests,
                        MetricsThroughputResource.getTotalConnectionTimeouts()
                );
            }
        } catch (Exception e) {
            LOGGER.warning("ApacheBench execution skipped or failed: " + e.getMessage() + ". Falling back to internal benchmark.");
        }
        return null;
    }

    private BenchmarkResult runInternalJavaBenchmark(String profileName, int poolMaxSize, int workerThreads,
                                                     String targetUrl, int totalRequests, int concurrency) {
        LOGGER.info(String.format("Running internal load generator: %d requests, %d concurrency against %s",
                totalRequests, concurrency, targetUrl));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(3000))
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Callable<Long>> tasks = new ArrayList<>();
        AtomicLong failedRequests = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long overallStart = System.nanoTime();

        for (int i = 0; i < totalRequests; i++) {
            tasks.add(() -> {
                long reqStart = System.nanoTime();
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(targetUrl))
                            .timeout(Duration.ofMillis(4000))
                            .GET()
                            .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    long latencyMs = (System.nanoTime() - reqStart) / 1_000_000;
                    latencies.add(latencyMs);

                    if (resp.statusCode() != 200) {
                        failedRequests.incrementAndGet();
                    }
                    return latencyMs;
                } catch (Exception ex) {
                    failedRequests.incrementAndGet();
                    return -1L;
                }
            });
        }

        try {
            List<Future<Long>> futures = executor.invokeAll(tasks);
            for (Future<Long> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            LOGGER.severe("Benchmark execution interrupted: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        long totalDurationMs = (System.nanoTime() - overallStart) / 1_000_000;
        double rps = totalDurationMs > 0 ? ((double) totalRequests / totalDurationMs) * 1000.0 : 0.0;

        List<Long> validLatencies = latencies.stream().filter(l -> l >= 0).sorted().toList();
        double meanLatency = validLatencies.isEmpty() ? 0 : validLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double p99Latency = 0;
        if (!validLatencies.isEmpty()) {
            int p99Index = (int) Math.ceil(0.99 * validLatencies.size()) - 1;
            p99Latency = validLatencies.get(Math.max(0, Math.min(p99Index, validLatencies.size() - 1)));
        }

        return new BenchmarkResult(
                profileName,
                poolMaxSize,
                workerThreads,
                totalRequests,
                concurrency,
                rps,
                meanLatency,
                p99Latency,
                failedRequests.get(),
                MetricsThroughputResource.getTotalConnectionTimeouts()
        );
    }

    private double extractRegexDouble(String text, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }
}
