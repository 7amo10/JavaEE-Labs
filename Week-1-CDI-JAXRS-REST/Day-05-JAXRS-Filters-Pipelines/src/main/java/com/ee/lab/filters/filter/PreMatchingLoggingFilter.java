package com.ee.lab.filters.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.UUID;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public class PreMatchingLoggingFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        long startNanos = System.nanoTime();
        
        // Extract or generate Correlation ID
        String correlationId = requestContext.getHeaderString("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        // Store correlation metadata in request properties for downstream filters and resources
        requestContext.setProperty("startTimeNanos", startNanos);
        requestContext.setProperty("correlationId", correlationId);

        System.out.println("[PRE-MATCHING FILTER] " + requestContext.getMethod() 
            + " " + requestContext.getUriInfo().getRequestUri().getPath() 
            + " | Correlation-ID: " + correlationId);
    }
}
