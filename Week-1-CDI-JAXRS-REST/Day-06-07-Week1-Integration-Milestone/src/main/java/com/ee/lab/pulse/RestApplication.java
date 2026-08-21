package com.ee.lab.pulse;

import com.ee.lab.pulse.filter.BearerAuthFilter;
import com.ee.lab.pulse.filter.PreMatchingCorrelationFilter;
import com.ee.lab.pulse.filter.ResponseEnrichmentFilter;
import com.ee.lab.pulse.mapper.GenericExceptionMapper;
import com.ee.lab.pulse.mapper.InvalidSnapshotPayloadMapper;
import com.ee.lab.pulse.mapper.SnapshotNotFoundExceptionMapper;
import com.ee.lab.pulse.resource.TelemetryResource;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class RestApplication extends ResourceConfig {

    public RestApplication() {
        register(TelemetryResource.class);
        register(PreMatchingCorrelationFilter.class);
        register(BearerAuthFilter.class);
        register(ResponseEnrichmentFilter.class);
        register(SnapshotNotFoundExceptionMapper.class);
        register(InvalidSnapshotPayloadMapper.class);
        register(GenericExceptionMapper.class);
        register(JsonBindingFeature.class);
        register(JsonProcessingFeature.class);
    }
}
