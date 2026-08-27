package com.ee.lab.jpa.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "cluster_nodes")
@NamedQueries({
    @NamedQuery(
        name = "ClusterNode.findByStatus",
        query = "SELECT n FROM ClusterNode n WHERE n.status = :status ORDER BY n.nodeName ASC"
    ),
    @NamedQuery(
        name = "ClusterNode.findWithTelemetryFetch",
        query = "SELECT DISTINCT n FROM ClusterNode n LEFT JOIN FETCH n.telemetryRecords JOIN FETCH n.rack WHERE n.status = :status"
    ),
    @NamedQuery(
        name = "ClusterNode.findOverloadedNodes",
        query = "SELECT n FROM ClusterNode n JOIN n.telemetryRecords t WHERE t.cpuLoad > :minCpu GROUP BY n HAVING COUNT(t) >= :minRecords"
    )
})
public class ClusterNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private Long id;

    @Column(name = "node_name", nullable = false, length = 64, unique = true)
    private String nodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NodeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id")
    private ServerRack rack;

    @OneToMany(mappedBy = "clusterNode", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TelemetryRecord> telemetryRecords = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "node_tags", joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "tag_name", nullable = false, length = 32)
    private Set<String> tags = new HashSet<>();

    public ClusterNode() {
        this.status = NodeStatus.ONLINE;
    }

    public ClusterNode(String nodeName, NodeStatus status) {
        this.nodeName = nodeName;
        this.status = status;
    }

    public void addTelemetryRecord(TelemetryRecord record) {
        telemetryRecords.add(record);
        record.setClusterNode(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public ServerRack getRack() { return rack; }
    public void setRack(ServerRack rack) { this.rack = rack; }

    public List<TelemetryRecord> getTelemetryRecords() { return telemetryRecords; }
    public void setTelemetryRecords(List<TelemetryRecord> telemetryRecords) { this.telemetryRecords = telemetryRecords; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
}
