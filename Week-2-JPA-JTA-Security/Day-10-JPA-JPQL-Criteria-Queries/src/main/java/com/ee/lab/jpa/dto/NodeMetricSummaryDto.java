package com.ee.lab.jpa.dto;

import com.ee.lab.jpa.entity.NodeStatus;

public class NodeMetricSummaryDto {

    private final Long nodeId;
    private final String nodeName;
    private final NodeStatus status;
    private final Double avgHeapUsedMb;
    private final Double maxCpuLoad;
    private final Long telemetryRecordCount;

    // Constructor Expression Target for JPQL "SELECT new ..."
    public NodeMetricSummaryDto(Long nodeId, String nodeName, NodeStatus status, 
                                Double avgHeapUsedMb, Double maxCpuLoad, Long telemetryRecordCount) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.status = status;
        this.avgHeapUsedMb = avgHeapUsedMb != null ? avgHeapUsedMb : 0.0;
        this.maxCpuLoad = maxCpuLoad != null ? maxCpuLoad : 0.0;
        this.telemetryRecordCount = telemetryRecordCount != null ? telemetryRecordCount : 0L;
    }

    public Long getNodeId() { return nodeId; }
    public String getNodeName() { return nodeName; }
    public NodeStatus getStatus() { return status; }
    public Double getAvgHeapUsedMb() { return avgHeapUsedMb; }
    public Double getMaxCpuLoad() { return maxCpuLoad; }
    public Long getTelemetryRecordCount() { return telemetryRecordCount; }

    @Override
    public String toString() {
        return String.format("NodeSummaryDto[id=%d, name='%s', status=%s, avgHeap=%.1f MB, maxCpu=%.1f%%, records=%d]",
                nodeId, nodeName, status, avgHeapUsedMb, maxCpuLoad, telemetryRecordCount);
    }
}
