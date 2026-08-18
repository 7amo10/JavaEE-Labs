package com.ee.lab.jaxrs.model;

import java.time.Instant;

public class TelemetryReport {
    private String id;
    private String nodeId;
    private double cpuLoad;
    private long heapUsedMb;
    private String status;
    private String timestamp;

    public TelemetryReport() {
        this.timestamp = Instant.now().toString();
    }

    public TelemetryReport(String id, String nodeId, double cpuLoad, long heapUsedMb, String status) {
        this.id = id;
        this.nodeId = nodeId;
        this.cpuLoad = cpuLoad;
        this.heapUsedMb = heapUsedMb;
        this.status = status;
        this.timestamp = Instant.now().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public double getCpuLoad() { return cpuLoad; }
    public void setCpuLoad(double cpuLoad) { this.cpuLoad = cpuLoad; }

    public long getHeapUsedMb() { return heapUsedMb; }
    public void setHeapUsedMb(long heapUsedMb) { this.heapUsedMb = heapUsedMb; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "TelemetryReport[id=" + id + ", node=" + nodeId + ", cpu=" + cpuLoad + "%, heap=" + heapUsedMb + "MB, status=" + status + "]";
    }
}
