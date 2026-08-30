package com.ee.lab.jta.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "telemetry_quotas")
public class TelemetryQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quota_id")
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true, length = 64)
    private String nodeName;

    @Column(name = "allocated_mb", nullable = false)
    private double allocatedMb;

    @Column(name = "consumed_mb", nullable = false)
    private double consumedMb;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public TelemetryQuota() {
    }

    public TelemetryQuota(String nodeName, double allocatedMb) {
        this.nodeName = nodeName;
        this.allocatedMb = allocatedMb;
        this.consumedMb = 0.0;
    }

    public void consume(double amountMb) {
        this.consumedMb += amountMb;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public double getAllocatedMb() { return allocatedMb; }
    public void setAllocatedMb(double allocatedMb) { this.allocatedMb = allocatedMb; }

    public double getConsumedMb() { return consumedMb; }
    public void setConsumedMb(double consumedMb) { this.consumedMb = consumedMb; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
