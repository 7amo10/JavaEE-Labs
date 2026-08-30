package com.ee.lab.jta.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transaction_logs")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "details", nullable = false, length = 256)
    private String details;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    public TransactionLog() {
        this.loggedAt = Instant.now();
    }

    public TransactionLog(String action, String status, String details) {
        this.action = action;
        this.status = status;
        this.details = details;
        this.loggedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Instant loggedAt) { this.loggedAt = loggedAt; }
}
