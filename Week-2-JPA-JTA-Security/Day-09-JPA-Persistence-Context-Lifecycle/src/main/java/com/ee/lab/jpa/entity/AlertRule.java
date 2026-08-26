package com.ee.lab.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "alert_rules")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 64, unique = true)
    private String ruleName;

    @Column(name = "metric_threshold", nullable = false)
    private double metricThreshold;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public AlertRule() {
        this.enabled = true;
    }

    public AlertRule(String ruleName, double metricThreshold) {
        this.ruleName = ruleName;
        this.metricThreshold = metricThreshold;
        this.enabled = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public double getMetricThreshold() { return metricThreshold; }
    public void setMetricThreshold(double metricThreshold) { this.metricThreshold = metricThreshold; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
