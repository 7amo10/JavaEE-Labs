package com.ee.lab.audit;

import com.ee.lab.audit.boundary.AuditBoundaryResource;
import com.ee.lab.audit.security.AuditSecurityFilter;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api")
public class AuditApplication extends ResourceConfig {
    public AuditApplication() {
        register(AuditBoundaryResource.class);
        register(AuditSecurityFilter.class);
    }
}
