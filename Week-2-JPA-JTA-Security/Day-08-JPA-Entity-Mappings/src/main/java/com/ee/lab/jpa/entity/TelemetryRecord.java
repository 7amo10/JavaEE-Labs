package com.ee.lab.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "telemetry_records")
public class TelemetryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @Column(name = "heap_used_mb", nullable = false)
    private long heapUsedMb;

    @Column(name = "cpu_load", nullable = false)
    private double cpuLoad;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private ClusterNode clusterNode;

    public TelemetryRecord() {
        this.recordedAt = Instant.now();
    }

    public TelemetryRecord(long heapUsedMb, double cpuLoad) {
        this.heapUsedMb = heapUsedMb;
        this.cpuLoad = cpuLoad;
        this.recordedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public long getHeapUsedMb() { return heapUsedMb; }
    public void setHeapUsedMb(long heapUsedMb) { this.heapUsedMb = heapUsedMb; }

    public double getCpuLoad() { return cpuLoad; }
    public void setCpuLoad(double cpuLoad) { this.cpuLoad = cpuLoad; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public ClusterNode getClusterNode() { return clusterNode; }
    public void setClusterNode(ClusterNode clusterNode) { this.clusterNode = clusterNode; }
}
