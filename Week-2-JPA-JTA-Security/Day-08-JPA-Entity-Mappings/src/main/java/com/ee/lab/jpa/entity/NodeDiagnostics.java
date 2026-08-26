package com.ee.lab.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "node_diagnostics")
public class NodeDiagnostics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diagnostics_id")
    private Long id;

    @Column(name = "firmware_version", nullable = false, length = 32)
    private String firmwareVersion;

    @Column(name = "kernel_version", nullable = false, length = 64)
    private String kernelVersion;

    @Column(name = "uptime_seconds", nullable = false)
    private long uptimeSeconds;

    @Column(name = "last_boot_time")
    private Instant lastBootTime;

    @OneToOne(mappedBy = "diagnostics", fetch = FetchType.LAZY)
    private ClusterNode clusterNode;

    public NodeDiagnostics() {
        this.lastBootTime = Instant.now();
    }

    public NodeDiagnostics(String firmwareVersion, String kernelVersion, long uptimeSeconds) {
        this.firmwareVersion = firmwareVersion;
        this.kernelVersion = kernelVersion;
        this.uptimeSeconds = uptimeSeconds;
        this.lastBootTime = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }

    public String getKernelVersion() { return kernelVersion; }
    public void setKernelVersion(String kernelVersion) { this.kernelVersion = kernelVersion; }

    public long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

    public Instant getLastBootTime() { return lastBootTime; }
    public void setLastBootTime(Instant lastBootTime) { this.lastBootTime = lastBootTime; }

    public ClusterNode getClusterNode() { return clusterNode; }
    public void setClusterNode(ClusterNode clusterNode) { this.clusterNode = clusterNode; }
}
