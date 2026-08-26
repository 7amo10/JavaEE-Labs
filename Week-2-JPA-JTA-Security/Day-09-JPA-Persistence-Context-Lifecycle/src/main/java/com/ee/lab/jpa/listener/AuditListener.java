package com.ee.lab.jpa.listener;

import com.ee.lab.jpa.entity.IncidentReport;
import jakarta.persistence.*;

public class AuditListener {

    @PrePersist
    public void onPrePersist(Object entity) {
        if (entity instanceof IncidentReport report) {
            System.out.println("  [AUDIT LISTENER @PrePersist] Preparing new incident: '" + report.getIncidentTitle() + "'");
        }
    }

    @PostPersist
    public void onPostPersist(Object entity) {
        if (entity instanceof IncidentReport report) {
            System.out.println("  [AUDIT LISTENER @PostPersist] Successfully stored incident ID=" + report.getId() + " in database");
        }
    }

    @PreUpdate
    public void onPreUpdate(Object entity) {
        if (entity instanceof IncidentReport report) {
            System.out.println("  [AUDIT LISTENER @PreUpdate] Updating incident ID=" + report.getId() + " | Status=" + report.getStatus());
        }
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        if (entity instanceof IncidentReport report) {
            System.out.println("  [AUDIT LISTENER @PostUpdate] Updated incident ID=" + report.getId() + " in database (Rev=" + report.getRevision() + ")");
        }
    }

    @PreRemove
    public void onPreRemove(Object entity) {
        if (entity instanceof IncidentReport report) {
            System.out.println("  [AUDIT LISTENER @PreRemove] Decommissioning incident ID=" + report.getId());
        }
    }

    @PostRemove
    public void onPostRemove(Object entity) {
        if (entity instanceof IncidentReport report) {
            System.out.println("  [AUDIT LISTENER @PostRemove] Deletion finalized for incident ID=" + report.getId());
        }
    }

    @PostLoad
    public void onPostLoad(Object entity) {
        if (entity instanceof IncidentReport report) {
            System.out.println("  [AUDIT LISTENER @PostLoad] Loaded incident ID=" + report.getId() + " into persistence context");
        }
    }
}
