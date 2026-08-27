package com.ee.lab.jpa.dto;

import com.ee.lab.jpa.entity.NodeStatus;

public class NodeSearchCriteria {

    private NodeStatus status;
    private String dataCenterZone;
    private String nameKeyword;
    private Double minCpuLoad;

    public NodeSearchCriteria() {
    }

    public NodeSearchCriteria(NodeStatus status, String dataCenterZone, String nameKeyword, Double minCpuLoad) {
        this.status = status;
        this.dataCenterZone = dataCenterZone;
        this.nameKeyword = nameKeyword;
        this.minCpuLoad = minCpuLoad;
    }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public String getDataCenterZone() { return dataCenterZone; }
    public void setDataCenterZone(String dataCenterZone) { this.dataCenterZone = dataCenterZone; }

    public String getNameKeyword() { return nameKeyword; }
    public void setNameKeyword(String nameKeyword) { this.nameKeyword = nameKeyword; }

    public Double getMinCpuLoad() { return minCpuLoad; }
    public void setMinCpuLoad(Double minCpuLoad) { this.minCpuLoad = minCpuLoad; }
}
