package com.ee.lab.jpa.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "cluster_nodes")
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

    @Embedded
    private HardwareSpec hardwareSpec;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "node_tags", joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "tag_name", nullable = false, length = 32)
    private Set<String> tags = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostics_id", unique = true)
    private NodeDiagnostics diagnostics;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id")
    private ServerRack rack;

    @OneToMany(mappedBy = "clusterNode", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TelemetryRecord> telemetryRecords = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
        name = "node_security_assignments",
        joinColumns = @JoinColumn(name = "node_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<SecurityGroup> securityGroups = new HashSet<>();

    public ClusterNode() {
        this.status = NodeStatus.ONLINE;
    }

    public ClusterNode(String nodeName, NodeStatus status, HardwareSpec hardwareSpec) {
        this.nodeName = nodeName;
        this.status = status;
        this.hardwareSpec = hardwareSpec;
    }

    // Bidirectional Helper Methods
    public void setDiagnostics(NodeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
        if (diagnostics != null) {
            diagnostics.setClusterNode(this);
        }
    }

    public void addTelemetryRecord(TelemetryRecord record) {
        telemetryRecords.add(record);
        record.setClusterNode(this);
    }

    public void removeTelemetryRecord(TelemetryRecord record) {
        telemetryRecords.remove(record);
        record.setClusterNode(null);
    }

    public void addSecurityGroup(SecurityGroup group) {
        securityGroups.add(group);
        group.getNodes().add(this);
    }

    public void removeSecurityGroup(SecurityGroup group) {
        securityGroups.remove(group);
        group.getNodes().remove(this);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public HardwareSpec getHardwareSpec() { return hardwareSpec; }
    public void setHardwareSpec(HardwareSpec hardwareSpec) { this.hardwareSpec = hardwareSpec; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }

    public NodeDiagnostics getDiagnostics() { return diagnostics; }

    public ServerRack getRack() { return rack; }
    public void setRack(ServerRack rack) { this.rack = rack; }

    public List<TelemetryRecord> getTelemetryRecords() { return telemetryRecords; }
    public void setTelemetryRecords(List<TelemetryRecord> telemetryRecords) { this.telemetryRecords = telemetryRecords; }

    public Set<SecurityGroup> getSecurityGroups() { return securityGroups; }
    public void setSecurityGroups(Set<SecurityGroup> securityGroups) { this.securityGroups = securityGroups; }
}
