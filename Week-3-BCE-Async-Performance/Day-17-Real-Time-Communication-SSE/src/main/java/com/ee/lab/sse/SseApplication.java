package com.ee.lab.sse;

import com.ee.lab.sse.boundary.ClusterStreamResource;
import com.ee.lab.sse.boundary.JobStreamResource;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class SseApplication extends ResourceConfig {

    public SseApplication() {
        register(JobStreamResource.class);
        register(ClusterStreamResource.class);
        register(JsonBindingFeature.class);
        register(SseFeature.class);
    }
}
