package com.ee.lab.perf.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "batched_nodes")
public class BatchedNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true)
    private String nodeName;

    @Column(nullable = false)
    private String datacenter;

    @OneToMany(mappedBy = "batchedNode", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 10)
    private List<TelemetryMetric> metrics = new ArrayList<>();

    public BatchedNode() {}

    public BatchedNode(String nodeName, String datacenter) {
        this.nodeName = nodeName;
        this.datacenter = datacenter;
    }

    public void addMetric(TelemetryMetric metric) {
        metrics.add(metric);
        metric.setBatchedNode(this);
    }

    public Long getId() { return id; }
    public String getNodeName() { return nodeName; }
    public String getDatacenter() { return datacenter; }
    public List<TelemetryMetric> getMetrics() { return metrics; }
}
