package com.ee.lab.sse.entity;

public record JobProgressEvent(
    String jobId,
    JobStage stage,
    int percentage,
    String message,
    long timestamp
) {
    public static JobProgressEvent of(String jobId, JobStage stage, int percentage, String message) {
        return new JobProgressEvent(jobId, stage, percentage, message, System.currentTimeMillis());
    }
}
