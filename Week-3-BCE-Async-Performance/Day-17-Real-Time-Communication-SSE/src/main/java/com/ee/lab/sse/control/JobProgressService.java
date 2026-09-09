package com.ee.lab.sse.control;

import com.ee.lab.sse.entity.JobProgressEvent;
import com.ee.lab.sse.entity.JobStage;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JobProgressService {

    private static final JobProgressService INSTANCE = new JobProgressService();

    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setName("sse-job-worker-" + System.currentTimeMillis() % 1000);
        t.setDaemon(true);
        return t;
    });

    public static JobProgressService getInstance() {
        return INSTANCE;
    }

    public void streamJobProgress(String jobId, SseEventSink eventSink, Sse sse) {
        executor.submit(() -> {
            System.out.println("[JOB-SERVICE] Starting async SSE stream for jobId=" + jobId + " on thread=" + Thread.currentThread().getName());
            AtomicInteger eventSeq = new AtomicInteger(1);

            JobProgressEvent[] pipelineStages = new JobProgressEvent[] {
                JobProgressEvent.of(jobId, JobStage.SUBMITTED, 0, "Job accepted into cluster queue"),
                JobProgressEvent.of(jobId, JobStage.EXTRACTING, 25, "Extracting telemetry dataset from distributed nodes"),
                JobProgressEvent.of(jobId, JobStage.TRANSFORMING, 55, "Transforming metrics and calculating percentiles"),
                JobProgressEvent.of(jobId, JobStage.LOADING, 85, "Loading aggregate snapshots into time-series cache"),
                JobProgressEvent.of(jobId, JobStage.COMPLETED, 100, "ETL Pipeline completed successfully")
            };

            try {
                for (JobProgressEvent stageEvent : pipelineStages) {
                    if (eventSink.isClosed() || Thread.currentThread().isInterrupted()) {
                        System.out.println("[JOB-SERVICE] Client disconnected prematurely for jobId=" + jobId + ". Terminating stream.");
                        return;
                    }

                    boolean isLast = (stageEvent.stage() == JobStage.COMPLETED);
                    String eventName = isLast ? "job-completed" : "job-progress";

                    OutboundSseEvent event = sse.newEventBuilder()
                        .name(eventName)
                        .id(jobId + "-" + eventSeq.getAndIncrement())
                        .reconnectDelay(3000)
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(JobProgressEvent.class, stageEvent)
                        .comment("Cluster Pipeline Telemetry Pulse")
                        .build();

                    eventSink.send(event);
                    System.out.printf("[JOB-SERVICE] Dispatched SSE Event [name=%s, id=%s, stage=%s, progress=%d%%]%n",
                        eventName, event.getId(), stageEvent.stage(), stageEvent.percentage());

                    // Simulate processing latency between stages
                    TimeUnit.MILLISECONDS.sleep(120);
                }
            } catch (InterruptedException e) {
                System.out.println("[JOB-SERVICE] Stream worker gracefully interrupted for jobId=" + jobId);
            } catch (Exception e) {
                System.err.println("[JOB-SERVICE] Exception during SSE dispatch: " + e.getMessage());
            } finally {
                if (!eventSink.isClosed()) {
                    System.out.println("[JOB-SERVICE] Closing SseEventSink for jobId=" + jobId);
                    try {
                        eventSink.close();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }
}
