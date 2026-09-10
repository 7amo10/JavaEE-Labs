package com.ee.lab.perf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "telemetry_metrics")
public class TelemetryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_key", nullable = false)
    private String metricKey;

    @Column(name = "metric_value", nullable = false)
    private double metricValue;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private ServerNode serverNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batched_node_id")
    private BatchedNode batchedNode;

    public TelemetryMetric() {}

    public TelemetryMetric(String metricKey, double metricValue, Instant capturedAt) {
        this.metricKey = metricKey;
        this.metricValue = metricValue;
        this.capturedAt = capturedAt;
    }

    public Long getId() { return id; }
    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public double getMetricValue() { return metricValue; }
    public void setMetricValue(double metricValue) { this.metricValue = metricValue; }
    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }
    public ServerNode getServerNode() { return serverNode; }
    public void setServerNode(ServerNode serverNode) { this.serverNode = serverNode; }
    public BatchedNode getBatchedNode() { return batchedNode; }
    public void setBatchedNode(BatchedNode batchedNode) { this.batchedNode = batchedNode; }
}
