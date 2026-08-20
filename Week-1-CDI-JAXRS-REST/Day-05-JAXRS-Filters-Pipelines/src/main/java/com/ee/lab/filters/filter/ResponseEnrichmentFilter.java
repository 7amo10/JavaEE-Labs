package com.ee.lab.filters.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class ResponseEnrichmentFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Long startNanos = (Long) requestContext.getProperty("startTimeNanos");
        String correlationId = (String) requestContext.getProperty("correlationId");

        double durationMs = 0.0;
        if (startNanos != null) {
            durationMs = (System.nanoTime() - startNanos) / 1_000_000.0;
        }

        // Decorate response headers
        if (correlationId != null) {
            responseContext.getHeaders().putSingle("X-Correlation-Id", correlationId);
        }
        responseContext.getHeaders().putSingle("X-Execution-Time-Millis", String.format("%.3f", durationMs));
        responseContext.getHeaders().putSingle("X-Security-Policy", "Strict-Transport-Security; Jakarta-EE-10");

        System.out.println("[RESPONSE FILTER] Outgoing Status: " + responseContext.getStatus() 
            + " | Duration: " + String.format("%.3f ms", durationMs)
            + " | Correlation-ID: " + correlationId + "\n");
    }
}
