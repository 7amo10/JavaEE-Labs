package com.ee.lab.filters;

import com.ee.lab.filters.filter.BearerTokenAuthFilter;
import com.ee.lab.filters.filter.PreMatchingLoggingFilter;
import com.ee.lab.filters.filter.ResponseEnrichmentFilter;
import com.ee.lab.filters.resource.ProtectedMetricsResource;
import com.ee.lab.filters.resource.PublicDiagnosticsResource;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class RestApplication extends ResourceConfig {

    public RestApplication() {
        // Register Resources
        register(PublicDiagnosticsResource.class);
        register(ProtectedMetricsResource.class);

        // Register Filters
        register(PreMatchingLoggingFilter.class);
        register(BearerTokenAuthFilter.class);
        register(ResponseEnrichmentFilter.class);

        // Register JSON-B Binding Feature
        register(JsonBindingFeature.class);
    }
}
