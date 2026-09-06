package com.ee.lab.bce.control;

import com.ee.lab.bce.entity.ClusterNode;
import com.ee.lab.bce.entity.NodeStatus;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Control stereotype in Adam Bien's BCE pattern.
 * Manages the state machine rules and lifecycle validation for cluster nodes.
 */
@ApplicationScoped
public class NodeLifecycleController {

    /**
     * Validates and applies state transitions.
     * Enforces enterprise safety: An ACTIVE node with active workloads cannot jump directly to MAINTENANCE;
     * it must first transition through DRAINING.
     */
    public void transitionStatus(ClusterNode node, NodeStatus targetStatus) {
        if (node == null || targetStatus == null) {
            throw new IllegalArgumentException("Node and target status must not be null");
        }

        NodeStatus currentStatus = node.getStatus();
        if (currentStatus == targetStatus) {
            return; // No-op
        }

        switch (currentStatus) {
            case ACTIVE -> {
                if (targetStatus == NodeStatus.MAINTENANCE) {
                    if (node.getActiveThreads() > 0) {
                        throw new IllegalStateException("Cannot transition ACTIVE node with " + node.getActiveThreads() +
                                " active threads directly to MAINTENANCE. Node must be in DRAINING status first.");
                    }
                }
                node.setStatus(targetStatus);
            }
            case DRAINING -> {
                if (targetStatus == NodeStatus.MAINTENANCE && node.getActiveThreads() > 5) {
                    throw new IllegalStateException("Cannot enter MAINTENANCE while active threads remain above threshold (>5 threads). Current: " + node.getActiveThreads());
                }
                node.setStatus(targetStatus);
            }
            case MAINTENANCE, OFFLINE -> {
                node.setStatus(targetStatus);
            }
        }
    }
}
