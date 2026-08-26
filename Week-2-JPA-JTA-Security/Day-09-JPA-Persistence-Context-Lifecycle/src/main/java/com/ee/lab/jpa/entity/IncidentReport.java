package com.ee.lab.jpa.entity;

import com.ee.lab.jpa.listener.AuditListener;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "incident_reports")
@EntityListeners(AuditListener.class)
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incident_id")
    private Long id;

    @Column(name = "incident_title", nullable = false, length = 128)
    private String incidentTitle;

    @Column(name = "description", nullable = false, length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private SeverityLevel severity;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "resolution_notes", length = 512)
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "revision", nullable = false)
    private int revision;

    @Transient // Calculated on-the-fly during @PostLoad
    private long durationOpenSeconds;

    public IncidentReport() {
        this.status = "OPEN";
        this.revision = 0;
    }

    public IncidentReport(String incidentTitle, String description, SeverityLevel severity) {
        this.incidentTitle = incidentTitle;
        this.description = description;
        this.severity = severity;
        this.status = "OPEN";
        this.revision = 0;
    }

    // Entity-internal Lifecycle Callbacks
    @PrePersist
    public void initTimestamps() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.revision = 1;
    }

    @PreUpdate
    public void updateTimestamps() {
        this.updatedAt = Instant.now();
        this.revision++;
    }

    @PostLoad
    public void calculateOpenDuration() {
        if (this.createdAt != null) {
            this.durationOpenSeconds = Duration.between(this.createdAt, Instant.now()).toSeconds();
        }
    }

    // Domain business methods
    public void resolve(String resolutionNotes) {
        this.status = "RESOLVED";
        this.resolutionNotes = resolutionNotes;
    }

    public void reopen(String reason) {
        this.status = "OPEN";
        this.resolutionNotes = "Reopened: " + reason;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIncidentTitle() { return incidentTitle; }
    public void setIncidentTitle(String incidentTitle) { this.incidentTitle = incidentTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public SeverityLevel getSeverity() { return severity; }
    public void setSeverity(SeverityLevel severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public int getRevision() { return revision; }

    public long getDurationOpenSeconds() { return durationOpenSeconds; }
}
