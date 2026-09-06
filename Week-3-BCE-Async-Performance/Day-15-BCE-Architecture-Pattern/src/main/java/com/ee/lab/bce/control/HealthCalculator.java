package com.ee.lab.bce.control;

import com.ee.lab.bce.entity.ClusterNode;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Control stereotype in Adam Bien's BCE pattern.
 * Encapsulates reusable algorithmic business logic (health calculation).
 * Package-scoped or CDI managed, invoked strictly by Boundaries or other Controls.
 */
@ApplicationScoped
public class HealthCalculator {

    public enum HealthStatus {
        OPTIMAL,
        DEGRADED,
        CRITICAL
    }

    public record HealthAssessment(
            double healthScore,
            HealthStatus status,
            String recommendation
    ) {}

    /**
     * Calculates a normalized health score (0 to 100) using weighted system metrics:
     * - CPU usage: 40%
     * - Memory usage: 35%
     * - Active thread saturation: 25%
     */
    public HealthAssessment assessHealth(ClusterNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null for health assessment");
        }

        double cpuPenalty = (node.getCpuUsage() / 100.0) * 40.0;
        double memPenalty = Math.min(node.getMemoryUsageMb() / 4096.0, 1.0) * 35.0;
        double threadPenalty = Math.min(node.getActiveThreads() / 150.0, 1.0) * 25.0;

        double totalPenalty = cpuPenalty + memPenalty + threadPenalty;
        double healthScore = Math.max(0.0, Math.round((100.0 - totalPenalty) * 10.0) / 10.0);

        HealthStatus status;
        String recommendation;

        if (healthScore >= 80.0) {
            status = HealthStatus.OPTIMAL;
            recommendation = "Node is operating within optimal capacity parameters.";
        } else if (healthScore >= 50.0) {
            status = HealthStatus.DEGRADED;
            recommendation = "High resource consumption detected. Consider workload rebalancing.";
        } else {
            status = HealthStatus.CRITICAL;
            recommendation = "Immediate intervention required. Drain node to prevent cascading node failure.";
        }

        return new HealthAssessment(healthScore, status, recommendation);
    }
}
