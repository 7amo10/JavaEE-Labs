package com.ee.lab.security.service;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

@DeclareRoles({"ADMIN", "OPERATOR", "VIEWER"})
public class ClusterControlService {

    @RolesAllowed({"ADMIN"})
    public String rebootClusterNode(String nodeId) {
        return "SUCCESS: Node [" + nodeId + "] reboot command dispatched by Administrator.";
    }

    @RolesAllowed({"ADMIN", "OPERATOR"})
    public String scaleWorkerThreads(String nodeId, int threadCount) {
        return "SUCCESS: Node [" + nodeId + "] scaled to " + threadCount + " threads.";
    }

    @RolesAllowed({"ADMIN", "OPERATOR", "VIEWER"})
    public String viewTelemetryDashboard(String nodeId) {
        return "TELEMETRY_DATA: Node [" + nodeId + "] CPU: 42%, Heap: 512MB, State: HEALTHY";
    }

    @PermitAll
    public String getPublicSystemStatus() {
        return "SYSTEM_STATUS: Cluster gateway is operational. All security services active.";
    }

    @DenyAll
    public String emergencyFactoryReset() {
        return "UNREACHABLE: Factory reset blocked by security policy.";
    }
}
