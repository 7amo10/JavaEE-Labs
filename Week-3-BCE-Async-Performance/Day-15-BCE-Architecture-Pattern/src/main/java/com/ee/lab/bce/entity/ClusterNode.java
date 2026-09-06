package com.ee.lab.bce.entity;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Rich domain entity serving both as the persistence model and the REST resource model.
 * Demonstrates Adam Bien's BCE principle: "Entity as DTO" to eliminate mapping layers.
 */
@Entity
@Table(name = "cluster_nodes")
public class ClusterNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true, length = 64)
    private String nodeName;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NodeStatus status;

    @Column(name = "cpu_usage")
    private double cpuUsage;

    @Column(name = "memory_usage_mb")
    private double memoryUsageMb;

    @Column(name = "active_threads")
    private int activeThreads;

    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Version
    @JsonbTransient // Internal concurrency version hidden from REST clients
    @Column(name = "version")
    private Long version;

    public ClusterNode() {
        this.status = NodeStatus.ACTIVE;
        this.lastHeartbeat = Instant.now();
    }

    public ClusterNode(String nodeName, String ipAddress) {
        this.nodeName = nodeName;
        this.ipAddress = ipAddress;
        this.status = NodeStatus.ACTIVE;
        this.lastHeartbeat = Instant.now();
    }

    // Rich domain methods directly on Entity
    public void updateMetrics(double cpuUsage, double memoryUsageMb, int activeThreads) {
        if (cpuUsage < 0.0 || cpuUsage > 100.0) {
            throw new IllegalArgumentException("CPU usage must be between 0.0% and 100.0%");
        }
        this.cpuUsage = cpuUsage;
        this.memoryUsageMb = memoryUsageMb;
        this.activeThreads = activeThreads;
        this.lastHeartbeat = Instant.now();
    }

    public boolean isOverloaded() {
        return this.cpuUsage > 85.0 || this.activeThreads > 100;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }

    public double getMemoryUsageMb() { return memoryUsageMb; }
    public void setMemoryUsageMb(double memoryUsageMb) { this.memoryUsageMb = memoryUsageMb; }

    public int getActiveThreads() { return activeThreads; }
    public void setActiveThreads(int activeThreads) { this.activeThreads = activeThreads; }

    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public String toString() {
        return "ClusterNode{" +
                "id=" + id +
                ", name='" + nodeName + '\'' +
                ", status=" + status +
                ", cpu=" + cpuUsage + "%" +
                ", threads=" + activeThreads +
                '}';
    }
}
