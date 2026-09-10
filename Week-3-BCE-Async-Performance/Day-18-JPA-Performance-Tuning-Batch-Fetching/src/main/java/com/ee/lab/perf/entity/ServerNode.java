package com.ee.lab.perf.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "server_nodes")
@NamedEntityGraph(
    name = "ServerNode.withMetrics",
    attributeNodes = @NamedAttributeNode("metrics")
)
@NamedEntityGraph(
    name = "ServerNode.fullGraph",
    attributeNodes = {
        @NamedAttributeNode("metrics"),
        @NamedAttributeNode("auditLogs")
    }
)
public class ServerNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true)
    private String nodeName;

    @Column(nullable = false)
    private String datacenter;

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "serverNode", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<TelemetryMetric> metrics = new ArrayList<>();

    @OneToMany(mappedBy = "serverNode", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SecurityAuditLog> auditLogs = new ArrayList<>();

    public ServerNode() {}

    public ServerNode(String nodeName, String datacenter, String status) {
        this.nodeName = nodeName;
        this.datacenter = datacenter;
        this.status = status;
    }

    public void addMetric(TelemetryMetric metric) {
        metrics.add(metric);
        metric.setServerNode(this);
    }

    public void addAuditLog(SecurityAuditLog log) {
        auditLogs.add(log);
        log.setServerNode(this);
    }

    public Long getId() { return id; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getDatacenter() { return datacenter; }
    public void setDatacenter(String datacenter) { this.datacenter = datacenter; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<TelemetryMetric> getMetrics() { return metrics; }
    public List<SecurityAuditLog> getAuditLogs() { return auditLogs; }
}
