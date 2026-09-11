package com.ee.lab.tuning;

import com.ee.lab.tuning.boundary.MetricsThroughputResource;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api")
public class TuningApplication extends ResourceConfig {
    public TuningApplication() {
        register(MetricsThroughputResource.class);
    }
}
