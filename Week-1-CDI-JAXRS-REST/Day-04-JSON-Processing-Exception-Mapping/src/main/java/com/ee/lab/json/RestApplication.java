package com.ee.lab.json;

import com.ee.lab.json.mapper.DuplicateResourceExceptionMapper;
import com.ee.lab.json.mapper.GenericExceptionMapper;
import com.ee.lab.json.mapper.InvalidPayloadExceptionMapper;
import com.ee.lab.json.mapper.ResourceNotFoundExceptionMapper;
import com.ee.lab.json.resource.AnalysisJobResource;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class RestApplication extends ResourceConfig {

    public RestApplication() {
        // Register REST resources
        register(AnalysisJobResource.class);

        // Register Exception Mappers (@Provider)
        register(ResourceNotFoundExceptionMapper.class);
        register(InvalidPayloadExceptionMapper.class);
        register(DuplicateResourceExceptionMapper.class);
        register(GenericExceptionMapper.class);

        // Register JSON-B and JSON-P media features
        register(JsonBindingFeature.class);
        register(JsonProcessingFeature.class);
    }
}
