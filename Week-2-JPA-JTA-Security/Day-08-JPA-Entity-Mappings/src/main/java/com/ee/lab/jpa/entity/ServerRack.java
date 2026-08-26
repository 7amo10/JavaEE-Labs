package com.ee.lab.jpa.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "server_racks")
public class ServerRack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rack_id")
    private Long id;

    @Column(name = "rack_tag", nullable = false, length = 32, unique = true)
    private String rackTag;

    @Column(name = "datacenter_zone", nullable = false, length = 32)
    private String dataCenterZone;

    @OneToMany(mappedBy = "rack", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClusterNode> nodes = new ArrayList<>();

    public ServerRack() {
    }

    public ServerRack(String rackTag, String dataCenterZone) {
        this.rackTag = rackTag;
        this.dataCenterZone = dataCenterZone;
    }

    public void addNode(ClusterNode node) {
        nodes.add(node);
        node.setRack(this);
    }

    public void removeNode(ClusterNode node) {
        nodes.remove(node);
        node.setRack(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRackTag() { return rackTag; }
    public void setRackTag(String rackTag) { this.rackTag = rackTag; }

    public String getDataCenterZone() { return dataCenterZone; }
    public void setDataCenterZone(String dataCenterZone) { this.dataCenterZone = dataCenterZone; }

    public List<ClusterNode> getNodes() { return nodes; }
    public void setNodes(List<ClusterNode> nodes) { this.nodes = nodes; }
}
