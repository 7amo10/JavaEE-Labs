package com.ee.lab.jwt.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sec_audit_log")
public class SecurityAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "principal_name", nullable = false, length = 64)
    private String principalName;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "details", nullable = false, length = 256)
    private String details;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public SecurityAuditEntry() {
        this.timestamp = Instant.now();
    }

    public SecurityAuditEntry(String action, String principalName, String ipAddress, String status, String details) {
        this.action = action;
        this.principalName = principalName;
        this.ipAddress = ipAddress;
        this.status = status;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPrincipalName() { return principalName; }
    public void setPrincipalName(String principalName) { this.principalName = principalName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
