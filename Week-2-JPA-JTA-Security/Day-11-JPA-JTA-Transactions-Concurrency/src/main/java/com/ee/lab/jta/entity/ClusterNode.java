package com.ee.lab.jta.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cluster_nodes")
public class ClusterNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true, length = 64)
    private String nodeName;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "active_threads", nullable = false)
    private int activeThreads;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public ClusterNode() {
        this.status = "ONLINE";
        this.activeThreads = 0;
    }

    public ClusterNode(String nodeName, int activeThreads) {
        this.nodeName = nodeName;
        this.activeThreads = activeThreads;
        this.status = "ONLINE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getActiveThreads() { return activeThreads; }
    public void setActiveThreads(int activeThreads) { this.activeThreads = activeThreads; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
