package com.ee.lab.pulse.model;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import java.time.LocalDateTime;
import java.util.UUID;

public class JvmSnapshot {

    @JsonbProperty("snapshot_id")
    private String snapshotId;

    @JsonbProperty("node_name")
    private String nodeName;

    @JsonbProperty("health_status")
    private HealthStatus healthStatus;

    @JsonbProperty("captured_at")
    @JsonbDateFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime capturedAt;

    @JsonbProperty("heap_used_mb")
    private long heapUsedMb;

    @JsonbProperty("heap_max_mb")
    private long heapMaxMb;

    @JsonbProperty("thread_count")
    private int threadCount;

    @JsonbProperty("cpu_load_pct")
    private double cpuLoadPct;

    @JsonbTransient // Security Redaction: Node internal signature never exposed to API
    private String nodeSecuritySecret;

    public JvmSnapshot() {
        this.capturedAt = LocalDateTime.now();
        this.nodeSecuritySecret = "SECRET-KEY-" + UUID.randomUUID();
    }

    public JvmSnapshot(String snapshotId, String nodeName, HealthStatus healthStatus, 
                       long heapUsedMb, long heapMaxMb, int threadCount, double cpuLoadPct) {
        this.snapshotId = snapshotId;
        this.nodeName = nodeName;
        this.healthStatus = healthStatus;
        this.heapUsedMb = heapUsedMb;
        this.heapMaxMb = heapMaxMb;
        this.threadCount = threadCount;
        this.cpuLoadPct = cpuLoadPct;
        this.capturedAt = LocalDateTime.now();
        this.nodeSecuritySecret = "SECRET-KEY-" + UUID.randomUUID();
    }

    public String getSnapshotId() { return snapshotId; }
    public void setSnapshotId(String snapshotId) { this.snapshotId = snapshotId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public HealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthStatus healthStatus) { this.healthStatus = healthStatus; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }

    public long getHeapUsedMb() { return heapUsedMb; }
    public void setHeapUsedMb(long heapUsedMb) { this.heapUsedMb = heapUsedMb; }

    public long getHeapMaxMb() { return heapMaxMb; }
    public void setHeapMaxMb(long heapMaxMb) { this.heapMaxMb = heapMaxMb; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public double getCpuLoadPct() { return cpuLoadPct; }
    public void setCpuLoadPct(double cpuLoadPct) { this.cpuLoadPct = cpuLoadPct; }

    public String getNodeSecuritySecret() { return nodeSecuritySecret; }
    public void setNodeSecuritySecret(String nodeSecuritySecret) { this.nodeSecuritySecret = nodeSecuritySecret; }
}
