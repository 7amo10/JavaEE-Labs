package com.ee.lab.bce.entity;

/**
 * Lightweight read-only projection record demonstrating how Java records
 * replace bloated DTO classes in Adam Bien's BCE pattern.
 */
public record NodeSummary(
        Long id,
        String nodeName,
        NodeStatus status,
        double cpuUsage
) {}
