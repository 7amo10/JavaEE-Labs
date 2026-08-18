package com.ee.lab.jaxrs;

import com.ee.lab.jaxrs.resource.TelemetryResource;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class RestApplication extends ResourceConfig {

    public RestApplication() {
        // Register REST resources and JSON-B features
        packages("com.ee.lab.jaxrs.resource");
        register(TelemetryResource.class);
        register(JsonBindingFeature.class);
    }
}
